package org.wordpress.android.fluxc.example

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.android.support.AndroidSupportInjection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.NotificationAction.FETCH_NOTIFICATION
import org.wordpress.android.fluxc.action.NotificationAction.FETCH_NOTIFICATIONS
import org.wordpress.android.fluxc.action.NotificationAction.MARK_NOTIFICATIONS_SEEN
import org.wordpress.android.fluxc.action.NotificationAction.UPDATE_NOTIFICATION
import org.wordpress.android.fluxc.example.databinding.FragmentNotificationsBinding
import org.wordpress.android.fluxc.example.ui.common.showSiteSelectorDialog
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Subkind
import org.wordpress.android.fluxc.persistence.NotificationSqlUtils
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationPayload
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationsPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsReadPayload
import org.wordpress.android.fluxc.store.NotificationStore.MarkNotificationsSeenPayload
import org.wordpress.android.fluxc.store.NotificationStore.OnNotificationChanged
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.DateTimeUtils
import java.util.Date
import javax.inject.Inject

class NotificationsFragment : Fragment() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var notificationStore: NotificationStore
    @Inject internal lateinit var siteStore: SiteStore
    @Inject internal lateinit var notificationSqlUtils: NotificationSqlUtils

    private var typeSelectionDialog: NotificationTypeSubtypeDialog? = null
    private var selectedSite: SiteModel? = null
    private var selectedPos: Int = -1

    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    override fun onAttach(context: Context) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = FragmentNotificationsBinding.inflate(inflater, container, false).root

    @Suppress("LongMethod", "ComplexMethod")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(FragmentNotificationsBinding.bind(view)) {
            notifsFetchAll.setOnClickListener {
                prependToLog("Fetching all notifications from the api...\n")
                dispatcher.dispatch(NotificationActionBuilder.newFetchNotificationsAction(FetchNotificationsPayload()))
            }

            notifsFetchForSite.setOnClickListener {
                prependToLog("Getting all notifications for the first site...\n")
                selectedSite?.let { site ->
                    val notifs = notificationStore.getNotificationsForSite(site)
                    prependToLog("SUCCESS! ${notifs.size} pulled from the database for ${site.name}")
                } ?: prependToLog("No site selected!")
            }

            notifsByTypeSubtype.setOnClickListener {
                showNotificationTypeSubtypeDialog(object : NotificationTypeSubtypeDialog.Listener {
                    override fun onSubmitted(type: String, subtype: String) {
                        prependToLog("Fetching notifications matching $type or $subtype...\n")
                        val notifs = notificationStore.getNotifications(listOf(type), listOf(subtype))
                        val groups = notifs.groupingBy { notif ->
                            notif.subtype?.name?.takeIf { subtype -> subtype != Subkind.UNKNOWN.name } ?: notif.type.name
                        }.fold(0) { acc, _ -> acc + 1 }
                        prependToLog("SUCCESS! Total records matching filtered selections:" +
                                "\n- $type: ${groups[type] ?: 0}\n- $subtype: ${groups[subtype] ?: 0}")
                    }
                })
            }

            notifsHasUnread.setOnClickListener {
                if (selectedSite == null) {
                    prependToLog("No site selected")
                } else {
                    showNotificationTypeSubtypeDialog(object : NotificationTypeSubtypeDialog.Listener {
                        override fun onSubmitted(type: String, subtype: String) {
                            prependToLog("Checking unread notifications matching $type or $subtype...\n")
                            val hasUnread = notificationStore.hasUnreadNotificationsForSite(
                                    selectedSite!!,
                                    listOf(type),
                                    listOf(subtype)
                            )
                            prependToLog("Has unread notifications is $hasUnread")
                        }
                    })
                }
            }

            notifsMarkSeen.setOnClickListener {
                prependToLog("Setting notifications last seen time to now\n")
                dispatcher.dispatch(NotificationActionBuilder
                        .newMarkNotificationsSeenAction(MarkNotificationsSeenPayload(Date().time)))
            }

            notifsFetchFirst.setOnClickListener {
                selectedSite?.let { site ->
                    notificationStore.getNotificationsForSite(site).firstOrNull()?.let { note ->
                        prependToLog("Fetching a single notification with remoteNoteId = ${note.remoteNoteId}\n")
                        dispatcher.dispatch(NotificationActionBuilder
                                .newFetchNotificationAction(FetchNotificationPayload(note.remoteNoteId)))
                    } ?: prependToLog("No notifications found for selected site!")
                } ?: prependToLog("No site selected!")
            }

            notifsMarkRead.setOnClickListener {
                selectedSite?.let { site ->
                    notificationStore.getNotificationsForSite(site).firstOrNull()?.let { note ->
                        prependToLog("Setting notification with remoteNoteId of ${note.remoteNoteId} as read\n")
                        coroutineScope.launch {
                            val result = notificationStore.markNotificationsRead(MarkNotificationsReadPayload(listOf(note)))
                            result.changedNotificationLocalIds.forEach {
                                notificationStore.getNotificationByLocalId(it)?.let { notif ->
                                    prependToLog("SUCCESS! ${notif.toLogString()}")
                                }
                            }
                        }
                    } ?: prependToLog("No notifications found for selected site!")
                } ?: prependToLog("No site selected!")
            }

            notifsMarkAllRead.setOnClickListener {
                // Fetch only unread notifications from the database for the first site
                selectedSite?.let { site ->
                    notificationStore.getNotificationsForSite(site).filter { note -> !note.read }
                            .takeIf { list -> list.isNotEmpty() }?.let { notes ->
                                prependToLog("Marking [${notes.size}] unread notifications as read...\n")
                                coroutineScope.launch {
                                    val result = notificationStore.markNotificationsRead(
                                            MarkNotificationsReadPayload(notes)
                                    )
                                    result.changedNotificationLocalIds.forEach {
                                        notificationStore.getNotificationByLocalId(it)?.let { notif ->
                                            prependToLog("SUCCESS! ${notif.toLogString()}")
                                        }
                                    }
                                }
                            } ?: prependToLog("No unread notifications found!\n")
                } ?: prependToLog("No site selected!")
            }

            notifsUpdateFirst.setOnClickListener {
                selectedSite?.let { site ->
                    notificationStore.getNotificationsForSite(site).firstOrNull()?.let {
                        val note = it.copy(read = !it.read)
                        prependToLog("Updating notification with remoteNoteId " +
                                "of ${note.remoteNoteId} to [read = ${note.read}]\n")
                        dispatcher.dispatch(NotificationActionBuilder.newUpdateNotificationAction(note))
                    } ?: prependToLog("No notifications found for selected site!")
                } ?: prependToLog("No site selected!")
            }

            notifsSelectSite.setOnClickListener {
                showSiteSelectorDialog(selectedPos, object : SiteSelectorDialog.Listener {
                    override fun onSiteSelected(site: SiteModel, pos: Int) {
                        selectedSite = site
                        selectedPos = pos
                        toggleSiteDependentButtons(true)
                        notifSelectedSite.text = site.name ?: site.displayName
                    }
                })
            }

            notifsDeleteAll.setOnClickListener {
                prependToLog("Deleting all the notifications in the DB...\n")
                val count = notificationSqlUtils.deleteAllNotifications()
                prependToLog("SUCCESS! [$count] records deleted")
            }

            notifsDeleteHalf.setOnClickListener {
                prependToLog("Deleting roughly half of notifications in the db...\n")
                val count = Math.round(notificationSqlUtils.getNotificationsCount() / 2.0).toInt()
                notificationStore.getNotifications().take(count).sumBy { notif ->
                    notificationSqlUtils.deleteNotificationByRemoteId(notif.remoteNoteId)
                }.also { total -> prependToLog("Success! [$total] records deleted") }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        dispatcher.register(this)
    }

    override fun onStop() {
        super.onStop()
        dispatcher.unregister(this)
        typeSelectionDialog?.dismiss()
        typeSelectionDialog = null
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onNotificationChanged(event: OnNotificationChanged) {
        if (event.isError) {
            prependToLog("Error from ${event.causeOfChange} - error: ${event.error.type}")
            return
        }

        when (event.causeOfChange) {
            FETCH_NOTIFICATIONS -> {
                prependToLog("SUCCESS! - Fetched ${event.rowsAffected} notifications from the API")
            }
            FETCH_NOTIFICATION -> {
                val localNoteId = event.changedNotificationLocalIds[0]
                notificationStore.getNotificationByLocalId(localNoteId)?.let {
                    prependToLog("SUCCESS! ${it.toLogString()}")
                } ?: prependToLog("Error! Notification not found in db!")
            }
            UPDATE_NOTIFICATION -> {
                val localNoteId = event.changedNotificationLocalIds[0]
                notificationStore.getNotificationByLocalId(localNoteId)?.let {
                    prependToLog("SUCCESS! ${it.toLogString()}")
                }
            }
            MARK_NOTIFICATIONS_SEEN -> {
                val lastSeenDate = event.lastSeenTime?.let {
                    DateTimeUtils.iso8601FromTimestamp(it)
                } ?: ""
                prependToLog("SUCCESS! Last seen set to $lastSeenDate")
            }
            else -> {}
        }
    }

    private fun showNotificationTypeSubtypeDialog(listener: NotificationTypeSubtypeDialog.Listener) {
        fragmentManager?.let { fm ->
            val dialog = NotificationTypeSubtypeDialog.newInstance(listener)
            dialog.show(fm, "NotificationFragment")
        }
    }

    private fun FragmentNotificationsBinding.toggleSiteDependentButtons(enabled: Boolean) {
        notifsFetchForSite.isEnabled = enabled
        notifsMarkAllRead.isEnabled = enabled
        notifsFetchFirst.isEnabled = enabled
        notifsMarkRead.isEnabled = enabled
        notifsUpdateFirst.isEnabled = enabled
        notifsHasUnread.isEnabled = enabled
    }
}
