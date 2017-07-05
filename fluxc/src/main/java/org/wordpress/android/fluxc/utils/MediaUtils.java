package org.wordpress.android.fluxc.utils;

import android.text.TextUtils;
import android.util.ArrayMap;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MediaUtils {
    //
    // MIME types
    //

    // ref https://en.support.wordpress.com/accepted-filetypes/
    // Ref https://codex.wordpress.org/Uploading_Files
    private static final Map<String, String> EXTENSIONS_TO_MIME_TYPE;
    static {
        Map<String, String> aMap = new HashMap<>();
        aMap.put("jpg", "image/jpeg");
        aMap.put("jpeg", "image/jpeg");
        aMap.put("jpe", "image/jpeg");
        aMap.put("gif", "image/gif");
        aMap.put("png", "image/png");
        // Video formats.
        aMap.put("wmv", "video/x-ms-wmv");
        aMap.put("wmx", "video/x-ms-wmx");
        aMap.put("wm", "video/x-ms-wm");
        aMap.put("avi", "video/avi");
        aMap.put("mov", "video/quicktime");
        aMap.put("qt", "video/quicktime");
        aMap.put("mpeg", "video/mpeg");
        aMap.put("mpg", "video/mpeg");
        aMap.put("mpe", "video/mpeg");
        aMap.put("mp4", "video/mp4");
        aMap.put("m4v", "video/mp4");
        aMap.put("ogv", "video/ogg");
        aMap.put("webm", "video/webm");
        aMap.put("3gp", "video/3gpp"); // Can also be audio
        aMap.put("3gpp", "video/3gpp"); // Can also be audio
        aMap.put("3g2", "video/3gpp2"); // Can also be audio
        aMap.put("3gp2", "video/3gpp2"); // Can also be audio
        // Audio formats.
        aMap.put("mp3", "audio/mpeg");
        aMap.put("m4a", "audio/mpeg");
        aMap.put("m4b", "audio/mpeg");
        aMap.put("wav", "audio/wav");
        aMap.put("ogg", "audio/ogg");
        aMap.put("oga", "audio/ogg");
        // Document formats
        aMap.put("pdf", "application/pdf");
        aMap.put("odt", "application/vnd.oasis.opendocument.text");
        aMap.put("doc", "application/msword");
        aMap.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        aMap.put("ppt", "application/vnd.ms-powerpoint");
        aMap.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
        aMap.put("pps", "application/vnd.ms-powerpoint");
        aMap.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");
        aMap.put("xls", "application/vnd.ms-excel");
        aMap.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        aMap.put("key", "application/vnd.apple.keynote");
        aMap.put("zip", "application/zip");


        EXTENSIONS_TO_MIME_TYPE = Collections.unmodifiableMap(aMap);
    }

    public static final String MIME_TYPE_IMAGE = "image/";
    public static final String MIME_TYPE_VIDEO = "video/";
    public static final String MIME_TYPE_AUDIO = "audio/";
    public static final String MIME_TYPE_APPLICATION = "application/";

    public static boolean isSupportedImageExt(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_IMAGE);
    }

    public static boolean isSupportedVideoExt(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_VIDEO);
    }

    public static boolean isSupportedAudioExt(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_AUDIO);
    }

    public static boolean isSupportedApplicationExt(String extension) {
        String mimeType = EXTENSIONS_TO_MIME_TYPE.get(extension);
        return !TextUtils.isEmpty(mimeType) && mimeType.startsWith(MIME_TYPE_APPLICATION);
    }

    public static boolean isSupportedFileExt(String extension) {
        return isSupportedImageExt(extension)
                || isSupportedVideoExt(extension)
                || isSupportedAudioExt(extension)
                || isSupportedApplicationExt(extension);
    }
    
    public static String getMimeTypeForExtension(String extension) {
        return EXTENSIONS_TO_MIME_TYPE.get(extension);
    }

    public static boolean isSupportedMimeType(String mime) {
        return EXTENSIONS_TO_MIME_TYPE.containsValue(mime);
    }

    public static boolean isVideoMimeType(String type) {
        return !TextUtils.isEmpty(type)
                && isSupportedMimeType(type)
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
}
