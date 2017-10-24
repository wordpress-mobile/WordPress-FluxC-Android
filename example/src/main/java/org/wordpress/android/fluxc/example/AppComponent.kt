package org.wordpress.android.fluxc.example

import dagger.Component
import org.wordpress.android.fluxc.module.AppContextModule
import org.wordpress.android.fluxc.module.ReleaseBaseModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AppContextModule::class,
        AppSecretsModule::class,
        ReleaseOkHttpClientModule::class,
        ReleaseBaseModule::class,
        ReleaseNetworkModule::class))
interface AppComponent {
    fun inject(app: ExampleApp)
    fun inject(activity: MainExampleActivity)
    fun inject(fragment: SitesFragment)
    fun inject(fragment: MainFragment)
    fun inject(fragment: MediaFragment)
    fun inject(fragment: CommentsFragment)
    fun inject(fragment: PostsFragment)
    fun inject(fragment: AccountFragment)
    fun inject(fragment: SignedOutActionsFragment)
    fun inject(fragment: TaxonomiesFragment)
    fun inject(fragment: UploadsFragment)
    fun inject(fragment: ThemeFragment)
}
