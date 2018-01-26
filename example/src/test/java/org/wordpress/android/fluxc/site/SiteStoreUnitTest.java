package org.wordpress.android.fluxc.site;

import android.content.Context;
import android.database.sqlite.SQLiteException;

import com.wellsql.generated.SiteModelTable;
import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.WellSqlTestUtils;
import org.wordpress.android.fluxc.model.PostFormatModel;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.SitesModel;
import org.wordpress.android.fluxc.network.rest.wpcom.site.SiteRestClient;
import org.wordpress.android.fluxc.network.xmlrpc.site.SiteXMLRPCClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils.DuplicateSiteException;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.UpdateSitesResult;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.wordpress.android.fluxc.site.SiteUtils.generateJetpackSiteOverRestOnly;
import static org.wordpress.android.fluxc.site.SiteUtils.generateJetpackSiteOverXMLRPC;
import static org.wordpress.android.fluxc.site.SiteUtils.generatePostFormats;
import static org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedNonJPSite;
import static org.wordpress.android.fluxc.site.SiteUtils.generateSelfHostedSiteFutureJetpack;
import static org.wordpress.android.fluxc.site.SiteUtils.generateTestSite;
import static org.wordpress.android.fluxc.site.SiteUtils.generateWPComSite;

@RunWith(RobolectricTestRunner.class)
public class SiteStoreUnitTest {
    private SiteStore mSiteStore = new SiteStore(new Dispatcher(), Mockito.mock(SiteRestClient.class),
            Mockito.mock(SiteXMLRPCClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();

        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testSimpleInsertionAndRetrieval() {
        SiteModel siteModel = new SiteModel();
        siteModel.setSiteId(42);
        WellSql.insert(siteModel).execute();

        Assert.assertEquals(1, mSiteStore.getSitesCount());

        Assert.assertEquals(42, mSiteStore.getSites().get(0).getSiteId());
    }

    @Test
    public void testInsertOrUpdateSite() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel site = generateWPComSite();
        SiteSqlUtils.insertOrUpdateSite(site);

        Assert.assertTrue(mSiteStore.hasSiteWithLocalId(site.getId()));
        Assert.assertEquals(site.getSiteId(), mSiteStore.getSiteByLocalId(site.getId()).getSiteId());
    }

    @Test
    public void testHasSiteAndgetCountMethods() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        Assert.assertFalse(mSiteStore.hasSite());
        Assert.assertTrue(mSiteStore.getSites().isEmpty());

        // Test counts with .COM site
        SiteModel wpComSite = generateWPComSite();
        SiteSqlUtils.insertOrUpdateSite(wpComSite);

        Assert.assertTrue(mSiteStore.hasSite());
        Assert.assertTrue(mSiteStore.hasWPComSite());
        Assert.assertFalse(mSiteStore.hasSiteAccessedViaXMLRPC());

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertEquals(1, mSiteStore.getWPComSitesCount());

        // Test counts with one .COM and one self-hosted site
        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        Assert.assertTrue(mSiteStore.hasSite());
        Assert.assertTrue(mSiteStore.hasWPComSite());
        Assert.assertTrue(mSiteStore.hasSiteAccessedViaXMLRPC());

        Assert.assertEquals(2, mSiteStore.getSitesCount());
        Assert.assertEquals(1, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaWPComRestCount());

        // Test counts with one .COM, one self-hosted and one Jetpack site
        SiteModel jetpackSiteOverRest = generateJetpackSiteOverRestOnly();
        SiteSqlUtils.insertOrUpdateSite(jetpackSiteOverRest);

        Assert.assertTrue(mSiteStore.hasSite());
        Assert.assertTrue(mSiteStore.hasWPComSite());
        Assert.assertTrue(mSiteStore.hasSiteAccessedViaXMLRPC());

        Assert.assertEquals(3, mSiteStore.getSitesCount());
        Assert.assertEquals(1, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());
        Assert.assertEquals(2, mSiteStore.getSitesAccessedViaWPComRestCount());
    }

