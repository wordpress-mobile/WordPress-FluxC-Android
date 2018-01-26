package org.wordpress.android.fluxc.theme;

import android.content.Context;

import com.yarolegovich.wellsql.WellSql;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.network.rest.wpcom.theme.ThemeRestClient;
import org.wordpress.android.fluxc.persistence.SiteSqlUtils;
import org.wordpress.android.fluxc.persistence.ThemeSqlUtils;
import org.wordpress.android.fluxc.persistence.WellSqlConfig;
import org.wordpress.android.fluxc.site.SiteUtils;
import org.wordpress.android.fluxc.store.ThemeStore;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
public class ThemeStoreUnitTest {
    private ThemeStore mThemeStore = new ThemeStore(new Dispatcher(), Mockito.mock(ThemeRestClient.class));

    @Before
    public void setUp() {
        Context appContext = RuntimeEnvironment.application.getApplicationContext();
        WellSqlConfig config = new WellSqlConfig(appContext);
        WellSql.init(config);
        config.reset();
    }

    @Test
    public void testActiveTheme() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateWPComSite();
        SiteSqlUtils.insertOrUpdateSite(site);
        assertNull(mThemeStore.getActiveThemeForSite(site));

        final ThemeModel firstTheme = generateTestTheme(site.getId(), "first-active", "First Active");
        final ThemeModel secondTheme = generateTestTheme(site.getId(), "second-active", "Second Active");
        firstTheme.setActive(true);
        secondTheme.setActive(true);

        // set first theme active and verify
        mThemeStore.setActiveThemeForSite(site, firstTheme);
        List<ThemeModel> activeThemes = ThemeSqlUtils.getActiveThemeForSite(site);
        Assert.assertNotNull(activeThemes);
        Assert.assertTrue(activeThemes.size() == 1);
        ThemeModel firstActiveTheme = activeThemes.get(0);
        Assert.assertEquals(firstTheme.getThemeId(), firstActiveTheme.getThemeId());
        Assert.assertEquals(firstTheme.getName(), firstActiveTheme.getName());

