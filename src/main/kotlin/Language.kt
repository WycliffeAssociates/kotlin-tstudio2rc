package org.wycliffeassociates

data class Language(private val rc: Any, private val language: Map<String, String>) {

    val identifier: String
        get() {
            return language.getOrDefault("identifier", "")
                .lowercase()
                .ifEmpty {
                    language.getOrDefault("slug", "")
                        .lowercase()
                        .ifEmpty {
                            language.getOrDefault("id", "")
                                .lowercase()
                                .ifEmpty { "en" }
                        }
                }
        }

    val direction: String
        get() = language.getOrDefault("direction", language.getOrDefault("dir", "ltr"))

    val title: String
        get() = language.getOrDefault("title", language.getOrDefault("name", "English"))
}
