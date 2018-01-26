package org.wordpress.android.fluxc.post;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.wordpress.android.fluxc.model.post.PostLocation;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class PostLocationTest {
    private static final double MAX_LAT = 90;
    private static final double MIN_LAT = -90;
    private static final double MAX_LNG = 180;
    private static final double MIN_LNG = -180;
    private static final double INVALID_LAT_MAX = 91;
    private static final double INVALID_LAT_MIN = -91;
    private static final double INVALID_LNG_MAX = 181;
    private static final double INVALID_LNG_MIN = -181;
    private static final double EQUATOR_LAT = 0;
    private static final double EQUATOR_LNG = 0;

    @Test
    public void testInstantiateValidLocation() {
        PostLocation locationZero = new PostLocation(EQUATOR_LAT, EQUATOR_LNG);
        Assert.assertTrue("ZeroLoc did not instantiate valid location", locationZero.isValid());
        Assert.assertEquals("ZeroLoc did not return correct lat", EQUATOR_LAT, locationZero.getLatitude());
        Assert.assertEquals("ZeroLoc did not return correct lng", EQUATOR_LNG, locationZero.getLongitude());

        PostLocation locationMax = new PostLocation(MAX_LAT, MAX_LNG);
        Assert.assertTrue("MaxLoc did not instantiate valid location", locationMax.isValid());
        Assert.assertEquals("MaxLoc did not return correct lat", MAX_LAT, locationMax.getLatitude());
        Assert.assertEquals("MaxLoc did not return correct lng", MAX_LNG, locationMax.getLongitude());

        PostLocation locationMin = new PostLocation(MIN_LAT, MIN_LNG);
        Assert.assertTrue("MinLoc did not instantiate valid location", locationMin.isValid());
        Assert.assertEquals("MinLoc did not return correct lat", MIN_LAT, locationMin.getLatitude());
        Assert.assertEquals("MinLoc did not return correct lng", MIN_LNG, locationMin.getLongitude());

        double miscLat = 34;
        double miscLng = -60;
        PostLocation locationMisc = new PostLocation(miscLat, miscLng);
        Assert.assertTrue("MiscLoc did not instantiate valid location", locationMisc.isValid());
        Assert.assertEquals("MiscLoc did not return correct lat", miscLat, locationMisc.getLatitude());
        Assert.assertEquals("MiscLoc did not return correct lng", miscLng, locationMisc.getLongitude());
    }

    @Test
    public void testDefaultLocationInvalid() {
        PostLocation location = new PostLocation();
        Assert.assertFalse("Empty location should be invalid", location.isValid());
    }

    @Test
    public void testInvalidLatitude() {
        PostLocation location = new PostLocation();

        try {
            location.setLatitude(INVALID_LAT_MAX);
            Assert.fail("Lat less than min should have failed");
        } catch (IllegalArgumentException e) {
            Assert.assertFalse("Invalid setLatitude and still valid", location.isValid());
        }

        try {
            location.setLatitude(INVALID_LAT_MIN);
            Assert.fail("Lat less than min should have failed");
        } catch (IllegalArgumentException e) {
            Assert.assertFalse("Invalid setLatitude and still valid", location.isValid());
        }
    }

    @Test
    public void testInvalidLongitude() {
        PostLocation location = new PostLocation();

        try {
            location.setLongitude(INVALID_LNG_MAX);
            Assert.fail("Lng less than min should have failed");
        } catch (IllegalArgumentException e) {
            Assert.assertFalse("Invalid setLongitude and still valid", location.isValid());
        }

        try {
            location.setLongitude(INVALID_LNG_MIN);
            Assert.fail("Lat less than min should have failed");
        } catch (IllegalArgumentException e) {
            Assert.assertFalse("Invalid setLongitude and still valid", location.isValid());
        }
    }
}
