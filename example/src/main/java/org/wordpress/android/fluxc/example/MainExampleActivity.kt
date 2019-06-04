package org.wordpress.android.fluxc.example

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_example.*
import javax.inject.Inject

class MainExampleActivity : FragmentActivity(), HasSupportFragmentInjector {
    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_example)

        if (savedInstanceState == null) {
            val mf = MainFragment()
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, mf).commit()
        }
        log.movementMethod = ScrollingMovementMethod()
        prependToLog("I'll log stuff here.")
    }

    fun prependToLog(s: String) {
        val output = s + "\n" + log.text
        log.text = output
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector
}
