package org.wordpress.android.fluxc.utils;

import junit.framework.Assert;

import org.junit.Test;

public class MediaUtilsTest {
    @Test
    public void testSupportedAndUnsupportedMimeTypeWPCOM() {
        // Image
        final String[] validImageMimeTypes = {
                "image/jpeg", "image/png"
        };
        final String[] invalidImageMimeTypes = {
                "image/jpg", "imagejpg", "video/jpg", "", null, "/", "image/jpg/png", "jpg", "jpg/image", "image/mp4",
                "image/x-icon", "image/tiff"
        };

        for (String validImageMimeType : validImageMimeTypes) {
            Assert.assertTrue(MediaUtils.isSupportedMimeTypeWPCOM(validImageMimeType));
        }
        for (String invalidImageMimeType : invalidImageMimeTypes) {
            Assert.assertFalse(MediaUtils.isSupportedMimeTypeWPCOM(invalidImageMimeType));
        }

        // Video
        final String[] validVideoMimeTypes = {
                "video/mp4", "video/3gpp"
        };
        final String[] invalidVideoMimeTypes = {
                "videomp4", "image/mp4", "", null, "/", "video/mp4/mkv", "mp4", "mp4/video", "video/*", "video/png"
        };

        for (String validVideoMimeType : validVideoMimeTypes) {
            Assert.assertTrue(MediaUtils.isSupportedMimeTypeWPCOM(validVideoMimeType));
        }
        for (String invalidVideoMimeType : invalidVideoMimeTypes) {
            Assert.assertFalse(MediaUtils.isSupportedMimeTypeWPCOM(invalidVideoMimeType));
        }

        // Audio
        final String[] validAudioMimeTypes = {
                "audio/mpeg", "audio/wav"
        };
        final String[] invalidAudioMimeTypes = {
                "audio/mp3", "video/mp3", "", null, "/", "audio/mp4/mkv", "mp3", "mp4/audio", "audio/png"
        };

        for (String validAudioMimeType : validAudioMimeTypes) {
            Assert.assertTrue(MediaUtils.isSupportedMimeTypeWPCOM(validAudioMimeType));
        }
        for (String invalidAudioMimeType : invalidAudioMimeTypes) {
            Assert.assertFalse(MediaUtils.isSupportedMimeTypeWPCOM(invalidAudioMimeType));
        }

        // Document
        final String[] validApplicationMimeTypes = {
                "application/pdf", "application/vnd.ms-powerpoint"
        };
        final String[] invalidApplicationMimeTypes = {
                "application/octet-stream", "applicationpdf", "audio/pdf", "", null, "/",
                "application/pdf/doc", "pdf", "pdf/application", "application/png"
        };

        for (String validApplicationMimeType : validApplicationMimeTypes) {
            Assert.assertTrue(MediaUtils.isSupportedMimeTypeWPCOM(validApplicationMimeType));
        }
        for (String invalidApplicationMimeType : invalidApplicationMimeTypes) {
            Assert.assertFalse(MediaUtils.isSupportedMimeTypeWPCOM(invalidApplicationMimeType));
        }
    }

    @Test
    public void testSupportedAndUnsupportedMimeTypeSelfHosted() {
        // Image
        final String[] validImageMimeTypes = {
                "image/x-icon", "image/tiff"
        };

        for (String validImageMimeType : validImageMimeTypes) {
            Assert.assertTrue(MediaUtils.isSupportedMimeTypeSelfHosted(validImageMimeType));
        }

        // Document
        final String[] validApplicationMimeTypes = {
                "application/octet-stream"
        };
        for (String validApplicationMimeType : validApplicationMimeTypes) {
            Assert.assertTrue(MediaUtils.isSupportedMimeTypeSelfHosted(validApplicationMimeType));
        }
    }

    @Test
    public void testSupportedImageFileRecognition() {
        final String[] supportedImageTypes = {"jpg", "gif", "jpeg"};
        for (String supportedImageType : supportedImageTypes) {
            String currentMime = MediaUtils.getMimeTypeForExtension(supportedImageType);
            Assert.assertTrue(currentMime != null && currentMime.startsWith("image/"));
        }
    }

    @Test
    public void testSupportedVideoFileRecognition() {
        final String[] supportedVideoTypes = {"mov", "mp4", "ogv", "mpg"};
        for (String supportedVideoType : supportedVideoTypes) {
            String currentMime = MediaUtils.getMimeTypeForExtension(supportedVideoType);
            Assert.assertTrue(currentMime != null && currentMime.startsWith("video/"));
        }
    }

    @Test
    public void testSupportedAudioFileRecognition() {
        final String[] supportedAudioTypes = {"m4a", "mp3", "ogg", "wav"};
        for (String supportedAudioType : supportedAudioTypes) {
            String currentMime = MediaUtils.getMimeTypeForExtension(supportedAudioType);
            Assert.assertTrue(currentMime != null && currentMime.startsWith("audio/"));
        }
    }
}
