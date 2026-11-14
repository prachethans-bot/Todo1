package com.todo.foradhd.data.remote

import com.todo.foradhd.BuildConfig
import com.todo.foradhd.data.AssistantResponse
import com.todo.foradhd.data.BuddyActivity
import com.todo.foradhd.data.BuddyState
import com.todo.foradhd.data.BuddyViewData
import com.todo.foradhd.data.LocalBuddy
import com.todo.foradhd.data.SavedTask
import com.todo.foradhd.data.TaskMode
import com.todo.foradhd.data.UserSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.SecureRandom
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class SupabaseApi {
    private fun cleanSecret(raw: String): String {
        val trimmed = raw.trim().trim('"')
        return if (trimmed.equals("UNSET", ignoreCase = true) || trimmed.startsWith("YOUR_")) "" else trimmed
    }

    private val baseUrl: String = cleanSecret(BuildConfig.VITE_SUPABASE_URL).trimEnd('/')
    private val anonKey: String = cleanSecret(BuildConfig.VITE_SUPABASE_ANON_KEY)
    private val publicUrl: String = cleanSecret(BuildConfig.VITE_PUBLIC_URL).trimEnd('/')

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    val isConfigured: Boolean
        get() = baseUrl.isNotEmpty() && anonKey.isNotEmpty()

    sealed class AuthResult {
        data class Success(val session: UserSession?, val message: String) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    sealed class VerifyResult {
        data class Done(val approved: Boolean, val reason: String) : VerifyResult()
        data class Failure(val message: String) : VerifyResult()
    }

    data class BuddyPollResult(
        val state: BuddyState,
        val lastId: Long,
        val emojis: List<String>
    )

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext AuthResult.Error("Cloud account setup is not configured yet.")
        val body = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }.toString()
        val resp = request("POST", "$baseUrl/auth/v1/token?grant_type=password", body = body)
        if (!resp.ok) {
            return@withContext AuthResult.Error(extractError(resp.body) ?: "Sign in failed.")
        }
        val obj = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
        val token = obj?.get("access_token")?.jsonPrimitive?.contentOrNull
        val refresh = obj?.get("refresh_token")?.jsonPrimitive?.contentOrNull.orEmpty()
        val userObj = obj?.get("user")?.jsonObject
        val userId = userObj?.get("id")?.jsonPrimitive?.contentOrNull
        val userEmail = userObj?.get("email")?.jsonPrimitive?.contentOrNull ?: email.trim()
        if (token.isNullOrBlank() || userId.isNullOrBlank()) {
            AuthResult.Error("Unexpected authentication response.")
        } else {
            AuthResult.Success(UserSession(token, refresh, userId, userEmail), "Signed in successfully.")
        }
    }

    suspend fun signUp(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext AuthResult.Error("Cloud account setup is not configured yet.")
        val body = buildJsonObject {
            put("email", email.trim())
            put("password", password)
        }.toString()
        val resp = request("POST", "$baseUrl/auth/v1/signup", body = body)
        if (!resp.ok) {
            return@withContext AuthResult.Error(extractError(resp.body) ?: "Could not create account.")
        }
        val obj = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
        val token = obj?.get("access_token")?.jsonPrimitive?.contentOrNull
        val refresh = obj?.get("refresh_token")?.jsonPrimitive?.contentOrNull.orEmpty()
        val userObj = obj?.get("user")?.jsonObject
        val userId = userObj?.get("id")?.jsonPrimitive?.contentOrNull
        val userEmail = userObj?.get("email")?.jsonPrimitive?.contentOrNull ?: email.trim()
        if (!token.isNullOrBlank() && !userId.isNullOrBlank()) {
            AuthResult.Success(UserSession(token, refresh, userId, userEmail), "Signed in successfully.")
        } else {
            AuthResult.Success(null, "Check your email for the confirmation link, then sign in.")
        }
    }

    suspend fun signOut(session: UserSession?) = withContext(Dispatchers.IO) {
        if (!isConfigured || session == null) return@withContext
        request("POST", "$baseUrl/auth/v1/logout", bearer = session.accessToken, body = "{}")
    }

    suspend fun fetchTasks(session: UserSession): List<SavedTask>? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val resp = request(
            "GET",
            "$baseUrl/rest/v1/tasks?select=*&order=created_at.desc&limit=100",
            bearer = session.accessToken
        )
        if (!resp.ok) return@withContext null
        val arr = runCatching { json.parseToJsonElement(resp.body).jsonArray }.getOrNull() ?: return@withContext null
        arr.mapNotNull { elem -> runCatching { taskFromJson(elem.jsonObject) }.getOrNull() }
    }

    suspend fun upsertTask(task: SavedTask, session: UserSession): String? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val payload = buildJsonObject {
            put("id", task.id)
            put("user_id", session.userId)
            put("title", task.title)
            put("mode", task.mode.name)
            put("micro_step", task.microStep)
            put("created_at", task.createdAt)
            put("completed", task.completed)
            if (task.reminderAt != null) put("reminder_at", task.reminderAt) else put("reminder_at", JsonNull)
            if (task.estimateMinutes != null) put("estimate_minutes", task.estimateMinutes) else put("estimate_minutes", JsonNull)
            if (task.actualMinutes != null) put("actual_minutes", task.actualMinutes) else put("actual_minutes", JsonNull)
            put("progress_note", task.progressNote)
        }.toString()
        val resp = request(
            method = "POST",
            urlString = "$baseUrl/rest/v1/tasks",
            bearer = session.accessToken,
            body = payload,
            extraHeaders = mapOf("Prefer" to "resolution=merge-duplicates")
        )
        if (resp.ok) null else extractError(resp.body) ?: "Sync failed (${resp.code})"
    }

    suspend fun deleteTask(taskId: String, session: UserSession): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        val encoded = URLEncoder.encode(taskId, "UTF-8")
        val resp = request(
            method = "DELETE",
            urlString = "$baseUrl/rest/v1/tasks?id=eq.$encoded",
            bearer = session.accessToken
        )
        resp.ok
    }

    suspend fun invokeStepwiseAssistant(task: String, mode: TaskMode, session: UserSession): AssistantResponse? =
        withContext(Dispatchers.IO) {
            if (!isConfigured) return@withContext null
            val body = buildJsonObject {
                put("task", task)
                put("mode", mode.name)
            }.toString()
            val resp = request(
                method = "POST",
                urlString = "$baseUrl/functions/v1/stepwise-assistant",
                bearer = session.accessToken,
                body = body
            )
            if (!resp.ok) return@withContext null
            runCatching { json.decodeFromString<AssistantResponse>(resp.body) }.getOrNull()
        }

    suspend fun invokeVerifyTaskStart(
        taskTitle: String,
        microStep: String,
        userResponse: String,
        session: UserSession
    ): VerifyResult = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext VerifyResult.Failure("Cloud verification is not configured.")
        val body = buildJsonObject {
            put("taskTitle", taskTitle)
            put("microStep", microStep)
            put("userResponse", userResponse)
        }.toString()
        val resp = request(
            method = "POST",
            urlString = "$baseUrl/functions/v1/verify-task-start",
            bearer = session.accessToken,
            body = body
        )
        if (!resp.ok) {
            return@withContext VerifyResult.Failure(extractError(resp.body) ?: "AI checking is unavailable right now.")
        }
        val obj = runCatching { json.parseToJsonElement(resp.body).jsonObject }.getOrNull()
            ?: return@withContext VerifyResult.Failure("The AI checker returned an unexpected response.")
        val approved = obj["approved"]?.jsonPrimitive?.booleanOrNull
            ?: return@withContext VerifyResult.Failure("The AI checker returned an unexpected response.")
        val reason = obj["reason"]?.jsonPrimitive?.contentOrNull.orEmpty()
        VerifyResult.Done(approved, reason)
    }

    fun buildInviteLink(token: String): String {
        return if (publicUrl.isNotEmpty()) "$publicUrl/?buddy=$token"
        else "com.todo.foradhd://?buddy=$token"
    }

    suspend fun createBuddyInvite(ownerName: String): LocalBuddy? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val token = randomHex(18)
        val ownerKey = randomHex(18)
        val body = buildJsonObject {
            put("p_token", token)
            put("p_owner_key", ownerKey)
            put("p_name", ownerName.take(40))
        }.toString()
        val resp = request("POST", "$baseUrl/rest/v1/rpc/buddy_create", body = body)
        if (!resp.ok) return@withContext null
        LocalBuddy(
            token = token,
            ownerKey = ownerKey,
            ownerName = ownerName.take(40),
            state = BuddyState.PENDING,
            lastCheerId = 0L
        )
    }

    suspend fun fetchBuddyView(token: String): BuddyViewData? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val body = buildJsonObject { put("p_token", token) }.toString()
        val resp = request("POST", "$baseUrl/rest/v1/rpc/buddy_view", body = body)
        if (!resp.ok) return@withContext null
        val arr = runCatching { json.parseToJsonElement(resp.body).jsonArray }.getOrNull()
        val row = arr?.firstOrNull()?.jsonObject ?: return@withContext null
        BuddyViewData(
            ownerName = row["owner_name"]?.jsonPrimitive?.contentOrNull ?: "Your friend",
            state = parseBuddyState(row["state"]?.jsonPrimitive?.contentOrNull),
            taskLabel = row["task_label"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            activity = parseBuddyActivity(row["activity"]?.jsonPrimitive?.contentOrNull),
            sessionId = row["session_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            cheersUsed = row["cheers_used"]?.jsonPrimitive?.intOrNull ?: 0
        )
    }

    suspend fun acceptBuddyInvite(token: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        val body = buildJsonObject { put("p_token", token) }.toString()
        val resp = request("POST", "$baseUrl/rest/v1/rpc/buddy_accept", body = body)
        resp.ok && resp.body.trim() == "true"
    }

    suspend fun sendCheer(token: String, emoji: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        val body = buildJsonObject {
            put("p_token", token)
            put("p_emoji", emoji)
        }.toString()
        val resp = request("POST", "$baseUrl/rest/v1/rpc/buddy_cheer", body = body)
        resp.ok && resp.body.trim() == "true"
    }

    suspend fun pushBuddyStatus(
        buddy: LocalBuddy,
        label: String,
        activity: BuddyActivity,
        sessionId: String
    ) = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext
        val body = buildJsonObject {
            put("p_token", buddy.token)
            put("p_owner_key", buddy.ownerKey)
            put("p_label", label)
            put("p_activity", activity.raw)
            put("p_session", sessionId)
        }.toString()
        request("POST", "$baseUrl/rest/v1/rpc/buddy_push", body = body)
    }

    suspend fun setBuddyState(buddy: LocalBuddy, state: BuddyState): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false
        val body = buildJsonObject {
            put("p_token", buddy.token)
            put("p_owner_key", buddy.ownerKey)
            put("p_state", state.raw)
        }.toString()
        val resp = request("POST", "$baseUrl/rest/v1/rpc/buddy_set_state", body = body)
        resp.ok && resp.body.trim() == "true"
    }

    suspend fun pollOwner(buddy: LocalBuddy): BuddyPollResult? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val body = buildJsonObject {
            put("p_token", buddy.token)
            put("p_owner_key", buddy.ownerKey)
            put("p_after", buddy.lastCheerId)
        }.toString()
        val resp = request("POST", "$baseUrl/rest/v1/rpc/buddy_owner_poll", body = body)
        if (!resp.ok) return@withContext null
        val arr = runCatching { json.parseToJsonElement(resp.body).jsonArray }.getOrNull()
        val row = arr?.firstOrNull()?.jsonObject ?: return@withContext null
        val emojis = (row["emojis"] as? JsonArray)?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
        BuddyPollResult(
            state = parseBuddyState(row["state"]?.jsonPrimitive?.contentOrNull),
            lastId = row["last_id"]?.jsonPrimitive?.longOrNull ?: 0L,
            emojis = emojis
        )
    }

    private fun taskFromJson(row: JsonObject): SavedTask {
        val reminderAt = row["reminder_at"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull
        val reminderTime = reminderAt?.let { iso ->
            runCatching {
                val instant = Instant.parse(iso)
                val local = instant.atZone(ZoneId.systemDefault()).toLocalTime()
                DateTimeFormatter.ofPattern("HH:mm").format(local)
            }.getOrNull()
        }
        return SavedTask(
            id = row["id"]!!.jsonPrimitive.content,
            title = row["title"]!!.jsonPrimitive.content,
            mode = runCatching { TaskMode.valueOf(row["mode"]!!.jsonPrimitive.content) }.getOrDefault(TaskMode.NEW_TASK),
            microStep = row["micro_step"]!!.jsonPrimitive.content,
            createdAt = row["created_at"]!!.jsonPrimitive.content,
            completed = row["completed"]?.jsonPrimitive?.booleanOrNull ?: false,
            reminderTime = reminderTime,
            reminderAt = reminderAt,
            estimateMinutes = row["estimate_minutes"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull,
            actualMinutes = row["actual_minutes"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.intOrNull,
            progressNote = row["progress_note"]?.takeIf { it !is JsonNull }?.jsonPrimitive?.contentOrNull.orEmpty()
        )
    }

    private fun parseBuddyState(raw: String?): BuddyState =
        BuddyState.entries.firstOrNull { it.raw == raw } ?: BuddyState.PENDING

    private fun parseBuddyActivity(raw: String?): BuddyActivity =
        BuddyActivity.entries.firstOrNull { it.raw == raw } ?: BuddyActivity.IDLE

    private fun randomHex(bytes: Int): String {
        val arr = ByteArray(bytes)
        SecureRandom().nextBytes(arr)
        return arr.joinToString("") { "%02x".format(it) }
    }

    private fun extractError(body: String): String? {
        val obj = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        return obj["error_description"]?.jsonPrimitive?.contentOrNull
            ?: obj["msg"]?.jsonPrimitive?.contentOrNull
            ?: obj["message"]?.jsonPrimitive?.contentOrNull
            ?: obj["error"]?.let { elem ->
                if (elem is JsonPrimitive) elem.contentOrNull
                else elem.jsonObject["message"]?.jsonPrimitive?.contentOrNull
            }
    }

    private data class HttpResponse(val code: Int, val body: String) {
        val ok: Boolean get() = code in 200..299
    }

    private fun request(
        method: String,
        urlString: String,
        bearer: String? = null,
        body: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): HttpResponse {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 12000
                readTimeout = 15000
                setRequestProperty("apikey", anonKey)
                setRequestProperty("Authorization", "Bearer ${bearer ?: anonKey}")
                setRequestProperty("Content-Type", "application/json")
                extraHeaders.forEach { (k, v) -> setRequestProperty(k, v) }
                if (body != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
                    doOutput = true
                    outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
            HttpResponse(code, text)
        } catch (e: Exception) {
            HttpResponse(0, """{"error":"${e.message?.replace("\"", "'") ?: "Network error"}"}""")
        }
    }
}
