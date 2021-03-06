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

package io.infinitic.pulsar

import io.infinitic.pulsar.config.Mode
import io.infinitic.pulsar.config.WorkerConfig
import io.infinitic.pulsar.config.getKeyValueStorage
import io.infinitic.pulsar.config.loadConfigFromFile
import io.infinitic.pulsar.config.loadConfigFromResource
import io.infinitic.pulsar.transport.PulsarConsumerFactory
import io.infinitic.pulsar.transport.PulsarOutputs
import io.infinitic.pulsar.workers.startPulsarMonitoringGlobalWorker
import io.infinitic.pulsar.workers.startPulsarMonitoringPerNameWorker
import io.infinitic.pulsar.workers.startPulsarTaskEngineWorker
import io.infinitic.pulsar.workers.startPulsarTaskExecutorWorker
import io.infinitic.pulsar.workers.startPulsarWorkflowEngineWorker
import io.infinitic.storage.inMemory.InMemoryStorage
import io.infinitic.tasks.executor.register.TaskExecutorRegisterImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.apache.pulsar.client.api.PulsarClient
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
class InfiniticWorker(
    @JvmField val pulsarClient: PulsarClient,
    @JvmField val config: WorkerConfig
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        /*
        Create InfiniticWorker from a WorkerConfig
        */
        @JvmStatic
        fun fromConfig(config: WorkerConfig): InfiniticWorker {
            // build Pulsar client from config
            val pulsarClient: PulsarClient = PulsarClient
                .builder()
                .serviceUrl(config.pulsar.serviceUrl)
                .build()

            return InfiniticWorker(pulsarClient, config)
        }

        /*
        Create InfiniticWorker from a WorkerConfig loaded from a resource
        */
        @JvmStatic
        fun fromResource(vararg resources: String) =
            fromConfig(loadConfigFromResource(resources.toList()))

        /*
        Create InfiniticWorker from a WorkerConfig loaded from a file
        */
        @JvmStatic
        fun fromFile(vararg files: String) =
            fromConfig(loadConfigFromFile(files.toList()))
    }

    /*
    Close workers
    */
    fun close() = pulsarClient.close()

    /*
    Start workers
    */
    fun start() = runBlocking {
        logger.info("InfiniticWorker - starting with config {}", config)

        val tenant = config.pulsar.tenant
        val namespace = config.pulsar.namespace
        val pulsarConsumerFactory = PulsarConsumerFactory(pulsarClient, tenant, namespace)
        val pulsarOutputs = PulsarOutputs.from(pulsarClient, tenant, namespace, producerName = getWorkerName(config))

        startWorkflowEngineWorkers(config, pulsarConsumerFactory, pulsarOutputs)

        startTaskEngineWorkers(config, pulsarConsumerFactory, pulsarOutputs)

        startMonitoringWorkers(config, pulsarConsumerFactory, pulsarOutputs)

        startTaskExecutorWorkers(config, pulsarConsumerFactory, pulsarOutputs)

        println("Ready")
    }

    private fun getWorkerName(config: WorkerConfig) = when (config.name) {
        null -> null
        else -> "worker: ${config.name}"
    }

    private fun CoroutineScope.startWorkflowEngineWorkers(
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        config.workflowEngine?.let {
            if (it.mode == Mode.worker) {
                val keyValueStorage = it.stateStorage!!.getKeyValueStorage(config, "workflowStates")
                print("Workflow engine".padEnd(25) + ": starting ${it.consumers} instances...")
                repeat(it.consumers) { counter ->
                    logger.info("InfiniticWorker - starting workflow engine {}", counter)
                    startPulsarWorkflowEngineWorker(
                        counter,
                        pulsarConsumerFactory.newWorkflowEngineConsumer(consumerName = getWorkerName(config), counter),
                        pulsarOutputs.workflowEngineOutput,
                        pulsarOutputs.sendToWorkflowEngineDeadLetters,
                        keyValueStorage
                    )
                }
                println(" done")
            }
        }
    }

    private fun CoroutineScope.startTaskEngineWorkers(
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        config.taskEngine?.let {
            if (it.mode == Mode.worker) {
                val keyValueStorage = it.stateStorage!!.getKeyValueStorage(config, "taskStates")
                print("Task engine".padEnd(25) + ": starting ${it.consumers} instances...")
                repeat(it.consumers) { counter ->
                    logger.info("InfiniticWorker - starting task engine {}", counter)
                    startPulsarTaskEngineWorker(
                        counter,
                        pulsarConsumerFactory.newTaskEngineConsumer(consumerName = getWorkerName(config), counter),
                        pulsarOutputs.taskEngineOutput,
                        pulsarOutputs.sendToTaskEngineDeadLetters,
                        keyValueStorage
                    )
                }
                println(" done")
            }
        }
    }

    private fun CoroutineScope.startMonitoringWorkers(
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        config.monitoring?.let {
            if (it.mode == Mode.worker) {
                val keyValueStorage = it.stateStorage!!.getKeyValueStorage(config, "monitoringStates")
                repeat(it.consumers) { counter ->
                    logger.info("InfiniticWorker - starting monitoring per name {}", counter)
                    startPulsarMonitoringPerNameWorker(
                        counter,
                        pulsarConsumerFactory.newMonitoringPerNameEngineConsumer(consumerName = getWorkerName(config), counter),
                        pulsarOutputs.monitoringPerNameOutput,
                        pulsarOutputs.sendToMonitoringPerNameDeadLetters,
                        keyValueStorage
                    )
                }

                logger.info("InfiniticWorker - starting monitoring global")
                startPulsarMonitoringGlobalWorker(
                    pulsarConsumerFactory.newMonitoringGlobalEngineConsumer(consumerName = getWorkerName(config)),
                    pulsarOutputs.sendToMonitoringGlobalDeadLetters,
                    InMemoryStorage()
                )
            }
        }
    }

    private fun CoroutineScope.startTaskExecutorWorkers(
        config: WorkerConfig,
        pulsarConsumerFactory: PulsarConsumerFactory,
        pulsarOutputs: PulsarOutputs
    ) {
        val dispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()
        val taskExecutorRegister = TaskExecutorRegisterImpl()

        for (workflow in config.workflows) {
            if (workflow.mode == Mode.worker) {
                taskExecutorRegister.register(workflow.name) { workflow.getInstance() }

                repeat(workflow.consumers) {
                    print("Workflow executor".padEnd(25) + ": starting ${workflow.concurrency} instances for ${workflow.name}...")
                    logger.info("InfiniticWorker - starting workflow executor for {}", workflow.name)
                    startPulsarTaskExecutorWorker(
                        dispatcher,
                        workflow.name,
                        it,
                        pulsarConsumerFactory.newWorkflowExecutorConsumer(consumerName = getWorkerName(config), it, workflow.name),
                        pulsarOutputs.taskExecutorOutput,
                        pulsarOutputs.sendToTaskExecutorDeadLetters,
                        taskExecutorRegister,
                        workflow.concurrency
                    )
                    println(" done")
                }
            }
        }

        for (task in config.tasks) {
            if (task.mode == Mode.worker) {
                if (task.shared) {
                    val instance = task.getInstance()
                    taskExecutorRegister.register(task.name) { instance }
                } else {
                    taskExecutorRegister.register(task.name) { task.getInstance() }
                }

                repeat(task.consumers) {
                    print("Task executor".padEnd(25) + ": starting ${task.concurrency} instances for ${task.name}...")
                    logger.info("InfiniticWorker - starting task executor for {}", task.name)
                    startPulsarTaskExecutorWorker(
                        dispatcher,
                        task.name,
                        it,
                        pulsarConsumerFactory.newTaskExecutorConsumer(consumerName = getWorkerName(config), it, task.name),
                        pulsarOutputs.taskExecutorOutput,
                        pulsarOutputs.sendToTaskExecutorDeadLetters,
                        taskExecutorRegister,
                        task.concurrency
                    )
                    println(" done")
                }
            }
        }
    }
}
