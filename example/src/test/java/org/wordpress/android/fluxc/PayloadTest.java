package org.wordpress.android.fluxc;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.network.BaseRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class PayloadTest {
    private class CloneableRequestPayload extends RequestPayload implements Cloneable {
        @Override
        public CloneableRequestPayload clone() {
            try {
                return (CloneableRequestPayload) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(); // Can't happen
            }
        }
    }

    @Test
    public void testRequestClone() {
        // Cloning default (no error) payload
        CloneableRequestPayload errorlessPayload = new CloneableRequestPayload();

        CloneableRequestPayload errorlessClone = errorlessPayload.clone();

        assertFalse(errorlessPayload == errorlessClone);
        assertNull(errorlessPayload.error);
        assertNull(errorlessClone.error);

        // Cloning payload with error field
        CloneableRequestPayload errorPayload = new CloneableRequestPayload();

        errorPayload.error = new BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.SERVER_ERROR);

        CloneableRequestPayload errorClone = errorPayload.clone();

        assertFalse(errorPayload == errorClone);

        // The error field should be cloned
        assertNotEquals(errorClone.error, errorPayload.error);
        assertEquals(errorClone.error.type, errorPayload.error.type);
    }

    private class CloneableResponsePayload extends RequestPayload implements Cloneable {
        @Override
        public CloneableResponsePayload clone() {
            try {
                return (CloneableResponsePayload) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError(); // Can't happen
            }
        }
    }

    @Test
    public void testResponseClone() {
        // Cloning default (no error) payload
        CloneableResponsePayload errorlessPayload = new CloneableResponsePayload();

        CloneableResponsePayload errorlessClone = errorlessPayload.clone();

        assertFalse(errorlessPayload == errorlessClone);
        assertNull(errorlessPayload.error);
        assertNull(errorlessClone.error);

        // Cloning payload with error field
        CloneableResponsePayload errorPayload = new CloneableResponsePayload();

        errorPayload.error = new BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.SERVER_ERROR);

        CloneableResponsePayload errorClone = errorPayload.clone();

        assertFalse(errorPayload == errorClone);

        // The error field should be cloned
        assertNotEquals(errorClone.error, errorPayload.error);
        assertEquals(errorClone.error.type, errorPayload.error.type);
    }
}
