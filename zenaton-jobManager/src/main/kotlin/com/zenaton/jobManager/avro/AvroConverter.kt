package com.zenaton.jobManager.avro

import com.zenaton.common.json.Json
import com.zenaton.jobManager.engine.JobEngineState
import com.zenaton.jobManager.messages.AvroCancelJob
import com.zenaton.jobManager.messages.AvroDispatchJob
import com.zenaton.jobManager.messages.AvroJobAttemptCompleted
import com.zenaton.jobManager.messages.AvroJobAttemptDispatched
import com.zenaton.jobManager.messages.AvroJobAttemptFailed
import com.zenaton.jobManager.messages.AvroJobAttemptStarted
import com.zenaton.jobManager.messages.AvroJobCanceled
import com.zenaton.jobManager.messages.AvroJobCompleted
import com.zenaton.jobManager.messages.AvroJobCreated
import com.zenaton.jobManager.messages.AvroJobStatusUpdated
import com.zenaton.jobManager.messages.AvroRetryJob
import com.zenaton.jobManager.messages.AvroRetryJobAttempt
import com.zenaton.jobManager.messages.AvroRunJob
import com.zenaton.jobManager.messages.CancelJob
import com.zenaton.jobManager.messages.DispatchJob
import com.zenaton.jobManager.messages.JobAttemptCompleted
import com.zenaton.jobManager.messages.JobAttemptDispatched
import com.zenaton.jobManager.messages.JobAttemptFailed
import com.zenaton.jobManager.messages.JobAttemptStarted
import com.zenaton.jobManager.messages.JobCanceled
import com.zenaton.jobManager.messages.JobCompleted
import com.zenaton.jobManager.messages.JobCreated
import com.zenaton.jobManager.messages.JobStatusUpdated
import com.zenaton.jobManager.messages.RetryJob
import com.zenaton.jobManager.messages.RetryJobAttempt
import com.zenaton.jobManager.messages.RunJob
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroForJobEngineMessageType
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringGlobalMessageType
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroForMonitoringPerNameMessageType
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.messages.envelopes.AvroForWorkerMessageType
import com.zenaton.jobManager.messages.envelopes.ForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkerMessage
import com.zenaton.jobManager.monitoringGlobal.MonitoringGlobalState
import com.zenaton.jobManager.monitoringPerName.MonitoringPerNameState
import com.zenaton.jobManager.states.AvroJobEngineState
import com.zenaton.jobManager.states.AvroMonitoringGlobalState
import com.zenaton.jobManager.states.AvroMonitoringPerNameState
import com.zenaton.workflowManager.messages.AvroTaskCompleted
import org.apache.avro.specific.SpecificRecordBase

/**
 * This class does the mapping between avro-generated classes and classes actually used by our code
 */
object AvroConverter {

    /**
     *  States
     */
    fun fromStorage(avro: AvroJobEngineState) = convertJson<JobEngineState>(avro)
    fun fromStorage(avro: AvroMonitoringGlobalState) = convertJson<MonitoringGlobalState>(avro)
    fun fromStorage(avro: AvroMonitoringPerNameState) = convertJson<MonitoringPerNameState>(avro)

    fun toStorage(state: JobEngineState) = convertJson<AvroJobEngineState>(state)
    fun toStorage(state: MonitoringGlobalState) = convertJson<AvroMonitoringGlobalState>(state)
    fun toStorage(state: MonitoringPerNameState) = convertJson<AvroMonitoringPerNameState>(state)

