package org.wordpress.android.fluxc.example

import android.app.Activity
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_example.*

class MainExampleActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example)

        if (savedInstanceState == null) {
            val mf = MainFragment()
            fragmentManager.beginTransaction().add(R.id.fragment_container, mf).commit()
        }
        log.movementMethod = ScrollingMovementMethod()
        prependToLog("I'll log stuff here.")
    }

    fun prependToLog(s: String) {
        val output = s + "\n" + log.text
        log.text = output
    }
}
