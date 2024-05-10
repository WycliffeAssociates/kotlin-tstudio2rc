package org.wycliffeassociates

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.wycliffeassociates.entity.ProjectManifest
import org.wycliffeassociates.entity.mapToLanguageEntity
import org.wycliffeassociates.resourcecontainer.entity.Checking
import org.wycliffeassociates.resourcecontainer.entity.DublinCore
import org.wycliffeassociates.resourcecontainer.entity.Manifest
import org.wycliffeassociates.resourcecontainer.entity.Source
import java.io.File
import java.time.LocalDate

class RC(
    directory: String? = null,
    repoName: String? = null,
    manifest: Map<String, Any?>? = null
) {

    private var _dir: String? = directory
    private var loadedManifestFile = false
    private var _manifest: Map<String, Any?>? = manifest
    private var _repoName: String? = repoName
    private var _projects: MutableList<Project> = mutableListOf()
    val errorMessages: MutableSet<String> = mutableSetOf()

    init {
        _dir?.let { assert(File(it).isDirectory) }
    }

    val metadata: ProjectManifest = parseManifestFile()

    val rawManifest: Map<String, Any?>
        get() = _manifest ?: getManifestFromDir()

    private fun parseManifestFile(): ProjectManifest {
        val file = File("$path/manifest.json")
        val mapper = ObjectMapper(JsonFactory()).registerKotlinModule()
        val metadata: ProjectManifest = mapper.readValue(file)
        return metadata
    }

    private fun getManifestFromDir(): Map<String, Any?> {
        loadedManifestFile = false
        return if (!path.isNullOrEmpty() && File(path).isDirectory) {
            try {
                loadYamlObject("$path/manifest.yaml")
            } catch (e: Exception) {
                addErrorMessage("Badly formed 'manifest.yaml' in $repoName: ${e.message}")
                null
            } ?: try {
                loadJsonObject("$path/manifest.json")
            } catch (e: Exception) {
                addErrorMessage("Badly formed 'manifest.json' in $repoName: ${e.message}")
                null
            } ?: try {
                loadJsonObject("$path/package.json")
            } catch (e: Exception) {
                addErrorMessage("Badly formed 'package.json' in $repoName: ${e.message}")
                null
            } ?: try {
                loadJsonObject("$path/project.json")
            } catch (e: Exception) {
                addErrorMessage("Badly formed 'project.json' in $repoName: ${e.message}")
                null
            } ?: try {
                loadJsonObject("$path/meta.json")
            } catch (e: Exception) {
                addErrorMessage("Badly formed 'meta.json' in $repoName: ${e.message}")
                null
            } ?: mapOf()// TODO: getManifestFromRepoName(repoName)
        } else {
            // TODO: getManifestFromRepoName(repoName)
            mapOf()
        }
    }

    val path: String
        get() = _dir?.trimEnd('/') ?: ""

    val repoName: String
        get() = _repoName ?: path.takeIf { it.isNotEmpty() }?.let { File(it).name } ?: ""

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

    val checkingEntity: List<String>
        get() = rawManifest["checking"]
            ?.let { it as Map<String, List<String>> }
            ?.get("checking_entity")
            ?: mutableListOf("Wycliffe Associates")

    val checkingLevel: String
        get() = rawManifest["checking"]
            ?.let { it as Map<String, List<String>> }
            ?.getOrDefault("checking_level", "1")
            ?.toString()
            ?: "1"

    private val projects: List<Project>
        get() {
            if (_projects.isEmpty()) {
                if ("projects" in rawManifest && rawManifest["projects"] is List<*>) {
                    val projectList = rawManifest["projects"] as List<*>
                    for (p in projectList) {
                        val project = Project(this, p as Map<String, Any>)
                        _projects.add(project)
                    }
                } else if ("project" in rawManifest && rawManifest["project"] is Map<*, *>) {
                    val projectMap = rawManifest["project"] as Map<*, *>
                    val project = Project(this, projectMap as Map<String, Any>)
                    _projects.add(project)
                }
                if (_projects.isEmpty()) {
                    _projects.add(Project(this, mapOf())) // will rely on info in the resource
                }
            }
            return _projects
        }

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

    fun project(identifier: String? = null): Project? {
        return identifier?.let { projects.find { it.identifier == identifier } }
            ?: projects.singleOrNull()
            ?: if (projects.isEmpty()) Project(this) else null
    }

    fun chapters(identifier: String? = null): List<String> {
        val project = project(identifier) ?: return emptyList()
        return File("$path/${project.path}")
                .listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
                ?.map { it.name }
                ?.filter { chunks(identifier, it).isNotEmpty() }
                ?.sorted()
                ?: emptyList()
    }

    fun chunks(projectIdentifier: String?, chapterIdentifier: String? = null): List<String> {
        val projectId = projectIdentifier ?: return emptyList()
        val chapterId = chapterIdentifier ?: projectId
        val project = project(projectId) ?: return emptyList()
        return File("$path/${project.path}/$chapterId")
            .listFiles { file ->
                file.isFile &&
                        !file.name.startsWith(".") &&
                        file.extension in listOf("", "txt", "text", "md", "usfm")
            }
            ?.map { it.name }
            ?.sorted()
            ?: listOf()
    }

    fun usfmFiles(identifier: String? = null): List<String> {
        val project = project(identifier) ?: return emptyList()
        return path?.let { path ->
            File("$path/${project.path}")
                .listFiles { file -> file.isFile && file.extension == "usfm" }
                ?.map { it.name }
                ?: emptyList()
        } ?: emptyList()
    }

    // TODO
//    fun config(projectIdentifier: String? = null): Map<String, Any?>? {
//        val project = project(projectIdentifier) ?: return null
//        if (project.configYaml == null) {
//            val filePath = "$path/${project.path}/config.yaml"
//            project.configYaml = try {
//                loadYamlObject(filePath)
//            } catch (e: Exception) {
//                addErrorMessage("Badly formed 'config.yaml' in $repoName: ${e.message}")
//                null
//            }
//        }
//        return project.configYaml
//    }
//
    // TODO
//    fun toc(projectIdentifier: String? = null): Map<String, Any?>? {
//        val project = project(projectIdentifier) ?: return null
//        if (project.tocYaml == null) {
//            val filePath = "$path/${project.path}/toc.yaml"
//            project.tocYaml = try {
//                loadYamlObject(filePath)
//            } catch (e: Exception) {
//                addErrorMessage("Badly formed 'toc.yaml' in $repoName: ${e.message}")
//                null
//            }
//        }
//        return project.tocYaml
//    }

    fun toYAMLManifest(): Manifest {
        return Manifest(
            dublinCore = dublinCore,
            checking = Checking(checkingEntity, checkingLevel),
            projects = listOf(rcProject)
        )
    }
}