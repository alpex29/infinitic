/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows.data.steps

import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import kotlinx.serialization.Serializable

@Serializable
sealed class StepStatus

@Serializable
object StepStatusOngoing : StepStatus() {
    // as we can not define a data class without parameter, we add manually the equals func
    override fun equals(other: Any?) = javaClass == other?.javaClass
}

@Serializable
data class StepStatusCompleted(
    val completionResult: StepOutput,
    val completionWorkflowTaskIndex: WorkflowTaskIndex
) : StepStatus()

@Serializable
data class StepStatusCanceled(
    val cancellationResult: StepOutput,
    val cancellationWorkflowTaskIndex: WorkflowTaskIndex
) : StepStatus()
