@file:JvmName("FragmentExtensions")
package org.wordpress.android.fluxc.example

import android.support.v4.app.Fragment

/**
 * Shortcut for appending messages to the log in MainActivity
 */
fun Fragment.prependToLog(s: String) {
    (activity as? MainExampleActivity)?.prependToLog(s)
}
