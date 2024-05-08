package org.wycliffeassociates

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

fun loadYamlObject(path: String): Map<String, Any> {
    val mapper = ObjectMapper(YAMLFactory())
        .registerKotlinModule()

    return mapper.readValue(File(path))
}

fun loadJsonObject(path: String): Map<String, Any> {
    val mapper = ObjectMapper(JsonFactory())
        .registerKotlinModule()

    return mapper.readValue(File(path))
}

fun addErrorMessage(message: String) {

}

fun getVersification(): Map<String, BookVersification> {
    val mapper = ObjectMapper(JsonFactory())
        .registerKotlinModule()

    val path = BookVersification::class.java.classLoader.getResource("verse_counts.json").file

    return mapper.readValue(File(path))
}