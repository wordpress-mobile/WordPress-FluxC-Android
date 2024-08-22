package org.wordpress.android.fluxc.example.di

import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.fluxc.example.AccountFragment
import org.wordpress.android.fluxc.example.CommentsFragment
import org.wordpress.android.fluxc.example.DomainsFragment
import org.wordpress.android.fluxc.example.EditorThemeFragment
import org.wordpress.android.fluxc.example.ExperimentsFragment
import org.wordpress.android.fluxc.example.JetpackAIFragment
import org.wordpress.android.fluxc.example.MainFragment
import org.wordpress.android.fluxc.example.MediaFragment
import org.wordpress.android.fluxc.example.NotificationsFragment
import org.wordpress.android.fluxc.example.PlansFragment
import org.wordpress.android.fluxc.example.PluginsFragment
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
import org.wordpress.android.fluxc.example.ui.coupons.WooCouponsFragment
import org.wordpress.android.fluxc.example.ui.coupons.WooUpdateCouponFragment
import org.wordpress.android.fluxc.example.ui.customer.WooCustomersFragment
import org.wordpress.android.fluxc.example.ui.customer.creation.WooCustomerCreationFragment
import org.wordpress.android.fluxc.example.ui.customer.search.WooCustomersSearchFragment
import org.wordpress.android.fluxc.example.ui.gateways.WooGatewaysFragment
import org.wordpress.android.fluxc.example.ui.helpsupport.WooHelpSupportFragment
import org.wordpress.android.fluxc.example.ui.leaderboards.WooLeaderboardsFragment
import org.wordpress.android.fluxc.example.ui.metadata.CustomFieldsFragment
import org.wordpress.android.fluxc.example.ui.onboarding.WooOnboardingFragment
import org.wordpress.android.fluxc.example.ui.orders.AddressEditDialogFragment
import org.wordpress.android.fluxc.example.ui.orders.WooOrdersFragment
import org.wordpress.android.fluxc.example.ui.products.WooAddonsTestFragment
import org.wordpress.android.fluxc.example.ui.products.WooBatchGenerateVariationsFragment
import org.wordpress.android.fluxc.example.ui.products.WooBatchUpdateVariationsFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductAttributeFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductCategoriesFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductFiltersFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductTagsFragment
import org.wordpress.android.fluxc.example.ui.products.WooProductsFragment
import org.wordpress.android.fluxc.example.ui.products.WooUpdateProductFragment
import org.wordpress.android.fluxc.example.ui.products.WooUpdateVariationFragment
import org.wordpress.android.fluxc.example.ui.refunds.WooRefundsFragment
import org.wordpress.android.fluxc.example.ui.shippinglabels.WooShippingLabelFragment
import org.wordpress.android.fluxc.example.ui.shippinglabels.WooVerifyAddressFragment
import org.wordpress.android.fluxc.example.ui.stats.WooRevenueStatsFragment
import org.wordpress.android.fluxc.example.ui.storecreation.WooStoreCreationFragment
import org.wordpress.android.fluxc.example.ui.taxes.WooTaxFragment
import org.wordpress.android.fluxc.example.ui.wooadmin.WooAdminFragment

@Module
internal interface FragmentsModule {
    @ContributesAndroidInjector
    fun provideMainFragmentInjector(): MainFragment

    @ContributesAndroidInjector
    fun provideAccountFragmentInjector(): AccountFragment

    @ContributesAndroidInjector
    fun provideCommentsFragmentInjector(): CommentsFragment

    @ContributesAndroidInjector
    fun provideMediaFragmentInjector(): MediaFragment

    @ContributesAndroidInjector
    fun providePostsFragmentInjector(): PostsFragment

    @ContributesAndroidInjector
    fun provideSignedOutActionsFragmentInjector(): SignedOutActionsFragment

    @ContributesAndroidInjector
    fun provideSitesFragmentInjector(): SitesFragment

    @ContributesAndroidInjector
    fun provideTaxonomiesFragmentInjector(): TaxonomiesFragment

    @ContributesAndroidInjector
    fun provideThemeFragmentInjector(): ThemeFragment

    @ContributesAndroidInjector
    fun provideUploadsFragmentInjector(): UploadsFragment

    @ContributesAndroidInjector
    fun provideWooCommerceFragmentInjector(): WooCommerceFragment

