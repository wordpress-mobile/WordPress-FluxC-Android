package org.wordpress.android.fluxc.utils;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.wordpress.android.fluxc.model.MediaModel;
import org.wordpress.android.fluxc.network.BaseUploadRequestBody;

import java.io.File;

public class MediaUtils {
    public static final double MEMORY_LIMIT_FILESIZE_MULTIPLIER = 0.75D;

    //
    // MIME types
    //

    public static final String MIME_TYPE_IMAGE = "image/";
    public static final String MIME_TYPE_VIDEO = "video/";
    public static final String MIME_TYPE_AUDIO = "audio/";
    public static final String MIME_TYPE_APPLICATION = "application/";

    // ref https://en.support.wordpress.com/accepted-filetypes/
    public static final String[] SUPPORTED_IMAGE_SUBTYPES = {
            "jpg", "jpeg", "png", "gif"
    };
    public static final String[] SUPPORTED_VIDEO_SUBTYPES = {
            "mp4", "m4v", "mov", "wmv", "avi", "mpg", "ogv", "3gp", "3gpp", "3gpp2", "3g2", "mpeg", "quicktime", "webm"
    };

    // Following https://en.support.wordpress.com/accepted-filetypes
    // accepted audio file types (extensions) are : mp3, m4a, ogg, wav
    // In the Android API for each file type there is corresponding mime type
    // which can be checked with usage of MimeTypeMap.getSingleton().getMimeTypeFromExtension()
    // Here https://android.googlesource.com/platform/frameworks/base/+/56a2301/media/java/android/media/MediaFile.java
    // we can get mapping of file type extension and mime type {extension : mime_type}
    // which gives supported audio file extensions and mime types ->
    // {"mp3" : "audio/mpeg", "m4a" : "audio/mp4", "ogg" : "audio/ogg", "wav" : "audio/x-wav"}
    // Special case -> On Samsung device for "wav" audio file extension Android API returns "audio/vnd.wave"
    public static final String[] SUPPORTED_AUDIO_SUBTYPES = {
            "mpeg", "mp4", "ogg", "x-wav", "vnd.wave"
    };
    public static final String[] SUPPORTED_APPLICATION_SUBTYPES = {
            "pdf", "doc", "ppt", "odt", "pptx", "docx", "pps", "ppsx", "xls", "xlsx", "key", ".zip"
    };

    public static boolean isImageMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_IMAGE, type);
    }

    public static boolean isVideoMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_VIDEO, type);
    }

    public static boolean isAudioMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_AUDIO, type);
    }

    public static boolean isApplicationMimeType(String type) {
        return isExpectedMimeType(MIME_TYPE_APPLICATION, type);
    }

    public static boolean isSupportedImageMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_IMAGE, SUPPORTED_IMAGE_SUBTYPES, type);
    }

    public static boolean isSupportedVideoMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_VIDEO, SUPPORTED_VIDEO_SUBTYPES, type);
    }

    public static boolean isSupportedAudioMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_AUDIO, SUPPORTED_AUDIO_SUBTYPES, type);
    }

    public static boolean isSupportedApplicationMimeType(String type) {
        return isSupportedMimeType(MIME_TYPE_APPLICATION, SUPPORTED_APPLICATION_SUBTYPES, type);
    }

    public static boolean isSupportedMimeType(String type) {
        return isSupportedImageMimeType(type)
                || isSupportedVideoMimeType(type)
                || isSupportedAudioMimeType(type)
                || isSupportedApplicationMimeType(type);
    }

    public static String getMimeTypeForExtension(String extension) {
        if (isSupportedImageMimeType(MIME_TYPE_IMAGE + extension)) {
            return MIME_TYPE_IMAGE + extension;
        }
        if (isSupportedVideoMimeType(MIME_TYPE_VIDEO + extension)) {
            return MIME_TYPE_VIDEO + extension;
        }
        if (isSupportedAudioMimeType(MIME_TYPE_AUDIO + extension)) {
            return MIME_TYPE_AUDIO + extension;
        }
        if (isSupportedApplicationMimeType(MIME_TYPE_APPLICATION + extension)) {
            return MIME_TYPE_APPLICATION + extension;
        }
        return null;
    }

    private static boolean isExpectedMimeType(String expected, String type) {
        if (type == null) return false;
        String[] split = type.split("/");
        return split.length == 2 && expected.startsWith(split[0]);
    }

    private static boolean isSupportedMimeType(String type, String[] supported, String mimeType) {
        if (type == null || supported == null || mimeType == null) return false;
        for (String supportedSubtype : supported) {
            if (mimeType.equals(type + supportedSubtype)) return true;
        }
        return false;
    }

    //
    // File operations
    //

    public static String getMediaValidationError(@NonNull MediaModel media) {
        return BaseUploadRequestBody.hasRequiredData(media);
    }

    /**
     * Queries filesystem to determine if a given file can be read.
     */
    public static boolean canReadFile(String filePath) {
        if (filePath == null || TextUtils.isEmpty(filePath)) return false;
        File file = new File(filePath);
        return file.canRead();
    }

    /**
     * Returns the substring of characters that follow the final '.' in the given string.
     */
    public static String getExtension(String filePath) {
        if (TextUtils.isEmpty(filePath) || !filePath.contains(".")) return null;
        if (filePath.lastIndexOf(".") + 1 >= filePath.length()) return null;
        return filePath.substring(filePath.lastIndexOf(".") + 1);
    }

    /**
     * Returns the substring of characters that follow the final '/' in the given string.
     */
    public static String getFileName(String filePath) {
        if (TextUtils.isEmpty(filePath) || !filePath.contains("/")) return null;
        if (filePath.lastIndexOf("/") + 1 >= filePath.length()) return null;
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }

    /**
     * Given the memory limit for media for a site, returns the maximum 'safe' file size we can upload to that site.
     */
    public static double getMaxFilesizeForMemoryLimit(double mediaMemoryLimit) {
        return MEMORY_LIMIT_FILESIZE_MULTIPLIER * mediaMemoryLimit;
    }
}
