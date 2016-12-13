package org.wordpress.android.fluxc.instaflux;

import android.net.Uri;

import org.wordpress.android.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stolen from https://github.com/wordpress-mobile/WordPress-Android/blob/develop/WordPress/src/main/java/org/wordpress/android/ui/reader/utils/ReaderImageScanner.java
 * Originally developed by @nbradbury, ty!
 */
public class ImageUtils {
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile(
            "<img(\\s+.*?)(?:src\\s*=\\s*(?:'|\")(.*?)(?:'|\"))(.*?)>",
            Pattern.DOTALL| Pattern.CASE_INSENSITIVE);

    // regex for matching src attributes in tags
    private static final Pattern SRC_ATTR_PATTERN = Pattern.compile(
            "src\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    // regex for matching width attributes in tags
    private static final Pattern WIDTH_ATTR_PATTERN = Pattern.compile(
            "width\\s*=\\s*(?:'|\")(.*?)(?:'|\")",
            Pattern.DOTALL|Pattern.CASE_INSENSITIVE);

    /*
     * used when a post doesn't have a featured image assigned, searches post's content
     * for an image that may be large enough to be suitable as a featured image
     */
    public String getLargestImage(String content, int minImageWidth) {
        if (content == null || !content.contains("<img")) {
            return null;
        }

        String currentImageUrl = null;
        int currentMaxWidth = minImageWidth;

        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(content);
        while (imgMatcher.find()) {
            String imgTag = content.substring(imgMatcher.start(), imgMatcher.end());
            String imageUrl = getSrcAttrValue(imgTag);

            int width = Math.max(getWidthAttrValue(imgTag), getIntQueryParam(imageUrl, "w"));
            if (width > currentMaxWidth) {
                currentImageUrl = imageUrl;
                currentMaxWidth = width;
            }
        }

        return currentImageUrl;
    }

    /*
     * returns the value from the src attribute in the passed html tag
     */
    private static String getSrcAttrValue(final String tag) {
        if (tag == null) {
            return null;
        }

        Matcher matcher = SRC_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            // remove "src=" and quotes from the result
            return tag.substring(matcher.start() + 5, matcher.end() - 1);
        } else {
            return null;
        }
    }

    /*
    * returns the integer value from the width attribute in the passed html tag
    */
    private static int getWidthAttrValue(final String tag) {
        if (tag == null) {
            return 0;
        }

        Matcher matcher = WIDTH_ATTR_PATTERN.matcher(tag);
        if (matcher.find()) {
            // remove "width=" and quotes from the result
            return StringUtils.stringToInt(tag.substring(matcher.start() + 7, matcher.end() - 1), 0);
        } else {
            return 0;
        }
    }

    /*
     * returns the integer value of the passed query param in the passed url - returns zero
     * if the url is invalid, or the param doesn't exist, or the param value could not be
     * converted to an int
     */
    private static int getIntQueryParam(final String url,
                                        @SuppressWarnings("SameParameterValue") final String param) {
        if (url == null
                || param == null
                || !url.startsWith("http")
                || !url.contains(param + "=")) {
            return 0;
        }
        return StringUtils.stringToInt(Uri.parse(url).getQueryParameter(param));
    }
}
