package org.wycliffeassociates.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown=true)
data class ProjectManifest(
    @JsonProperty("package_version")
    val packageVersion: Int,
    val format: String,
    val generator: Generator,
    @JsonProperty("target_language")
    val targetLanguage: TargetLanguage,
    val project: Project,
    val type: Type,
    val resource: Resource,
    @JsonProperty("source_translations")
    val sourceTranslations: List<SourceTranslation>,
    val translators: List<String>,
    @JsonProperty("finished_chunks")
    val finishedChunks: List<String>
)

data class Generator(
    val name: String,
    val build: String
)

data class TargetLanguage(
    val id: String,
    val name: String,
    val direction: String
)

data class Project(
    val id: String,
    val name: String
)

data class Type(
    val id: String,
    val name: String
)

data class Resource(
    val id: String,
    val name: String
)

data class SourceTranslation(
    @JsonProperty("language_id")
    val languageId: String,
    @JsonProperty("resource_id")
    val resourceId: String,
    @JsonProperty("checking_level")
    val checkingLevel: String,
    @JsonProperty("date_modified")
    val dateModified: String,
    val version: String
)