    @Test
    public void testSelfHostedAndJetpackSites() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        // Note: not using the helper methods to make sure of the SiteModel definition
        SiteModel ponySite = new SiteModel();
        ponySite.setXmlRpcUrl("http://pony.com/xmlrpc.php");
        ponySite.setSiteId(1);
        ponySite.setIsWPCom(false);
        ponySite.setOrigin(SiteModel.ORIGIN_XMLRPC);
        SiteSqlUtils.insertOrUpdateSite(ponySite);

        SiteModel jetpackOverXMLRPC = new SiteModel();
        jetpackOverXMLRPC.setXmlRpcUrl("http://pony2.com/xmlrpc.php");
        jetpackOverXMLRPC.setSiteId(2);
        jetpackOverXMLRPC.setIsWPCom(false);
        jetpackOverXMLRPC.setIsJetpackInstalled(true);
        jetpackOverXMLRPC.setIsJetpackConnected(true);
        jetpackOverXMLRPC.setOrigin(SiteModel.ORIGIN_XMLRPC);
        SiteSqlUtils.insertOrUpdateSite(jetpackOverXMLRPC);

        SiteModel jetpackOverRest = new SiteModel();
        jetpackOverRest.setXmlRpcUrl("http://pony3.com/xmlrpc.php");
        jetpackOverRest.setSiteId(3);
        jetpackOverRest.setIsWPCom(false);
        jetpackOverRest.setIsJetpackInstalled(true);
        jetpackOverRest.setIsJetpackConnected(true);
        jetpackOverRest.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        SiteSqlUtils.insertOrUpdateSite(jetpackOverRest);

        Assert.assertEquals(3, mSiteStore.getSitesCount());
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(2, mSiteStore.getSitesAccessedViaXMLRPCCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaWPComRestCount());

        // User "install and connect" ponySite site to Jetpack via his connected .com account

        ponySite.setIsJetpackInstalled(true);
        ponySite.setIsJetpackConnected(true);
        ponySite.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        SiteSqlUtils.insertOrUpdateSite(ponySite);

