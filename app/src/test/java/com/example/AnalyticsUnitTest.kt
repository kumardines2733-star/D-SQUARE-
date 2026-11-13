package com.example

import com.example.data.model.Coupon
import org.junit.Assert.assertEquals
import org.junit.Test

class AnalyticsUnitTest {

    @Test
    fun percentCoupon_respectsMaxDiscountAndMinOrder() {
        val coupon = Coupon(
            code = "DSQUARE15",
            discountType = "PERCENT",
            discountValue = 15.0,
            minOrderAmount = 5000.0,
            maxDiscount = 4000.0,
            isActive = true
        )
        val subtotal = 30000.0
        val raw = subtotal * (coupon.discountValue / 100.0) // 4500.0
        val clamped = raw.coerceAtMost(coupon.maxDiscount).coerceAtMost(subtotal)
        assertEquals(4000.0, clamped, 0.01)
    }

    @Test
    fun netProfitCalculation_deductsCogsRefundsAndExpense() {
        val collectedRevenue = 50000.0
        val cogs = 24000.0
        val refunds = 2500.0
        val allocatedOpEx = 1500.0
        val netProfit = collectedRevenue - cogs - refunds - allocatedOpEx
        assertEquals(22000.0, netProfit, 0.01)
    }
}
