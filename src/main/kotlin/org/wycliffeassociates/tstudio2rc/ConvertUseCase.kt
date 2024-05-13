package org.wycliffeassociates.tstudio2rc

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.wycliffeassociates.resourcecontainer.entity.Manifest
import java.io.File
import java.nio.file.Files

class ConvertUseCase {

    private val bookDirPattern = Regex("(.*)(/\\d{1,3})")
    private val verseCounts = getVersification()

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
    private fun buildManifest(dir: String): Manifest {
        val tstudioMetadata = TstudioMetadata(dir)
        val manifest = tstudioMetadata.rcManifest()
        manifest.dublinCore.creator = "BTT-Writer"

        val projectSlug = tstudioMetadata.rcProject.identifier
        val projectPath = "./${makeUsfmFilename(projectSlug)}"
        val anthology = if ((verseCounts[projectSlug.uppercase()]?.sort ?: 0) < 40) "ot" else "nt"

        manifest.projects.forEach { p ->
            if (p.identifier == projectSlug) {
                p.title = tstudioMetadata.manifest.project.name
                p.path = projectPath
                p.sort = verseCounts[projectSlug.uppercase()]?.sort ?: 0
                p.versification = "ulb"
                p.categories = listOf("bible-$anthology")
            }
        }

        return manifest
    }

    // unzip project and returns the extracted path
    private fun extractTstudio(file: File, destinationDir: File): String {
        unzipFile(file, destinationDir)
        return prepareProjectDir(destinationDir)!!
    }

    private fun prepareProjectDir(dir: File): String? {
        var rootDir: String? = null

        dir.walkTopDown().forEach { file ->
            // remove .git folder
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

    fun convertToOratureFile(inputFile: File, outputDir: String): File {
        val tempDir = Files.createTempDirectory("tempDir").toFile()

        val fileNameNoExt = inputFile.nameWithoutExtension
        val rcConvertDir = File(outputDir, fileNameNoExt)
        val outputFilePath = File(outputDir, "RC")
        rcConvertDir.mkdirs()

        val sourceDir = extractTstudio(inputFile, tempDir)
        val converter = TextToUSFM()
        converter.convertFolder(sourceDir, rcConvertDir.absolutePath)

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
        tempDir.deleteRecursively()

        return oratureFile
    }

    fun convertDir(inputDir: File, outputDir: File): File {
        val outputRc = outputDir.resolve("${inputDir.name}.zip")

        val tempConvertDir = outputDir.resolve(inputDir.name)
        tempConvertDir.mkdirs()

        val converter = TextToUSFM()
        converter.convertFolder(inputDir.invariantSeparatorsPath, tempConvertDir.invariantSeparatorsPath)

        // manifest.yaml
        val manifest = buildManifest(inputDir.invariantSeparatorsPath)
        val manifestFile = tempConvertDir.resolve("manifest.yaml")
        val mapper = ObjectMapper(YAMLFactory())
            .registerKotlinModule()
        mapper.writeValue(manifestFile, manifest)

        zipDirectory(tempConvertDir, outputRc)
        tempConvertDir.deleteRecursively()

        return outputRc
    }
}
