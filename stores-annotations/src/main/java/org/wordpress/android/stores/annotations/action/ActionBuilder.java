package org.wordpress.android.stores.annotations.action;

public abstract class ActionBuilder {
    public static Action<Void> generateNoPayloadAction(IAction actionType) {
        return new Action<>(actionType, null);
    }
}
