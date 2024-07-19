package org.wordpress.android.fluxc.example

import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager.OnBackStackChangedListener
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.wordpress.android.fluxc.example.databinding.ActivityExampleBinding
import javax.inject.Inject

class MainExampleActivity : AppCompatActivity(), OnBackStackChangedListener, HasAndroidInjector {
    @Inject lateinit var dispatchingAndroidInjector: DispatchingAndroidInjector<Any>
    private lateinit var binding: ActivityExampleBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = ActivityExampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportFragmentManager.addOnBackStackChangedListener(this@MainExampleActivity)

        if (savedInstanceState == null) {
            val mf = MainFragment()
            supportFragmentManager.beginTransaction().add(R.id.fragment_container, mf).commit()
        }

        binding.log.movementMethod = ScrollingMovementMethod()
        prependToLog("I'll log stuff here.")

        updateBackArrow()
    }

    fun prependToLog(s: String) {
        val output = s + "\n" + binding.log.text
        binding.log.text = output
    }

    override fun androidInjector(): AndroidInjector<Any> = dispatchingAndroidInjector

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackStackChanged() {
        updateBackArrow()
    }

    private fun updateBackArrow() {
        val showBackArrow = supportFragmentManager.backStackEntryCount > 0
        supportActionBar?.setDisplayHomeAsUpEnabled(showBackArrow)
    }
}
