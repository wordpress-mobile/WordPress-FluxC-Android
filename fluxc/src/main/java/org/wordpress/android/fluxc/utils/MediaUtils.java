package org.wordpress.android.fluxc.utils;

import android.text.TextUtils;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MediaUtils {
    public static final double MEMORY_LIMIT_FILESIZE_MULTIPLIER = 0.75D;

    //
    // MIME types
    //

    // ref https://en.support.wordpress.com/accepted-filetypes/
    private static final Map<String, String> EXTENSIONS_TO_MIME_TYPE_WPCOM;
    // Ref https://codex.wordpress.org/Uploading_Files
    private static final Map<String, String> EXTENSIONS_TO_MIME_TYPE_SELF_HOSTED;
    static {
        Map<String, String> wpcomMap = new HashMap<>();
        wpcomMap.put("jpg", "image/jpeg");
        wpcomMap.put("jpeg", "image/jpeg");
        wpcomMap.put("jpe", "image/jpeg");
        wpcomMap.put("gif", "image/gif");
        wpcomMap.put("png", "image/png");
        // Video formats.
        wpcomMap.put("wmv", "video/x-ms-wmv");
        wpcomMap.put("wmx", "video/x-ms-wmx");
        wpcomMap.put("wm", "video/x-ms-wm");
        wpcomMap.put("avi", "video/avi");
        wpcomMap.put("mov", "video/quicktime");
        wpcomMap.put("qt", "video/quicktime");
        wpcomMap.put("mpeg", "video/mpeg");
        wpcomMap.put("mpg", "video/mpeg");
        wpcomMap.put("mpe", "video/mpeg");
        wpcomMap.put("mp4", "video/mp4");
        wpcomMap.put("m4v", "video/mp4");
        wpcomMap.put("ogv", "video/ogg");
        wpcomMap.put("webm", "video/webm");
        wpcomMap.put("3gp", "video/3gpp"); // Can also be audio
        wpcomMap.put("3gpp", "video/3gpp"); // Can also be audio
        wpcomMap.put("3g2", "video/3gpp2"); // Can also be audio
        wpcomMap.put("3gp2", "video/3gpp2"); // Can also be audio
        // Audio formats.
        wpcomMap.put("mp3", "audio/mpeg");
        wpcomMap.put("m4a", "audio/mpeg");
        wpcomMap.put("m4b", "audio/mpeg");
        wpcomMap.put("wav", "audio/wav");
        wpcomMap.put("ogg", "audio/ogg");
        wpcomMap.put("oga", "audio/ogg");
        // Document formats
        wpcomMap.put("pdf", "application/pdf");
        wpcomMap.put("odt", "application/vnd.oasis.opendocument.text");
        wpcomMap.put("doc", "application/msword");
        wpcomMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        wpcomMap.put("ppt", "application/vnd.ms-powerpoint");
        wpcomMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        wpcomMap.put("pps", "application/vnd.ms-powerpoint");
        wpcomMap.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        wpcomMap.put("xls", "application/vnd.ms-excel");
        wpcomMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        wpcomMap.put("key", "application/vnd.apple.keynote");
        wpcomMap.put("zip", "application/zip");

        EXTENSIONS_TO_MIME_TYPE_WPCOM = Collections.unmodifiableMap(wpcomMap);

        // Ref https://codex.wordpress.org/Uploading_Files
        Map<String, String> selfHostedMap = new HashMap<>();
        // Images
        selfHostedMap.put("tiff", "image/tiff");
        selfHostedMap.put("ico", "image/x-icon");
        // Document
        selfHostedMap.put("psd", "application/octet-stream");

        selfHostedMap.putAll(wpcomMap);
        EXTENSIONS_TO_MIME_TYPE_SELF_HOSTED = Collections.unmodifiableMap(selfHostedMap);
    }

    public static final String MIME_TYPE_IMAGE = "image/";
    public static final String MIME_TYPE_VIDEO = "video/";
    public static final String MIME_TYPE_AUDIO = "audio/";
    public static final String MIME_TYPE_APPLICATION = "application/";

    public static boolean isSupportedImageExtWPCOM(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE_WPCOM.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_IMAGE);
    }

    public static boolean isSupportedVideoExtWPCOM(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE_WPCOM.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_VIDEO);
    }

    public static boolean isSupportedAudioExtWPCOM(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE_WPCOM.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_AUDIO);
    }

    public static boolean isSupportedApplicationExtWPCOM(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE_WPCOM.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_APPLICATION);
    }

    public static boolean isSupportedFileExtWPCOM(String extension) {
        return isSupportedImageExtWPCOM(extension)
                || isSupportedVideoExtWPCOM(extension)
                || isSupportedAudioExtWPCOM(extension)
                || isSupportedApplicationExtWPCOM(extension);
    }

    public static String getMimeTypeForExtension(String extension) {
        // Use the self hosted map since it does contain more items.
        return EXTENSIONS_TO_MIME_TYPE_SELF_HOSTED.get(extension);
    }

    public static boolean isSupportedMimeTypeWPCOM(String mime) {
        return EXTENSIONS_TO_MIME_TYPE_WPCOM.containsValue(mime);
    }

    public static boolean isSupportedMimeTypeSelfHosted(String mime) {
        return EXTENSIONS_TO_MIME_TYPE_SELF_HOSTED.containsValue(mime);
    }

    public static boolean isVideoMimeType(String type) {
        return !TextUtils.isEmpty(type)
                && isSupportedMimeTypeSelfHosted(type)
                && type.startsWith(MIME_TYPE_VIDEO);
    }

    //
    // File operations
    //

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
