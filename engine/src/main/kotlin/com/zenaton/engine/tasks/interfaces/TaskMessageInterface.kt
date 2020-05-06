package com.zenaton.engine.tasks.interfaces

import com.zenaton.engine.interfaces.MessageInterface
import com.zenaton.engine.interfaces.data.DateTime
import com.zenaton.engine.tasks.data.TaskId

interface TaskMessageInterface : MessageInterface {
    val taskId: TaskId
    override var sentAt: DateTime?
    override var receivedAt: DateTime?
    override fun getKey() = taskId.id
}