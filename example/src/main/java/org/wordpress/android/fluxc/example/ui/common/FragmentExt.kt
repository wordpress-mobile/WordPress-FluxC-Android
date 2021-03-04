package org.wordpress.android.fluxc.example.ui.common

import androidx.fragment.app.Fragment
import org.wordpress.android.fluxc.example.SiteSelectorDialog
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog

fun Fragment.showStoreSelectorDialog(selectedPos: Int, listener: StoreSelectorDialog.Listener) {
    parentFragmentManager.let { fm ->
        val dialog = StoreSelectorDialog.newInstance(listener, selectedPos)
        dialog.show(fm, "StoreSelectorDialog")
    }
}

fun Fragment.showSiteSelectorDialog(selectedPos: Int, listener: SiteSelectorDialog.Listener) {
    parentFragmentManager.let { fm ->
        val dialog = SiteSelectorDialog.newInstance(listener, selectedPos)
        dialog.show(fm, "SiteSelectorDialog")
    }
}
