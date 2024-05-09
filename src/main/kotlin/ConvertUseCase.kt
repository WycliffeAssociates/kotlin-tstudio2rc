package org.wycliffeassociates

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipFile

class TstudioToRC {

    private val bookDirPattern = Regex("(.*)(/\\d{1,3})")
    private val verseCounts = getVersification()
    private val converterScript = TextToUSFM

    // source: txt2USFM-RC.py
    private fun makeUsfmFilename(bookSlug: String): String {
        val bookId = bookSlug.uppercase()

        val num = verseCounts[bookId]?.usfmNumber ?: ""
        return if (num.isNotEmpty()) {
            "$num-$bookId.usfm"
        } else {
            val pathComponents = File("").absolutePath.split("/")
            "${pathComponents.last()}.usfm"
        }
    }

    // constructs the manifest based on the manifest.json in project directory
    private fun buildManifest(dir: String): Map<String, Any?> {
        val rc = RC(directory = dir)
        var manifest = rc.asDict()
        manifest["dublin_core"].let { it as MutableMap<String, String> }["creator"] = "BTT-Writer"
        val projectSlug = rc.project()!!.identifier
        val projectPath = "./${makeUsfmFilename(projectSlug)}"
        val anthology = if ((verseCounts[projectSlug.uppercase()]?.sort ?: 0) < 40) "ot" else "nt"

        manifest["projects"].let { it as List<RCProject> }.forEach { p ->
            if (p.identifier == projectSlug) {
                p.path = projectPath
                p.sort = verseCounts[projectSlug.uppercase()]?.sort ?: 0
                p.versification = "ufw"
                p.categories = listOf("bible-$anthology")
            }
        }

        return manifest
    }

    // unzip project and returns the extracted path
    private fun extractTstudio(file: File, destinationDir: String): String {
        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val entryDestination = Paths.get(destinationDir, entry.name)
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

        return prepareProjectDir(destinationDir)!!
    }

    private fun prepareProjectDir(dir: String): String? {
        var rootDir: String? = null

        File(dir).walkTopDown().forEach { file ->
            if (file.isDirectory && file.name == ".git") {
                file.deleteRecursively()
            }
            if (file.isDirectory) {
                val match = bookDirPattern.find(file.path)
                if (match != null) {
                    rootDir = match.groupValues[1]
                    return@forEach
                }
            }
        }

        return rootDir
    }

    fun clearDirectory(directoryPath: String) {
        val directory = File(directoryPath)
        if (directory.exists() && directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    clearDirectory(file.absolutePath)
                }
                file.delete()
            }
        }
    }

    fun convert(inputFile: File, outputDir: String) {
        val tempDir = Files.createTempDirectory("tempDir").toFile()

        val fileNameNoExt = inputFile.nameWithoutExtension
        val rcConvertDir = File(outputDir, fileNameNoExt)
        val outputFilePath = File(outputDir, "RC")
        rcConvertDir.mkdirs()

        val sourceDir = extractTstudio(inputFile, tempDir.absolutePath)
        converterScript.sourceDir = sourceDir
        converterScript.targetDir = rcConvertDir.absolutePath
        converterScript.convertFolder(sourceDir)

        // manifest.yaml
        val manifest = buildManifest(sourceDir)
        val manifestFile = rcConvertDir.resolve("manifest.yaml")
        val mapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()
        mapper.writeValue(manifestFile, manifest)

        val zipFileName = outputFilePath.absolutePath
        zipDirectory(rcConvertDir, File("$zipFileName.zip"))

        val outputFileName = "$fileNameNoExt.orature"
        val oratureFile = File(outputFilePath.parent, outputFileName)

        File("$zipFileName.zip").renameTo(oratureFile)
    }

    fun convertDir(inputDir: String, outputDir: String) {
        val rcConvertDir = File(outputDir, File(inputDir).name)
        val outputFilePath = File(outputDir, "RC")
        rcConvertDir.mkdirs()

        converterScript.sourceDir = inputDir
        converterScript.targetDir = rcConvertDir.absolutePath
        converterScript.convertFolder(inputDir)

        // manifest.yaml
        val manifest = buildManifest(inputDir)
        val manifestFile = rcConvertDir.resolve("manifest.yaml")
        val mapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()
        mapper.writeValue(manifestFile, manifest)

        val zipFileName = outputFilePath.absolutePath
        zipDirectory(rcConvertDir, File("$zipFileName.zip"))

        val outputFileName = "${File(inputDir).name}.orature"
        val oratureFile = File(outputFilePath.parent, outputFileName)

        File("$zipFileName.zip").renameTo(oratureFile)
    }
}
