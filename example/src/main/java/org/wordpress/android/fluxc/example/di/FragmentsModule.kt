package org.wordpress.android.fluxc.example.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.example.AccountFragment
import org.wordpress.android.fluxc.example.CommentsFragment
import org.wordpress.android.fluxc.example.EditorThemeFragment
import org.wordpress.android.fluxc.example.MainFragment
import org.wordpress.android.fluxc.example.MediaFragment
import org.wordpress.android.fluxc.example.NotificationsFragment
import org.wordpress.android.fluxc.example.PostsFragment
import org.wordpress.android.fluxc.example.ReactNativeFragment
import org.wordpress.android.fluxc.example.SignedOutActionsFragment
import org.wordpress.android.fluxc.example.SiteSelectorDialog
import org.wordpress.android.fluxc.example.SitesFragment
import org.wordpress.android.fluxc.example.TaxonomiesFragment
import org.wordpress.android.fluxc.example.ThemeFragment
import org.wordpress.android.fluxc.example.UploadsFragment
import org.wordpress.android.fluxc.example.ui.StoreSelectorDialog
import org.wordpress.android.fluxc.example.ui.WooCommerceFragment
import org.wordpress.android.fluxc.example.ui.gateways.WooGatewaysFragment
import org.wordpress.android.fluxc.example.ui.orders.WooOrdersFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductFiltersFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductsFragment
import org.wordpress.android.fluxc.example.ui.products.WooUpdateProductFragment
import org.wordpress.android.fluxc.example.ui.refunds.WooRefundsFragment
import org.wordpress.android.fluxc.example.ui.shippinglabels.WooShippingLabelFragment
import org.wordpress.android.fluxc.example.ui.stats.WooStatsFragment
import org.wordpress.android.fluxc.example.ui.stats.WooRevenueStatsFragment
import org.wordpress.android.fluxc.example.ui.taxes.WooTaxFragment

@Module
internal abstract class FragmentsModule {
    @ContributesAndroidInjector
    abstract fun provideMainFragmentInjector(): MainFragment

    @ContributesAndroidInjector
    abstract fun provideAccountFragmentInjector(): AccountFragment

    @ContributesAndroidInjector
    abstract fun provideCommentsFragmentInjector(): CommentsFragment

    @ContributesAndroidInjector
    abstract fun provideMediaFragmentInjector(): MediaFragment

    @ContributesAndroidInjector
    abstract fun providePostsFragmentInjector(): PostsFragment

    @ContributesAndroidInjector
    abstract fun provideSignedOutActionsFragmentInjector(): SignedOutActionsFragment

    @ContributesAndroidInjector
    abstract fun provideSitesFragmentInjector(): SitesFragment

    @ContributesAndroidInjector
    abstract fun provideTaxonomiesFragmentInjector(): TaxonomiesFragment

    @ContributesAndroidInjector
    abstract fun provideThemeFragmentInjector(): ThemeFragment

    @ContributesAndroidInjector
    abstract fun provideUploadsFragmentInjector(): UploadsFragment

    @ContributesAndroidInjector
    abstract fun provideWooCommerceFragmentInjector(): WooCommerceFragment

    @ContributesAndroidInjector
    abstract fun provideNotificationsFragmentInjector(): NotificationsFragment

    @ContributesAndroidInjector
    abstract fun provideWooStatsFragmentInjector(): WooStatsFragment

    @ContributesAndroidInjector
    abstract fun provideWooRevenueStatsFragmentInjector(): WooRevenueStatsFragment

    @ContributesAndroidInjector
    abstract fun provideWooProductsFragmentInjector(): WooProductsFragment

    @ContributesAndroidInjector
    abstract fun provideWooUpdateProductFragmentInjector(): WooUpdateProductFragment

    @ContributesAndroidInjector
    abstract fun provideWooProductFiltersFragmentInjector(): WooProductFiltersFragment

    @ContributesAndroidInjector
    abstract fun provideWooOrdersFragmentInjector(): WooOrdersFragment

    @ContributesAndroidInjector
    abstract fun provideWooRefundsFragmentInjector(): WooRefundsFragment

    @ContributesAndroidInjector
    abstract fun provideWooGatewaysFragmentInjector(): WooGatewaysFragment

    @ContributesAndroidInjector
    abstract fun provideWooTaxFragmentInjector(): WooTaxFragment

    @ContributesAndroidInjector
    abstract fun provideWooShippingLabelFragmentInjector(): WooShippingLabelFragment

    @ContributesAndroidInjector
    abstract fun provideSiteSelectorDialogInjector(): SiteSelectorDialog

    @ContributesAndroidInjector
    abstract fun provideStoreSelectorDialogInjector(): StoreSelectorDialog

    @ContributesAndroidInjector
    abstract fun provideReactNativeFramgmentInjector(): ReactNativeFragment

    @ContributesAndroidInjector
    abstract fun provideEditorThemeFramgmentInjector(): EditorThemeFragment
}
