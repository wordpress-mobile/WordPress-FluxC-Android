package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.EditorSettingsAction
import org.wordpress.android.fluxc.action.EditorSettingsAction.FETCH_EDITOR_SETTINGS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.generated.EditorThemeActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.persistence.RawEditorSettingsSqlUtils
import org.wordpress.android.fluxc.store.EditorThemeStore.UpdateEditorThemePayload
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Error
import org.wordpress.android.fluxc.store.ReactNativeFetchResponse.Success
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
// import org.wordpress.android.util.helpers.Version
import javax.inject.Inject
import javax.inject.Singleton

private const val EDITOR_SETTINGS_REQUEST_PATH = "__experimental/wp-block-editor/v1/settings?context=mobile"

@Singleton
class RawEditorSettingsStore
@Inject constructor(
    private val reactNativeStore: ReactNativeStore,
    private val coroutineEngine: CoroutineEngine,
    dispatcher: Dispatcher
) : Store(dispatcher) {
    private val rawEditorSettingsSqlUtils = RawEditorSettingsSqlUtils()

    class FetchEditorSettingsPayload @JvmOverloads constructor(val site: SiteModel) :
            Payload<BaseNetworkError>()

    data class OnEditorSettingsChanged(
        val rawEditorSettings: String?,
        val siteId: Int,
        val causeOfChange: EditorSettingsAction
    ) : Store.OnChanged<EditorSettingsError>() {
        constructor(error: EditorSettingsError, causeOfChange: EditorSettingsAction) :
                this(rawEditorSettings = null, siteId = -1, causeOfChange = causeOfChange) {
            this.error = error
        }
    }
    class EditorSettingsError(var message: String? = null) : OnChangedError

    fun getRawEditorSettingsForSite(site: SiteModel): String {
        return rawEditorSettingsSqlUtils.getRawEditorSettingsForSite(site)
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? EditorSettingsAction ?: return
        when (actionType) {
            FETCH_EDITOR_SETTINGS -> {
                coroutineEngine.launch(
                        AppLog.T.API,
                        this,
                        RawEditorSettingsStore::class.java.simpleName + ": On FETCH_EDITOR_SETTINGS"
                ) {
                    val payload = action.payload as FetchEditorSettingsPayload
                    handleFetchEditorSettings(payload.site, actionType)
                }
            }
        }
    }

    override fun onRegister() {
        AppLog.d(AppLog.T.API, RawEditorSettingsStore::class.java.simpleName + " onRegister")
    }

    private suspend fun handleFetchEditorSettings(site: SiteModel, action: EditorSettingsAction) {
        val response = reactNativeStore.executeRequest(site, EDITOR_SETTINGS_REQUEST_PATH, false)

        when (response) {
            is Success -> {
                val noEditorSettingsError = OnEditorSettingsChanged(EditorSettingsError("Response does not contain editor settings"), action)
                if (response.result == null || !response.result.isJsonObject) {
                    emitChange(noEditorSettingsError)
                    return
                }

                val editorSettings = response.result.asJsonObject
                if (editorSettings == null) {
                    emitChange(noEditorSettingsError)
                    return
                }

                val rawEditorSettings = editorSettings.toString()
                val existingRawEditorSettings = rawEditorSettingsSqlUtils.getRawEditorSettingsForSite(site)

                if (rawEditorSettings != existingRawEditorSettings) {
                    rawEditorSettingsSqlUtils.replaceRawEditorSettingsForSite(site, rawEditorSettings)
                    val onChanged = OnEditorSettingsChanged(rawEditorSettings, site.id, action)
                    emitChange(onChanged)
                }

                // Update the theme editor store with the fetched theme data
                val payload = UpdateEditorThemePayload(site, editorSettings)
                mDispatcher.dispatch(EditorThemeActionBuilder.newUpdateEditorThemeAction(payload))
            }
            is Error -> {
                val onChanged = OnEditorSettingsChanged(EditorSettingsError(response.error.message), action)
                emitChange(onChanged)
            }
        }
    }
}
