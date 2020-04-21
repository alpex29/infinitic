package com.zenaton.engine.attributes.workflows.states

import com.zenaton.engine.attributes.delays.DelayId
import com.zenaton.engine.attributes.events.EventData
import com.zenaton.engine.attributes.events.EventId
import com.zenaton.engine.attributes.events.EventName
import com.zenaton.engine.attributes.tasks.TaskId
import com.zenaton.engine.attributes.tasks.TaskOutput
import com.zenaton.engine.attributes.types.DateTime
import com.zenaton.engine.attributes.workflows.WorkflowId
import com.zenaton.engine.attributes.workflows.WorkflowOutput

sealed class Action(
    open val actionId: ActionId,
    open val decidedAt: DateTime,
    open val status: ActionStatus
)

data class WaitingForTask(
    val taskId: TaskId,
    val taskOutput: TaskOutput?,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(ActionId(taskId), decidedAt, status)

data class WaitingForWorkflow(
    val workflowId: WorkflowId,
    val workflowOutput: WorkflowOutput?,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(ActionId(workflowId), decidedAt, status)

data class WaitingForDelay(
    val delayId: DelayId,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(ActionId(delayId), decidedAt, status)

data class WaitingForEvent(
    val eventId: EventId,
    val eventName: EventName,
    val eventData: EventData?,
    override val decidedAt: DateTime,
    override val status: ActionStatus
) : Action(ActionId(eventId), decidedAt, status)