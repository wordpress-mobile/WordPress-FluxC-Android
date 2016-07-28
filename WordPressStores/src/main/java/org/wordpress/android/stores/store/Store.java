package org.wordpress.android.stores.store;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.annotations.action.Action;

public abstract class Store {
    protected final Dispatcher mDispatcher;

    Store(Dispatcher dispatcher) {
        mDispatcher = dispatcher;
        mDispatcher.register(this);
    }
    public class OnChanged {}

    /**
     * onAction should {@link Subscribe} with ASYNC {@link ThreadMode}.
     */
    public abstract void onAction(Action action);
    public abstract void onRegister();

    protected void emitChange(OnChanged onChangedEvent) {
        mDispatcher.emitChange(onChangedEvent);
    }
}
