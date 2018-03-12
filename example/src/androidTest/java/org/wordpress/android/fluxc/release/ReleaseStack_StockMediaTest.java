package org.wordpress.android.fluxc.release;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.greenrobot.eventbus.Subscribe;
import org.junit.Test;
import org.wordpress.android.fluxc.TestUtils;
import org.wordpress.android.fluxc.generated.MediaActionBuilder;
import org.wordpress.android.fluxc.generated.StockMediaActionBuilder;
import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.model.StockMediaModel;
import org.wordpress.android.fluxc.store.MediaStore;
import org.wordpress.android.fluxc.store.StockMediaStore;
import org.wordpress.android.util.AppLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.wordpress.android.fluxc.network.rest.wpcom.stockmedia.StockMediaRestClient
        .DEFAULT_NUM_STOCK_MEDIA_PER_FETCH;

public class ReleaseStack_StockMediaTest extends ReleaseStack_WPComBase {
    @Inject StockMediaStore mStockMediaStore;

    private enum TestEvents {
        NONE,
        FETCHED_STOCK_MEDIA_LIST_PAGE_ONE,
        FETCHED_STOCK_MEDIA_LIST_PAGE_TWO,
        UPLOADED_STOCK_MEDIA_SINGLE,
        UPLOADED_STOCK_MEDIA_MULTI
    }

    private TestEvents mNextEvent;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mReleaseStackAppComponent.inject(this);
        init();
    }

    private static final String SEARCH_TERM = "beach";
    private List<StockMediaModel> mFirstPageMedia;

    @Test
    public void testFetchStockMediaList() throws InterruptedException {
        mNextEvent = TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_ONE;
        fetchStockMediaList(SEARCH_TERM, 1);

        mNextEvent = TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_TWO;
        fetchStockMediaList(SEARCH_TERM, 2);
    }

    @Test
    public void testUploadStockMedia() throws InterruptedException {
        mNextEvent = TestEvents.UPLOADED_STOCK_MEDIA_SINGLE;
        StockMediaModel testStockMedia1 = newStockMedia(902152);
        List<StockMediaModel> testStockMediaList = new ArrayList<>();
        testStockMediaList.add(testStockMedia1);
        uploadStockMedia(testStockMediaList);

        mNextEvent = TestEvents.UPLOADED_STOCK_MEDIA_MULTI;
        StockMediaModel testStockMedia2 = newStockMedia(208803);
        testStockMediaList.add(testStockMedia2);
        uploadStockMedia(testStockMediaList);
    }

    private StockMediaModel newStockMedia(int id) {
        String name = "pexels-photo-" + id;
        String url = "https://images.pexels.com/photos/" + id + "/" + name + ".jpeg?w=320";

        StockMediaModel stockMedia = new StockMediaModel();
        stockMedia.setName(name);
        stockMedia.setTitle(name);
        stockMedia.setUrl(url);

        return stockMedia;
    }

    private void fetchStockMediaList(@NonNull String searchTerm, int page) throws InterruptedException {
        StockMediaStore.FetchStockMediaListPayload fetchPayload =
                new StockMediaStore.FetchStockMediaListPayload(searchTerm, page);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(StockMediaActionBuilder.newFetchStockMediaAction(fetchPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void uploadStockMedia(@NonNull List<StockMediaModel> stockMediaList) throws InterruptedException {
        StockMediaStore.UploadStockMediaPayload uploadPayload =
                new StockMediaStore.UploadStockMediaPayload(sSite, stockMediaList);
        mCountDownLatch = new CountDownLatch(1);
        mDispatcher.dispatch(StockMediaActionBuilder.newUploadStockMediaAction(uploadPayload));
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private boolean isInFirstPageOfResults(@NonNull StockMediaModel media) {
        for (StockMediaModel modelFromFirstPage : mFirstPageMedia) {
            if (modelFromFirstPage.equals(media)) {
                AppLog.w(AppLog.T.MEDIA, "Found dup stock media with ID " + media.getId()
                        + " (" + modelFromFirstPage.getId() + ")");
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onStockMediaListFetched(StockMediaStore.OnStockMediaListFetched event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: "
                    + event.error.type);
        }

        boolean isPageOne = event.nextPage == 2;
        boolean isPageTwo = event.nextPage == 3;

        if (isPageOne) {
            assertEquals(mNextEvent, TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_ONE);
        } else if (isPageTwo) {
            assertEquals(mNextEvent, TestEvents.FETCHED_STOCK_MEDIA_LIST_PAGE_TWO);
        } else {
            throw new AssertionError("Wrong next page received: " + event.nextPage);
        }
        assertEquals(event.mediaList.size(), DEFAULT_NUM_STOCK_MEDIA_PER_FETCH);

        // remember the results if this is the first page, otherwise make sure the second page
        // isn't the same as the first page (note that dups between pages are possible)
        if (isPageOne) {
            mFirstPageMedia = event.mediaList;
        } else {
            boolean areBothPagesTheSame = true;
            for (StockMediaModel media : event.mediaList) {
                if (!isInFirstPageOfResults(media)) {
                    areBothPagesTheSame = false;
                    break;
                }
            }
            assertFalse(areBothPagesTheSame);
        }

        mCountDownLatch.countDown();
    }

    @SuppressWarnings("unused")
    @Subscribe
    public void onStockMediaUploaded(StockMediaStore.OnStockMediaUploaded event) {
        if (event.isError()) {
            throw new AssertionError("Unexpected error occurred with type: "
                                     + event.error.type);
        }

        deleteUploadedMedia(event.mediaList);

        boolean isSingleUpload = mNextEvent == TestEvents.UPLOADED_STOCK_MEDIA_SINGLE;
        boolean isMultiUpload = mNextEvent == TestEvents.UPLOADED_STOCK_MEDIA_MULTI;

        if (isSingleUpload) {
            assertEquals(event.mediaList.size(), 1);
        } else if (isMultiUpload) {
            assertEquals(event.mediaList.size(), 2);
        } else {
            throw new AssertionError("Wrong event after upload");
        }

        mCountDownLatch.countDown();
    }

    private void deleteUploadedMedia(@Nullable List<MediaModel> mediaList) {
        if (mediaList == null) return;

        for (MediaModel media: mediaList) {
            MediaStore.MediaPayload deletePayload = new MediaStore.MediaPayload(sSite, media);
            mDispatcher.dispatch(MediaActionBuilder.newDeleteMediaAction(deletePayload));
        }
    }
}
