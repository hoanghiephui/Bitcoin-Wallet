package com.bitcoin.wallet.btc.utils;

public class Event<T> {
    private final T content;
    private boolean hasBeenHandled = false;

    public static Event<Void> simple() {
        return new Event<>(null);
    }

    public Event(final T content) {
        this.content = content;
    }

    public boolean hasBeenHandled() {
        return hasBeenHandled;
    }

    public T getContentOrThrow() {
        hasBeenHandled = true;
        return content;
    }

    public T getContentIfNotHandled() {
        if (hasBeenHandled)
            return null;
        hasBeenHandled = true;
        return content;
    }

    public static abstract class Observer<T> implements androidx.lifecycle.Observer<Event<T>> {
        @Override
        public final void onChanged(final Event<T> event) {
            if (!event.hasBeenHandled())
                onEvent(event.getContentOrThrow());
        }

        public abstract void onEvent(final T content);
    }
}