        // set second theme active and verify
        mThemeStore.setActiveThemeForSite(site, secondTheme);
        activeThemes = ThemeSqlUtils.getActiveThemeForSite(site);
        Assert.assertNotNull(activeThemes);
        Assert.assertTrue(activeThemes.size() == 1);
        ThemeModel secondActiveTheme = activeThemes.get(0);
        Assert.assertEquals(secondTheme.getThemeId(), secondActiveTheme.getThemeId());
        Assert.assertEquals(secondTheme.getName(), secondActiveTheme.getName());
    }

    @Test
    public void testInsertOrUpdateTheme() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        SiteSqlUtils.insertOrUpdateSite(site);

        final String testThemeId = "fluxc-ftw";
        final String testThemeName = "FluxC FTW";
        final String testUpdatedName = testThemeName + " v2";
        final ThemeModel insertTheme = generateTestTheme(site.getId(), testThemeId, testThemeName);

        // verify theme doesn't already exist
        assertNull(mThemeStore.getInstalledThemeByThemeId(site, testThemeId));

        // insert new theme and verify it exists
        ThemeSqlUtils.insertOrUpdateSiteTheme(site, insertTheme);
        ThemeModel insertedTheme = mThemeStore.getInstalledThemeByThemeId(site, testThemeId);
        Assert.assertNotNull(insertedTheme);
        Assert.assertEquals(testThemeName, insertedTheme.getName());

        // update the theme and verify the updated attributes
        insertedTheme.setName(testUpdatedName);
        ThemeSqlUtils.insertOrUpdateSiteTheme(site, insertedTheme);
        insertedTheme = mThemeStore.getInstalledThemeByThemeId(site, testThemeId);
        Assert.assertNotNull(insertedTheme);
        Assert.assertEquals(testUpdatedName, insertedTheme.getName());
    }

    @Test
    public void testInsertOrReplaceWpThemes() {
        final List<ThemeModel> firstTestThemes = generateThemesTestList(20);
        final List<ThemeModel> secondTestThemes = generateThemesTestList(30);
        final List<ThemeModel> thirdTestThemes = generateThemesTestList(10);

        // first add 20 themes and make sure the count is correct
        ThemeSqlUtils.insertOrReplaceWpComThemes(firstTestThemes);
        Assert.assertEquals(20, mThemeStore.getWpComThemes().size());

        // next add a larger list of themes (with 20 being duplicates) and make sure the count is correct
        ThemeSqlUtils.insertOrReplaceWpComThemes(secondTestThemes);
        Assert.assertEquals(30, mThemeStore.getWpComThemes().size());

        // lastly add a smaller list of themes (all duplicates) and make sure count is correct
        ThemeSqlUtils.insertOrReplaceWpComThemes(thirdTestThemes);
        Assert.assertEquals(10, mThemeStore.getWpComThemes().size());
    }

    @Test
    public void testInsertOrReplaceInstalledThemes() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        SiteSqlUtils.insertOrUpdateSite(site);

        final List<ThemeModel> firstTestThemes = generateThemesTestList(5);
        final List<ThemeModel> secondTestThemes = generateThemesTestList(10);
        final List<ThemeModel> thirdTestThemes = generateThemesTestList(1);

        // first add 5 installed themes
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, firstTestThemes);
        Assert.assertEquals(firstTestThemes.size(), mThemeStore.getThemesForSite(site).size());

        // then replace them all with a new list of 10
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, secondTestThemes);
        Assert.assertEquals(secondTestThemes.size(), mThemeStore.getThemesForSite(site).size());

        // then replace them all with a single theme
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, thirdTestThemes);
        Assert.assertEquals(thirdTestThemes.size(), mThemeStore.getThemesForSite(site).size());
    }

    @Test
    public void testGetWpThemesAsCursor() {
        final List<ThemeModel> firstTestThemes = generateThemesTestList(20);
        final List<ThemeModel> secondTestThemes = generateThemesTestList(30);

        // insert themes and verify count
        Assert.assertEquals(0, mThemeStore.getWpComThemesCursor().getCount());
        ThemeSqlUtils.insertOrReplaceWpComThemes(firstTestThemes);
        Assert.assertEquals(firstTestThemes.size(), mThemeStore.getWpComThemesCursor().getCount());

        // insert new themes list and verify count
        ThemeSqlUtils.insertOrReplaceWpComThemes(secondTestThemes);
        Assert.assertEquals(secondTestThemes.size(), mThemeStore.getWpComThemesCursor().getCount());
    }

    @Test
    public void testRemoveThemesWithNoSite() {
        final List<ThemeModel> testThemes = generateThemesTestList(20);

        // insert and verify count
        Assert.assertEquals(0, mThemeStore.getWpComThemesCursor().getCount());
        ThemeSqlUtils.insertOrReplaceWpComThemes(testThemes);
        Assert.assertEquals(testThemes.size(), mThemeStore.getWpComThemes().size());

        // remove and verify count
        ThemeSqlUtils.removeWpComThemes();
        Assert.assertEquals(0, mThemeStore.getWpComThemes().size());
    }

    @Test
    public void testRemoveInstalledSiteThemes() throws SiteSqlUtils.DuplicateSiteException {
        final SiteModel site = SiteUtils.generateJetpackSiteOverRestOnly();
        SiteSqlUtils.insertOrUpdateSite(site);

        final List<ThemeModel> testThemes = generateThemesTestList(5);

        // add site themes and verify count
        ThemeSqlUtils.insertOrReplaceInstalledThemes(site, testThemes);
        Assert.assertEquals(testThemes.size(), mThemeStore.getThemesForSite(site).size());

        // remove and verify count
        ThemeSqlUtils.removeSiteThemes(site);
        Assert.assertEquals(0, mThemeStore.getThemesForSite(site).size());
    }

    private ThemeModel generateTestTheme(int siteId, String themeId, String themeName) {
        ThemeModel theme = new ThemeModel();
        theme.setLocalSiteId(siteId);
        theme.setThemeId(themeId);
        theme.setName(themeName);
        return theme;
    }

    private List<ThemeModel> generateThemesTestList(int num) {
        List<ThemeModel> testThemes = new ArrayList<>();
        for (int i = 0; i < num; ++i) {
            testThemes.add(generateTestTheme(0, "themeid" + i, "themename" + i));
        }
        return testThemes;
    }
}
