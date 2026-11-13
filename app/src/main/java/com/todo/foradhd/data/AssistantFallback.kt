package com.todo.foradhd.data

import java.util.UUID
import kotlin.math.max
import kotlin.math.min

object AssistantFallback {
    private val crisisPattern = Regex(
        """\b(suicid(e|al)|kill myself|end my life|self[- ]?harm|hurt myself|can't keep myself safe|cannot keep myself safe)\b""",
        RegexOption.IGNORE_CASE
    )

    fun getDemoResponse(task: String, mode: TaskMode): AssistantResponse {
        val title = task.trim().ifEmpty { "Get started" }
        if (crisisPattern.containsMatchIn(title)) {
            return AssistantResponse(
                mode = mode,
                optimizedTaskTitle = "Get support right now",
                theMicroStep = "Please pause the task for now and reach out to a trusted adult or someone you trust who can stay with you. If you may be in immediate danger, contact your local emergency service now.",
                dopamineHook = "You deserve support and do not have to handle this alone.",
                alarmSettings = AlarmSettings(enabled = false, at = null, label = "Reminders paused"),
                timeGuessPrompt = "Can you contact someone you trust right now?",
                timeFeedback = "There is no task or time goal to meet right now. Your safety matters most."
            )
        }
        val step = when (mode) {
            TaskMode.TOO_HARD ->
                "Make the task smaller again: open the thing you need and do just one visible action. Stopping after that is allowed."
            TaskMode.NEXT_STEP ->
                "Look at what is already done. Pick the next visible action and work on it for 30 seconds."
            TaskMode.TIME_CHECK ->
                "Pause for a moment. Notice what you have done, then choose one small next action. There is no penalty for changing the plan."
            TaskMode.NEW_TASK ->
                "Open what you need for “$title”. Put it in front of you and spend 30 seconds making a start."
        }
        return AssistantResponse(
            mode = mode,
            optimizedTaskTitle = title,
            theMicroStep = step,
            dopamineHook = "Starting is the win. You only need to do this one step.",
            alarmSettings = AlarmSettings(enabled = false, at = null, label = "Optional gentle reminder"),
            timeGuessPrompt = "How long do you think this tiny step will take?",
            timeFeedback = "If it takes longer, that is useful information—not a score. No judgement."
        )
    }

    data class VerificationResult(
        val approved: Boolean,
        val reason: String
    )

    /**
     * Local relevance check used only when cloud Supabase verification is not configured.
     * Mirrors verify-task-start rules: reject empty/generic/gibberish updates, accept concrete progress or next action.
     */
    fun verifyLocally(taskTitle: String, microStep: String, userResponse: String): VerificationResult {
        val update = userResponse.trim()
        if (update.length < 4) {
            return VerificationResult(
                approved = false,
                reason = "Add a little more detail about what you did or plan to do next for \"$taskTitle\"."
            )
        }
        val genericOnly = setOf("done", "ok", "okay", "yes", "finished", "complete", "completed", "did it", "asdf", "test")
        if (update.lowercase() in genericOnly) {
            return VerificationResult(
                approved = false,
                reason = "Tell us one specific detail connected to \"$taskTitle\" (what you opened, wrote, or did) so we can log your progress."
            )
        }
        val words = update.split(Regex("\\s+")).filter { it.length >= 2 }
        if (words.size < 2) {
            return VerificationResult(
                approved = false,
                reason = "Write a short sentence describing what you did or your next step for \"$taskTitle\"."
            )
        }
        val contextTokens = (taskTitle + " " + microStep)
            .lowercase()
            .split(Regex("[^a-z0-9]+"))
            .filter { it.length >= 3 }
            .toSet()
        val updateTokens = update.lowercase().split(Regex("[^a-z0-9]+")).filter { it.length >= 3 }.toSet()
        val sharesContext = contextTokens.any { it in updateTokens }
        val actionVerbs = setOf(
            "opened", "open", "started", "start", "wrote", "write", "read", "finished", "sent",
            "called", "cleaned", "built", "fixed", "reviewed", "checked", "made", "drafted",
            "worked", "prepared", "organized", "set", "sorted", "packed", "practiced", "studied",
            "going", "will", "next", "completed", "moved", "step"
        )
        val hasAction = updateTokens.any { it in actionVerbs }
        return if (sharesContext || (hasAction && words.size >= 3)) {
            VerificationResult(approved = true, reason = "Great work logging a real step on \"$taskTitle\".")
        } else {
            VerificationResult(
                approved = false,
                reason = "That update doesn't clearly connect to \"$taskTitle\" yet. Mention what part of the task you worked on or will do next."
            )
        }
    }

    fun buildTimedPlanSteps(task: String, totalMinutes: Int): List<PlanStep> {
        val lower = task.lowercase()
        val isStudy = Regex("""study|math|chapter|exam|revision|revise|learn|read|homework|science|physics|chemistry|biology""")
            .containsMatchIn(lower)
        val template = if (isStudy) {
            listOf(
                PlanStep(UUID.randomUUID().toString(), "Set up your book, notes and workspace", 3, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Read the first section and mark key ideas", 20, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Take a short reset break", 5, PlanStepKind.BREAK),
                PlanStep(UUID.randomUUID().toString(), "Continue the chapter and make brief notes", 25, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Solve practice questions", 25, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Take a short reset break", 5, PlanStepKind.BREAK),
                PlanStep(UUID.randomUUID().toString(), "Review mistakes and recall the main points", 15, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Wrap up and choose what to revisit", 5, PlanStepKind.WORK)
            )
        } else {
            val label = task.trim().ifEmpty { "your task" }
            listOf(
                PlanStep(UUID.randomUUID().toString(), "Set up what you need", 3, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Start: $label", 20, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Take a short reset break", 5, PlanStepKind.BREAK),
                PlanStep(UUID.randomUUID().toString(), "Continue the main task", 25, PlanStepKind.WORK),
                PlanStep(UUID.randomUUID().toString(), "Check progress and finish one clear part", 15, PlanStepKind.WORK)
            )
        }

        val target = max(15, min(480, totalMinutes))
        var remaining = target
        val result = mutableListOf<PlanStep>()
        for (step in template) {
            if (remaining <= 0) break
            val mins = min(step.minutes, remaining)
            if (mins > 0) {
                result.add(step.copy(minutes = mins))
                remaining -= mins
            }
        }
        if (remaining > 0) {
            result.add(PlanStep(UUID.randomUUID().toString(), "Keep working on the next section", remaining, PlanStepKind.WORK))
        }
        return result
    }
}
