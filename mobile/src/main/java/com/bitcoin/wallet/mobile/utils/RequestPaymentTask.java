package com.bitcoin.wallet.mobile.utils;

import android.os.Handler;
import android.os.Looper;
import androidx.annotation.Nullable;
import com.bitcoin.wallet.mobile.Constants;
import com.bitcoin.wallet.mobile.R;
import com.bitcoin.wallet.mobile.data.PaymentIntent;
import okhttp3.CacheControl;
import okhttp3.Call;
import okhttp3.Request;
import okhttp3.Response;
import org.bitcoinj.protocols.payments.PaymentProtocol;

import java.io.IOException;
import java.io.InputStream;

public abstract class RequestPaymentTask {
    private final Handler backgroundHandler;
    private final Handler callbackHandler;
    private final ResultCallback resultCallback;

    //private static final Logger log = LoggerFactory.getLogger(RequestPaymentTask.class);todo log

    public interface ResultCallback {
        void onPaymentIntent(PaymentIntent paymentIntent);

        void onFail(int messageResId, Object... messageArgs);
    }

    public RequestPaymentTask(final Handler backgroundHandler, final ResultCallback resultCallback) {
        this.backgroundHandler = backgroundHandler;
        this.callbackHandler = new Handler(Looper.myLooper());
        this.resultCallback = resultCallback;
    }

    public final static class HttpRequestTask extends RequestPaymentTask {
        @Nullable
        private final String userAgent;

        public HttpRequestTask(final Handler backgroundHandler, final ResultCallback resultCallback,
                               @Nullable final String userAgent) {
            super(backgroundHandler, resultCallback);

            this.userAgent = userAgent;
        }

        @Override
        public void requestPaymentRequest(final String url) {
            super.backgroundHandler.post(new Runnable() {
                @Override
                public void run() {
                    //log.info("trying to request payment request from {}", url);

                    final Request.Builder request = new Request.Builder();
                    request.url(url);
                    request.cacheControl(new CacheControl.Builder().noCache().build());
                    request.header("Accept", PaymentProtocol.MIMETYPE_PAYMENTREQUEST);
                    if (userAgent != null)
                        request.header("User-Agent", userAgent);

                    final Call call = Constants.HTTP_CLIENT.newCall(request.build());
                    try {
                        final Response response = call.execute();
                        if (response.isSuccessful()) {
                            final String contentType = response.header("Content-Type");
                            final InputStream is = response.body().byteStream();
                            new InputParser.StreamInputParser(contentType, is) {
                                @Override
                                protected void handlePaymentIntent(final PaymentIntent paymentIntent) {
                                    //log.info("received {} via http", paymentIntent);

                                    onPaymentIntent(paymentIntent);
                                }

                                @Override
                                protected void error(final int messageResId, final Object... messageArgs) {
                                    onFail(messageResId, messageArgs);
                                }
                            }.parse();
                            is.close();
                        } else {
                            final int responseCode = response.code();
                            final String responseMessage = response.message();

                            //log.info("got http error {}: {}", responseCode, responseMessage);
                            onFail(R.string.error_http, responseCode, responseMessage);
                        }
                    } catch (final IOException x) {
                        //log.info("problem sending", x);

                        onFail(R.string.error_io, x.getMessage());
                    }
                }
            });
        }
    }

    public abstract void requestPaymentRequest(String url);

    protected void onPaymentIntent(final PaymentIntent paymentIntent) {
        callbackHandler.post(new Runnable() {
            @Override
            public void run() {
                resultCallback.onPaymentIntent(paymentIntent);
            }
        });
    }

    protected void onFail(final int messageResId, final Object... messageArgs) {
        callbackHandler.post(() -> resultCallback.onFail(messageResId, messageArgs));
    }
}
