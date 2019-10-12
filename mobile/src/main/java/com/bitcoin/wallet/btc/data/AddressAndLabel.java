package com.bitcoin.wallet.btc.data;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

import com.bitcoin.wallet.btc.Constants;
import com.google.common.base.Objects;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.NetworkParameters;

public class AddressAndLabel implements Parcelable {
    public static final Parcelable.Creator<AddressAndLabel> CREATOR = new Parcelable.Creator<AddressAndLabel>() {
        @Override
        public AddressAndLabel createFromParcel(final Parcel in) {
            return new AddressAndLabel(in);
        }

        @Override
        public AddressAndLabel[] newArray(final int size) {
            return new AddressAndLabel[size];
        }
    };
    public final Address address;
    public final String label;

    public AddressAndLabel(final Address address, @Nullable final String label) {
        this.address = address;
        this.label = label;
    }

    public AddressAndLabel(final NetworkParameters addressParams, final String address, @Nullable final String label)
            throws AddressFormatException {
        this(Address.fromString(addressParams, address), label);
    }

    private AddressAndLabel(final Parcel in) {
        address = Address.fromString(Constants.NETWORK_PARAMETERS, in.readString());
        label = in.readString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        final AddressAndLabel other = (AddressAndLabel) o;
        return Objects.equal(this.address, other.address) && Objects.equal(this.label, other.label);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(address, label);
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName());
        builder.append('[');
        builder.append(address.toString());
        if (label != null) {
            builder.append(',');
            builder.append(label);
        }
        builder.append(']');
        return builder.toString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(final Parcel dest, final int flags) {
        dest.writeString(address.toString());
        dest.writeString(label);
    }
}
