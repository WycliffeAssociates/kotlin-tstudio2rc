package org.wycliffeassociates.tstudio2rc

import org.wycliffeassociates.resourcecontainer.entity.Checking
import org.wycliffeassociates.resourcecontainer.entity.DublinCore
import org.wycliffeassociates.resourcecontainer.entity.Language
import org.wycliffeassociates.resourcecontainer.entity.Project
import org.wycliffeassociates.resourcecontainer.entity.Source
import java.io.File
import java.time.LocalDate
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class MetadataTest {

    @Test
    fun testManifest() {
        val projectDir = prepareProjectDir()
        val tstudioMetadata = TstudioMetadata(projectDir.invariantSeparatorsPath)
        val dublinCore = DublinCore(
            type = "book",
            conformsTo = "rc0.2",
            format = "text/usfm",
            identifier = "ulb",
            title = "Unlocked Literal Bible",
            subject = "Bible",
            description = "",
            language = Language(
                direction = "ltr",
                identifier = "aac",
                title = "Ari"
            ),
            source = mutableListOf(
                Source(
                    identifier = "ulb",
                    language = "hi",
                    version = "5"
                )
            ),
            rights = "CC BY-SA 4.0",
            creator = "Unknown Creator",
            contributor = mutableListOf(),
            relation = mutableListOf(),
            publisher = "Door43",
            issued = LocalDate.now().toString(),
            modified = LocalDate.now().toString(),
            version = "1"
        )
        val project = Project(
            title = "Jude",
            versification = "kjv",
            identifier = "jud",
            sort = 1,
            path = "./",
            categories = emptyList(),
            config = null
        )
        val checking = Checking(mutableListOf("Wycliffe Associates"), "1")

        val manifest = tstudioMetadata.rcManifest()

        assertEquals(dublinCore, manifest.dublinCore)
        assertEquals(mutableListOf(project), manifest.projects)
        assertEquals(checking, manifest.checking) // optional
    }

    private fun prepareProjectDir(): File {
        val path = javaClass.classLoader.getResource("aac_jud_text_ulb.tstudio").file
        val tempDir = createTempDirectory().toFile()
        unzipFile(File(path), tempDir)

        return tempDir.apply { deleteOnExit() }
    }
}