package org.wycliffeassociates.tstudio2rc

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.wycliffeassociates.resourcecontainer.entity.Checking
import org.wycliffeassociates.resourcecontainer.entity.DublinCore
import org.wycliffeassociates.resourcecontainer.entity.Language
import org.wycliffeassociates.resourcecontainer.entity.Manifest
import org.wycliffeassociates.resourcecontainer.entity.Source
import org.wycliffeassociates.tstudio2rc.serializable.ProjectManifest
import org.wycliffeassociates.tstudio2rc.serializable.TargetLanguage
import java.io.File
import java.time.LocalDate

class RC(directory: String? = null) {

    private var _dir: String? = directory
    init {
        _dir?.let { assert(File(it).isDirectory) }
    }

    val metadata: ProjectManifest = parseManifestFile()

    private fun parseManifestFile(): ProjectManifest {
        val file = File("$path/manifest.json")
        val mapper = ObjectMapper(JsonFactory()).registerKotlinModule()
        val metadata: ProjectManifest = mapper.readValue(file)
        return metadata
    }

    val path: String
        get() = _dir?.trimEnd('/') ?: ""

    private val sources = metadata.sourceTranslations.map { Source(it.resourceId, it.languageId, it.version) }.toMutableList()

    private val dublinCore: DublinCore
        get() = DublinCore(
            type = "book",
            conformsTo = "rc0.2",
            format = "text/${metadata.format}",
            identifier = metadata.resource.id,
            title = metadata.resource.name,
            subject = "Bible",
            description = "",
            language = mapToLanguageEntity(metadata.targetLanguage),
            source = sources,
            rights = "CC BY-SA 4.0",
            creator = "Unknown Creator",
            contributor = metadata.translators.toMutableList(),
            relation = mutableListOf(),
            publisher = "Door43",
            issued = LocalDate.now().toString(),
            modified = LocalDate.now().toString(),
            version = "1"
        )

    val rcProject: RCProject
        get() {
            val projectPath = if (File("${path}/content").isDirectory) "./content" else "./"
            return RCProject(
                identifier = metadata.project.id,
                title = metadata.project.name,
                sort = 1,
                path = projectPath,
                versification = "kjv",
                categories = listOf()
            )
        }

    fun rcManifest(): Manifest {
        return Manifest(
            dublinCore = dublinCore,
            checking = Checking(
                checkingEntity = listOf("Wycliffe Associates"),
                checkingLevel = "1"
            ),
            projects = listOf(rcProject)
        )
    }
}

fun mapToLanguageEntity(targetLanguage: TargetLanguage) = Language(
    identifier = targetLanguage.id,
    title = targetLanguage.name,
    direction = targetLanguage.direction
)