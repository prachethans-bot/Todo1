package com.todo.foradhd

import com.todo.foradhd.data.AssistantFallback
import com.todo.foradhd.data.PlanStepKind
import com.todo.foradhd.data.TaskCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VinaiDomainLogicTest {

    @Test
    fun breakdown_mathBankingChapter_120Minutes_isNonUniformAndRealistic() {
        val steps = AssistantFallback.buildTimedPlanSteps(
            task = "Complete a mathematics chapter on Banking",
            totalMinutes = 120,
            strictTimeLimit = false
        )

        assertTrue(steps.size >= 5)
        assertEquals(120, steps.sumOf { it.minutes })
        assertTrue(steps.any { it.kind == PlanStepKind.BREAK })
        assertTrue(steps.map { it.minutes }.distinct().size >= 3)
        assertTrue(steps.all { it.title.isNotBlank() && it.description.isNotBlank() })
    }

    @Test
    fun breakdown_short10MinuteTask_fitsBudgetWithoutBreakBloat() {
        val steps = AssistantFallback.buildTimedPlanSteps(
            task = "Reply to 3 client emails",
            totalMinutes = 10,
            strictTimeLimit = true
        )

        assertEquals(3, steps.size)
        assertEquals(10, steps.sumOf { it.minutes })
        assertTrue(steps.none { it.kind == PlanStepKind.BREAK })
    }

    @Test
    fun breakdown_unclearShortGoal_addsClarifyingFirstStep() {
        val steps = AssistantFallback.buildTimedPlanSteps(
            task = "Project",
            totalMinutes = 55,
            strictTimeLimit = false
        )

        assertTrue(steps.isNotEmpty())
        assertEquals(55, steps.sumOf { it.minutes })
        assertTrue(steps.first().title.contains("Define", ignoreCase = true))
    }

    @Test
    fun rescaleBreakdown_preservesCompletedStepsAndAdjustsRemaining() {
        val initial = AssistantFallback.buildTimedPlanSteps(
            task = "Clean an entire room",
            totalMinutes = 60,
            strictTimeLimit = false
        )
        val withFirstDone = initial.mapIndexed { idx, step ->
            if (idx == 0) step.copy(completed = true) else step
        }
        val rescaled = AssistantFallback.rescaleBreakdown(withFirstDone, newTotalMinutes = 45)

        assertTrue(rescaled.first().completed)
        assertEquals(initial.first().minutes, rescaled.first().minutes)
        assertEquals(45, rescaled.sumOf { it.minutes })
    }

    @Test
    fun autoTagTask_classifiesAcrossCategories() {
        assertEquals(TaskCategory.STUDY.label, AssistantFallback.autoTagTask("Study AI/ML research chapter"))
        assertEquals(TaskCategory.WORK.label, AssistantFallback.autoTagTask("Make 200 sales calls for clients"))
        assertEquals(TaskCategory.HEALTH.label, AssistantFallback.autoTagTask("Drink 3-4L of water"))
        assertEquals(TaskCategory.ERRANDS.label, AssistantFallback.autoTagTask("Clean room and laundry"))
        assertEquals(TaskCategory.PERSONAL.label, AssistantFallback.autoTagTask("Call Mom tonight"))
    }

    @Test
    fun verifyLocally_rejectsGenericAndAcceptsRelevantDetails() {
        val generic = AssistantFallback.verifyLocally("Study Banking chapter", "Skim headings", "done")
        assertFalse(generic.approved)

        val relevant = AssistantFallback.verifyLocally(
            "Study Banking chapter",
            "Skim headings",
            "Opened the banking chapter and solved 3 recurring deposit formulas"
        )
        assertTrue(relevant.approved)
    }
}
