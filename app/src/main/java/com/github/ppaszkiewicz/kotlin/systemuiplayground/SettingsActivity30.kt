package com.github.ppaszkiewicz.kotlin.systemuiplayground

import android.os.Bundle
import android.view.View
import android.view.WindowManager.LayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
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
 * Activity that shows off new [WindowInsetsCompat] and [WindowInsetsControllerCompat].
 */
class SettingsActivity30 : SettingsActivityBase() {
    var lastInsets: WindowInsetsCompat? = null

    override fun instantiateFragment() = SettingsFragment30()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setOnApplyWindowInsetsListenerCompat is helper extension (bottom of file)
        b.toolbar.setOnApplyWindowInsetsListenerCompat { v, insets ->
            v.updatePadding(top = insets.getInsets(Type.systemBars()).top)
            insets
        }
        b.settings.setOnApplyWindowInsetsListenerCompat { v, insets ->
            lastInsets = insets // store bc fragment might want to access them
            v.updatePadding(
                top = insets.getInsets(Type.systemBars()).top,
                //    bottom = insets.getInsets(Type.systemBars()).bottom
            )
            // a little ugly, just send inset to preference recycler view instead of propagating it
            b.settings.findViewById<RecyclerView>(R.id.recycler_view).updatePadding(
                bottom = insets.getInsets(Type.systemBars()).bottom
            )
            insets
        }
        // inset display helpers
        b.topTextContainer.setOnApplyWindowInsetsListenerCompat { v, insets ->
            v.updatePadding(top = insets.getInsetsIgnoringVisibility(Type.systemBars()).top)
            val topInset = insets.getInsets(Type.systemBars()).top
            val visible = insets.isVisible(Type.statusBars())
            b.topText.text = "${topInset}px $visible"
            insets
        }
        b.bottomTextContainer.setOnApplyWindowInsetsListenerCompat { v, insets ->
            v.updatePadding(bottom = insets.getInsetsIgnoringVisibility(Type.systemBars()).bottom)
            val bottomInset = insets.getInsets(Type.systemBars()).bottom
            val visible = insets.isVisible(Type.navigationBars())
            b.bottomText.text = "${bottomInset}px $visible"
            insets
        }
        b.imeTextContainer.apply {
            isVisible = true
            // this is not possible on lower APIs
            setOnApplyWindowInsetsListenerCompat { v, insets ->
                val imeInsets = insets.getInsets(Type.ime())
                val visible = insets.isVisible(Type.statusBars())
                v.updatePadding(bottom = imeInsets.bottom)
                b.imeText.text = "${imeInsets.bottom}px $visible"
                insets
            }
        }
    }

    class SettingsFragment30 : PreferenceFragmentCompat() {
        val window
            get() = requireActivity().window
        lateinit var controller: WindowInsetsControllerCompat

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences30, rootKey)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            controller = WindowCompat.getInsetsController(window, view)!!
            //general system ui
            findPreference<SwitchPreferenceCompat>("fit_windows").listen {
                (requireActivity() as SettingsActivityBase).b.rootFrame.apply {
                    if (it != fitsSystemWindows) {
                        fitsSystemWindows = it as Boolean
                        requestApplyInsets()
                    }
                }
            }
            // visiblity for insets
            findPreference<SwitchPreferenceCompat>("30_system_visible").show(Type.systemBars())
            findPreference<SwitchPreferenceCompat>("30_status_visible").show(Type.statusBars())
            findPreference<SwitchPreferenceCompat>("30_nav_visible").show(Type.navigationBars())
            findPreference<SwitchPreferenceCompat>("30_ime_visible").show(Type.ime())
            findPreference<SwitchPreferenceCompat>("30_caption_visible").show(Type.captionBar())
            findPreference<SwitchPreferenceCompat>("30_cutout_visible").show(Type.displayCutout())
            findPreference<SwitchPreferenceCompat>("30_system_g_visible").show(Type.systemGestures())
            findPreference<SwitchPreferenceCompat>("30_m_system_g_visible").show(Type.mandatorySystemGestures())
            findPreference<SwitchPreferenceCompat>("30_tap_visible").show(Type.tappableElement())

            // general window flags
            findPreference<ListPreference>("30_behavior").listen {
                controller.systemBarsBehavior = getBehavior(it as String)
            }
            findPreference<SwitchPreferenceCompat>("30_decor_fit").listen {
                WindowCompat.setDecorFitsSystemWindows(window, it as Boolean)
            }
            findPreference<SwitchPreferenceCompat>("nolimit").windowFlag(LayoutParams.FLAG_LAYOUT_NO_LIMITS)
            findPreference<SwitchPreferenceCompat>("bounds").windowFlag(LayoutParams.FLAG_LAYOUT_IN_SCREEN)
            findPreference<SwitchPreferenceCompat>("draw").windowFlag(LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)


            // navigation
            findPreference<SwitchPreferenceCompat>("n_light").listen {
                controller.isAppearanceLightNavigationBars = it as Boolean
            }
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
            findPreference<SwitchPreferenceCompat>("s_light").listen {
                controller.isAppearanceLightStatusBars = it as Boolean
            }
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

        fun SwitchPreferenceCompat?.show(what: Int) {
            this!!.onPreferenceChangeListener = Preference.OnPreferenceChangeListener { _, v ->
                if (v == true) {
                    controller.show(what)
                } else {
                    controller.hide(what)
                }
                true
            }
            trackChanges {
                (requireActivity() as? SettingsActivity30)?.lastInsets?.isVisible(what)
            }
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
            trackChanges {
                window.attributes.flags and flag == flag
            }
        }

        // track changes: start by invalidating self
        private fun SwitchPreferenceCompat.trackChanges(chk: () -> Boolean?) {
            sharedPreferences.getBoolean(key, false).let {
                onPreferenceChangeListener.onPreferenceChange(this, it)
            }
            lifecycleScope.launch {
                while (isActive) {
                    delay(500)
                    val hasFlag = chk()
                    if (hasFlag != null && isChecked != hasFlag) {
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
                "Transparent" -> R.color.transparent
                else -> android.R.color.background_dark
            }
        )

        fun getBehavior(behavior: String) = when (behavior) {
            "BEHAVIOR_SHOW_BARS_BY_SWIPE" -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_SWIPE
            "BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE" -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            else -> WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        }
    }
}

/** Listener that wraps received insets with compat implementations. */
fun View.setOnApplyWindowInsetsListenerCompat(l: (View, WindowInsetsCompat) -> WindowInsetsCompat) {
    setOnApplyWindowInsetsListener { v, insets ->
        l(v, toWindowInsetsCompat(insets, v)).toWindowInsets()
    }
}