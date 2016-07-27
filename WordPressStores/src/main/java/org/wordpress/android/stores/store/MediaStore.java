package org.wordpress.android.stores.store;

import org.wordpress.android.stores.Dispatcher;
import org.wordpress.android.stores.action.MediaAction;
import org.wordpress.android.stores.annotations.action.Action;
import org.wordpress.android.stores.network.rest.wpcom.media.MediaRestClient;

import javax.inject.Inject;

public class MediaStore extends Store {
    private MediaRestClient mMediaRestClient;

    @Inject
    public MediaStore(Dispatcher dispatcher, MediaRestClient mediaRestClient) {
        super(dispatcher);
        mMediaRestClient = mediaRestClient;
    }

    public MediaStore(Dispatcher dispatcher) {
        super(dispatcher);
    }

    @Override
    public void onAction(Action action) {
        if (action.getType() == MediaAction.FETCH_ALL_MEDIA) {
            MediaRestClient.MediaFetchPayload payload = (MediaRestClient.MediaFetchPayload) action.getPayload();
            mMediaRestClient.fetchAllMedia(payload.site);
        }
    }

    @Override
    public void onRegister() {
    }
}
