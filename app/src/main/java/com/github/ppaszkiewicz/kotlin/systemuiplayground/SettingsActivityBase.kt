package com.github.ppaszkiewicz.kotlin.systemuiplayground

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.github.ppaszkiewicz.kotlin.systemuiplayground.databinding.SettingsActivityBinding

abstract class SettingsActivityBase : AppCompatActivity() {
    abstract fun instantiateFragment(): Fragment

    lateinit var b: SettingsActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            SettingsActivityBinding.inflate(layoutInflater).let { b = it; b.root }
        )
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings, instantiateFragment())
                .commit()
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // default apply inset behavior is to dispatch to first child that fitsSystemWindows;
        // alter it so its dispatched to all
        b.rootFrame.setOnApplyWindowInsetsListener { v, insets ->
            (v as ViewGroup).children.forEach {
                if (it.fitsSystemWindows) it.dispatchApplyWindowInsets(insets)
            }
            insets
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.dialog -> {
                BottomDialogFragment().show(supportFragmentManager, "BOTTOM")
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}