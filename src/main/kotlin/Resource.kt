package org.wycliffeassociates

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class Resource(private val rc: RC, resource: Map<String, Any?>) {

    private val resource: Map<String, Any?>
    private var _language: Language? = null

    init {
        require(rc is RC) { "Invalid RC instance" }
        this.resource = resource
        if (resource !is Map<*, *>) {
            throw Exception("Missing dict parameter: resource")
        }
        _language = null
    }

    val conformsTo: String
        get() = resource.getOrDefault("conformsto", "rc0.2") as? String ?: "rc0.2"

    val format: String?
        get() {
            val oldFormat = resource.getOrDefault("format", null) as? String
            return if (oldFormat != null && !oldFormat.contains("/")) {
                "text/${oldFormat.lowercase()}"
            } else {
                oldFormat ?: resource.getOrDefault("content_mime_type", null) as? String
                ?: rc.manifest.getOrDefault("content_mime_type", null) as? String
                ?: rc.manifest.getOrDefault("format", null) as? String
                ?: if (rc.usfmFiles().isNotEmpty()) "text/usfm" else null
            }
        }

    val fileExt: String
        get() {
            val formatExtensions = mapOf(
                "text/usx" to "usx",
                "text/usfm" to "usfm",
                "text/usfm3" to "usfm",
                "text/markdown" to "md",
                "text/tsv" to "tsv"
            )
            return formatExtensions[format] ?: (if (identifier == "bible") "usfm" else "txt")
        }

    val type: String
        get() {
            val resourceType = resource.getOrDefault("type", null) as? String
            return if (resourceType != null) {
                resourceType.lowercase()
            } else if (fileExt == "usfm") {
                if (rc.usfmFiles().isNotEmpty()) "bundle" else "book"
            } else {
                "book"
            }
        }

    val identifier: String?
        get() = resource.getOrDefault("identifier", null) as? String
            ?: resource.getOrDefault("id", null) as? String
            ?: (resource.getOrDefault("type", null) as? Map<String, Any?>)?.getOrDefault("id", null) as? String
            ?: resource.getOrDefault("slug", null) as? String

    val title: String?
        get() = resource.getOrDefault("title", null) as? String
            ?: resource.getOrDefault("name", null) as? String
            ?: identifier

    val subject: String
        get() = resource.getOrDefault("subject", title) as? String ?: title ?: ""

    val description: String
        get() = resource.getOrDefault("description", title) as? String ?: title ?: ""

    val relation: List<String>
        get() = resource.getOrDefault("relation", emptyList<String>()) as? List<String> ?: emptyList()

    val publisher: String
        get() = resource.getOrDefault("publisher", "Door43") as? String ?: "Door43"

    val issued: String
        get() {
            val issuedResult = resource.getOrDefault("issued", null)
            // TODO: parse timestamp if exists
            return issuedResult as? String ?: LocalDate.now().toString()
        }

    val modified: String
        get() {
            val modifiedResult = resource.getOrDefault("modified", null)
            // TODO: parse modified timestamp if exists
            return modifiedResult as? String ?: LocalDate.now().toString()
        }

    val rights: String
        get() = resource.getOrDefault("rights", "CC BY-SA 4.0") as? String ?: "CC BY-SA 4.0"

    val creator: String
        get() = resource.getOrDefault("creator", "Unknown Creator") as? String ?: "Unknown Creator"

    val language: Language
        get() {
            if (_language == null) {
                _language = (resource.getOrDefault("language", null) as? Map<String, String>)
                    ?.let { Language(rc, it) }
                    ?: (resource.getOrDefault("target_language", null) as? Map<String, String>)
                        ?.let { Language(rc, it) }
                            ?: (rc.manifest.getOrDefault("target_language", null) as? Map<String, String>)
                        ?.let { Language(rc, it) }
                            ?: Language(rc, mapOf("identifier" to "en", "title" to "English", "direction" to "ltr"))
            }
            return _language!!
        }

    val contributor: List<String>
        get() {
            val contributors = mutableListOf<String>()
            resource.getOrDefault("contributor", null)
                ?.let { contributors.addAll(it as? List<String> ?: emptyList()) }
            resource.getOrDefault("translators", null)
                ?.let {
                    when (it) {
                        is List<*> -> it.forEach { translator ->
                            when (translator) {
                                is Map<*, *> -> contributors.add(translator["name"] as? String ?: "")
                                is String -> contributors.add(translator)
                                else -> Unit
                            }
                        }
                        else -> Unit
                    }
                }
            return contributors
        }

    val source: List<Map<String, String>>
        get() {
            val sources = mutableListOf<Map<String, String>>()
            resource.getOrDefault("source", null)
                ?.let { sources.addAll(it as? List<Map<String, String>> ?: emptyList()) }
            if (sources.isEmpty()) {
                val sourceTranslations = resource.getOrDefault("source_translations", null)
                    ?: (resource.getOrDefault("status", null) as? Map<String, Any?>)
                        ?.getOrDefault("source_translations", null)
                sourceTranslations?.let {
                    when (it) {
                        is List<*> -> it.forEach { sourceTranslation ->
                            val source = mutableMapOf<String, String>()
                            (sourceTranslation as? Map<String, Any?>)?.let { st ->
                                st.getOrDefault("resource_id", null)
                                    ?.let { source["identifier"] = it as? String ?: "" }
                                    ?: st.getOrDefault("resource_slug", null)
                                        ?.let { source["identifier"] = it as? String ?: "" }
                                st.getOrDefault("language_id", null)
                                    ?.let { source["language"] = it as? String ?: "" }
                                    ?: st.getOrDefault("language_slug", null)
                                        ?.let { source["language"] = it as? String ?: "" }
                                st.getOrDefault("version", null)
                                    ?.let { source["version"] = it as? String ?: "" }
                            }
                            if (source.isNotEmpty()) sources.add(source)
                        }
                        else -> Unit
                    }
                }
            }
            return sources
        }

    val version: String
        get() = resource.getOrDefault("version", "1") as? String ?: "1"

//    private fun Date.formatDate(): String {
//        val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
//        return formatter.print(this)
//    }

    companion object {
        private fun loadYamlObject(filePath: String): Map<String, Any?>? {
            // Implement YAML file loading logic here
            return null
        }

        private fun loadJsonObject(filePath: String): Map<String, Any?>? {
            // Implement JSON file loading logic here
            return null
        }
    }
}