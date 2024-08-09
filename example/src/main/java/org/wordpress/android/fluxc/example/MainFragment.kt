package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.AccountAction
import org.wordpress.android.fluxc.example.SigninDialog.SigningListener
import org.wordpress.android.fluxc.example.ThreeEditTextDialog.Listener
import org.wordpress.android.fluxc.example.databinding.FragmentMainBinding
import org.wordpress.android.fluxc.example.ui.WooCommerceFragment
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.AuthenticationActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.network.HTTPAuthManager
import org.wordpress.android.fluxc.network.MemorizingTrustManager
import org.wordpress.android.fluxc.network.discovery.SelfHostedEndpointFinder.DiscoveryError
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator.CookieNonceAuthenticationResult.Error
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceAuthenticator.CookieNonceAuthenticationResult.Success
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.AuthenticatePayload
import org.wordpress.android.fluxc.store.AccountStore.AuthenticateTwoFactorPayload
import org.wordpress.android.fluxc.store.AccountStore.AuthenticationErrorType
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import org.wordpress.android.fluxc.store.AccountStore.OnDiscoveryResponse
import org.wordpress.android.fluxc.store.AccountStore.OnTwoFactorAuthStarted
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.FetchSitesPayload
import org.wordpress.android.fluxc.store.SiteStore.FetchWPAPISitePayload
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.fluxc.store.SiteStore.RefreshSitesXMLRPCPayload
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

class MainFragment : Fragment() {
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var httpAuthManager: HTTPAuthManager
    @Inject internal lateinit var memorizingTrustManager: MemorizingTrustManager
    @Inject internal lateinit var cookieNonceAuthenticator: CookieNonceAuthenticator

    // Would be great to not have to keep this state, but it makes HTTPAuth and self signed SSL management easier
    private var selfHostedPayload: RefreshSitesXMLRPCPayload? = null

    private var authenticatePayload: AuthenticatePayload? = null

