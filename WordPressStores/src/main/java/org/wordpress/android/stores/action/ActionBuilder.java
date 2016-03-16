package org.wordpress.android.stores.action;

public class ActionBuilder {
    public static Action<Void> generateNoPayloadAction(IAction actionType) {
        return new Action<>(actionType, null);
    }
}
