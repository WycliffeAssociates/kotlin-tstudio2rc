package org.wycliffeassociates.tstudio2rc

import org.wycliffeassociates.resourcecontainer.ResourceContainer
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

class ConverterTest {

    private val outputDir = createTempDirectory("test").toFile()
        .apply { deleteOnExit() }

    private val dublinCore = DublinCore(
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
        creator = "BTT-Writer",
        contributor = mutableListOf(),
        relation = mutableListOf(),
        publisher = "Door43",
        issued = LocalDate.now().toString(),
        modified = LocalDate.now().toString(),
        version = "1"
    )
    private val project = Project(
        title = "Jude",
        versification = "ulb",
        identifier = "jud",
        sort = 65,
        path = "./66-JUD.usfm",
        categories = listOf("bible-nt"),
        config = null
    )
    private val checking = Checking(mutableListOf("Wycliffe Associates"), "1")

    @Test
    fun testConvertTsFile() {
        val input = getResourceFile()
        val result = ConvertUseCase().convertToOratureFile(input, outputDir)
        ResourceContainer.load(result).use { rc ->
            assertEquals(dublinCore, rc.manifest.dublinCore)
            assertEquals(mutableListOf(project), rc.manifest.projects)
            assertEquals(checking, rc.manifest.checking)        }
    }

    private fun getResourceFile(): File {
        val path = javaClass.classLoader.getResource("aac_jud_text_ulb.tstudio").file
        return File(path)
    }
}