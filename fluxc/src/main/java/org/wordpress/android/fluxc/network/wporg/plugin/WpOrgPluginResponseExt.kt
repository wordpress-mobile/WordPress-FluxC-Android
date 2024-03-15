package org.wordpress.android.fluxc.network.wporg.plugin

import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.plugin.WPOrgPluginModel

fun WPOrgPluginResponse.toWPOrgPluginModel(): WPOrgPluginModel {
    val wpOrgPluginModel = WPOrgPluginModel()
    wpOrgPluginModel.authorAsHtml = this.authorAsHtml
    wpOrgPluginModel.banner = this.banner
    wpOrgPluginModel.descriptionAsHtml = this.descriptionAsHtml
    wpOrgPluginModel.faqAsHtml = this.faqAsHtml
    wpOrgPluginModel.homepageUrl = this.homepageUrl
    wpOrgPluginModel.icon = this.icon
    wpOrgPluginModel.installationInstructionsAsHtml = this.installationInstructionsAsHtml
    wpOrgPluginModel.lastUpdated = this.lastUpdated
    wpOrgPluginModel.displayName = StringEscapeUtils.unescapeHtml4(this.name)
    wpOrgPluginModel.rating = this.rating
    wpOrgPluginModel.requiredWordPressVersion = this.requiredWordPressVersion
    wpOrgPluginModel.slug = this.slug
    wpOrgPluginModel.version = this.version
    wpOrgPluginModel.whatsNewAsHtml = this.whatsNewAsHtml
    wpOrgPluginModel.downloadCount = this.downloadCount
    wpOrgPluginModel.numberOfRatings = this.numberOfRatings
    wpOrgPluginModel.numberOfRatingsOfOne = this.numberOfRatingsOfOne
    wpOrgPluginModel.numberOfRatingsOfTwo = this.numberOfRatingsOfTwo
    wpOrgPluginModel.numberOfRatingsOfThree = this.numberOfRatingsOfThree
    wpOrgPluginModel.numberOfRatingsOfFour = this.numberOfRatingsOfFour
    wpOrgPluginModel.numberOfRatingsOfFive = this.numberOfRatingsOfFive
    return wpOrgPluginModel
}