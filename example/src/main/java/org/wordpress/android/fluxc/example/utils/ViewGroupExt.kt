package org.wordpress.android.fluxc.example.utils

import android.view.View
import android.view.ViewGroup
import android.widget.Button

fun ViewGroup.toggleSiteDependentButtons(enabled: Boolean = true) =
        childViewsAsSequence()
                .filter { it is Button }
                .forEach { it.isEnabled = enabled }

fun ViewGroup.childViewsAsSequence(): Sequence<View> =
        mutableListOf<View>().apply {
                for (it in 0 until childCount) {
                        add(getChildAt(it))
                }
        }.asSequence()
