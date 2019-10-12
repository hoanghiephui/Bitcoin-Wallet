package com.bitcoin.wallet.btc.repository.data

import androidx.lifecycle.MutableLiveData
import androidx.paging.ItemKeyedDataSource
import com.bitcoin.wallet.btc.api.BitcoinEndpoints
import com.bitcoin.wallet.btc.extension.addTo
import com.bitcoin.wallet.btc.extension.applySchedulers
import com.bitcoin.wallet.btc.model.explorer.address.AddressResponse
import com.bitcoin.wallet.btc.model.explorer.details.BlockDetailResponse
import com.bitcoin.wallet.btc.model.explorer.transaction.TransactionsResponse
import com.bitcoin.wallet.btc.repository.NetworkState
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.Action
import io.reactivex.functions.BiFunction

class TransactionsDataSource(
    private val api: BitcoinEndpoints,
    private val compositeDisposable: CompositeDisposable,
    private val hash: String
) : ItemKeyedDataSource<Int, Any>() {
    val networkState = MutableLiveData<NetworkState>()
    var isBlock = false
    private var isTx = false

    val initialLoad = MutableLiveData<NetworkState>()
    private var pageNumber = 0
    private var pageSize = 0
    private val url = "https://explorer.bitcoin.com/api/btc/"
    /**
     * Keep Completable reference for the retry event
     */
    private var retryCompletable: Completable? = null

    fun retryAllFailed() {
        retryCompletable?.applySchedulers()
            ?.subscribe({ }, { throwable -> throwable.printStackTrace() })
            ?.addTo(compositeDisposable)
    }

    private fun setRetry(action: Action?) {
        if (action == null) {
            this.retryCompletable = null
        } else {
            this.retryCompletable = Completable.fromAction(action)
        }
    }

    override fun loadInitial(params: LoadInitialParams<Int>, callback: LoadInitialCallback<Any>) {
        // update network states.
        // we also provide an initial load state to the listeners so that the UI can know when the
        // very first list is loaded.
        networkState.postValue(NetworkState.LOADING)
        initialLoad.postValue(NetworkState.LOADING)
        val list: MutableList<Any> = mutableListOf()
        onInitialBlockTransactions(pageNumber)
            .distinctUntilChanged()
            .applySchedulers()
            .subscribe(
                {
                    // clear retry since last request succeeded
                    setRetry(null)
                    networkState.postValue(NetworkState.LOADED)
                    initialLoad.postValue(NetworkState.LOADED)
                    pageNumber++
                    list.add(it.blockResponse)
                    list.add(
                        Response.Title(
                            "Transactions",
                            it.blockResponse.tx?.size?.toString() ?: "0"
                        )
                    )
                    it.transactionResponse.txs?.let { it1 -> list.addAll(it1) }
                    pageSize = it.transactionResponse.pagesTotal ?: pageSize
                    isBlock = true
                    callback.onResult(list)
                },
                {
                    // keep a Completable for future retry
                    setRetry(Action { loadInitial(params, callback) })
                    val error = NetworkState.error(it.message)
                    // publish the error
                    if (error.msg?.contains("404") == true) {
                        onInitialAddressTransactions(pageNumber)
                            .applySchedulers()
                            .subscribe({ data ->
                                setRetry(null)
                                networkState.postValue(NetworkState.LOADED)
                                initialLoad.postValue(NetworkState.LOADED)
                                list.add(data.addressResponse)
                                list.add(
                                    Response.Title(
                                        "Transactions",
                                        data.addressResponse.txApperances?.toString() ?: "0"
                                    )
                                )
                                data.transactionResponse.txs?.let { it1 -> list.addAll(it1) }
                                pageNumber++
                                isBlock = false
                                pageSize = data.transactionResponse.pagesTotal ?: pageSize
                                callback.onResult(list)
                            }, {
                                api.getTransactions(hash)
                                    .distinctUntilChanged()
                                    .applySchedulers()
                                    .subscribe(
                                        { txResponse ->
                                            setRetry(null)
                                            isTx = true
                                            networkState.postValue(NetworkState.LOADED)
                                            initialLoad.postValue(NetworkState.LOADED)
                                            list.add(txResponse)
                                            list.add(
                                                Response.Title(
                                                    "Inputs",
                                                    if (txResponse.vin != null && txResponse.vin[0].addr != null) txResponse?.vin.size.toString() else "0"
                                                )
                                            )
                                            list.addAll(txResponse.vin ?: mutableListOf())
                                            list.add(
                                                Response.Title(
                                                    "Outputs",
                                                    if (txResponse.vout != null && txResponse.vout[0].scriptPubKey != null) txResponse?.vout.size.toString() else "0"
                                                )
                                            )
                                            list.addAll(txResponse.vout ?: mutableListOf())
                                            callback.onResult(list)
                                        },
                                        {
                                            isTx = false
                                            networkState.postValue(NetworkState.error("No results found"))
                                            initialLoad.postValue(networkState.value)
                                        }
                                    )
                                    .addTo(compositeDisposable)
                            }).addTo(compositeDisposable)

                    } else {
                        networkState.postValue(error)
                        initialLoad.postValue(networkState.value)
                    }
                }
            )
            .addTo(compositeDisposable)
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Any>) {
        if (params.key > pageSize || isTx) {
            return
        }
        // set network value to loading.
        networkState.postValue(NetworkState.LOADING)
        if (isBlock) {
            api.getListTransaction(url.plus("txs/?block=$hash&pageNum=${params.key}"))
                .distinctUntilChanged()
                .applySchedulers()
                .subscribe(
                    {
                        // clear retry since last request succeeded
                        setRetry(null)
                        networkState.postValue(NetworkState.LOADED)
                        pageNumber++
                        pageSize = it.pagesTotal ?: pageSize
                        it.txs?.let { txs -> callback.onResult(txs) }
                    },
                    {
                        // keep a Completable for future retry
                        setRetry(Action { loadAfter(params, callback) })
                        // publish the error
                        networkState.postValue(NetworkState.error(it.message))
                    }
                )
                .addTo(compositeDisposable)
        } else {
            api.getListTransactionByAddress(url.plus("txs/?address=$hash&pageNum=${params.key}"))
                .distinctUntilChanged()
                .applySchedulers()
                .subscribe(
                    {
                        // clear retry since last request succeeded
                        setRetry(null)
                        networkState.postValue(NetworkState.LOADED)
                        pageNumber++
                        pageSize = it.pagesTotal ?: pageSize
                        it.txs?.let { txs -> callback.onResult(txs) }
                    },
                    {
                        // keep a Completable for future retry
                        setRetry(Action { loadAfter(params, callback) })
                        // publish the error
                        networkState.postValue(NetworkState.error(it.message))
                    }
                )
                .addTo(compositeDisposable)
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Any>) {

    }

    override fun getKey(item: Any): Int {
        return pageNumber
    }

    private fun onInitialAddressTransactions(currentPage: Int): Observable<Response.ZipAddressAndTransaction> {
        return Observable.zip(
            api.getAddress(url.plus("addr/$hash")),
            api.getListTransactionByAddress(url.plus("txs/?address=$hash&pageNum=$currentPage")),
            BiFunction<AddressResponse, TransactionsResponse, Response.ZipAddressAndTransaction> { t1, t2 ->
                Response.ZipAddressAndTransaction(t1, t2)
            }
        )
    }

    private fun onInitialBlockTransactions(currentPage: Int): Observable<Response.ZipBlockAndTransaction> {
        return Observable.zip(
            api.getBlockDetailsByHash(hash),
            api.getListTransaction(url.plus("txs/?block=$hash&pageNum=$currentPage")),
            BiFunction<BlockDetailResponse, TransactionsResponse, Response.ZipBlockAndTransaction> { t1, t2 ->
                Response.ZipBlockAndTransaction(t1, t2)
            }
        )
    }

    sealed class Response {
        data class ZipAddressAndTransaction(
            val addressResponse: AddressResponse,
            val transactionResponse: TransactionsResponse
        ) : Response()

        data class ZipBlockAndTransaction(
            val blockResponse: BlockDetailResponse,
            val transactionResponse: TransactionsResponse
        ) : Response()

        data class Title(
            val title: String,
            val content: String
        ) : Response()
    }
}