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

package io.infinitic.inMemory

import io.infinitic.client.InfiniticClient
import io.infinitic.inMemory.tasks.TaskA
import io.infinitic.inMemory.tasks.TaskAImpl
import io.infinitic.inMemory.transport.InMemoryClientOutput
import io.infinitic.inMemory.workers.startInMemory
import io.infinitic.inMemory.workflows.WorkflowA
import io.infinitic.inMemory.workflows.WorkflowAImpl
import io.infinitic.inMemory.workflows.WorkflowB
import io.infinitic.inMemory.workflows.WorkflowBImpl
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.tasks.engine.transport.TaskEngineMessageToProcess
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import io.infinitic.workflows.engine.transport.WorkflowEngineMessageToProcess
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

fun main() {
    val taskEngineCommandsChannel = Channel<TaskEngineMessageToProcess>()
    val workflowEngineCommandsChannel = Channel<WorkflowEngineMessageToProcess>()

    runBlocking {
        val client = InfiniticClient(
            InMemoryClientOutput(this, taskEngineCommandsChannel, workflowEngineCommandsChannel)
        )

        val taskExecutorRegister = TaskExecutorRegisterImpl().apply {
            register(TaskA::class.java.name) { TaskAImpl() }
            register(WorkflowA::class.java.name) { WorkflowAImpl() }
            register(WorkflowB::class.java.name) { WorkflowBImpl() }
        }

        startInMemory(taskExecutorRegister, InMemoryStorage(), taskEngineCommandsChannel, workflowEngineCommandsChannel)

        repeat(1) {
            client.startTask<TaskA> { await(2000) }
            client.startWorkflow<WorkflowA> { seq1() }
        }
    }
}
