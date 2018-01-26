package org.wordpress.android.fluxc.utils;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.util.DateTimeUtils;

import java.util.Date;

@RunWith(RobolectricTestRunner.class)
public class DateTimeUtilsTest {
    @Test
    public void test8601DateStringToDateObject() {
        String iso8601date = "1955-11-05T06:15:00-0800";
        String iso8601dateUTC = "1955-11-05T14:15:00+00:00";

        // A UTC ISO 8601 date converted to Date and back should be unaltered
        Date result = DateTimeUtils.dateUTCFromIso8601(iso8601dateUTC);
        Assert.assertEquals(iso8601dateUTC, DateTimeUtils.iso8601UTCFromDate(result));

        // An ISO 8601 date with timezone offset converted to Date and back should be in UTC format
        result = DateTimeUtils.dateUTCFromIso8601(iso8601date);
        Assert.assertEquals(iso8601dateUTC, DateTimeUtils.iso8601UTCFromDate(result));
    }
}
