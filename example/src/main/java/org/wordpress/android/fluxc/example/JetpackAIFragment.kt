package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.android.synthetic.main.fragment_jetpackai.*
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.example.ui.StoreSelectingFragment
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse.Error
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIQueryResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAITranscriptionRestClient.JetpackAITranscriptionResponse
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.jetpackai.JetpackAIStore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import javax.inject.Inject

class JetpackAIFragment : StoreSelectingFragment() {
    @Inject internal lateinit var store: JetpackAIStore

    @Inject internal lateinit var siteStore: SiteStore

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? =
        inflater.inflate(R.layout.fragment_jetpackai, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setHaikuButton()
        setTranscribeAudioButton()
        setJetpackAIQueryButton()
        setJetpackAIAssistantFeatureButton()
    }

    private fun setHaikuButton() {
        generate_haiku.setOnClickListener {
            selectedSite?.let {
                lifecycleScope.launch {
                    val result = store.fetchJetpackAICompletions(
                        site = it,
                        prompt = "Please make me a haiku",
                        feature = "fluxc-example"
                    )

                    when (result) {
                        is Success -> {
                            prependToLog("Haiku:\n${result.completion}")
                        }

                        is Error -> {
                            prependToLog("Error fetching haiku: ${result.message}")
                        }
                    }
                }
            }
        }
    }

    private fun setTranscribeAudioButton() {
        transcribe_audio.setOnClickListener {
            siteStore.sites[0].let {
                lifecycleScope.launch {
                    getAudioFile()?.let { file ->
                        val result = store.fetchJetpackAITranscription(
                            site = it,
                            feature = "fluxc-example",
                            audioFile = file
                        )

                        when (result) {
                            is JetpackAITranscriptionResponse.Success -> {
                                prependToLog("Transcribed:\n${result.model}")
                            }

                            is JetpackAITranscriptionResponse.Error -> {
                                prependToLog("Error transcribing audio: ${result.message}")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setJetpackAIQueryButton() {
        jetpack_ai_query.setOnClickListener {
            siteStore.sites[0].let {
                lifecycleScope.launch {
                    val result = store.fetchJetpackAIQuery(
                        site = it,
                        feature = "fluxc-example",
                        stream = false,
                        type = "voice-to-content-simple-draft",
                        role = "jetpack-ai",
                        message = "This is a test message"
                    )

                    when (result) {
                        is JetpackAIQueryResponse.Success -> {
                            val content = result.choices[0].message.content
                            prependToLog("Jetpack AI Query Processed:\n$content}")
                        }

                        is JetpackAIQueryResponse.Error -> {
                            prependToLog("Error post processing: ${result.message}")
                        }
                    }
                }
            }
        }
    }

    private fun setJetpackAIAssistantFeatureButton() {
        jetpack_ai_assistant_feature.setOnClickListener {
            siteStore.sites[0].let {
                lifecycleScope.launch {
                    val result = store.fetchJetpackAIAssistantFeature(site = it)

                    prependToLog("Jetpack AI Assistant Feature:\n$result")
                }
            }
        }
    }

    private fun getAudioFile(): File? {
        val result = runCatching {
            getFileFromAssets(requireContext())
        }

        return result.getOrElse {
            prependToLog("Error getting test audio file ${it.message}")
            null
        }
    }

    private fun getFileFromAssets(context: Context): File {
        val fileName = "jetpack-ai-transcription-test-audio-file.m4a"
        val file = File(context.filesDir, fileName)
        context.assets.open(fileName).use { inputStream ->
            copyInputStreamToFile(inputStream, file)
        }
        return file
    }

    private fun copyInputStreamToFile(inputStream: InputStream, outputFile: File) {
        FileOutputStream(outputFile).use { outputStream ->
            val buffer = ByteArray(KILO_BYTE)
            var length: Int
            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }
            outputStream.flush()
        }
        inputStream.close()
    }

    companion object {
        private const val KILO_BYTE = 1024
    }
}
