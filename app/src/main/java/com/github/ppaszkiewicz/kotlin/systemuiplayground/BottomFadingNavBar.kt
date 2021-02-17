package com.github.ppaszkiewicz.kotlin.systemuiplayground

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.widget.AppCompatTextView
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.children
import androidx.core.view.updateLayoutParams
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomDialogFragment : BottomSheetDialogFragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottomdialog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        BottomFadingNavBar.installOn(requireDialog() as BottomSheetDialog)
    }
}

fun BottomSheetDialog.addFadingNavBar() = BottomFadingNavBar.installOn(this)

// dialog that has scrollable background behind nav bar
class BottomFadingNavBar {
    companion object {
        fun isO() = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
        fun installOn(dialog: BottomSheetDialog) {
            BottomFadingNavBar().modifyDialog(dialog)
        }
    }

    fun modifyDialog(dialog: BottomSheetDialog) {
        val container = dialog.findViewById<ViewGroup>(R.id.container)!!
        if (isO()) {
            container.systemUiVisibility =
                container.systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
        container.fitsSystemWindows = false
        (container.getChildAt(0) as ViewGroup).run {
            check(children.none { it is BottomDimView }) { "BottomFadingNavBar is already installed" }
            // note: coordinator has systemUi FLAG_LAYOUT_FULLSCREEN set, that's why it's laid out on top
            //of the nav bar
            setOnApplyWindowInsetsListener { v, insets ->
                // dispatch to all children
                children.forEach { it.dispatchApplyWindowInsets(insets) }
                insets
            }
            val clip = if (isO()) BottomDimView.PaddingClip.BEHIND_GRADIENT else
                BottomDimView.PaddingClip.NONE
            addView(BottomDimView(dialog.context, 0, clip))
        }
    }

    @SuppressLint("ViewConstructor")
    internal class BottomDimView(
        context: Context,
        val topOffset: Int,
        val paddingClip: PaddingClip
    ) : AppCompatTextView(context) {
        enum class PaddingClip {
            NONE, FULL_PADDING, BEHIND_GRADIENT
        }

        private val mBehavior = object : CoordinatorLayout.Behavior<View>() {
            override fun layoutDependsOn(
                parent: CoordinatorLayout,
                child: View,
                dependency: View
            ): Boolean {
                return dependency is FrameLayout
            }

            override fun onDependentViewChanged(
                parent: CoordinatorLayout,
                child: View,
                dependency: View
            ): Boolean {
                if (child.height == 0) return false
                child.elevation = dependency.elevation // clone dependency elevation
                val depTop = dependency.top + topOffset + topPad(dependency)
                val targetT = (depTop - child.top).coerceIn(0, child.height).toFloat()
                return if (targetT != child.translationY) {
                    child.translationY = targetT
                    true
                } else false
            }

            private fun topPad(v: View): Int {
                val child = (v as ViewGroup).getChildAt(0) ?: return 0
                return when (paddingClip) {
                    PaddingClip.NONE -> 0
                    PaddingClip.FULL_PADDING -> child.paddingTop
                    PaddingClip.BEHIND_GRADIENT -> (child.paddingTop - (height / 4)).coerceAtLeast(0)
                }
            }
        }

        init {
            layoutParams =
                CoordinatorLayout.LayoutParams(CoordinatorLayout.LayoutParams.MATCH_PARENT, 0)
                    .apply {
                        behavior = mBehavior
                        gravity = Gravity.BOTTOM
                    }
            gravity = Gravity.START or Gravity.TOP
            fitsSystemWindows = true
            setOnApplyWindowInsetsListener { v, insets ->
                v.updateLayoutParams<ViewGroup.LayoutParams> {
                    height = insets.systemWindowInsetBottom
                }
                text = "${insets.systemWindowInsetBottom}"
                insets
            }

            // use black background on lower OS version because dark buttons can't be enforced
            if (isO()) {
                val c = bgColor(context)
                val gradient = GradientDrawable(
                    GradientDrawable.Orientation.TOP_BOTTOM,
                    intArrayOf(Color.TRANSPARENT, c, c, c) // use four colors for 25% split
                )
                background = gradient
                setTextColor(Color.BLACK)
            } else {
                setBackgroundColor(Color.BLACK)
                setTextColor(Color.WHITE)
            }
        }

        // color for white fade efect
        fun bgColor(context: Context) = Color.WHITE
    }
}