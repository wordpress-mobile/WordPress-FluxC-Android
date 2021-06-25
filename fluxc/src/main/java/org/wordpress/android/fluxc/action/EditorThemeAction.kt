package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.EditorThemeStore.FetchEditorThemePayload
import org.wordpress.android.fluxc.store.EditorThemeStore.UpdateEditorThemePayload

@ActionEnum
enum class EditorThemeAction : IAction {
    @Action(payloadType = FetchEditorThemePayload::class)
    FETCH_EDITOR_THEME,
    @Action(payloadType = UpdateEditorThemePayload::class)
    UPDATE_EDITOR_THEME
}
