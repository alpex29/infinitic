package com.zenaton.jobManager.avro

import com.zenaton.jobManager.messages.Message
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForJobEngine
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringGlobal
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForMonitoringPerName
import com.zenaton.jobManager.messages.envelopes.AvroEnvelopeForWorker
import com.zenaton.jobManager.messages.envelopes.ForJobEngineMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringGlobalMessage
import com.zenaton.jobManager.messages.envelopes.ForMonitoringPerNameMessage
import com.zenaton.jobManager.messages.envelopes.ForWorkerMessage
import com.zenaton.jobManager.utils.TestFactory
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.TestConfiguration
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.stringSpec
import io.kotest.matchers.shouldBe
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import kotlin.reflect.KClass
import org.apache.avro.specific.SpecificRecordBase

class AvroConsistencyTests : StringSpec({
    // From Message
    Message::class.sealedSubclasses.forEach {
        val msg = TestFactory.get(it)
        if (msg is ForWorkerMessage) {
            include(checkAvroConversionToEnvelopeForWorker(msg))
        }
        if (msg is ForJobEngineMessage) {
            include(checkAvroConversionToEnvelopeForJobEngine(msg))
        }
        if (msg is ForMonitoringPerNameMessage) {
            include(checkAvroConversionToEnvelopeForMonitoringPerName(msg))
        }
        if (msg is ForMonitoringGlobalMessage) {
            include(checkAvroConversionToEnvelopeForMonitoringGlobal(msg))
        }
    }

    // From Avro
    checkAllSubTypesFromEnvelope<ForWorkerMessage>(this, AvroEnvelopeForWorker())
    checkAllSubTypesFromEnvelope<ForJobEngineMessage>(this, AvroEnvelopeForJobEngine())
    checkAllSubTypesFromEnvelope<ForMonitoringGlobalMessage>(this, AvroEnvelopeForMonitoringGlobal())
    checkAllSubTypesFromEnvelope<ForMonitoringPerNameMessage>(this, AvroEnvelopeForMonitoringPerName())
})

fun checkAvroConversionToEnvelopeForWorker(msg: ForWorkerMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForWorker::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toWorkers(msg)
        }
    }
}

fun checkAvroConversionToEnvelopeForJobEngine(msg: ForJobEngineMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForJobEngine::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toJobEngine(msg)
        }
    }
}

fun checkAvroConversionToEnvelopeForMonitoringPerName(msg: ForMonitoringPerNameMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForMonitoringPerName::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toMonitoringPerName(msg)
        }
    }
}

fun checkAvroConversionToEnvelopeForMonitoringGlobal(msg: ForMonitoringGlobalMessage) = stringSpec {
    "${msg::class.simpleName!!} should be convertible to ${AvroEnvelopeForMonitoringGlobal::class.simpleName}" {
        shouldNotThrowAny {
            AvroConverter.toMonitoringGlobal(msg)
        }
    }
}

inline fun <reified T> checkAllSubTypesFromEnvelope(config: TestConfiguration, msg: GenericRecord) {
    msg.schema.getField("type").schema().enumSymbols.forEach {
        val schema = msg.schema.getField(it).schema()
        config.include(checkEnvelopeSchema(it, schema, msg::class))
        val name = schema.types[1].name
        val namespace = schema.types[1].namespace
        config.include(checkConversionFromAvro<T>(name, namespace))
    }
}

fun checkEnvelopeSchema(field: String, schema: Schema, klass: KClass<out GenericRecord>) = stringSpec {
    "Checking schema for field $field of ${klass.simpleName}" {
        // check that type is an union
        schema.isUnion shouldBe true
        // check that first type is null
        schema.types[0].isNullable shouldBe true
        // check size
        schema.types.size shouldBe 2
    }
}

inline fun <reified T> checkConversionFromAvro(name: String, namespace: String) = stringSpec {
    "$name should be convertible from avro" {
        // get class name
        @Suppress("UNCHECKED_CAST")
        val klass = Class.forName("$namespace.$name").kotlin as KClass<SpecificRecordBase>
        val message = AvroConverter.convertFromAvro(TestFactory.get(klass))
        (message is T) shouldBe true
        (message is Message) shouldBe true
    }
}