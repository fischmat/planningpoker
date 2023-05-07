package de.matthiasfisch.planningpoker.util

import com.fasterxml.jackson.annotation.JacksonAnnotationsInside
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanProperty
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.ContextualSerializer
import com.fasterxml.jackson.databind.ser.std.StdSerializer


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = MaskingSerializer::class)
annotation class JsonMasked

private class MaskingSerializer: StdSerializer<Any>(Any::class.java), ContextualSerializer {
    override fun serialize(value: Any?, gen: JsonGenerator, provider: SerializerProvider?) {
        if (value != null) {
            gen.writeString("****")
        } else {
            gen.writeNull()
        }
    }

    override fun createContextual(prov: SerializerProvider?, property: BeanProperty?): JsonSerializer<*>? {
        return property?.takeIf { it.getAnnotation(JsonMasked::class.java) != null }?.let { MaskingSerializer() }
    }
}