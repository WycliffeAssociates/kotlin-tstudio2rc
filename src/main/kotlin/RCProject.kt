package org.wycliffeassociates

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
data class RCProject(
    var title: String = "",
    var versification: String = "",
    var identifier: String = "",
    var sort: Int = 0,
    var path: String = "",
    var categories: List<String> = arrayListOf()
)
