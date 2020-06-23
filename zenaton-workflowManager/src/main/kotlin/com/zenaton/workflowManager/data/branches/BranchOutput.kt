package com.zenaton.workflowManager.data.branches

import com.zenaton.common.data.SerializationType
import com.zenaton.common.data.SerializedOutput

data class BranchOutput(
    override val serializedData: ByteArray,
    override val serializationType: SerializationType
) : SerializedOutput(serializedData, serializationType)