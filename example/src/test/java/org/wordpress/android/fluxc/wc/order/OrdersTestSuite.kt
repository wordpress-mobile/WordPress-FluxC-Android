package org.wordpress.android.fluxc.wc.order

import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@RunWith(Suite::class)
@SuiteClasses(OrderSqlUtilsTest::class, WCOrderModelTest::class, WCOrderStoreTest::class)
class OrdersTestSuite
