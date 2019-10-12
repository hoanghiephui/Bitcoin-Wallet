package com.bitcoin.wallet.btc.ui.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bitcoin.wallet.btc.R
import com.bitcoin.wallet.btc.base.BaseBottomSheetDialogFragment
import com.bitcoin.wallet.btc.extension.observeNotNull
import com.bitcoin.wallet.btc.repository.localdb.AugmentedSkuDetails
import com.bitcoin.wallet.btc.ui.adapter.SkuDetailsAdapter
import com.bitcoin.wallet.btc.viewmodel.BillingViewModel
import kotlinx.android.synthetic.main.fragment_make_purchase.*
import javax.inject.Inject

class MakePurchaseDialogFragment : BaseBottomSheetDialogFragment() {
    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private val billingViewModel: BillingViewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)[BillingViewModel::class.java]
    }


    override fun layoutRes(): Int {
        return R.layout.fragment_make_purchase
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val subsAdapter = object : SkuDetailsAdapter() {
            override fun onSkuDetailsClicked(item: AugmentedSkuDetails) {
                onPurchase(view, item)
            }

            override fun onSkuRestoreClicked() {
                //billingViewModel.queryPurchases()
                dismissDialog()
            }
        }
        with(recyclerView) {
            layoutManager = LinearLayoutManager(requireContext())
            addItemDecoration(
                DividerItemDecoration(
                    requireContext(),
                    DividerItemDecoration.VERTICAL
                )
            )
            setHasFixedSize(true)
            adapter = subsAdapter
        }
        billingViewModel.subsSkuDetailsListLiveData.observe(viewLifecycleOwner, Observer {
            it?.let { subsAdapter.setSkuDetailsList(it) }
        })
        billingViewModel.networkViewModel.observeNotNull(viewLifecycleOwner) {
            //progressBar.isVisible = it == NetworkState.LOADING
        }
    }

    private fun onPurchase(view: View, item: AugmentedSkuDetails) {
        billingViewModel.makePurchase(activity as Activity, item)
        Log.d(
            MakePurchaseDialogFragment::class.java.simpleName,
            "starting purchase flow for SkuDetail:\n ${item}"
        )
    }

    companion object {
        fun show(activity: FragmentActivity) {
            MakePurchaseDialogFragment().apply {
                show(
                    activity.supportFragmentManager,
                    MakePurchaseDialogFragment::class.java.simpleName
                )
            }
        }
    }
}