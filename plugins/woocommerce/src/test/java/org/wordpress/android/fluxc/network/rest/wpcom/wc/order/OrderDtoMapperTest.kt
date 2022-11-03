package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.AdditionalAnswers
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.LocalOrRemoteId
import org.wordpress.android.fluxc.model.OrderEntity

internal class OrderDtoMapperTest {
    private val stripOrder: StripOrder = mock {
        on { invoke(any()) }.then(AdditionalAnswers.returnsFirstArg<OrderEntity>())
    }
    private val stripOrderMetaData: StripOrderMetaData = mock()

    val localSiteId = LocalOrRemoteId.LocalId(0)
    val sut = OrderDtoMapper(stripOrder, stripOrderMetaData)

    @Test
    fun `when is_editable is NULL and the status is an editable status the order in Editable`() {
        for (status in OrderDtoMapper.EDITABLE_STATUSES) {
            val oldOrderDTO = getEditableOrder(
                isEditable = null,
                status = status
            )
            val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
            assertThat(orderEntity.isEditable).isEqualTo(true)
        }
    }

    @Test
    fun `when is_editable is NULL and the status is not an editable status the order is not Editable`() {
        val oldOrderDTO = getEditableOrder(isEditable = null, status = "")
        val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
        assertThat(orderEntity.isEditable).isEqualTo(false)
    }

    @Test
    fun `when is_editable field is true the order is Editable`() {
        val oldOrderDTO = getEditableOrder(isEditable = true, status = "")
        val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
        assertThat(orderEntity.isEditable).isEqualTo(true)
    }

    @Test
    fun `when is_editable field is false the order is not Editable`() {
        // We only check for editable statuses if the is_editable field is null
        for (status in OrderDtoMapper.EDITABLE_STATUSES) {
            val oldOrderDTO = getEditableOrder(isEditable = false, status = status)
            val (orderEntity, _) = sut.toDatabaseEntity(oldOrderDTO, localSiteId)
            assertThat(orderEntity.isEditable).isEqualTo(false)
        }
    }

    private fun getEditableOrder(
        isEditable: Boolean?,
        status: String = "auto-draft"
    ): OrderDto {
        val json = JsonObject().apply {
            addProperty("status", status)
            isEditable?.let { value -> addProperty("is_editable", value) }
        }
        return Gson().fromJson(json, OrderDto::class.java)
    }
}
