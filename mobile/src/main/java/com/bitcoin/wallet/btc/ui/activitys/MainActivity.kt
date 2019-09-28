package com.bitcoin.wallet.btc.ui.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT
import androidx.lifecycle.ViewModelProviders
import com.bitcoin.wallet.btc.Constants.API_KEY
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseActivity
import com.bitcoin.wallet.btc.extension.isTrue
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.extension.replace
import com.bitcoin.wallet.btc.service.BlockchainService
import com.bitcoin.wallet.btc.service.BlockchainService.NOTYFY_RECEP
import com.bitcoin.wallet.btc.ui.fragments.BackupDialog
import com.bitcoin.wallet.btc.ui.fragments.MainFragment
import com.bitcoin.wallet.btc.ui.fragments.TermFragment
import com.bitcoin.wallet.btc.utils.Event
import com.bitcoin.wallet.btc.viewmodel.MainViewModel
import com.bitcoin.wallet.btc.works.NotifyWorker
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType.FLEXIBLE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : BaseActivity(), InstallStateUpdatedListener {

    val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[MainViewModel::class.java]
    }
    private val handler by lazy { Handler() }
    private var appUpdateManager: AppUpdateManager? = null

    override fun layoutRes(): Int {
        return R.layout.activity_main
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        appUpdateManager = AppUpdateManagerFactory.create(this)
        if (savedInstanceState == null) {
            if (!sharedPreferences.getBoolean("policy", false)) {
                replace(R.id.container, TermFragment(), TermFragment::class.java.simpleName)
            } else {
                replace(R.id.container, MainFragment(), MainFragment.TAG)
            }
        }
        viewModel.backupWalletStatus.observe(this, object : Event.Observer<BackupDialog.BackUpStatus>() {
            override fun onEvent(content: BackupDialog.BackUpStatus?) {
                content?.let {
                    onShowSnackbar(
                        if (it.isStatus)
                            HtmlCompat.fromHtml(
                                getString(R.string.export_success, it.mes),
                                FROM_HTML_MODE_COMPACT
                            ).toString()
                        else getString(R.string.export_failure, it.mes)
                        , object : CallbackSnack {
                            override fun onOke() {

                            }
                        }, 10
                    )
                }
            }
        })

        viewModel.showFailureDialog.observeNotNull(this) {
            val title = getString(R.string.import_failure, it.contentOrThrow)
            onShowSnackbar(
                title
                , object : CallbackSnack {
                    override fun onOke() {

                    }
                }, 5
            )
        }
        viewModel.showSuccessDialog.observeNotNull(this) {
            val message = StringBuilder()
            message.append(getString(R.string.restore_success))
            message.append("\n\n")
            message.append(getString(R.string.restore_success_replay))
            if (it.contentOrThrow == true) {
                message.append("\n\n")
                message.append(getString(R.string.restore_encrypted))
            }
            onShowSnackbar(
                message.toString()
                , object : CallbackSnack {
                    override fun onOke() {
                        finish()
                    }
                }, 6
            )
        }
        viewModel.walletEncrypted.observeNotNull(this) {
            invalidateOptionsMenu()
        }
        openTransactionNotify(intent)
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            checkForUpdate()
        }
    }

    fun openMain() {
        replace(R.id.container, MainFragment(), MainFragment.TAG)
        sharedPreferences.edit {
            putBoolean("policy", true)
        }
    }

    private fun openTransactionNotify(intent: Intent?) {
        if (intent?.getIntExtra("noty", 0) == NOTYFY_RECEP) {
            handler.postDelayed({
                startActivity(Intent(this@MainActivity, WalletTransactionsActivity::class.java))
            }, 1000)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        openTransactionNotify(intent)
    }

    override fun onResume() {
        handler.postDelayed({
            BlockchainService.start(this@MainActivity, true)
        }, 1000)
        if (application.config.nofity) {
            NotifyWorker.enqueue("BTC", API_KEY, application)
        } else {
            NotifyWorker.clearNotify(application)
        }
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            appUpdateManager?.appUpdateInfo
                ?.addOnSuccessListener {
                    // If the update is downloaded but not installed,
                    // notify the user to complete the update.
                    if (it.installStatus() == InstallStatus.DOWNLOADED) {
                        popupSnackbarForCompleteUpdate()
                    }
                }
        }
        super.onResume()
    }

    override fun onPause() {
        handler.removeCallbacksAndMessages(null)
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_UPDATE) {
            if (resultCode != RESULT_OK) {
                Log.e("System out", "Update flow failed! Result code: $resultCode")
                Snackbar.make(
                    container,
                    "Bitcoin Wallet recommends that you update to the latest version.",
                    Snackbar.LENGTH_INDEFINITE
                ).apply {
                    setAction("OK") { this.dismiss() }
                    setActionTextColor(
                        ContextCompat.getColor(
                            this@MainActivity,
                            R.color.colorAccent
                        )
                    )
                    show()
                }
                // If the update is cancelled or fails,
                // you can request to start the update again.
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
            for (fragment in supportFragmentManager.fragments) {
                fragment.onActivityResult(requestCode, resultCode, data)
            }
        }
    }

    override fun onBackPressed() {
        (supportFragmentManager.findFragmentByTag(MainFragment.TAG) as? MainFragment)?.onBackPressed()?.isTrue {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onDestroy() {
        appUpdateManager?.unregisterListener(this)
        super.onDestroy()
    }

    override fun onStateUpdate(state: InstallState?) {
        if (state?.installStatus() == InstallStatus.DOWNLOADED) {
            // After the update is downloaded, show a notification
            // and request user confirmation to restart the app.
            popupSnackbarForCompleteUpdate()
        }
    }

    private fun checkForUpdate() {
        appUpdateManager?.registerListener(this)
        // Checks that the platform will allow the specified type of update.
        appUpdateManager?.appUpdateInfo?.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                it.isUpdateTypeAllowed(FLEXIBLE)
            ) {
                appUpdateManager?.startUpdateFlowForResult(
                    it,
                    FLEXIBLE,
                    this,
                    REQUEST_CODE_UPDATE
                )
            }
        }

    }

    /* Displays the snackbar notification and call to action. */
    fun popupSnackbarForCompleteUpdate() {
        Snackbar.make(
            container,
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).apply {
            setAction("RESTART") {
                this.dismiss()
                appUpdateManager?.completeUpdate()
            }
            setActionTextColor(ContextCompat.getColor(this@MainActivity, R.color.colorAccent))
            show()
        }
    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 1001
    }
}