        Assert.assertEquals(3, mSiteStore.getSitesCount());
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
        Assert.assertEquals(1, mSiteStore.getSitesAccessedViaXMLRPCCount());
        // Now ponySite is accessed via the WPCom REST API
        Assert.assertEquals(2, mSiteStore.getSitesAccessedViaWPComRestCount());
    }

    @Test
    public void testWPComSiteVisibility() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        // Should not cause any errors
        mSiteStore.isWPComSiteVisibleByLocalId(45);
        SiteSqlUtils.setSiteVisibility(null, true);

        SiteModel selfHostedNonJPSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(selfHostedNonJPSite);

        // Attempt to use with id of self-hosted site
        SiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false);
        // The self-hosted site should not be affected
        Assert.assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());


        SiteModel wpComSite = generateWPComSite();
        SiteSqlUtils.insertOrUpdateSite(wpComSite);

        // Attempt to use with legitimate .com site
        SiteSqlUtils.setSiteVisibility(selfHostedNonJPSite, false);
        Assert.assertFalse(mSiteStore.getSiteByLocalId(wpComSite.getId()).isVisible());
        Assert.assertFalse(mSiteStore.isWPComSiteVisibleByLocalId(wpComSite.getId()));
    }

    @Test
    public void testSetAllWPComSitesVisibility() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel selfHostedNonJPSite = generateSelfHostedNonJPSite();
        SiteSqlUtils.insertOrUpdateSite(selfHostedNonJPSite);

        // Attempt to use with id of self-hosted site
        for (SiteModel site : mSiteStore.getWPComSites()) {
            SiteSqlUtils.setSiteVisibility(site, false);
        }
        // The self-hosted site should not be affected
        Assert.assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());

        SiteModel wpComSite1 = generateWPComSite();
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setId(44);
        wpComSite2.setSiteId(284);

        SiteSqlUtils.insertOrUpdateSite(wpComSite1);
        SiteSqlUtils.insertOrUpdateSite(wpComSite2);

        // Attempt to use with legitimate .com site
        for (SiteModel site : mSiteStore.getWPComSites()) {
            SiteSqlUtils.setSiteVisibility(site, false);
        }
        Assert.assertTrue(mSiteStore.getSiteByLocalId(selfHostedNonJPSite.getId()).isVisible());
        Assert.assertFalse(mSiteStore.getSiteByLocalId(wpComSite1.getId()).isVisible());
        Assert.assertFalse(mSiteStore.getSiteByLocalId(wpComSite2.getId()).isVisible());
    }

    @Test
    public void testGetIdForIdMethods() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        Assert.assertEquals(0, mSiteStore.getLocalIdForRemoteSiteId(555));
        Assert.assertEquals(0, mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(2626, ""));
        Assert.assertEquals(0, mSiteStore.getSiteIdForLocalId(5577));

        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackSite = generateJetpackSiteOverXMLRPC();
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);
        SiteSqlUtils.insertOrUpdateSite(wpComSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackSite);

        Assert.assertEquals(selfHostedSite.getId(),
                mSiteStore.getLocalIdForRemoteSiteId(selfHostedSite.getSelfHostedSiteId()));
        Assert.assertEquals(wpComSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(wpComSite.getSiteId()));

        // Should be able to look up a Jetpack site by .com and by .org id (assuming it's been set)
        Assert.assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.getSiteId()));
        Assert.assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForRemoteSiteId(jetpackSite.getSelfHostedSiteId()));

        Assert.assertEquals(selfHostedSite.getId(), mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                selfHostedSite.getSelfHostedSiteId(), selfHostedSite.getXmlRpcUrl()));
        Assert.assertEquals(jetpackSite.getId(), mSiteStore.getLocalIdForSelfHostedSiteIdAndXmlRpcUrl(
                jetpackSite.getSelfHostedSiteId(), jetpackSite.getXmlRpcUrl()));

        Assert.assertEquals(selfHostedSite.getSelfHostedSiteId(), mSiteStore.getSiteIdForLocalId(selfHostedSite.getId()));
        Assert.assertEquals(wpComSite.getSiteId(), mSiteStore.getSiteIdForLocalId(wpComSite.getId()));
        Assert.assertEquals(jetpackSite.getSiteId(), mSiteStore.getSiteIdForLocalId(jetpackSite.getId()));
    }

    @Test
    public void testGetSiteBySiteId() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        assertNull(mSiteStore.getSiteBySiteId(555));

        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackSiteOverXMLRPC = generateJetpackSiteOverXMLRPC();
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);
        SiteSqlUtils.insertOrUpdateSite(wpComSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackSiteOverXMLRPC);

        Assert.assertEquals(1, SiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount());
        Assert.assertNotNull(mSiteStore.getSiteBySiteId(wpComSite.getSiteId()));
        Assert.assertNotNull(mSiteStore.getSiteBySiteId(jetpackSiteOverXMLRPC.getSiteId()));
        assertNull(mSiteStore.getSiteBySiteId(selfHostedSite.getSiteId()));
    }

    @Test
    public void testDeleteSite() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel wpComSite = generateWPComSite();

        // Should not cause any errors
        SiteSqlUtils.deleteSite(wpComSite);

        SiteSqlUtils.insertOrUpdateSite(wpComSite);
        int affectedRows = SiteSqlUtils.deleteSite(wpComSite);

        Assert.assertEquals(1, affectedRows);
        Assert.assertEquals(0, mSiteStore.getSitesCount());
    }

    @Test
    public void testGetWPComSites() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackSiteOverXMLRPC = generateJetpackSiteOverXMLRPC();
        SiteModel jetpackSiteOverRestOnly = generateJetpackSiteOverRestOnly();

        SiteSqlUtils.insertOrUpdateSite(wpComSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackSiteOverXMLRPC);
        SiteSqlUtils.insertOrUpdateSite(jetpackSiteOverRestOnly);

        Assert.assertEquals(2, SiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount());

        List<SiteModel> wpComSites = SiteSqlUtils.getWPComSites().getAsModel();
        Assert.assertEquals(1, wpComSites.size());
        for (SiteModel site : wpComSites) {
            assertNotEquals(jetpackSiteOverXMLRPC.getId(), site.getId());
        }
    }

    @Test
    public void testInsertDuplicateSites() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        // Insert a self hosted site that will later be converted to Jetpack
        SiteSqlUtils.insertOrUpdateSite(futureJetpack);

        // Insert the same site but Jetpack powered this time
        SiteSqlUtils.insertOrUpdateSite(jetpack);

        // Previous site should be converted to a Jetpack site and we should see only one site
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        Assert.assertEquals(1, sitesCount);

        List<SiteModel> wpComSites = SiteSqlUtils.getWPComSites().getAsModel();
        Assert.assertEquals(0, wpComSites.size());
        Assert.assertEquals(1, SiteSqlUtils.getSitesAccessedViaWPComRest().getAsCursor().getCount());
        List<SiteModel> jetpackSites =
                SiteSqlUtils.getSitesWith(SiteModelTable.IS_JETPACK_CONNECTED, true).getAsModel();
        Assert.assertEquals(jetpack.getSiteId(), jetpackSites.get(0).getSiteId());
        Assert.assertTrue(jetpackSites.get(0).isJetpackConnected());
        Assert.assertFalse(jetpackSites.get(0).isWPCom());
    }

    @Test
    public void testInsertDuplicateSitesError() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        // Insert a Jetpack powered site
        SiteSqlUtils.insertOrUpdateSite(jetpack);
        boolean duplicate = false;
        try {
            // Insert the same site but via self hosted this time (this should fail)
            SiteSqlUtils.insertOrUpdateSite(futureJetpack);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        Assert.assertTrue(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        Assert.assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertDuplicateSitesDifferentSchemesError1() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        futureJetpack.setXmlRpcUrl("https://pony.com/xmlrpc.php");
        jetpack.setXmlRpcUrl("http://pony.com/xmlrpc.php");

        // Insert a Jetpack powered site
        SiteSqlUtils.insertOrUpdateSite(jetpack);
        boolean duplicate = false;
        try {
            // Insert the same site but via self hosted this time (this should fail)
            SiteSqlUtils.insertOrUpdateSite(futureJetpack);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        Assert.assertTrue(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        Assert.assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertDuplicateSitesDifferentSchemesError2() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel futureJetpack = generateSelfHostedSiteFutureJetpack();
        SiteModel jetpack = generateJetpackSiteOverRestOnly();

        futureJetpack.setXmlRpcUrl("http://pony.com/xmlrpc.php");
        jetpack.setXmlRpcUrl("https://pony.com/xmlrpc.php");

        // Insert a Jetpack powered site
        SiteSqlUtils.insertOrUpdateSite(jetpack);
        boolean duplicate = false;
        try {
            // Insert the same site but via self hosted this time (this should fail)
            SiteSqlUtils.insertOrUpdateSite(futureJetpack);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        Assert.assertTrue(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        Assert.assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertDuplicateXmlRpcJetpackSite() throws DuplicateSiteException {
        SiteModel jetpackXmlRpcSite = generateJetpackSiteOverXMLRPC();

        jetpackXmlRpcSite.setUrl("http://some.url");

        // Insert a Jetpack powered site over XML-RPC
        SiteSqlUtils.insertOrUpdateSite(jetpackXmlRpcSite);

        // Set up the same site (by URL/XML-RPC URL), but don't identify it as a Jetpack site
        // This simulates sites resulting from wp.getUsersBlogs, which don't have the site ID and can't be identified
        // as Jetpack or not (wp.getOptions is the call that returns that information)
        SiteModel jetpackXmlRpcSite2 = generateSelfHostedNonJPSite();
        jetpackXmlRpcSite2.setXmlRpcUrl(jetpackXmlRpcSite.getXmlRpcUrl());
        jetpackXmlRpcSite2.setUrl(jetpackXmlRpcSite.getUrl());
        jetpackXmlRpcSite2.setSelfHostedSiteId(jetpackXmlRpcSite.getSelfHostedSiteId());
        jetpackXmlRpcSite2.setUsername(jetpackXmlRpcSite.getUsername());
        jetpackXmlRpcSite2.setPassword(jetpackXmlRpcSite.getPassword());

        boolean duplicate = false;
        try {
            // Insert the same site but not identified as a Jetpack site
            // (this should succeed, replacing the existing site, because the site replaced is not using the REST API)
            SiteSqlUtils.insertOrUpdateSite(jetpackXmlRpcSite2);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        Assert.assertFalse(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        Assert.assertEquals(1, sitesCount);
    }

    @Test
    public void testGetPostFormats() throws DuplicateSiteException {
        SiteModel site = generateWPComSite();
        SiteSqlUtils.insertOrUpdateSite(site);

        // Set 3 post formats
        SiteSqlUtils.insertOrReplacePostFormats(site, generatePostFormats("Video", "Image", "Standard"));
        List<PostFormatModel> postFormats = mSiteStore.getPostFormats(site);
        Assert.assertEquals(3, postFormats.size());

        // Set 1 post format
        SiteSqlUtils.insertOrReplacePostFormats(site, generatePostFormats("Standard"));
        postFormats = mSiteStore.getPostFormats(site);
        Assert.assertEquals("Standard", postFormats.get(0).getDisplayName());
    }

    @Test
    public void testSearchSitesByNameMatching() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel wpComSite1 = generateWPComSite();
        wpComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setName("Shield Eyes from light");
        wpComSite2.setSiteId(557);
        SiteModel wpComSite3 = generateWPComSite();
        wpComSite3.setName("I remember when this was all farmland as far as the eye could see");
        wpComSite2.setSiteId(558);

        SiteSqlUtils.insertOrUpdateSite(wpComSite1);
        SiteSqlUtils.insertOrUpdateSite(wpComSite2);
        SiteSqlUtils.insertOrUpdateSite(wpComSite3);

        List<SiteModel> matchingSites = SiteSqlUtils.getSitesByNameOrUrlMatching("eye");
        Assert.assertEquals(2, matchingSites.size());

        matchingSites = SiteSqlUtils.getSitesByNameOrUrlMatching("EYE");
        Assert.assertEquals(2, matchingSites.size());
    }

    @Test
    public void testSearchSitesByNameOrUrlMatching() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel wpComSite1 = generateWPComSite();
        wpComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setUrl("shieldeyesfromlight.wordpress.com");
        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        selfHostedSite.setName("I remember when this was all farmland as far as the eye could see.");

        SiteSqlUtils.insertOrUpdateSite(wpComSite1);
        SiteSqlUtils.insertOrUpdateSite(wpComSite2);
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        List<SiteModel> matchingSites = SiteSqlUtils.getSitesByNameOrUrlMatching("eye");
        Assert.assertEquals(2, matchingSites.size());

        matchingSites = SiteSqlUtils.getSitesByNameOrUrlMatching("EYE");
        Assert.assertEquals(2, matchingSites.size());
    }

    @Test
    public void testSearchWPComSitesByNameOrUrlMatching() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel wpComSite1 = generateWPComSite();
        wpComSite1.setName("Doctor Emmet Brown Homepage");
        SiteModel wpComSite2 = generateWPComSite();
        wpComSite2.setUrl("shieldeyesfromlight.wordpress.com");
        SiteModel selfHostedSite = generateSelfHostedNonJPSite();
        selfHostedSite.setName("I remember when this was all farmland as far as the eye could see.");

        SiteSqlUtils.insertOrUpdateSite(wpComSite1);
        SiteSqlUtils.insertOrUpdateSite(wpComSite2);
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        List<SiteModel> matchingSites = SiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching("eye");
        Assert.assertEquals(1, matchingSites.size());

        matchingSites = SiteSqlUtils.getSitesAccessedViaWPComRestByNameOrUrlMatching("EYE");
        Assert.assertEquals(1, matchingSites.size());
    }

    @Test
    public void testRemoveAllSites() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel wpComSite = generateWPComSite();
        SiteModel jetpackXMLRPCSite = generateJetpackSiteOverXMLRPC();
        SiteModel jetpackRestSite = generateJetpackSiteOverRestOnly();
        SiteModel selfHostedSite = generateSelfHostedNonJPSite();

        SiteSqlUtils.insertOrUpdateSite(wpComSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackXMLRPCSite);
        SiteSqlUtils.insertOrUpdateSite(jetpackRestSite);
        SiteSqlUtils.insertOrUpdateSite(selfHostedSite);

        // first make sure sites are inserted successfully
        Assert.assertEquals(4, mSiteStore.getSitesCount());

        SiteSqlUtils.deleteAllSites();

        Assert.assertEquals(0, mSiteStore.getSitesCount());
    }

    @Test
    public void testWPComAutomatedTransfer() throws DuplicateSiteException {
        WellSqlTestUtils.setupWordPressComAccount();

        SiteModel wpComSite = generateWPComSite();
        SiteSqlUtils.insertOrUpdateSite(wpComSite);

        // Turn WP.com site into an Automated Transfer (Jetpack) site
        SiteModel automatedTransferSite = generateWPComSite();
        automatedTransferSite.setIsJetpackInstalled(true);
        automatedTransferSite.setIsJetpackConnected(true);
        automatedTransferSite.setIsWPCom(false);
        automatedTransferSite.setIsAutomatedTransfer(true);

        SiteSqlUtils.insertOrUpdateSite(automatedTransferSite);

        Assert.assertEquals(1, mSiteStore.getSitesCount());
        Assert.assertEquals(0, mSiteStore.getWPComSitesCount());
    }

    @Test
    public void testBatchInsertSiteDuplicateWPCom()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        WellSqlTestUtils.setupWordPressComAccount();

        List<SiteModel> siteList = new ArrayList<>();
        siteList.add(generateTestSite(1, "https://pony1.com", "https://pony1.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(2, "https://pony2.com", "https://pony2.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(3, "https://pony3.com", "https://pony3.com/xmlrpc.php", true, true));
        // duplicate with a different id, we should ignore it
        siteList.add(generateTestSite(4, "https://pony3.com", "https://pony3.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(5, "https://pony4.com", "https://pony4.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(6, "https://pony5.com", "https://pony5.com/xmlrpc.php", true, true));

        SitesModel sites = new SitesModel(siteList);

        // Use reflection to call a private Store method: equivalent to mSiteStore.updateSites(sites)
        Method createOrUpdateSites = SiteStore.class.getDeclaredMethod("createOrUpdateSites", SitesModel.class);
        createOrUpdateSites.setAccessible(true);
        UpdateSitesResult res = (UpdateSitesResult) createOrUpdateSites.invoke(mSiteStore, sites);

        Assert.assertTrue(res.duplicateSiteFound);
        Assert.assertEquals(5, res.rowsAffected);
        Assert.assertEquals(5, mSiteStore.getSitesCount());
    }

    @Test
    public void testBatchInsertSiteNoDuplicateWPCom()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        WellSqlTestUtils.setupWordPressComAccount();

        List<SiteModel> siteList = new ArrayList<>();
        siteList.add(generateTestSite(1, "https://pony1.com", "https://pony1.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(2, "https://pony2.com", "https://pony2.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(3, "https://pony3.com", "https://pony3.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(4, "https://pony4.com", "https://pony4.com/xmlrpc.php", true, true));
        siteList.add(generateTestSite(5, "https://pony5.com", "https://pony5.com/xmlrpc.php", true, true));

        SitesModel sites = new SitesModel(siteList);

        // Use reflection to call a private Store method: equivalent to mSiteStore.updateSites(sites)
        Method createOrUpdateSites = SiteStore.class.getDeclaredMethod("createOrUpdateSites", SitesModel.class);
        createOrUpdateSites.setAccessible(true);
        UpdateSitesResult res = (UpdateSitesResult) createOrUpdateSites.invoke(mSiteStore, sites);

        Assert.assertFalse(res.duplicateSiteFound);
        Assert.assertEquals(5, res.rowsAffected);
        Assert.assertEquals(5, mSiteStore.getSitesCount());
    }

    @Test
    public void testSingleInsertSiteDuplicateWPCom()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        WellSqlTestUtils.setupWordPressComAccount();

        List<SiteModel> siteList = new ArrayList<>();
        siteList.add(generateTestSite(1, "https://pony1.com", "https://pony1.com/xmlrpc.php", true, true));
        SitesModel sites = new SitesModel(siteList);

        // Insert 1 site
        Method createOrUpdateSites = SiteStore.class.getDeclaredMethod("createOrUpdateSites", SitesModel.class);
        createOrUpdateSites.setAccessible(true);
        UpdateSitesResult res = (UpdateSitesResult) createOrUpdateSites.invoke(mSiteStore, sites);

        Assert.assertFalse(res.duplicateSiteFound);
        Assert.assertEquals(1, res.rowsAffected);
        Assert.assertEquals(1, mSiteStore.getSitesCount());

        // Insert same site with different id (considered a duplicate)
        List<SiteModel> siteList2 = new ArrayList<>();
        siteList2.add(generateTestSite(2, "https://pony1.com", "https://pony1.com/xmlrpc.php", true, true));
        SitesModel sites2 = new SitesModel(siteList2);
        createOrUpdateSites.setAccessible(true);
        UpdateSitesResult res2 = (UpdateSitesResult) createOrUpdateSites.invoke(mSiteStore, sites2);

        Assert.assertTrue(res2.duplicateSiteFound);
        Assert.assertEquals(0, res2.rowsAffected);
        Assert.assertEquals(1, mSiteStore.getSitesCount());
    }

    @Test
    public void testInsertSiteDuplicateXmlRpcTrailingSlash() throws DuplicateSiteException {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`,
        // sometimes just by a trailing slash
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        SiteModel selfhostedSite = generateSelfHostedNonJPSite();
        selfhostedSite.setUrl("http://some.url");

        SiteSqlUtils.insertOrUpdateSite(selfhostedSite);

        SiteModel selfhostedSite2 = generateSelfHostedNonJPSite();
        selfhostedSite2.setUrl("http://some.url/");

        boolean duplicate = false;
        try {
            // Insert the same site with a trailing slash (this should succeed, replacing the existing site)
            SiteSqlUtils.insertOrUpdateSite(selfhostedSite2);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        Assert.assertFalse(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        Assert.assertEquals(1, sitesCount);
    }

    @Test
    public void testInsertSiteDuplicateXmlRpcDifferentUrl() throws DuplicateSiteException {
        // It's possible for the URL in `wp.getOptions` to be different from the URL in `wp.getUsersBlogs`
        // This test checks that we can still identify two sites as being identical in this case, and that we quietly
        // update the existing site rather than throw a duplicate site exception
        SiteModel selfhostedSite = generateSelfHostedNonJPSite();
        selfhostedSite.setUrl("http://some.url");
        selfhostedSite.setXmlRpcUrl("http://some.url/xmlrpc.php");

        SiteSqlUtils.insertOrUpdateSite(selfhostedSite);

        SiteModel selfhostedSite2 = generateSelfHostedNonJPSite();
        selfhostedSite2.setUrl("http://user5242.stagingsite.url");
        selfhostedSite2.setXmlRpcUrl("http://some.url/xmlrpc.php");

        boolean duplicate = false;
        try {
            // Insert the same site with a different URL, but the same XML-RPC URL
            // (this should succeed, replacing the existing site)
            SiteSqlUtils.insertOrUpdateSite(selfhostedSite2);
        } catch (DuplicateSiteException e) {
            // Caught !
            duplicate = true;
        }
        Assert.assertFalse(duplicate);
        int sitesCount = WellSql.select(SiteModel.class).getAsCursor().getCount();
        Assert.assertEquals(1, sitesCount);
    }

    @Test
    public void testUpdateSiteUniqueConstraintFail() throws DuplicateSiteException {
        // Create 2 test sites
        SiteModel site1 = generateTestSite(0, "https://pony1.com", "https://pony1.com/xmlrpc.php", false, true);
        SiteSqlUtils.insertOrUpdateSite(site1);
        SiteModel site2 = generateTestSite(0, "https://pony2.com", "https://pony2.com/xmlrpc.php", false, true);
        SiteSqlUtils.insertOrUpdateSite(site2);

        // Update the second site and reuse the site url and id from the first
        site2.setUrl("https://pony1.com");
        boolean duplicate = false;
        try {
            SiteSqlUtils.insertOrUpdateSite(site2);
        } catch (SQLiteException e) {
            // We should catch a DuplicateSiteException here, but since Roboelectric uses a different SQL stack, we
            // can't catch `SQLiteConstraintException` in SiteSqlUtils.insertOrUpdateSite.
            duplicate = true;
        }
        Assert.assertTrue(duplicate);
    }

    @Test
    public void testJetpackSelfHostedAndForceXMLRPC() {
        SiteModel jetpackSite = generateJetpackSiteOverXMLRPC();
        jetpackSite.setOrigin(SiteModel.ORIGIN_WPCOM_REST);
        Assert.assertTrue(jetpackSite.isUsingWpComRestApi());

        // Force the origin, it should now use XMLRPC instead of REST.
        jetpackSite.setOrigin(SiteModel.ORIGIN_XMLRPC);
        Assert.assertFalse(jetpackSite.isUsingWpComRestApi());
    }

    @Test
    public void testDefaultUsageWpComRestApi() {
        SiteModel wpComSite = generateWPComSite();
        Assert.assertTrue(wpComSite.isUsingWpComRestApi());

        SiteModel jetpack1 = generateJetpackSiteOverRestOnly();
        Assert.assertTrue(jetpack1.isUsingWpComRestApi());

        SiteModel jetpack2 = generateJetpackSiteOverXMLRPC();
        Assert.assertFalse(jetpack2.isUsingWpComRestApi());

        SiteModel pureSelfHosted1 = generateSelfHostedNonJPSite();
        Assert.assertFalse(pureSelfHosted1.isUsingWpComRestApi());

        SiteModel pureSelfHosted2 = generateSelfHostedSiteFutureJetpack();
        Assert.assertFalse(pureSelfHosted2.isUsingWpComRestApi());
    }

    @Test
    public void testRemoveWPComRestSitesAbsentFromList()
            throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        WellSqlTestUtils.setupWordPressComAccount();

        final List<SiteModel> allSites = new ArrayList<>();
        final List<SiteModel> sitesToKeep = new ArrayList<>();

        for (int i = 0; i < 15; ++i) {
            switch (i % 3) {
                case 0:
                    // add a .com site
                    SiteModel wpComSite = generateWPComSite();
                    wpComSite.setSiteId(i + 1);
                    wpComSite.setUrl("https://pony" + i + ".com");
                    wpComSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php");
                    allSites.add(wpComSite);
                    break;
                case 1:
                    // add a self-hosted Jetpack site
                    SiteModel jetpackSite = generateJetpackSiteOverRestOnly();
                    jetpackSite.setSiteId(i + 1);
                    jetpackSite.setUrl("https://pony" + i + ".com");
                    jetpackSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php");
                    allSites.add(jetpackSite);
                    break;
                case 2:
                    // add a self-hosted non-Jetpack site
                    SiteModel selfHostedSite = generateSelfHostedNonJPSite();
                    selfHostedSite.setSiteId(i + 1);
                    selfHostedSite.setUrl("https://pony" + i + ".com");
                    selfHostedSite.setXmlRpcUrl("https://pony" + i + ".com/xmlrpc.php");
                    allSites.add(selfHostedSite);
                    break;
            }
        }

        // add all sites to DB
        Method createOrUpdateSites = SiteStore.class.getDeclaredMethod("createOrUpdateSites", SitesModel.class);
        createOrUpdateSites.setAccessible(true);
        UpdateSitesResult res = (UpdateSitesResult) createOrUpdateSites.invoke(mSiteStore, new SitesModel(allSites));

        Assert.assertFalse(res.duplicateSiteFound);
        Assert.assertTrue(res.rowsAffected == 15);
        Assert.assertTrue(mSiteStore.getSitesCount() == 15);

        // add 2 of each kind of site to keep
        sitesToKeep.addAll(allSites.subList(0, 6));

        // remove six sites (2/3 * (15 - 6))
        SiteSqlUtils.removeWPComRestSitesAbsentFromList(sitesToKeep);

        Assert.assertTrue(mSiteStore.getSitesCount() == 9);

        // make sure all sites in sitesToKeep are in the store
        for (SiteModel site : sitesToKeep) {
            Assert.assertTrue(mSiteStore.getSiteBySiteId(site.getSiteId()) != null);
        }
    }
}
