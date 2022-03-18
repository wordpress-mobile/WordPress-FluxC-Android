package org.wordpress.android.fluxc.network.rest.wpcom.wc.inbox

import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.persistence.entity.InboxNoteActionEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity
import org.wordpress.android.fluxc.persistence.entity.InboxNoteEntity.InboxNoteStatus.Unactioned

data class InboxNoteDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("type") val type: String?,
    @SerializedName("status") val status: String,
    @SerializedName("is_snoozable") val isSnoozable: Boolean = false,
    @SerializedName("source") val source: String?,
    @SerializedName("actions") val actions: List<InboxNoteActionDto> = emptyList(),
    @SerializedName("locale") val locale: String?,
    @SerializedName("title") val title: String,
    @SerializedName("content") val content: String,
    @SerializedName("layout") val layout: String?,
    @SerializedName("date_created") val dateCreated: String,
    @SerializedName("date_reminder") val dateReminder: String?
) {
    fun toDataModel(siteId: Long) =
        InboxNoteEntity(
            inboxNoteId = id,
            siteId = siteId,
            name = name,
            isSnoozable = isSnoozable,
            title = title,
            content = content,
            dateCreated = dateCreated,
            status = Unactioned,
            source = source,
            type = type,
            dateReminder = dateReminder,
        )
}

data class InboxNoteActionDto(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("label") val label: String,
    @SerializedName("query") val query: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("primary") val primary: Boolean = false,
    @SerializedName("actioned_text") val actionedText: String?,
    @SerializedName("nonce_action") val nonceAction: String?,
    @SerializedName("nonce_name") val nonceName: String?,
    @SerializedName("url") val url: String
) {
    fun toDataModel(inboxNoteId: Long) =
        InboxNoteActionEntity(
            actionId = id,
            inboxNoteId = inboxNoteId,
            name = name,
            label = label,
            url = url,
            query = query,
            status = status,
            primary = primary,
            actionedText = actionedText
        )
}
