package org.wordpress.android.fluxc;

import org.wordpress.android.fluxc.network.BaseRequest;
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError;

import java.lang.reflect.Field;

public abstract class BasePayload {
    public BaseNetworkError error;

    public boolean isError() {
        try {
            Field field = getClass().getField("error");
            return field.get(this) != null;
        } catch (Exception e) {
            return true;
        }
    }

    @Override
    protected BasePayload clone() throws CloneNotSupportedException {
        if (!(this instanceof Cloneable)) {
            throw new CloneNotSupportedException("Class " + getClass().getName() + " doesn't implement Cloneable");
        }

        BasePayload clonedPayload = (BasePayload) super.clone();

        // Clone non-primitive, mutable fields
        if (this.error != null) {
            clonedPayload.error = new BaseRequest.BaseNetworkError(this.error);
        }

        return clonedPayload;
    }
}
