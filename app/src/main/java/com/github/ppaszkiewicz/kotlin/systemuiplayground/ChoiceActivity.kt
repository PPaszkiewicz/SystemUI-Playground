package com.github.ppaszkiewicz.kotlin.systemuiplayground

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.github.ppaszkiewicz.kotlin.systemuiplayground.databinding.ChoiceBinding
import com.google.android.material.snackbar.Snackbar

class ChoiceActivity : AppCompatActivity(R.layout.choice) {
    lateinit var b: ChoiceBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ChoiceBinding.inflate(layoutInflater).let { b = it; b.root }
        )
        b.btnLegacy.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        b.btnModern.setOnClickListener {
            startActivity(Intent(this, SettingsActivity30::class.java))
        }
        b.btnAnim.setOnClickListener {
            startActivity(Intent(this, AnimActivity::class.java))
        }
        b.btnClear.setOnClickListener {
            PreferenceManager.getDefaultSharedPreferences(this).edit { clear() }
            Snackbar.make(b.root, "Clear ok", Snackbar.LENGTH_SHORT).show()
        }
    }
}