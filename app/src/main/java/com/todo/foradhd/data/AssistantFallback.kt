package com.todo.foradhd.data

import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object AssistantFallback {
    private val crisisPattern = Regex(
        """\b(suicid(e|al)|kill myself|end my life|self[- ]?harm|hurt myself|can't keep myself safe|cannot keep myself safe)\b""",
        RegexOption.IGNORE_CASE
    )

    fun autoTagTask(title: String, details: String = ""): String {
        val text = "$title $details".lowercase()
        return when {
            Regex("""\b(study|math|maths|chapter|exam|homework|assignment|revision|revise|lecture|class|college|university|school|research|ai/ml|ml|read|book|formula|notes|quiz|test|thesis|essay|lab)\b""")
                .containsMatchIn(text) -> TaskCategory.STUDY.label
            Regex("""\b(client|clients|sales|sales calls?|client calls?|cold calls?|email|meeting|deck|slides|presentation|pitch|project|deploy|code|bug|report|invoice|deadline|sprint|office|work|proposal|crm|follow[- ]?up)\b""")
                .containsMatchIn(text) -> TaskCategory.WORK.label
            Regex("""\b(water|drink|workout|gym|run|walk|meditate|sleep|pill|medicine|doctor|dentist|stretch|yoga|hydrate|meal|protein|therapy|health)\b""")
                .containsMatchIn(text) -> TaskCategory.HEALTH.label
            Regex("""\b(clean|room|laundry|dishes|grocery|groceries|shop|buy|bank|post office|errand|trash|declutter|pack|repair|pay bill|bills)\b""")
                .containsMatchIn(text) -> TaskCategory.ERRANDS.label
            else -> TaskCategory.PERSONAL.label
        }
    }

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
                "Put the first thing you need for \"$title\" within reach and spend 30 seconds starting."
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

    /**
     * Context-aware intelligent task breakdown generator.
     * Analyzes the objective, complexity, dependencies, and available time to produce
     * concrete, non-uniform subtasks with clear titles and descriptions.
     */
    fun buildTimedPlanSteps(
        task: String,
        totalMinutes: Int,
        strictTimeLimit: Boolean = false
    ): List<PlanStep> {
        val cleanGoal = task.trim().ifEmpty { "Your task" }
        val lower = cleanGoal.lowercase()
        val target = max(5, min(480, totalMinutes))

        val topicHint = extractTopicHint(cleanGoal)

        val blueprint: List<PlanStep> = when {
            // Very short sprint (<= 15 minutes)
            target <= 15 -> listOf(
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    title = "Gather what you need for $topicHint",
                    description = "Open the exact document, tool, or space and remove distractions.",
                    minutes = max(1, (target * 0.2f).roundToInt()),
                    kind = PlanStepKind.WORK
                ),
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    title = "Complete the first concrete part of $topicHint",
                    description = "Focus only on finishing the main immediate action without editing yourself.",
                    minutes = max(2, (target * 0.6f).roundToInt()),
                    kind = PlanStepKind.WORK
                ),
                PlanStep(
                    id = UUID.randomUUID().toString(),
                    title = "Quick check and wrap up",
                    description = "Verify nothing essential is missed and note where to pick up if needed.",
                    minutes = max(1, target - max(1, (target * 0.2f).roundToInt()) - max(2, (target * 0.6f).roundToInt())),
                    kind = PlanStepKind.WORK
                )
            )

            // Math / Chapter / Study / Exam
            Regex("""study|math|maths|chapter|banking|algebra|calculus|physics|chemistry|biology|exam|revision|revise|textbook|science|course""")
                .containsMatchIn(lower) -> {
                listOf(
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Understand the basic concepts of $topicHint",
                        description = "Skim headings, key definitions, and the chapter overview before diving deep.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Read the first core section carefully",
                        description = "Read actively and highlight or jot 3 bullet takeaways in your own words.",
                        minutes = 20,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Study important formulas & rules",
                        description = "List key formulas, conditions, or principles on one clean cheat-sheet.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Short reset & water break",
                        description = "Stand up, stretch, drink water, and rest your eyes before problem solving.",
                        minutes = 5,
                        kind = PlanStepKind.BREAK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Solve the first set of worked examples",
                        description = "Cover the solution and try each example yourself before checking steps.",
                        minutes = 20,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Complete practice exercise questions",
                        description = "Work through 4–6 representative questions independently to test recall.",
                        minutes = 30,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Review mistakes and do final recap",
                        description = "Mark anything tricky and summarize the 3 main ideas you want to remember.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    )
                )
            }

            // Assignment / Essay / Writing / Report
            Regex("""assignment|essay|report|paper|write|writing|draft|article|blog|homework""")
                .containsMatchIn(lower) -> {
                listOf(
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Clarify the prompt & list requirements",
                        description = "Check the rubric, word count, or questions so you know exact expectations.",
                        minutes = 10,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Create a rough 4-point outline",
                        description = "Jot down intro, main sections, and conclusion bullets without worrying about wording.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Draft the core body section first",
                        description = "Write ugly first-draft sentences for the main points; skip perfection.",
                        minutes = 30,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Quick movement break",
                        description = "Step away for a brief stretch so you return with fresh eyes.",
                        minutes = 5,
                        kind = PlanStepKind.BREAK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Write opening & closing paragraphs",
                        description = "Frame what you wrote with a clear intro and concise takeaway.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Proofread, format & submit check",
                        description = "Fix typos, verify citations or formatting, and save/export.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    )
                )
            }

            // Presentation / Slides / Pitch
            Regex("""presentation|slides|slide deck|powerpoint|keynote|pitch|talk|demo""")
                .containsMatchIn(lower) -> {
                listOf(
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Define audience & 3 key messages",
                        description = "Write down the single outcome you want listeners to remember.",
                        minutes = 10,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Sketch slide-by-slide story flow",
                        description = "Create empty slide titles for opening, problem, proof, and closing.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Fill core slides with concise bullets",
                        description = "Keep each slide to one main point and short scannable text.",
                        minutes = 25,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Short reset break",
                        description = "Breathe, hydrate, and reset before polishing visuals.",
                        minutes = 5,
                        kind = PlanStepKind.BREAK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Add visuals, charts & clean layout",
                        description = "Align typography, remove clutter, and verify readability.",
                        minutes = 20,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Run one timed dry rehearsal",
                        description = "Speak through the slides out loud once to spot awkward transitions.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    )
                )
            }

            // Cleaning / Organizing / Room / Home
            Regex("""clean|room|desk|kitchen|closet|tidy|declutter|organize|laundry|house|apartment""")
                .containsMatchIn(lower) -> {
                listOf(
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Grab a trash bag & laundry basket",
                        description = "Set up two containers at the door so you don't wander room to room.",
                        minutes = 3,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Clear visible trash & dishes",
                        description = "Do one fast sweep collecting only wrappers, cups, and obvious clutter.",
                        minutes = 10,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Gather clothes & loose fabrics",
                        description = "Toss dirty clothes in the hamper and fold or hang clean items.",
                        minutes = 12,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Clear flat surfaces (desk, bed, table)",
                        description = "Put items back in their home zone one surface at a time.",
                        minutes = 20,
                        kind = PlanStepKind.WORK
                    ),
                    PlanStep(
                        id = UUID.randomUUID().toString(),
                        title = "Wipe surfaces & sweep/vacuum floor",
                        description = "Finish with a quick wipe-down and floor pass for an instant fresh feel.",
                        minutes = 15,
                        kind = PlanStepKind.WORK
                    )
                )
            }

            // Work project / Coding / Sales / General complex task
            else -> {
                val isShortOrUnclear = cleanGoal.split(Regex("\\s+")).size <= 2
                if (isShortOrUnclear) {
                    listOf(
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Define the exact finish line for \"$cleanGoal\"",
                            description = "Write 1 sentence describing what 'done for today' looks like.",
                            minutes = 5,
                            kind = PlanStepKind.WORK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Prepare workspace & open required tools",
                            description = "Close unrelated tabs and place the exact files or items in front of you.",
                            minutes = 5,
                            kind = PlanStepKind.WORK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "First focused sprint on $topicHint",
                            description = "Complete the highest-priority piece without switching tasks.",
                            minutes = 20,
                            kind = PlanStepKind.WORK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Short reset break",
                            description = "Stretch and step back for a quick breather.",
                            minutes = 5,
                            kind = PlanStepKind.BREAK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Second sprint & wrap-up check",
                            description = "Finish remaining core details and save your next stopping point.",
                            minutes = 20,
                            kind = PlanStepKind.WORK
                        )
                    )
                } else {
                    listOf(
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Scope & set up $topicHint",
                            description = "List the 2–3 concrete deliverables and open the tools you need.",
                            minutes = 10,
                            kind = PlanStepKind.WORK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Build the foundational first milestone",
                            description = "Tackle the hardest or most foundational piece while focus is freshest.",
                            minutes = 25,
                            kind = PlanStepKind.WORK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Short reset break",
                            description = "Hydrate and rest your mind for 5 minutes.",
                            minutes = 5,
                            kind = PlanStepKind.BREAK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Execute the main working block",
                            description = "Push through the core remaining actions steadily.",
                            minutes = 30,
                            kind = PlanStepKind.WORK
                        ),
                        PlanStep(
                            id = UUID.randomUUID().toString(),
                            title = "Review, verify & lock in progress",
                            description = "Test/check quality, fix rough edges, and note any follow-up.",
                            minutes = 15,
                            kind = PlanStepKind.WORK
                        )
                    )
                }
            }
        }

        return scaleBlueprintToTarget(blueprint, target, strictTimeLimit)
    }

    fun rescaleBreakdown(existing: List<PlanStep>, newTotalMinutes: Int): List<PlanStep> {
        if (existing.isEmpty()) return existing
        val completedMinutes = existing.filter { it.completed }.sumOf { it.minutes }
        val incomplete = existing.filter { !it.completed }
        if (incomplete.isEmpty()) return existing
        val remainingBudget = max(incomplete.size, newTotalMinutes - completedMinutes)
        val scaledIncomplete = scaleBlueprintToTarget(incomplete, remainingBudget, strictTimeLimit = true)
        var incIdx = 0
        return existing.map { step ->
            if (step.completed) step else scaledIncomplete[incIdx++]
        }
    }

    private fun scaleBlueprintToTarget(
        blueprint: List<PlanStep>,
        targetMinutes: Int,
        strictTimeLimit: Boolean
    ): List<PlanStep> {
        val baseTotal = blueprint.sumOf { it.minutes }.coerceAtLeast(1)
        if (targetMinutes == baseTotal) return blueprint

        // When target is much shorter, trim optional break or late steps cleanly rather than making every step 1 min
        val workingList = if (targetMinutes < 35) {
            blueprint.filter { it.kind == PlanStepKind.WORK }.take(4)
        } else if (targetMinutes < 60 && blueprint.size > 5) {
            blueprint.filterIndexed { idx, step -> !(step.kind == PlanStepKind.BREAK && idx > 0) }.take(5)
        } else {
            blueprint
        }.ifEmpty { blueprint }

        val workSum = workingList.sumOf { it.minutes }.coerceAtLeast(1)
        val ratio = targetMinutes.toDouble() / workSum.toDouble()
        val scaled = workingList.map { step ->
            val newMins = if (step.kind == PlanStepKind.BREAK) {
                if (strictTimeLimit && targetMinutes < 45) 3 else 5
            } else {
                max(2, (step.minutes * ratio).roundToInt())
            }
            step.copy(minutes = newMins)
        }.toMutableList()

        // Adjust the longest WORK step so sum matches targetMinutes closely
        val diff = targetMinutes - scaled.sumOf { it.minutes }
        if (diff != 0) {
            val longestWorkIdx = scaled.indices
                .filter { scaled[it].kind == PlanStepKind.WORK }
                .maxByOrNull { scaled[it].minutes } ?: 0
            scaled[longestWorkIdx] = scaled[longestWorkIdx].copy(
                minutes = max(2, scaled[longestWorkIdx].minutes + diff)
            )
        }
        return scaled
    }

    private fun extractTopicHint(goal: String): String {
        val trimmed = goal.trim()
        return if (trimmed.length > 36) trimmed.take(36).trimEnd() + "…" else trimmed
    }
}
