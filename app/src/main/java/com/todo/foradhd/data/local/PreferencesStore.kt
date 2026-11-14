package com.todo.foradhd.data.local

import android.content.Context
import android.content.SharedPreferences
import com.todo.foradhd.data.LocalBuddy
import com.todo.foradhd.data.QuickNote
import com.todo.foradhd.data.RewardSettings
import com.todo.foradhd.data.SavedBreakdownState
import com.todo.foradhd.data.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PreferencesStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("vinai_prefs", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    var points: Int
        get() = prefs.getInt("points", 0)
        set(value) = prefs.edit().putInt("points", value).apply()

    var streak: Int
        get() = prefs.getInt("streak", 0)
        set(value) = prefs.edit().putInt("streak", value).apply()

    var lastDay: String
        get() = prefs.getString("lastDay", "") ?: ""
        set(value) = prefs.edit().putString("lastDay", value).apply()

    var isDarkTheme: Boolean
        get() = prefs.getBoolean("isDarkTheme", true)
        set(value) = prefs.edit().putBoolean("isDarkTheme", value).apply()

    var checkInFrequency: Int
        get() {
            val v = prefs.getInt("checkInFrequency", 10)
            return if (v in listOf(5, 10, 15)) v else 10
        }
        set(value) = prefs.edit().putInt("checkInFrequency", value).apply()

    var buddyOwnerName: String
        get() = prefs.getString("buddyOwnerName", "") ?: ""
        set(value) = prefs.edit().putString("buddyOwnerName", value).apply()

    var exactAlarmAsked: Boolean
        get() = prefs.getBoolean("exactAlarmAsked", false)
        set(value) = prefs.edit().putBoolean("exactAlarmAsked", value).apply()

    fun getRewardSettings(): RewardSettings {
        val raw = prefs.getString("rewardSettings", null) ?: return RewardSettings()
        return runCatching { json.decodeFromString<RewardSettings>(raw) }.getOrDefault(RewardSettings())
    }

    fun saveRewardSettings(settings: RewardSettings) {
        prefs.edit().putString("rewardSettings", json.encodeToString(settings)).apply()
    }

    fun getQualifiedFocusTasks(): Set<String> =
        prefs.getStringSet("qualifiedFocusTasks", emptySet()) ?: emptySet()

    fun saveQualifiedFocusTasks(ids: Set<String>) {
        prefs.edit().putStringSet("qualifiedFocusTasks", ids.toSet()).apply()
    }

    fun getFocusedTasks(): Set<String> =
        prefs.getStringSet("focusedTasks", emptySet()) ?: emptySet()

    fun saveFocusedTasks(ids: Set<String>) {
        prefs.edit().putStringSet("focusedTasks", ids.toSet()).apply()
    }

    fun getSharedTasks(): Set<String> =
        prefs.getStringSet("sharedTasks", emptySet()) ?: emptySet()

    fun getLocalBuddy(): LocalBuddy? {
        val raw = prefs.getString("buddy", null) ?: return null
        return runCatching { json.decodeFromString<LocalBuddy>(raw) }.getOrNull()
    }

    fun saveLocalBuddy(buddy: LocalBuddy?) {
        if (buddy == null) prefs.edit().remove("buddy").apply()
        else prefs.edit().putString("buddy", json.encodeToString(buddy)).apply()
    }

    fun isBuddyAccepted(token: String): Boolean =
        prefs.getBoolean("buddyAccepted_$token", false)

    fun setBuddyAccepted(token: String, accepted: Boolean) {
        prefs.edit().putBoolean("buddyAccepted_$token", accepted).apply()
    }

    fun getSession(): UserSession? {
        val raw = prefs.getString("session", null) ?: return null
        return runCatching { json.decodeFromString<UserSession>(raw) }.getOrNull()
    }

    fun saveSession(session: UserSession?) {
        if (session == null) prefs.edit().remove("session").apply()
        else prefs.edit().putString("session", json.encodeToString(session)).apply()
    }

    fun getQuickNotes(): List<QuickNote> {
        val raw = prefs.getString("quickNotes", null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<QuickNote>>(raw) }.getOrDefault(emptyList())
    }

    fun saveQuickNotes(notes: List<QuickNote>) {
        prefs.edit().putString("quickNotes", json.encodeToString(notes)).apply()
    }

    fun getBreakdownState(): SavedBreakdownState? {
        val raw = prefs.getString("breakdownState", null) ?: return null
        return runCatching { json.decodeFromString<SavedBreakdownState>(raw) }.getOrNull()
    }

    fun saveBreakdownState(state: SavedBreakdownState?) {
        if (state == null) prefs.edit().remove("breakdownState").apply()
        else prefs.edit().putString("breakdownState", json.encodeToString(state)).apply()
    }
}
