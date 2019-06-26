package com.bitcoin.wallet.btc.ui.activitys

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
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
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.UpdateAvailability


class MainActivity : BaseActivity() {

    val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[MainViewModel::class.java]
    }
    private val handler by lazy { Handler() }
    override fun layoutRes(): Int {
        return R.layout.activity_main
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
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
            NotifyWorker.enqueue("BTC", API_KEY)
        } else {
            NotifyWorker.clearNotify()
        }
        val updateManager = AppUpdateManagerFactory.create(this)
        updateManager.appUpdateInfo
            .addOnSuccessListener {
                if (it.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    updateManager.startUpdateFlowForResult(
                        it,
                        IMMEDIATE,
                        this,
                        REQUEST_CODE_UPDATE
                    )
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
            if (requestCode != RESULT_OK) {
                Log.e("System out", "Update flow failed! Result code: $resultCode")
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

    private fun checkForUpdate() {

        // Creates instance of the manager.
        val appUpdateManager = AppUpdateManagerFactory.create(this)

        // Checks that the platform will allow the specified type of update.
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                it.isUpdateTypeAllowed(IMMEDIATE)
            ) {
                appUpdateManager.startUpdateFlowForResult(
                    it,
                    IMMEDIATE,
                    this,
                    REQUEST_CODE_UPDATE
                )
            }
        }

    }

    companion object {
        private const val REQUEST_CODE_UPDATE = 1001
    }
}
