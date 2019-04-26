package com.bitcoin.wallet.btc.utils;

import android.view.View;
import android.view.ViewTreeObserver;

import java.util.concurrent.atomic.AtomicBoolean;

public class OnFirstPreDraw implements ViewTreeObserver.OnPreDrawListener {
    public static interface Callback {
        boolean onFirstPreDraw();
    }

    public static void listen(final View view, final Callback callback) {
        new OnFirstPreDraw(view.getViewTreeObserver(), callback);
    }

    private final ViewTreeObserver viewTreeObserver;
    private final Callback callback;
    private final AtomicBoolean fired = new AtomicBoolean(false);


    private OnFirstPreDraw(final ViewTreeObserver viewTreeObserver, final Callback callback) {
        this.viewTreeObserver = viewTreeObserver;
        this.callback = callback;
        viewTreeObserver.addOnPreDrawListener(this);
    }

    @Override
    public boolean onPreDraw() {
        if (viewTreeObserver.isAlive())
            viewTreeObserver.removeOnPreDrawListener(this);

        if (!fired.getAndSet(true))
            return callback.onFirstPreDraw();
        return true;
    }
}