    // Used for 2fa
    private var authenticateTwoFactorPayload: AuthenticateTwoFactorPayload? = null

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentMainBinding.inflate(inflater, container, false).root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentMainBinding.bind(view)) {
            signInFetchSitesButton.setOnClickListener { showSigninDialog() }

            signout.setOnClickListener { signOut() }

            signedOutActions.setOnClickListener {
                fragmentManager?.beginTransaction()
                    ?.replace(R.id.fragment_container, SignedOutActionsFragment())
                    ?.addToBackStack(null)
                    ?.commit()
            }

            account.setOnClickListener(getOnClickListener(AccountFragment()))
            sites.setOnClickListener(getOnClickListener(SitesFragment()))
            posts.setOnClickListener(getOnClickListener(PostsFragment()))
            comments.setOnClickListener(getOnClickListener(CommentsFragment()))
            media.setOnClickListener(getOnClickListener(MediaFragment()))
            taxonomies.setOnClickListener(getOnClickListener(TaxonomiesFragment()))
            uploads.setOnClickListener(getOnClickListener(UploadsFragment()))
            themes.setOnClickListener(getOnClickListener(ThemeFragment()))
            woo.setOnClickListener(getOnClickListener(WooCommerceFragment()))
            notifs.setOnClickListener(getOnClickListener(NotificationsFragment()))
            reactnative.setOnClickListener(getOnClickListener(ReactNativeFragment()))
            editortheme.setOnClickListener(getOnClickListener(EditorThemeFragment()))
            experiments.setOnClickListener(getOnClickListener(ExperimentsFragment()))
            plugins.setOnClickListener(getOnClickListener(PluginsFragment()))
            plans.setOnClickListener(getOnClickListener(PlansFragment()))
            domains.setOnClickListener(getOnClickListener(DomainsFragment()))
            jetpackai.setOnClickListener(getOnClickListener(JetpackAIFragment()))
        }
    }

    // Private methods

    private fun getOnClickListener(fragment: Fragment) =
        OnClickListener { replaceFragment(fragment) }

    private fun replaceFragment(fragment: Fragment) {
        if (siteStore.sitesCount == 0 && !accountStore.hasAccessToken()) {
            ToastUtils.showToast(activity, "You must be logged in")
            return
        }
        fragmentManager?.beginTransaction()
            ?.replace(R.id.fragment_container, fragment)
            ?.addToBackStack(null)
            ?.commit()
    }

    private fun showSSLWarningDialog(certifString: String) {
        val ft = fragmentManager?.beginTransaction()
        val newFragment = SSLWarningDialog.newInstance(
            { _, _ ->
                // Add the certificate to our list
                memorizingTrustManager.storeLastFailure()
                // Retry login action
                selfHostedPayload?.let {
                    signInAction(it.username, it.password, it.url, true)
                }
            }, certifString
        )
        ft?.let { newFragment.show(it, "dialog") }
    }

    private fun showSigninDialog() {
        val ft = fragmentManager?.beginTransaction()
        val newFragment = SigninDialog.newInstance(object : SigningListener {
            override fun onClick(
                username: String,
                password: String,
                url: String,
                isXmlrpc: Boolean
            ) {
                signInAction(username, password, url, isXmlrpc)
            }
        })
        ft?.let { newFragment.show(it, "dialog") }
    }

    private fun show2faDialog() {
        val ft = fragmentManager?.beginTransaction()
        val newFragment = ThreeEditTextDialog.newInstance(object : Listener {
            override fun onClick(text1: String, text2: String, text3: String) {
                if (TextUtils.isEmpty(text3)) {
                    prependToLog("2FA code required to login")
                    return
                }
                signIn2fa(text3)
            }
        }, "", "", "2FA Code")
        ft?.let { newFragment.show(it, "2fadialog") }
    }

    private fun signIn2fa(twoStepCode: String) {
        authenticateTwoFactorPayload = AuthenticateTwoFactorPayload(
            authenticatePayload?.username.orEmpty(),
            authenticatePayload?.password.orEmpty(),
            twoStepCode,
            true
        )

        dispatcher.dispatch(AuthenticationActionBuilder
            .newAuthenticateTwoFactorAction(authenticateTwoFactorPayload))
    }

    private fun showHTTPAuthDialog(url: String) {
        val ft = fragmentManager?.beginTransaction()
        val newFragment = ThreeEditTextDialog.newInstance(object : Listener {
            @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            override fun onClick(username: String, password: String, unused: String) {
                // Add credentials
                httpAuthManager.addHTTPAuthCredentials(username, password, url, null)
                // Retry login action
                selfHostedPayload?.let {
                    signInAction(it.username ?: "", it.password ?: "", url, isXmlrpc = true)
                }
            }
        }, "Username", "Password", "unused")
        ft?.let { newFragment.show(it, "dialog") }
    }

    /**
     * Called when the user tap OK on the SignIn Dialog. It authenticates and list user sites, on wpcom or self hosted
     * depending if the user filled the URL field.
     */
    private fun signInAction(username: String, password: String, url: String, isXmlrpc: Boolean) {
        when {
            TextUtils.isEmpty(url) -> wpcomFetchSites(username, password)
            isXmlrpc -> {
                selfHostedPayload = RefreshSitesXMLRPCPayload(username, password, url)
                dispatcher.dispatch(AuthenticationActionBuilder.newDiscoverEndpointAction(url))
            }
            else -> lifecycleScope.launch {
                cookieNonceAuthenticator.authenticate(url, username, password).let {
                    when (it) {
                        is Error -> prependToLog("Authentication error: ${it.type} - ${it.message}")
                        Success -> siteStore.fetchWPAPISite(
                            FetchWPAPISitePayload(
                                url = url,
                                username = username,
                                password = password
                            )
                        ).let { fetchResult ->
                            if (fetchResult.isError) {
                                prependToLog("Error fetching site: ${fetchResult.error}")
                            } else {
                                prependToLog("Successfully signed in using WPAPI")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun signOut() {
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
        // Remove all remaining sites
        for (site in siteStore.sites) {
            dispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(site))
        }
    }

    private fun wpcomFetchSites(username: String, password: String) {
        authenticatePayload = AuthenticatePayload(username, password)
        dispatcher.dispatch(AuthenticationActionBuilder.newAuthenticateAction(authenticatePayload))
    }

    private fun selfHostedFetchSites(username: String, password: String, xmlrpcEndpoint: String) {
        val payload = RefreshSitesXMLRPCPayload(username, password, xmlrpcEndpoint)
        selfHostedPayload = payload
        // Self Hosted don't have any "Authentication" request, try to list sites with user/password
        dispatcher.dispatch(SiteActionBuilder.newFetchSitesXmlRpcAction(payload))
    }

    // Event listeners

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAccountChanged(event: OnAccountChanged) {
        if (!accountStore.hasAccessToken()) {
            prependToLog("Signed Out")
            return
        }

        if (event.isError) {
            prependToLog("Account error: " + event.error.type)
        } else {
            if (!siteStore.hasSite() && event.causeOfChange == AccountAction.FETCH_ACCOUNT) {
                AppLog.d(T.API, "Account data fetched - fetching sites")
                dispatcher.dispatch(SiteActionBuilder.newFetchSitesAction(FetchSitesPayload()))
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        if (event.isError) {
            prependToLog("Authentication error: " + event.error.type)

            when (event.error.type) {
                AuthenticationErrorType.AUTHORIZATION_REQUIRED,
                AuthenticationErrorType.ACCESS_DENIED -> {
                    // You're not authorized to do that
                }
                AuthenticationErrorType.INVALID_CLIENT,
                AuthenticationErrorType.INVALID_GRANT,
                AuthenticationErrorType.UNSUPPORTED_GRANT_TYPE -> {
                    // You should fix your gradle.properties
                }
                AuthenticationErrorType.UNKNOWN_TOKEN,
                AuthenticationErrorType.INVALID_TOKEN,
                AuthenticationErrorType.NOT_AUTHENTICATED,
                AuthenticationErrorType.INCORRECT_USERNAME_OR_PASSWORD -> showSigninDialog()
                AuthenticationErrorType.HTTP_AUTH_ERROR ->
                    // Show a Dialog prompting for http username and password
                    selfHostedPayload?.let { showHTTPAuthDialog(it.url) }
                AuthenticationErrorType.INVALID_SSL_CERTIFICATE ->
                    // Show a SSL Warning Dialog
                    showSSLWarningDialog(memorizingTrustManager.lastFailure.toString())
                AuthenticationErrorType.NEEDS_2FA -> show2faDialog()
                AuthenticationErrorType.INVALID_OTP -> {}
                AuthenticationErrorType.INVALID_REQUEST,
                AuthenticationErrorType.UNSUPPORTED_RESPONSE_TYPE,
                AuthenticationErrorType.EMAIL_LOGIN_NOT_ALLOWED,
                AuthenticationErrorType.GENERIC_ERROR,
                AuthenticationErrorType.NEEDS_SECURITY_KEY -> Unit // Do nothing
                AuthenticationErrorType.WEBAUTHN_FAILED -> Unit // Do nothing
                null -> {
                    // Show Toast "Network Error"?
                }
            }
        } else {
            if (accountStore.hasAccessToken()) {
                AppLog.d(T.API, "Signed in to WordPress.com successfully, fetching account")
                dispatcher.dispatch(AccountActionBuilder.newFetchAccountAction())
                dispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction())
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onDiscoveryResponse(event: OnDiscoveryResponse) {
        if (event.isError) {
            when (event.error) {
                DiscoveryError.WORDPRESS_COM_SITE -> {
                    selfHostedPayload?.let {
                        wpcomFetchSites(it.username ?: "", it.password ?: "")
                    }
                }
                DiscoveryError.HTTP_AUTH_REQUIRED -> showHTTPAuthDialog(event.failedEndpoint)
                DiscoveryError.ERRONEOUS_SSL_CERTIFICATE -> {
                    selfHostedPayload = selfHostedPayload?.copy(url = event.failedEndpoint)
                    showSSLWarningDialog(memorizingTrustManager.lastFailure.toString())
                }
                else -> {}
            }
            prependToLog("Discovery failed with error: " + event.error)
            AppLog.e(T.API, "Discover error: " + event.error)
        } else {
            if (event.wpRestEndpoint != null && !event.wpRestEndpoint.isEmpty()) {
                prependToLog("Discovery succeeded, found WP-API endpoint: " + event.wpRestEndpoint)
            } else {
                prependToLog("Discovery succeeded, found XML-RPC endpoint: " + event.xmlRpcEndpoint)
            }
            selfHostedPayload?.let {
                selfHostedFetchSites(it.username, it.password, event.xmlRpcEndpoint)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        if (event.isError) {
            prependToLog("SiteChanged error: " + event.error)
            return
        }
        if (siteStore.hasSite()) {
            val firstSite = siteStore.sites[0]
            prependToLog(
                "First site name: " + firstSite.name + " - Total sites: " + siteStore.sitesCount +
                    " - rowsAffected: " + event.rowsAffected
            )
        }
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onTwoFactorAuthStarted(event: OnTwoFactorAuthStarted) {
        show2faDialog()
    }

    private fun prependToLog(s: String) = (activity as MainExampleActivity).prependToLog(s)
}
