package org.wordpress.android.fluxc.model

import org.junit.Test
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue

class WCMetaDataValueTest {
    @Test
    fun `when given a plain string, should return a StringValue`() {
        val string = WCMetaDataValue.fromRawString("string")
        assert(string is WCMetaDataValue.StringValue)
    }

    @Test
    fun `when given a number, should return a NumberValue`() {
        val number = WCMetaDataValue.fromRawString("1")
        assert(number is WCMetaDataValue.NumberValue)
    }

    @Test
    fun `when given a float, should return a NumberValue`() {
        val number = WCMetaDataValue.fromRawString("1.5")
        assert(number is WCMetaDataValue.NumberValue)
    }

    @Test
    fun `when given a boolean, should return a BooleanValue`() {
        val boolean = WCMetaDataValue.fromRawString("true")
        assert(boolean is WCMetaDataValue.BooleanValue)
    }

    @Test
    fun `when given a JSON object, should return a JsonObjectValue`() {
        val jsonObject = WCMetaDataValue.fromRawString("""{"key":"value"}""")
        assert(jsonObject is WCMetaDataValue.JsonObjectValue)
    }

    @Test
    fun `when given a JSON array, should return a JsonArrayValue`() {
        val jsonArray = WCMetaDataValue.fromRawString("""[{"key":"value"}]""")
        assert(jsonArray is WCMetaDataValue.JsonArrayValue)
    }
}
