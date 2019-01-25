package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.model.WCSettingsModel
import java.text.DecimalFormat

object WCCurrencyUtils {
    /**
     * Formats the given [rawValue] as a decimal based on the site's currency settings as stored in [siteSettings].
     *
     * Currency symbol and placement are not handled.
     */
    fun formatCurrencyForDisplay(rawValue: Double, siteSettings: WCSettingsModel): String {
        val decimalFormat = if (siteSettings.currencyDecimalNumber > 0) {
            DecimalFormat("#,##0.${"0".repeat(siteSettings.currencyDecimalNumber)}")
        } else {
            DecimalFormat("#,##0")
        }

        decimalFormat.decimalFormatSymbols = decimalFormat.decimalFormatSymbols.apply {
            // If no decimal separator is set, keep whatever the system default is
            siteSettings.currencyDecimalSeparator.takeIf { it.isNotEmpty() }?.let {
                decimalSeparator = it[0]
            }
            // If no thousands separator is set, assume it's intentional and clear it in the formatter
            siteSettings.currencyThousandSeparator.takeIf { it.isNotEmpty() }?.let {
                groupingSeparator = it[0]
            } ?: run { decimalFormat.isGroupingUsed = false }
        }

        return decimalFormat.format(rawValue)
    }
}
