package org.wycliffeassociates

import java.io.File

class Project(private val rc: RC, project: Map<String, Any?>? = null) {

    private val project: MutableMap<String, Any?>

    init {
        this.project = project?.toMutableMap() ?: mutableMapOf()
        if (rc !is RC) {
            throw Exception("Missing RC parameter: rc")
        }
//        configYaml = null
//        tocYaml = null
    }

    val identifier: String
        get() = project.getOrDefault("identifier", "")
            .toString()
            .lowercase()
            .ifEmpty {
                project.getOrDefault("id", "")
                    .toString()
                    .lowercase()
                    .ifEmpty {
                        project.getOrDefault("project_id", "")
                            .toString()
                            .lowercase()
                            .ifEmpty { rc.resource.identifier }
                            ?: ""
                    }
            }

    val title: String
        get() = project.getOrDefault("title", "")
            .toString()
            .ifEmpty {
                project.getOrDefault("name", "")
                    .toString()
                    .ifEmpty {
                        val filePath = rc.path?.let { path ->
                            listOf(
                                "${path}/${this.path}/title.txt",
                                "${path}/title.txt",
                                "${path}/front/title.txt"
                            ).firstOrNull { File(it).exists() }
                        }
                        filePath?.let { readFile(it) } ?: project.getOrDefault("id", "").toString()
                    }
            }

    val path: String
        get() = project.getOrDefault("path", "")
            .toString()
            .ifEmpty {
                rc.path?.let {
                    if (File("${it}/content").isDirectory) "./content" else "./"
                } ?: "./"
            }

    val sort: String
        get() = project.getOrDefault("sort", "1").toString()

    val versification: String
        get() = project.getOrDefault("versification", "kjv").toString()

    val categories: List<String>
        get() = project.getOrDefault("categories", emptyList<String>()).toString().split(",")

    // TODO
//    fun toc(): String = rc.toc(identifier)

    // TODO
//    fun config(): String = rc.config(identifier)

    fun chapters(): List<String> = rc.chapters(identifier)

    fun chunks(chapterIdentifier: String? = null): List<String> = rc.chunks(identifier, chapterIdentifier)

    fun usfmFiles(): List<String> = rc.usfmFiles(identifier)

    fun asDict(): Map<String, Any?> = mapOf(
        "categories" to categories,
        "identifier" to identifier,
        "path" to path,
        "sort" to sort,
        "title" to title,
        "versification" to versification
    )

    fun rcProject(): RCProject {
        return RCProject(
            identifier = identifier,
            categories = categories,
            path = path,
            sort = sort.toInt(),
            title = title,
            versification = versification
        )
    }

    companion object {
        fun readFile(filePath: String): String {
            // Implement file reading logic here
            return ""
        }
    }
}