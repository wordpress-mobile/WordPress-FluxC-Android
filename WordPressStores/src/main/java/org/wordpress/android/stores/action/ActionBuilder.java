package org.wordpress.android.stores.action;

import org.wordpress.android.stores.annotations.Action;
import org.wordpress.android.stores.annotations.IAction;

public class ActionBuilder {
    public static Action<Void> generateNoPayloadAction(IAction actionType) {
        return new Action<>(actionType, null);
    }
}
