package org.wordpress.android.fluxc;

import org.greenrobot.eventbus.EventBus;
import org.wordpress.android.fluxc.annotations.action.Action;
import org.wordpress.android.fluxc.store.Store;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import javax.inject.Singleton;

@Singleton
public class Dispatcher {
    private final EventBus mBus;

    public Dispatcher() {
        mBus = EventBus.builder()
                .logNoSubscriberMessages(true)
                .sendNoSubscriberEvent(true)
                .throwSubscriberException(true)
                .build();
    }

    public void register(final Object object) {
        mBus.register(object);
        if (object instanceof Store) {
            ((Store) object).onRegister();
        }
    }

    public void unregister(final Object object) {
        mBus.unregister(object);
    }

    public <T extends RequestPayload> RequestPayload dispatchAsk(Action<T> action) {
        dispatchNoReturn(action);
        return action.getPayload();
    }

    public <T extends ResponsePayload> void dispatchRet(Action<T> action) {
        dispatchNoReturn(action);
    }

    private void dispatchNoReturn(Action action) {
        AppLog.d(T.API, "Dispatching action: " + action.getType().getClass().getSimpleName()
                + "-" + action.getType().name());
        post(action);
    }

    public void emitChange(final Object changeEvent) {
        mBus.post(changeEvent);
    }

    private void post(final Object event) {
        mBus.post(event);
    }
}
