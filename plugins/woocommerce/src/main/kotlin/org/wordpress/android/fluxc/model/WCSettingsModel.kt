package org.wordpress.android.fluxc.model

import java.util.Locale

data class WCSettingsModel(
    val localSiteId: Int,
    val currencyCode: String, // The currency code for the site in 3-letter ISO 4217 format
    val currencyPosition: CurrencyPosition, // Where the currency symbol should be placed
    val currencyThousandSeparator: String, // The thousands separator character (e.g. the comma in 3,000)
    val currencyDecimalSeparator: String, // The decimal separator character (e.g. the dot in 41.12)
    val currencyDecimalNumber: Int, // How many decimal points to display
    val countryCode: String = "" // The country code for the site in 2-letter format i.e. US
) {
    enum class CurrencyPosition {
        LEFT, RIGHT, LEFT_SPACE, RIGHT_SPACE;

        companion object {
            private val reverseMap = CurrencyPosition.values().associateBy(CurrencyPosition::name)
            fun fromString(type: String?) = CurrencyPosition.reverseMap[type?.toUpperCase(Locale.US)] ?: LEFT
        }
    }

    enum class SiteCountry(val countryName: String) {
        // A
        AX("Åland Islands"),
        AF("Afghanistan"),
        AL("Albania"),
        DZ("Algeria"),
        AS("American Samoa"),
        AD("Andorra"),
        AO("Angola"),
        AI("Anguilla"),
        AQ("Antarctica"),
        AG("Antigua and Barbuda"),
        AR("Argentina"),
        AM("Armenia"),
        AW("Aruba"),
        AU("Australia"),
        AT("Austria"),
        AZ("Azerbaijan"),

        // B
        BS("Bahamas"),
        BH("Bahrain"),
        BD("Bangladesh"),
        BB("Barbados"),
        BY("Belarus"),
        PW("Belau"),
        BE("Belgium"),
        BZ("Belize"),
        BJ("Benin"),
        BM("Bermuda"),
        BT("Bhutan"),
        BO("Bolivia"),
        BQ("Bonaire, Saint Eustatius and Saba"),
        BA("Bosnia and Herzegovina"),
        BW("Botswana"),
        BV("Bouvet Island"),
        BR("Brazil"),
        IO("British Indian Ocean Territory"),
        VG("British Virgin Islands"),
        BN("Brunei"),
        BG("Bulgaria"),
        BF("Burkina Faso"),
        BI("Burundi"),

        // C
        KH("Cambodia"),
        CM("Cameroon"),
        CA("Canada"),
        CV("Cape Verde"),
        KY("Cayman Islands"),
        CF("Central African Republic"),
        TD("Chad"),
        CL("Chile"),
        CN("China"),
        CX("Christmas Island"),
        CC("Cocos (Keeling) Islands"),
        CO("Colombia"),
        KM("Comoros"),
        CG("Congo (Brazzaville)"),
        CD("Congo (Kinshasa)"),
        CK("Cook Islands"),
        CR("Costa Rica"),
        HR("Croatia"),
        CU("Cuba"),
        CW("Curacao"),
        CY("Cyprus"),
        CZ("Czech Republic"),

        // D
        DK("Denmark"),
        DJ("Djibouti"),
        DM("Dominica"),
        DO("Dominican Republic"),

        // E
        EC("Ecuador"),
        EG("Egypt"),
        SV("El Salvador"),
        GQ("Equatorial Guinea"),
        ER("Eritrea"),
        EE("Estonia"),
        ET("Ethiopia"),

        // F
        FK("Falkland Islands"),
        FO("Faroe Islands"),
        FJ("Fiji"),
        FI("Finland"),
        FR("France"),
        GF("French Guiana"),
        PF("French Polynesia"),
        TF("French Southern Territories"),

        // G
        GA("Gabon"),
        GM("Gambia"),
        GE("Georgia"),
        DE("Germany"),
        GH("Ghana"),
        GI("Gibraltar"),
        GR("Greece"),
        GL("Greenland"),
        GD("Grenada"),
        GP("Guadeloupe"),
        GU("Guam"),
        GT("Guatemala"),
        GG("Guernsey"),
        GN("Guinea"),
        GW("Guinea-Bissau"),
        GY("Guyana"),

        // H
        HT("Haiti"),
        HM("Heard Island and McDonald Islands"),
        HN("Honduras"),
        HK("Hong Kong"),
        HU("Hungary"),

        // I
        IS("Iceland"),
        IN("India"),
        ID("Indonesia"),
        IR("Iran"),
        IQ("Iraq"),
        IE("Ireland"),
        IM("Isle of Man"),
        IL("Israel"),
        IT("Italy"),
        CI("Ivory Coast"),

        // J
        JM("Jamaica"),
        JP("Japan"),
        JE("Jersey"),
        JO("Jordan"),

        // K
        KZ("Kazakhstan"),
        KE("Kenya"),
        KI("Kiribati"),
        KW("Kuwait"),
        KG("Kyrgyzstan"),

        // L
        LA("Laos"),
        LV("Latvia"),
        LB("Lebanon"),
        LS("Lesotho"),
        LR("Liberia"),
        LY("Libya"),
        LI("Liechtenstein"),
        LT("Lithuania"),
        LU("Luxembourg"),

        // M
        MO("Macao S.A.R., China"),
        MK("Macedonia"),
        MG("Madagascar"),
        MW("Malawi"),
        MY("Malaysia"),
        MV("Maldives"),
        ML("Mali"),
        MT("Malta"),
        MH("Marshall Islands"),
        MQ("Martinique"),
        MR("Mauritania"),
        MU("Mauritius"),
        YT("Mayotte"),
        MX("Mexico"),
        FM("Micronesia"),
        MD("Moldova"),
        MC("Monaco"),
        MN("Mongolia"),
        ME("Montenegro"),
        MS("Montserrat"),
        MA("Morocco"),
        MZ("Mozambique"),
        MM("Myanmar"),

        // N
        NA("Namibia"),
        NR("Nauru"),
        NP("Nepal"),
        NL("Netherlands"),
        NC("New Caledonia"),
        NZ("New Zealand"),
        NI("Nicaragua"),
        NE("Niger"),
        NG("Nigeria"),
        NU("Niue"),
        NF("Norfolk Island"),
        KP("North Korea"),
        MP("Northern Mariana Islands"),
        NO("Norway"),

        // O
        OM("Oman"),

        // P
        PK("Pakistan"),
        PS("Palestinian Territory"),
        PA("Panama"),
        PG("Papua New Guinea"),
        PY("Paraguay"),
        PE("Peru"),
        PH("Philippines"),
        PN("Pitcairn"),
        PL("Poland"),
        PT("Portugal"),
        PR("Puerto Rico"),

        // Q
        QA("Qatar"),

        // R
        RE("Reunion"),
        RO("Romania"),
        RU("Russia"),
        RW("Rwanda"),

        // S
        ST("São Tomé and Príncipe"),
        BL("Saint Barthélemy"),
        SH("Saint Helena"),
        KN("Saint Kitts and Nevis"),
        LC("Saint Lucia"),
        SX("Saint Martin (Dutch part)"),
        MF("Saint Martin (French part)"),
        PM("Saint Pierre and Miquelon"),
        VC("Saint Vincent and the Grenadines"),
        WS("Samoa"),
        SM("San Marino"),
        SA("Saudi Arabia"),
        SN("Senegal"),
        RS("Serbia"),
        SC("Seychelles"),
        SL("Sierra Leone"),
        SG("Singapore"),
        SK("Slovakia"),
        SI("Slovenia"),
        SB("Solomon Islands"),
        SO("Somalia"),
        ZA("South Africa"),
        GS("South Georgia/Sandwich Islands"),
        KR("South Korea"),
        SS("South Sudan"),
        ES("Spain"),
        LK("Sri Lanka"),
        SD("Sudan"),
        SR("Suriname"),
        SJ("Svalbard and Jan Mayen"),
        SZ("Swaziland"),
        SE("Sweden"),
        CH("Switzerland"),
        SY("Syria"),

        // T
        TW("Taiwan"),
        TJ("Tajikistan"),
        TZ("Tanzania"),
        TH("Thailand"),
        TL("Timor-Leste"),
        TG("Togo"),
        TK("Tokelau"),
        TO("Tonga"),
        TT("Trinidad and Tobago"),
        TN("Tunisia"),
        TR("Turkey"),
        TM("Turkmenistan"),
        TC("Turks and Caicos Islands"),
        TV("Tuvalu"),

        // U
        UG("Uganda"),
        UA("Ukraine"),
        AE("United Arab Emirates"),
        GB("United Kingdom"),
        US("United States"),
        UY("Uruguay"),
        UZ("Uzbekistan"),

        // V
        VU("Vanuatu"),
        VA("Vatican"),
        VE("Venezuela"),
        VN("Vietnam"),

        // W
        WF("Wallis and Futuna"),
        EH("Western Sahara"),

        // Y
        YE("Yemen"),

        // Z
        ZM("Zambia"),
        ZW("Zimbabwe");

        companion object {
            /**
             * Returns the name of the country associated with the current store.
             * The default store country is provided in a format like `US:NY`
             * This method will transform `US:NY` into `United States`
             * Will return nil if it can not figure out a valid country name
             * */
            fun fromCountryCode(value: String?): String? {
                return if (!value.isNullOrEmpty()) {
                    SiteCountry.valueOf(
                            value.split(":")[0]
                    ).countryName
                } else {
                    null
                }
            }
        }
    }
}
