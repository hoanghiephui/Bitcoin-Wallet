package com.bitcoin.wallet.btc.ui.widget;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import com.bitcoin.wallet.btc.R;

public class DialogBuilder extends AlertDialog.Builder {
    private final View customTitle;
    private final ImageView iconView;
    private final TextView titleView;

    public DialogBuilder(final Context context) {
        super(context);
        this.customTitle = LayoutInflater.from(context).inflate(R.layout.dialog_title, null);
        this.iconView = customTitle.findViewById(android.R.id.icon);
        this.titleView = customTitle.findViewById(android.R.id.title);
    }

    public static DialogBuilder warn(final Context context, final int titleResId) {
        final DialogBuilder builder = new DialogBuilder(context);
        builder.setIcon(R.drawable.ic_warning_grey_600_24dp);
        builder.setTitle(titleResId);
        return builder;
    }

    @Override
    public DialogBuilder setIcon(final Drawable icon) {
        if (icon != null) {
            setCustomTitle(customTitle);
            iconView.setImageDrawable(icon);
            iconView.setVisibility(View.VISIBLE);
        }

        return this;
    }

    @Override
    public DialogBuilder setIcon(final int iconResId) {
        if (iconResId != 0) {
            setCustomTitle(customTitle);
            iconView.setImageResource(iconResId);
            iconView.setVisibility(View.VISIBLE);
        }

        return this;
    }

    @Override
    public DialogBuilder setTitle(final CharSequence title) {
        if (title != null) {
            setCustomTitle(customTitle);
            titleView.setText(title);
        }

        return this;
    }

    @Override
    public DialogBuilder setTitle(final int titleResId) {
        if (titleResId != 0) {
            setCustomTitle(customTitle);
            titleView.setText(titleResId);
        }

        return this;
    }

    @Override
    public DialogBuilder setMessage(final CharSequence message) {
        super.setMessage(message);

        return this;
    }

    @Override
    public DialogBuilder setMessage(final int messageResId) {
        super.setMessage(messageResId);

        return this;
    }

    public DialogBuilder singleDismissButton(@Nullable final DialogInterface.OnClickListener dismissListener) {
        setNeutralButton(R.string.btn_dismiss, dismissListener);

        return this;
    }
}