    @ContributesAndroidInjector
    fun provideNotificationsFragmentInjector(): NotificationsFragment

    @ContributesAndroidInjector
    fun provideWooRevenueStatsFragmentInjector(): WooRevenueStatsFragment

    @ContributesAndroidInjector
    fun provideWooProductsFragmentInjector(): WooProductsFragment

    @ContributesAndroidInjector
    fun provideWooUpdateProductFragmentInjector(): WooUpdateProductFragment

    @ContributesAndroidInjector
    fun provideWooUpdateCouponFragmentInjector(): WooUpdateCouponFragment

    @ContributesAndroidInjector
    fun provideWooUpdateVariationFragmentInjector(): WooUpdateVariationFragment

    @ContributesAndroidInjector
    fun provideWooBatchUpdateVariationsFragmentInjector(): WooBatchUpdateVariationsFragment

    @ContributesAndroidInjector
    fun provideWooBatchGenerateVariationsFragmentInjector(): WooBatchGenerateVariationsFragment

    @ContributesAndroidInjector
    fun provideWooProductFiltersFragmentInjector(): WooProductFiltersFragment

    @ContributesAndroidInjector
    fun provideWooProductCategoriesFragmentInjector(): WooProductCategoriesFragment

    @ContributesAndroidInjector
    fun provideWooProductTagsFragmentInjector(): WooProductTagsFragment

    @ContributesAndroidInjector
    fun provideWooOrdersFragmentInjector(): WooOrdersFragment

    @ContributesAndroidInjector
    fun provideWooRefundsFragmentInjector(): WooRefundsFragment

    @ContributesAndroidInjector
    fun provideWooGatewaysFragmentInjector(): WooGatewaysFragment

    @ContributesAndroidInjector
    fun provideWooTaxFragmentInjector(): WooTaxFragment

    @ContributesAndroidInjector
    fun provideWooShippingLabelFragmentInjector(): WooShippingLabelFragment

    @ContributesAndroidInjector
    fun provideWooVerifyAddressFragment(): WooVerifyAddressFragment

    @ContributesAndroidInjector
    fun provideWooLeaderboardsFragmentInjector(): WooLeaderboardsFragment

    @ContributesAndroidInjector
    fun provideSiteSelectorDialogInjector(): SiteSelectorDialog

    @ContributesAndroidInjector
    fun provideStoreSelectorDialogInjector(): StoreSelectorDialog

    @ContributesAndroidInjector
    fun provideReactNativeFragmentInjector(): ReactNativeFragment

    @ContributesAndroidInjector
    fun provideEditorThemeFragmentInjector(): EditorThemeFragment

    @ContributesAndroidInjector
    fun provideExperimentsFragmentInjector(): ExperimentsFragment

    @ContributesAndroidInjector
    fun provideWooProductAttributeFragmentInjector(): WooProductAttributeFragment

    @ContributesAndroidInjector
    fun provideWooCustomersFragmentInjector(): WooCustomersFragment

    @ContributesAndroidInjector
    fun provideWooCouponsFragmentInjector(): WooCouponsFragment

    @ContributesAndroidInjector
    fun provideWooCustomersSearchInjector(): WooCustomersSearchFragment

    @ContributesAndroidInjector
    fun provideWooCustomerCreationFragment(): WooCustomerCreationFragment

    @ContributesAndroidInjector
    fun provideWooAddonsTestFragment(): WooAddonsTestFragment

    @ContributesAndroidInjector
    fun provideWooHelpSupportFragment(): WooHelpSupportFragment

    @ContributesAndroidInjector
    fun provideWooAddressEditDialogFragment(): AddressEditDialogFragment

    @ContributesAndroidInjector
    fun providePluginsFragment(): PluginsFragment

    @ContributesAndroidInjector
    fun provideWooStoreCreationFragment(): WooStoreCreationFragment

    @ContributesAndroidInjector
    fun providePlansFragment(): PlansFragment

    @ContributesAndroidInjector
    fun provideDomainsFragment(): DomainsFragment

    @ContributesAndroidInjector
    fun provideOnboardingFragment(): WooOnboardingFragment

    @ContributesAndroidInjector
    fun provideJetpackAIFragment(): JetpackAIFragment

    @ContributesAndroidInjector
    fun provideWooAdminFragment(): WooAdminFragment

    @ContributesAndroidInjector
    fun provideCustomFieldsFragment(): CustomFieldsFragment
}
