package org.wycliffeassociates.tstudio2rc

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.wycliffeassociates.tstudio2rc.serializable.BookVersification
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

internal fun loadJsonObject(path: String): Map<String, Any> {
    val mapper = ObjectMapper(JsonFactory())
        .registerKotlinModule()

    return mapper.readValue(File(path))
}

internal fun getVersification(): Map<String, BookVersification> {
    val mapper = ObjectMapper(JsonFactory())
        .registerKotlinModule()

    val path = BookVersification::class.java.classLoader.getResource("verse_counts.json").file

    return mapper.readValue(File(path))
}

// Returns true if the specified path looks like a collection of chapter folders
internal fun isBookFolder(path: String): Boolean {
    return File(path).resolve("front").isDirectory || File(path).resolve("01").isDirectory
}

internal fun zipDirectory(sourceDir: File, zipFile: File) {
    zipFile.createNewFile()
    ZipOutputStream(zipFile.outputStream()).use { zos ->
        sourceDir.walkTopDown().forEach { file ->
            if (file.isFile) {
                val entryPath = file.relativeTo(sourceDir).invariantSeparatorsPath
                val zipEntry = ZipEntry(entryPath)
                zos.putNextEntry(zipEntry)
                file.inputStream().use { input ->
                    input.copyTo(zos)
                }
            }
        }
    }
}

internal fun unzipFile(file: File, destinationDir: File) {
    ZipFile(file).use { zip ->
        zip.entries().asSequence().forEach { entry ->
            val entryDestination = Paths.get(destinationDir.invariantSeparatorsPath, entry.name)
            if (entry.isDirectory) {
                Files.createDirectories(entryDestination)
            } else {
                zip.getInputStream(entry).use { input ->
                    Files.newOutputStream(entryDestination).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
}