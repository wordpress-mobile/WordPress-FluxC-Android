package org.wordpress.android.fluxc.persistence

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.network.rest.wpcom.bloggingprompts.BloggingPromptsUtils
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao
import org.wordpress.android.fluxc.persistence.bloggingprompts.BloggingPromptsDao.BloggingPromptEntity
import java.io.IOException

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class BloggingPromptsDaoTest {
    private lateinit var prompts: BloggingPromptsDao
    private lateinit var db: WPAndroidDatabase

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
                context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()
        prompts = db.bloggingPromptsDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `test prompt insert and update`(): Unit = runBlocking {
        // when
        var prompt = generateBloggingPrompt()
        prompts.insert(listOf(prompt))

        // then
        var observedProduct = prompts.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(prompt)

        // when
        prompt = observedProduct.copy(text = "updated text")
        prompts.insert(listOf(prompt))

        // then
        observedProduct = prompts.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(prompt)
    }

    @Test
    fun `test prompt insert and update BloggingPromptModel with site Id`(): Unit = runBlocking {
        // when
        var promptEntity = generateBloggingPrompt()

        var prompt = promptEntity.toBloggingPrompt()
        prompts.insertForSite(localSideId, listOf(prompt))

        // then
        var observedProduct = prompts.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(promptEntity)

        // when
        promptEntity = promptEntity.copy(text = "updated text")
        prompt = promptEntity.toBloggingPrompt()
        prompts.insertForSite(localSideId, listOf(prompt))

        // then
        observedProduct = prompts.getPrompt(localSideId, prompt.id).first().first()
        assertThat(observedProduct).isEqualTo(promptEntity)
    }

    @Test
    fun `BloggingPromptEntity correctly converts to BloggingPromptModel`(): Unit = runBlocking {
        val promptEntity = generateBloggingPrompt()
        val prompt = promptEntity.toBloggingPrompt()

        assertThat(promptEntity.id).isEqualTo(prompt.id)
        assertThat(promptEntity.text).isEqualTo(prompt.text)
        assertThat(promptEntity.title).isEqualTo(prompt.title)
        assertThat(promptEntity.content).isEqualTo(prompt.content)
        assertThat(promptEntity.date).isEqualTo(prompt.date)
        assertThat(promptEntity.isAnswered).isEqualTo(prompt.isAnswered)
        assertThat(promptEntity.respondentsCount).isEqualTo(prompt.respondentsCount)
        assertThat(promptEntity.respondentsAvatars).isEqualTo(prompt.respondentsAvatars)
    }

    @Test
    fun `BloggingPromptModel correctly converts to BloggingPromptEntity`(): Unit = runBlocking {
        val promptEntity = generateBloggingPrompt()
        val prompt = promptEntity.toBloggingPrompt()

        val convertedEntity = BloggingPromptEntity.from(localSideId, prompt)

        assertThat(promptEntity).isEqualTo(convertedEntity)
    }

    companion object {
        private const val localSideId = 1234

        private fun generateBloggingPrompt() = BloggingPromptEntity(
                id = 1,
                siteLocalId = localSideId,
                text = "Cast the movie of your life.",
                title = "Prompt Title",
                content = "content of the prompt",
                date = BloggingPromptsUtils.stringToDate("2015-01-12"),
                isAnswered = false,
                respondentsCount = 5,
                respondentsAvatars = emptyList()
        )
    }
}
