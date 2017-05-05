package org.wordpress.android.fluxc.annotations.action;

public abstract class ActionBuilder {
    public static Action<Void> generateNoPayloadAction(IAction actionType) {
        return new Action<>(actionType, null);
    }

    public static NextableAction<Void> generateNoPayloadNextableAction(IAction actionType) {
        return new NextableAction<>(actionType, null);
    }
}
