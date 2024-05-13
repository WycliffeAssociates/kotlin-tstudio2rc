package org.wycliffeassociates

import org.wycliffeassociates.resourcecontainer.entity.Checking
import org.wycliffeassociates.resourcecontainer.entity.DublinCore
import org.wycliffeassociates.resourcecontainer.entity.Language
import org.wycliffeassociates.resourcecontainer.entity.Project
import org.wycliffeassociates.resourcecontainer.entity.Source
import org.wycliffeassociates.tstudio2rc.RC
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataParserTest {

    @Test
    fun testManifest() {
        val dir = javaClass.classLoader.getResource("tstudio/ckb_2pe_text_reg").file
        val rc = RC(dir)
        val manifest = rc.rcManifest()
        val dublinCore = DublinCore(
            type = "book",
            conformsTo = "rc0.2",
            format = "text/usfm",
            identifier = "reg",
            title = "Regular",
            subject = "Bible",
            description = "",
            language = Language(
                direction = "rtl",
                identifier = "ckb",
                title = "سورانی"
            ),
            source = mutableListOf(
                Source(
                    identifier = "nav",
                    language = "arb",
                    version = "2012-05-17"
                )
            ),
            rights = "CC BY-SA 4.0",
            creator = "Unknown Creator",
            contributor = mutableListOf("Omid.Rahmani.baneh", "dana"),
            relation = mutableListOf(),
            publisher = "Door43",
            issued = LocalDate.now().toString(),
            modified = LocalDate.now().toString(),
            version = "1"
        )
        val project = Project(
            title = "2 Peter",
            versification = "kjv",
            identifier = "2pe",
            sort = 1,
            path = "./",
            categories = mutableListOf()
        )
        val checking = Checking(mutableListOf("Wycliffe Associates"), "1")

        assertEquals(dublinCore, manifest.dublinCore)
        assertEquals(mutableListOf(project), manifest.projects)
        assertEquals(checking, manifest.checking) // optional
    }
}