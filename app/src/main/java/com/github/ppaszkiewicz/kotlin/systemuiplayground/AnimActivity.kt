package com.github.ppaszkiewicz.kotlin.systemuiplayground

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.Insets
import androidx.core.view.*
import androidx.core.view.WindowInsetsCompat.Type
import androidx.core.view.WindowInsetsCompat.toWindowInsetsCompat
import com.github.ppaszkiewicz.kotlin.systemuiplayground.databinding.ActivityAnimBinding
import kotlin.math.roundToInt

class AnimActivity : AppCompatActivity() {
    companion object {
        const val TAG = "AnimAct"
    }

    lateinit var b: ActivityAnimBinding
    lateinit var rootInsets: WindowInsetsCompat // is lateinit safe or its possible controller gets called before they're applied?

    val controller by lazy {
        ViewCompat.getWindowInsetsController(b.root)!!
    }
    val insetControllers = HashMap<Int, WindowInsetsAnimationControllerCompat?>()
    var displayAppliedInsetsOnce = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            ActivityAnimBinding.inflate(layoutInflater).let { b = it; b.root }
        )
        setSupportActionBar(b.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // default apply inset behavior is to dispatch to first child that fitsSystemWindows;
        // alter it so its dispatched to all
        b.root.setOnApplyWindowInsetsListener { v, insets ->
            rootInsets = toWindowInsetsCompat(insets)
            (v as ViewGroup).children.forEach {
                if (it.fitsSystemWindows) it.dispatchApplyWindowInsets(insets)
            }
            if (displayAppliedInsetsOnce) {
                updateDisplayedInsets(rootInsets, rootInsets)
                displayAppliedInsetsOnce = false
            }
            val systemInsets = rootInsets.getInsets(Type.systemBars() or Type.ime())
            v.updatePadding(bottom = systemInsets.bottom)
            insets
        }
        setupListeners()
        setupControllers()
    }

    fun updateDisplayedInsets(src: WindowInsetsCompat, target: WindowInsetsCompat) {
        // update IME
        updateImeInsetsText(
            src.getInsets(Type.ime()),
            target.tryGetInsetsIgnoringVisibility(Type.ime())
        )
    }

    fun updateImeInsetsText(src: Insets, target: Insets?) {
        val current = src.bottom
        val max = target?.bottom ?: -1
        b.imeValue.text = "$current / $max"
        b.imeValuePercent.text = "${(current.toPercent() / max).roundToInt()}%"
    }

    fun setupListeners() {
        b.toolbar.setOnApplyWindowInsetsListenerCompat { v, insets ->
            v.updatePadding(top = insets.getInsets(Type.systemBars()).top)
            insets
        }

        controller.addOnControllableInsetsChangedListener { controller, typeMask ->
            b.imeInfo.isSelected = typeMask has Type.ime()

        }
        val cb = object : WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_STOP) {
            override fun onProgress(
                insets: WindowInsetsCompat,
                runningAnimations: MutableList<WindowInsetsAnimationCompat>
            ): WindowInsetsCompat {
                Log.d(
                    TAG,
                    "progress ${runningAnimations.joinToString { it.fraction.toString() }} - insets: $insets"
                )
                // rotate for effect
                runningAnimations.find { it.typeMask has Type.ime() }?.let {
                    b.imgIme.rotation = 360 * it.fraction
                    alignImageToInsets(insets.getInsets(Type.ime()))
                }
                return insets
            }

            override fun onPrepare(animation: WindowInsetsAnimationCompat) {
                Log.d(TAG, "onPrepare")
                // capture pre-layout state (unused)
                super.onPrepare(animation)
            }

            override fun onStart(
                animation: WindowInsetsAnimationCompat,
                bounds: WindowInsetsAnimationCompat.BoundsCompat
            ): WindowInsetsAnimationCompat.BoundsCompat {
                Log.d(TAG, "onStart")
                // this happens after views are laidout with target insets but before they're drawn
                alignImageToInsets(bounds.lowerBound)
                return super.onStart(animation, bounds)
            }

            override fun onEnd(animation: WindowInsetsAnimationCompat) {
                Log.d(TAG, "onEnd")
                // cleanup
                b.imgIme.translationY = 0f
                b.imgIme.rotation = 0f
            }
        }
        ViewCompat.setWindowInsetsAnimationCallback(b.root, cb)
    }

    private fun alignImageToInsets(insets: Insets) {
        val nav = rootInsets.getInsets(Type.systemBars()).bottom
        val bot = insets.bottom.coerceAtLeast(nav)
        b.imgIme.translationY = (b.root.paddingBottom - bot).toFloat()
    }

    fun setupControllers() {
        b.imeInfo.setOnClickListener {
            insetControllers[Type.ime()]?.let {
                it.finish(false)
                insetControllers.remove(Type.ime())
                b.imeValueSeekbar.progress = 0
                b.imeValuePercent.text = "0%"
            }
        }
        b.imeValueSeekbar.setOnValueChangedListener { seekBar, progress, fromUser ->
            val f = progress / seekBar.max.toFloat()
            if (fromUser) {
                setImeProgress(f)
            }
        }
    }

    private fun setImeProgress(fraction: Float) {
        getAnimationController(Type.ime())?.let {
            val target = it.shownStateInsets.bottom * fraction
            val bottom = Insets.of(0, 0, 0, target.toInt())
            it.setInsetsAndAlpha(bottom, fraction, fraction)
            updateImeInsetsText(it.currentInsets, it.shownStateInsets)
        }
    }

    // lazily get or initialize controller for given insets
    fun getAnimationController(flag: Int): WindowInsetsAnimationControllerCompat? {
        return if (!insetControllers.containsKey(flag)) {
            controller.controlWindowInsetsAnimation(Type.ime(), -1, null, null,
                object : WindowInsetsAnimationControlListenerCompat {
                    override fun onReady(
                        controller: WindowInsetsAnimationControllerCompat,
                        types: Int
                    ) {
                        insetControllers[types] = controller
                        Log.d(TAG, "animation ready for IME")
                        // trigger first progress update IMMIDIATELY
                        setImeProgress(b.imeValueSeekbar.progress / b.imeValueSeekbar.max.toFloat())
                    }

                    override fun onFinished(controller: WindowInsetsAnimationControllerCompat) {
                        insetControllers.remove(Type.ime())
                        Log.d(TAG, "animation finished for IME")
                    }

                    override fun onCancelled(controller: WindowInsetsAnimationControllerCompat?) {
                        insetControllers.remove(Type.ime())
                        Log.d(TAG, "animation cancelled for IME")
                    }
                })
            Log.d(TAG, "no animation for IME - preparing")
            insetControllers[flag] = null
            null
        } else {
            insetControllers[flag]
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }
}

private fun WindowInsetsCompat.tryGetInsetsIgnoringVisibility(typeMask: Int) = try {
    getInsetsIgnoringVisibility(typeMask)
} catch (ie: IllegalArgumentException) {
    // cannot measure those
    null
}

private infix fun Int.has(flag: Int) = (this or flag) == flag
private fun Int.toPercent() = this.toFloat() * 100
private fun SeekBar.setOnValueChangedListener(listener: (seekBar: SeekBar, progress: Int, fromUser: Boolean) -> Unit) =
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            listener(seekBar, progress, fromUser)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {

        }
    })