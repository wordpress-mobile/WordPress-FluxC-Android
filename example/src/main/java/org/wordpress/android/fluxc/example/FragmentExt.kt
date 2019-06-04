@file:JvmName("FragmentExtensions")
package org.wordpress.android.fluxc.example

import androidx.fragment.app.Fragment

/**
 * Shortcut for appending messages to the log in MainActivity
 */
fun androidx.fragment.app.Fragment.prependToLog(s: String) {
    (activity as? MainExampleActivity)?.prependToLog(s)
}

fun androidx.fragment.app.Fragment.replaceFragment(fragment: androidx.fragment.app.Fragment) {
    fragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, fragment)
            ?.addToBackStack(null)
            ?.commit()
}
