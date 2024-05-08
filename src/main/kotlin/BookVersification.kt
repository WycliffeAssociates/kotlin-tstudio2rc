package org.wycliffeassociates

import com.fasterxml.jackson.annotation.JsonProperty

data class BookVersification(
    @JsonProperty("en_name")
    val enName: String = "",
    val chapters: Int? = null,
    val verses: List<Int> = listOf(),
    @JsonProperty("usfm_number")
    val usfmNumber: String = "",
    val sort: Int
)