package org.wordpress.android.fluxc.annotations.action;

public class NextableAction<T> extends Action<T> {
    private Action mNextAction;

    public NextableAction(IAction actionType, T payload) {
        super(actionType, payload);
    }

    public Action getNextAction() {
        return mNextAction;
    }

    public Action doNextOnSuccess(Action action) {
        mNextAction = action;
        return mNextAction;
    }

    public NextableAction doNextOnSuccess(NextableAction action) {
        mNextAction = action;
        return (NextableAction) mNextAction;
    }
}
