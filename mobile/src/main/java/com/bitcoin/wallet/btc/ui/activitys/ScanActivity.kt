package com.bitcoin.wallet.btc.ui.activitys

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.graphics.drawable.ColorDrawable
import android.hardware.Camera
import android.os.*
import android.view.*
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.Bundle
import com.bitcoin.wallet.btc.ui.widget.DialogBuilder
import com.bitcoin.wallet.btc.utils.CameraManager
import com.bitcoin.wallet.btc.utils.Event
import com.bitcoin.wallet.btc.utils.OnFirstPreDraw
import com.bitcoin.wallet.btc.viewmodel.ScanViewModel
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import kotlinx.android.synthetic.main.activity_scan.*
import java.util.*

class ScanActivity : BaseActivity(), TextureView.SurfaceTextureListener,
    ActivityCompat.OnRequestPermissionsResultCallback {
    companion object {
        private const val INTENT_EXTRA_SCENE_TRANSITION_X = "scene_transition_x"
        private const val INTENT_EXTRA_SCENE_TRANSITION_Y = "scene_transition_y"
        private const val VIBRATE_DURATION = 50L
        private const val AUTO_FOCUS_INTERVAL_MS = 2500L
        const val INTENT_EXTRA_RESULT = "result"
        const val REQUEST_CODE_SCAN = 0

        fun startForResult(activity: Activity, resultCode: Int) {
            activity.startActivityForResult(Intent(activity, ScanActivity::class.java), resultCode)
        }
    }

    private val cameraManager = CameraManager()
    @Volatile
    private var surfaceCreated = false
    private var sceneTransition: Animator? = null
    private var vibrator: Vibrator? = null
    private var cameraThread: HandlerThread? = null
    @Volatile
    private var cameraHandler: Handler? = null
    private val viewModel: ScanViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory).get(ScanViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
        super.onCreate(savedInstanceState)
    }

    override fun layoutRes(): Int {
        return R.layout.activity_scan
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        setupToolbar("Scan QR Code")
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        viewModel.showPermissionWarnDialog.observe(this, object : Event.Observer<Void>() {
            override fun onEvent(v: Void) {
                WarnDialogFragment.show(
                    supportFragmentManager, R.string.scan_permission,
                    getString(R.string.scan_permission_mes)
                )
            }
        })
        viewModel.showProblemWarnDialog.observe(this, object : Event.Observer<Void>() {
            override fun onEvent(v: Void) {
                WarnDialogFragment.show(
                    supportFragmentManager, R.string.scan_problem,
                    getString(R.string.scan_problem_mes)
                )
            }
        })

        cameraThread = HandlerThread("cameraThread", Process.THREAD_PRIORITY_BACKGROUND)
        cameraThread?.start()
        cameraHandler = Handler(cameraThread?.looper)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0)
        }

        if (savedInstanceState == null) {
            val intent = intent
            val x = intent.getIntExtra(INTENT_EXTRA_SCENE_TRANSITION_X, -1)
            val y = intent.getIntExtra(INTENT_EXTRA_SCENE_TRANSITION_Y, -1)
            if (x != -1 || y != -1) {
                // Using alpha rather than visibility because 'invisible' will cause the surface view to never
                // start up, so the animation will never start.
                val contentView = findViewById<View>(android.R.id.content)
                contentView?.alpha = 0f
                window
                    .setBackgroundDrawable(ColorDrawable(resources.getColor(android.R.color.transparent)))
                OnFirstPreDraw.listen(contentView) {
                    val finalRadius = Math.max(contentView.width, contentView.height).toFloat()
                    val duration = resources.getInteger(android.R.integer.config_mediumAnimTime)
                    sceneTransition = ViewAnimationUtils.createCircularReveal(contentView, x, y, 0f, finalRadius)
                    sceneTransition?.duration = duration.toLong()
                    sceneTransition?.interpolator = AccelerateInterpolator()
                    false
                }
            }
        }

        previewView.surfaceTextureListener = this
    }

    private fun maybeTriggerSceneTransition() {
        if (sceneTransition != null) {
            findViewById<View>(android.R.id.content)?.alpha = 1f
            sceneTransition?.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    window
                        .setBackgroundDrawable(ColorDrawable(resources.getColor(android.R.color.black)))
                }
            })
            sceneTransition?.start()
            sceneTransition = null
        }
    }

    override fun onResume() {
        super.onResume()

        maybeOpenCamera()
    }

    override fun onPause() {
        cameraHandler?.post(closeRunnable)

        super.onPause()
    }

    override fun onDestroy() {
        // cancel background thread
        cameraHandler?.removeCallbacksAndMessages(null)
        cameraThread?.quit()
        previewView.surfaceTextureListener = null
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            maybeOpenCamera()
        } else {
            viewModel.showPermissionWarnDialog.setValue(Event.simple())
        }
    }

    private fun maybeOpenCamera() {
        if (surfaceCreated && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
            cameraHandler?.post(openRunnable)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        surfaceCreated = true
        maybeOpenCamera()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        surfaceCreated = false
        return true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun onAttachedToWindow() {
        setShowWhenLocked(true)
    }

    override fun onBackPressed() {
        scannerView.visibility = View.GONE
        setResult(AppCompatActivity.RESULT_CANCELED)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_CAMERA ->
                // don't launch camera app
                return true
            KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_UP -> {
                cameraHandler?.post { cameraManager.setTorch(keyCode == KeyEvent.KEYCODE_VOLUME_UP) }
                return true
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    fun handleResult(scanResult: Result) {
        vibrator?.vibrate(VIBRATE_DURATION)

        scannerView.setIsResult(true)

        val result = Intent()
        val bundle = Bundle {
            putString(INTENT_EXTRA_RESULT, scanResult.text)
        }
        result.putExtras(bundle)
        setResult(AppCompatActivity.RESULT_OK, result)
        finish()
    }

    private fun postFinish() {
        Handler().postDelayed({ finish() }, 50)
    }

    private val openRunnable = object : Runnable {
        override fun run() {
            try {
                val camera = cameraManager.open(previewView, displayRotation())

                val framingRect = cameraManager.frame
                val framingRectInPreview = RectF(cameraManager.framePreview)
                framingRectInPreview.offsetTo(0f, 0f)
                val cameraFlip = cameraManager.facing == Camera.CameraInfo.CAMERA_FACING_FRONT
                val cameraRotation = cameraManager.orientation

                runOnUiThread {
                    scannerView.setFraming(
                        framingRect, framingRectInPreview, displayRotation(), cameraRotation,
                        cameraFlip
                    )
                }

                val focusMode = camera.parameters.focusMode
                val nonContinuousAutoFocus =
                    Camera.Parameters.FOCUS_MODE_AUTO == focusMode || Camera.Parameters.FOCUS_MODE_MACRO == focusMode

                if (nonContinuousAutoFocus)
                    cameraHandler?.post(AutoFocusRunnable(camera))

                maybeTriggerSceneTransition()
                cameraHandler?.post(fetchAndDecodeRunnable)
            } catch (x: Exception) {
                viewModel?.showProblemWarnDialog?.postValue(Event.simple())
            }

        }

        private fun displayRotation(): Int {
            val rotation = windowManager.defaultDisplay.rotation
            return when (rotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> throw IllegalStateException("rotation: $rotation")
            }
        }
    }

    private val closeRunnable = Runnable {
        cameraHandler?.removeCallbacksAndMessages(null)
        cameraManager.close()
    }

    private inner class AutoFocusRunnable(private val camera: Camera) : Runnable {

        private val autoFocusCallback = Camera.AutoFocusCallback { success, camera ->
            // schedule again
            cameraHandler?.postDelayed(this@AutoFocusRunnable, AUTO_FOCUS_INTERVAL_MS)
        }

        override fun run() {
            try {
                camera.autoFocus(autoFocusCallback)
            } catch (x: Exception) {
            }

        }
    }

    private val fetchAndDecodeRunnable = object : Runnable {
        private val reader = QRCodeReader()
        private val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)

        override fun run() {
            cameraManager.requestPreviewFrame { data, camera -> decode(data) }
        }

        private fun decode(data: ByteArray) {
            val source = cameraManager.buildLuminanceSource(data)
            val bitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                hints[DecodeHintType.NEED_RESULT_POINT_CALLBACK] =
                    ResultPointCallback { dot -> runOnUiThread { scannerView.addDot(dot) } }
                val scanResult = reader.decode(bitmap, hints)

                runOnUiThread { handleResult(scanResult) }
            } catch (x: ReaderException) {
                // retry
                cameraHandler?.post(this)
            } finally {
                reader.reset()
            }
        }
    }

    class WarnDialogFragment : DialogFragment() {

        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val args = arguments
            val dialog = DialogBuilder.warn(activity, args!!.getInt("title"))
            dialog.setMessage(args.getString("message"))
            dialog.singleDismissButton { _, _ -> activity?.finish() }
            return dialog.create()
        }

        override fun onCancel(dialog: DialogInterface?) {
            activity?.finish()
        }

        companion object {
            private val FRAGMENT_TAG = WarnDialogFragment::class.java.name

            fun show(fm: FragmentManager, titleResId: Int, message: String) {
                val newFragment = WarnDialogFragment()
                val args = Bundle()
                args.putInt("title", titleResId)
                args.putString("message", message)
                newFragment.arguments = args
                newFragment.show(fm, FRAGMENT_TAG)
            }
        }
    }
}