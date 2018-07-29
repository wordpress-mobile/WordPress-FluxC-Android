package org.wordpress.android.fluxc.wc

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses
import org.wordpress.android.fluxc.wc.order.OrdersTestSuite
import org.wordpress.android.fluxc.wc.stats.StatsTestSuite

@RunWith(Suite::class)
@SuiteClasses(
        WooCommerceStoreTest::class,
        OrdersTestSuite::class,
        StatsTestSuite::class)
class WooTestSuite