    /**
     *  Envelopes
     */
    fun toWorkers(message: ForWorkerMessage): AvroEnvelopeForWorker {
        val builder = AvroEnvelopeForWorker.newBuilder()
        builder.jobName = message.jobName.name
        when (message) {
            is RunJob -> builder.apply {
                runJob = convertToAvro(message)
                type = AvroForWorkerMessageType.RunJob
            }
            else -> throw Exception("Unknown ForWorkerMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun fromWorkers(input: AvroEnvelopeForWorker): ForWorkerMessage {
        return when (input.type) {
            AvroForWorkerMessageType.RunJob -> this.convertFromAvro(input.runJob)
            else -> throw Exception("Unknown AvroEnvelopeForWorker: ${input::class.qualifiedName}")
        }
    }

    fun toMonitoringGlobal(message: ForMonitoringGlobalMessage): AvroEnvelopeForMonitoringGlobal {
        val builder = AvroEnvelopeForMonitoringGlobal.newBuilder()
        when (message) {
            is JobCreated -> builder.apply {
                jobCreated = convertToAvro(message)
                type = AvroForMonitoringGlobalMessageType.JobCreated
            }
            else -> throw Exception("Unknown ForMonitoringGlobalMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun fromMonitoringGlobal(input: AvroEnvelopeForMonitoringGlobal): ForMonitoringGlobalMessage {
        return when (input.type) {
            AvroForMonitoringGlobalMessageType.JobCreated -> this.convertFromAvro(input.jobCreated)
            else -> throw Exception("Unknown AvroEnvelopeForMonitoringGlobal: ${input::class.qualifiedName}")
        }
    }

    fun toMonitoringPerName(message: ForMonitoringPerNameMessage): AvroEnvelopeForMonitoringPerName {
        val builder = AvroEnvelopeForMonitoringPerName.newBuilder()
        builder.jobName = message.jobName.name
        when (message) {
            is JobStatusUpdated -> builder.apply {
                jobStatusUpdated = convertToAvro(message)
                type = AvroForMonitoringPerNameMessageType.JobStatusUpdated
            }
            else -> throw Exception("Unknown ForMonitoringPerNameMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun fromMonitoringPerName(input: AvroEnvelopeForMonitoringPerName): ForMonitoringPerNameMessage {
        return when (input.type) {
            AvroForMonitoringPerNameMessageType.JobStatusUpdated -> this.convertFromAvro(input.jobStatusUpdated)
            else -> throw Exception("Unknown AvroEnvelopeForMonitoringPerName: ${input::class.qualifiedName}")
        }
    }

    fun toJobEngine(message: ForJobEngineMessage): AvroEnvelopeForJobEngine {
        val builder = AvroEnvelopeForJobEngine.newBuilder()
        builder.jobId = message.jobId.id
        when (message) {
            is CancelJob -> builder.apply {
                cancelJob = convertToAvro(message)
                type = AvroForJobEngineMessageType.CancelJob
            }
            is DispatchJob -> builder.apply {
                dispatchJob = convertToAvro(message)
                type = AvroForJobEngineMessageType.DispatchJob
            }
            is RetryJob -> builder.apply {
                retryJob = convertToAvro(message)
                type = AvroForJobEngineMessageType.RetryJob
            }
            is RetryJobAttempt -> builder.apply {
                retryJobAttempt = convertToAvro(message)
                type = AvroForJobEngineMessageType.RetryJobAttempt
            }
            is JobAttemptDispatched -> builder.apply {
                jobAttemptDispatched = convertToAvro(message)
                type = AvroForJobEngineMessageType.JobAttemptDispatched
            }
            is JobAttemptCompleted -> builder.apply {
                jobAttemptCompleted = convertToAvro(message)
                type = AvroForJobEngineMessageType.JobAttemptCompleted
            }
            is JobAttemptFailed -> builder.apply {
                jobAttemptFailed = convertToAvro(message)
                type = AvroForJobEngineMessageType.JobAttemptFailed
            }
            is JobAttemptStarted -> builder.apply {
                jobAttemptStarted = convertToAvro(message)
                type = AvroForJobEngineMessageType.JobAttemptStarted
            }
            is JobCanceled -> builder.apply {
                jobCanceled = convertToAvro(message)
                type = AvroForJobEngineMessageType.JobCanceled
            }
            is JobCompleted -> builder.apply {
                jobCompleted = convertToAvro(message)
                type = AvroForJobEngineMessageType.JobCompleted
            }
            else -> throw Exception("Unknown ForJobEngineMessage: ${message::class.qualifiedName}")
        }
        return builder.build()
    }

    fun fromJobEngine(input: AvroEnvelopeForJobEngine): ForJobEngineMessage {
        return when (input.type) {
            AvroForJobEngineMessageType.CancelJob -> this.convertFromAvro(input.cancelJob)
            AvroForJobEngineMessageType.DispatchJob -> this.convertFromAvro(input.dispatchJob)
            AvroForJobEngineMessageType.RetryJob -> this.convertFromAvro(input.retryJob)
            AvroForJobEngineMessageType.RetryJobAttempt -> this.convertFromAvro(input.retryJobAttempt)
            AvroForJobEngineMessageType.JobAttemptDispatched -> this.convertFromAvro(input.jobAttemptDispatched)
            AvroForJobEngineMessageType.JobAttemptCompleted -> this.convertFromAvro(input.jobAttemptCompleted)
            AvroForJobEngineMessageType.JobAttemptFailed -> this.convertFromAvro(input.jobAttemptFailed)
            AvroForJobEngineMessageType.JobAttemptStarted -> this.convertFromAvro(input.jobAttemptStarted)
            AvroForJobEngineMessageType.JobCanceled -> this.convertFromAvro(input.jobCanceled)
            AvroForJobEngineMessageType.JobCompleted -> this.convertFromAvro(input.jobCompleted)
            else -> throw Exception("Unknown AvroEnvelopeForJobEngine: ${input::class.qualifiedName}")
        }
    }

    /**
     *  Messages
     */

    private fun convertFromAvro(avro: AvroCancelJob) = convertJson<CancelJob>(avro)
    private fun convertFromAvro(avro: AvroDispatchJob) = convertJson<DispatchJob>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptCompleted) = convertJson<JobAttemptCompleted>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptDispatched) = convertJson<JobAttemptDispatched>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptFailed) = convertJson<JobAttemptFailed>(avro)
    private fun convertFromAvro(avro: AvroJobAttemptStarted) = convertJson<JobAttemptStarted>(avro)
    private fun convertFromAvro(avro: AvroJobCanceled) = convertJson<JobCanceled>(avro)
    private fun convertFromAvro(avro: AvroJobCompleted) = convertJson<JobCompleted>(avro)
    private fun convertFromAvro(avro: AvroJobCreated) = convertJson<JobCreated>(avro)
    private fun convertFromAvro(avro: AvroJobStatusUpdated) = convertJson<JobStatusUpdated>(avro)
    private fun convertFromAvro(avro: AvroRetryJob) = convertJson<RetryJob>(avro)
    private fun convertFromAvro(avro: AvroRetryJobAttempt) = convertJson<RetryJobAttempt>(avro)
    private fun convertFromAvro(avro: AvroRunJob) = convertJson<RunJob>(avro)

    private fun convertToAvro(message: CancelJob) = convertJson<AvroCancelJob>(message)
    private fun convertToAvro(message: DispatchJob) = convertJson<AvroDispatchJob>(message)
    private fun convertToAvro(message: JobAttemptCompleted) = convertJson<AvroJobAttemptCompleted>(message)
    private fun convertToAvro(message: JobAttemptDispatched) = convertJson<AvroJobAttemptDispatched>(message)
    private fun convertToAvro(message: JobAttemptFailed) = convertJson<AvroJobAttemptFailed>(message)
    private fun convertToAvro(message: JobAttemptStarted) = convertJson<AvroJobAttemptStarted>(message)
    private fun convertToAvro(message: JobCanceled) = convertJson<AvroJobCanceled>(message)
    private fun convertToAvro(message: JobCompleted) = convertJson<AvroJobCompleted>(message)
    private fun convertToAvro(message: JobCreated) = convertJson<AvroJobCreated>(message)
    private fun convertToAvro(message: JobStatusUpdated) = convertJson<AvroJobStatusUpdated>(message)
    private fun convertToAvro(message: RetryJob) = convertJson<AvroRetryJob>(message)
    private fun convertToAvro(message: RetryJobAttempt) = convertJson<AvroRetryJobAttempt>(message)
    private fun convertToAvro(message: RunJob) = convertJson<AvroRunJob>(message)

    /**
     *  Any Message
     */

    fun convertFromAvro(avro: SpecificRecordBase): Any = when (avro) {
        is AvroCancelJob -> this.convertFromAvro(avro)
        is AvroDispatchJob -> this.convertFromAvro(avro)
        is AvroJobAttemptCompleted -> this.convertFromAvro(avro)
        is AvroJobAttemptDispatched -> this.convertFromAvro(avro)
        is AvroJobAttemptFailed -> this.convertFromAvro(avro)
        is AvroJobAttemptStarted -> this.convertFromAvro(avro)
        is AvroJobCanceled -> this.convertFromAvro(avro)
        is AvroJobCompleted -> this.convertFromAvro(avro)
        is AvroJobCreated -> this.convertFromAvro(avro)
        is AvroJobStatusUpdated -> this.convertFromAvro(avro)
        is AvroRetryJob -> this.convertFromAvro(avro)
        is AvroRetryJobAttempt -> this.convertFromAvro(avro)
        is AvroRunJob -> this.convertFromAvro(avro)
        is AvroTaskCompleted -> this.convertFromAvro(avro)
        else -> throw Exception("Unknown SpecificRecordBase: ${avro::class.qualifiedName}")
    }

    /**
     *  Mapping function by Json serialization/deserialization
     */
    private inline fun <reified T : Any> convertJson(from: Any): T = Json.parse(Json.stringify(from))
}