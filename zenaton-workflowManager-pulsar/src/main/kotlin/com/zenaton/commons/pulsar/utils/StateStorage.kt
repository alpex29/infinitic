package com.zenaton.common.pulsar.utils

import com.zenaton.common.data.interfaces.StateInterface
import com.zenaton.common.avro.AvroSerDe
import com.zenaton.workflowManager.interfaces.StaterInterface
import org.apache.pulsar.functions.api.Context

class StateStorage<T : StateInterface>(private val context: Context) :
    StaterInterface<T> {

    // serializer injection
    var serDe = AvroSerDe

    override fun getState(key: String): T? {
        return null // context.getState(key) ?. let { serDe.deserialize(it) }
    }

    override fun createState(key: String, state: T) {
//        context.putState(key, serDe.serialize(state))
    }

    override fun updateState(key: String, state: T) {
//        context.putState(key, serDe.serialize(state))
    }

    override fun deleteState(key: String) {
        context.deleteState(key)
    }
}