package org.wycliffeassociates

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.wycliffeassociates.entity.ProjectManifest
import org.wycliffeassociates.resourcecontainer.entity.Checking
import org.wycliffeassociates.resourcecontainer.entity.DublinCore
import org.wycliffeassociates.resourcecontainer.entity.Language
import org.wycliffeassociates.resourcecontainer.entity.Manifest
import java.io.File

class RC(
    directory: String? = null,
    repoName: String? = null,
    manifest: Map<String, Any?>? = null
) {
    companion object {
        const val currentVersion = "0.2"
    }

    private var _dir: String? = directory
    private var loadedManifestFile = false
    private var _manifest: Map<String, Any?>? = manifest
    private var _repoName: String? = repoName
    private var _resource: Resource? = null
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

    fun asDict(): Map<String, Any?> = mapOf(
        "dublin_core" to mapOf(
            "type" to resource.type,
            "conformsto" to resource.conformsTo,
            "format" to resource.format,
            "identifier" to resource.identifier,
            "title" to resource.title,
            "subject" to resource.subject,
            "description" to resource.description,
            "language" to mapOf(
                "identifier" to resource.language.identifier,
                "title" to resource.language.title,
                "direction" to resource.language.direction
            ),
            "source" to resource.source,
            "rights" to resource.rights,
            "creator" to resource.creator,
            "contributor" to resource.contributor,
            "relation" to resource.relation,
            "publisher" to resource.publisher,
            "issued" to resource.issued,
            "modified" to resource.modified,
            "version" to resource.version
        ),
        "checking" to mapOf(
            "checking_entity" to checkingEntity,
            "checking_level" to checkingLevel
        ),
        "projects" to projectsAsDict
    )

    fun toYAMLManifest(): Manifest {
        val dc = DublinCore(
            type = resource.type,
            conformsTo = resource.conformsTo,
            format = resource.format,
            identifier = resource.identifier,
            title = resource.title,
            subject = resource.subject,
            description = resource.description,
            language = Language(
                identifier = resource.language.identifier,
                title = resource.language.title,
                direction = resource.language.direction
            ),
            source = mutableListOf(),//resource.source, // TODO
            rights = resource.rights,
            creator = resource.creator,
            contributor = resource.contributor.toMutableList(),
            relation = resource.relation.toMutableList(),
            publisher = resource.publisher,
            issued = resource.issued,
            modified = resource.modified,
            version = resource.version
        )
        Manifest(
            dublinCore = dc,
            checking = Checking(),
            projects = projectsAsDict
        )

        throw NotImplementedError()//  TODO
    }

    val path: String
        get() = _dir?.trimEnd('/') ?: ""

    val repoName: String
        get() = _repoName ?: path.takeIf { it.isNotEmpty() }?.let { File(it).name } ?: ""

    val resource: Resource
        get() = _resource ?: let {
            val resource = rawManifest.getOrDefault("dublin_core", null)
                ?: rawManifest.getOrDefault("resource", null)
                ?: rawManifest
            _resource = Resource(this, resource as? Map<String, Any?> ?: mapOf())
            _resource!!
        }

    val checkingEntity: List<String>
        get() = rawManifest["checking"]
//            .getOrDefault("checking_entity", listOf("Wycliffe Associates"))
                as? List<String>
            ?: listOf("Wycliffe Associates")

    val checkingLevel: String
        get() = rawManifest.getOrDefault("checking", null)
//            .getOrDefault("checking_level", "1")
                as? String
            ?: "1"

    val projects: List<Project>
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


    val projectsAsDict: List<RCProject>
        get() = projects.map { it.rcProject() }
//        get() = projects.map { it.asDict() }

    fun project(identifier: String? = null): Project? {
        return identifier?.let { projects.find { it.identifier == identifier } }
            ?: projects.singleOrNull()
            ?: if (projects.isEmpty()) Project(this) else null
    }

    val projectCount: Int
        get() = projects.size

    val projectIds: List<String>
        get() = projects.map { it.identifier }

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
        return path?.let { path ->
            File("$path/${project.path}/$chapterId")
                .listFiles { file ->
                    file.isFile &&
                            !file.name.startsWith(".") &&
                            file.extension in listOf("", "txt", "text", "md", "usfm")
                }
                ?.map { it.name }
                ?.sorted()
                ?: emptyList()
        } ?: emptyList()
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
}