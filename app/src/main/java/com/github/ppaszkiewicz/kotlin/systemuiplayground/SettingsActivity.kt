package com.github.ppaszkiewicz.kotlin.systemuiplayground

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Activity that lets you control most if not all window and system ui visibility flags.
 *
 * A lot of this is "deprecated" as of API 30.
 */
class SettingsActivity : SettingsActivityBase() {

    override fun instantiateFragment() = SettingsFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b.toolbar.setOnApplyWindowInsetsListener { v, insets ->
            v.updatePadding(top = insets.systemWindowInsetTop)
            insets
        }
        b.settings.setOnApplyWindowInsetsListener { v, insets ->
            v.updatePadding(
                top = insets.systemWindowInsetTop,
                //     bottom = insets.systemWindowInsetBottom
            )
            // a little ugly, just send inset to preference recycler view instead of propagating it
            b.settings.findViewById<RecyclerView>(R.id.recycler_view)
                .updatePadding(bottom = insets.systemWindowInsetBottom)
            insets
        }
        b.topTextContainer.setOnApplyWindowInsetsListener { v, insets ->
            v.updatePadding(top = insets.systemWindowInsetTop)
            ((v as ViewGroup).getChildAt(0) as TextView).text = "${insets.systemWindowInsetTop}px"
            insets
        }
        b.bottomTextContainer.setOnApplyWindowInsetsListener { v, insets ->
            v.updatePadding(bottom = insets.systemWindowInsetBottom)
            ((v as ViewGroup).getChildAt(0) as TextView).text =
                "${insets.systemWindowInsetBottom}px"
            insets
        }
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        val window
            get() = requireActivity().window

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            //general system ui
            findPreference<SwitchPreferenceCompat>("fit_windows").listen {
                (requireActivity() as SettingsActivityBase).b.rootFrame.apply {
                    if (it != fitsSystemWindows) {
                        fitsSystemWindows = it as Boolean
                        requestApplyInsets()
                    }
                }
            }
            findPreference<SwitchPreferenceCompat>("stable_lay").systemUiFlag(View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
            findPreference<SwitchPreferenceCompat>("low_profile").systemUiFlag(View.SYSTEM_UI_FLAG_LOW_PROFILE)
            findPreference<SwitchPreferenceCompat>("immersive").systemUiFlag(View.SYSTEM_UI_FLAG_IMMERSIVE)
            findPreference<SwitchPreferenceCompat>("immersive_sticky").systemUiFlag(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            findPreference<SwitchPreferenceCompat>("lay_fullscreen").systemUiFlag(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            // general window flags
            findPreference<SwitchPreferenceCompat>("fullscreen").windowFlag(LayoutParams.FLAG_FULLSCREEN)
            findPreference<SwitchPreferenceCompat>("not_fullscreen").windowFlag(LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)
            findPreference<SwitchPreferenceCompat>("nolimit").windowFlag(LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            findPreference<SwitchPreferenceCompat>("bounds").windowFlag(LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            findPreference<SwitchPreferenceCompat>("insetdecor").windowFlag(LayoutParams.FLAG_LAYOUT_INSET_DECOR)
            findPreference<SwitchPreferenceCompat>("draw").windowFlag(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)

            // navigation
            findPreference<SwitchPreferenceCompat>("n_hide").systemUiFlag(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            findPreference<SwitchPreferenceCompat>("n_lay_hide").systemUiFlag(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            findPreference<SwitchPreferenceCompat>("n_light").systemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
            findPreference<SwitchPreferenceCompat>("n_trans").windowFlag(LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            findPreference<SwitchPreferenceCompat>("n_contr").listen {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isNavigationBarContrastEnforced = it as Boolean
                }
            }
            findPreference<ListPreference>("n_color").listen {
                window.navigationBarColor = getColor(it as String)
            }
            findPreference<ListPreference>("n_divider_color").listen {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                    window.navigationBarDividerColor = getColor(it as String)
                }
            }

            // statusbar
            findPreference<SwitchPreferenceCompat>("s_light").systemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            findPreference<SwitchPreferenceCompat>("s_trans").windowFlag(LayoutParams.FLAG_TRANSLUCENT_STATUS)
            findPreference<SwitchPreferenceCompat>("s_contr").listen {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    window.isStatusBarContrastEnforced = it as Boolean
                }
            }
            findPreference<ListPreference>("s_color").listen {
                window.statusBarColor = getColor(it as String)
            }
        }


        // add uncancellable listener AND force invoke it
        fun Preference?.listen(l: (value: Any) -> Unit) {
            this!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
                l(v)
                true
            }
            this.sharedPreferences.all[key]?.let { l(it) }
        }


        fun SwitchPreferenceCompat?.systemUiFlag(flag: Int) {
            this!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
                if (v == true) {
                    window.addSystemUiFlag(flag)
                } else {
                    window.clearSystemUiFlag(flag)
                }
                true
            }
            trackChanges { window.decorView.systemUiVisibility and flag == flag }
        }

        fun SwitchPreferenceCompat?.windowFlag(flag: Int) {
            this!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
                if (v == true) {
                    window.addFlags(flag)
                } else {
                    window.clearFlags(flag)
                }
                true
            }
            trackChanges { window.attributes.flags and flag == flag }
        }

        // track changes: start by invalidating self
        private fun SwitchPreferenceCompat.trackChanges(chk: () -> Boolean) {
            sharedPreferences.getBoolean(key, false).let {
                onPreferenceChangeListener.onPreferenceChange(this, it)
            }
            lifecycleScope.launch {
                while (isActive) {
                    delay(500)
                    val hasFlag = chk()
                    if (isChecked != hasFlag) {
                        val l = onPreferenceChangeListener
                        onPreferenceChangeListener = null
                        isChecked = hasFlag
                        onPreferenceChangeListener = l
                    }
                }
            }
        }

        fun getColor(color: String) = ContextCompat.getColor(
            requireContext(), when (color) {
                "Blue" -> R.color.blue
                "Green" -> R.color.green
                "Yellow50" -> R.color.yellow50
                "Black" -> R.color.black
                "White" -> R.color.white
                "Translucent" -> R.color.translucent
                "Transparent" -> R.color.transparent
                else -> android.R.color.background_dark
            }
        )
    }
}


fun Window.addSystemUiFlag(flag: Int) {
    decorView.systemUiVisibility = decorView.systemUiVisibility or flag
}

fun Window.clearSystemUiFlag(flag: Int) {
    decorView.systemUiVisibility = decorView.systemUiVisibility and flag.inv()
}

fun View.addSystemUiFlag(flag: Int) {
    systemUiVisibility = systemUiVisibility or flag
}

fun View.clearSystemUiFlag(flag: Int) {
    systemUiVisibility = systemUiVisibility and flag.inv()
}