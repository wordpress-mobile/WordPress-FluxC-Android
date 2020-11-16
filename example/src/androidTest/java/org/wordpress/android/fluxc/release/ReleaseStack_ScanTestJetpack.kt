package org.wordpress.android.fluxc.release

import kotlin.jvm.Throws

class ReleaseStack_ScanTestJetpack : ReleaseStack_Base() {
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        // Register
        init()
    }
}
