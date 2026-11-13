package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccessControlState
import com.example.data.model.StoreSettings
import com.example.ui.components.OrderPipelineRingChart
import com.example.ui.components.RevenueProfitBarChart
import com.example.ui.components.formatMoney
import com.example.ui.theme.AmberPending
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RubyDanger
import com.example.ui.viewmodel.AnalyticsDateRange
import com.example.ui.viewmodel.BusinessAnalyticsSummary

@Composable
fun AnalyticsDashboardScreen(
    accessControl: AccessControlState,
    analytics: BusinessAnalyticsSummary,
    selectedRange: AnalyticsDateRange,
    customDays: Int,
    storeSettings: StoreSettings,
    onSelectDateRange: (AnalyticsDateRange) -> Unit,
    onSetCustomDays: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!accessControl.canViewAnalytics) {
        RestrictedAccessNotice(
            title = "Business Analytics Restricted",
            description = "Real-time P&L, COGS, revenue, and customer telemetry are visible only to the D SQUARE Owner (deepakdhudiya152@gmail.com) and authorized Analytics Staff."
        )
        return
    }

    var showCustomDaysDialog by remember { mutableStateOf(false) }
    val sym = storeSettings.currencySymbol

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Business Performance", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "Real-Time Revenue, Profit & Inventory Intelligence",
                        style = MaterialTheme.typography.bodySmall,
                        color = ChampagneGold
                    )
                }
                Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(28.dp))
            }
        }

        // Date Filters
        item(key = "date_filters") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AnalyticsDateRange.entries) { range ->
                    val label = if (range == AnalyticsDateRange.CUSTOM && selectedRange == AnalyticsDateRange.CUSTOM) {
                        "Last ${customDays}d"
                    } else {
                        range.label
                    }
                    FilterChip(
                        selected = selectedRange == range,
                        onClick = {
                            if (range == AnalyticsDateRange.CUSTOM) {
                                showCustomDaysDialog = true
                            } else {
                                onSelectDateRange(range)
                            }
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ChampagneGold,
                            selectedLabelColor = ObsidianBlack
                        ),
                        modifier = Modifier.testTag("analytics_range_${range.name}")
                    )
                }
            }
        }

        // Primary Financial Distinction: Revenue vs Profit vs Pending Payments
        item(key = "financial_trio") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        label = "Gross Revenue",
                        value = formatMoney(analytics.filteredRevenue, sym),
                        accent = ChampagneGold,
                        subtitle = "Selected period",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Estimated Net Profit",
                        value = formatMoney(analytics.estimatedProfit, sym),
                        accent = if (analytics.estimatedProfit >= 0) EmeraldProfit else RubyDanger,
                        subtitle = "After COGS & expenses",
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiCard(
                        label = "Pending Payments",
                        value = formatMoney(analytics.pendingPaymentsAmount, sym),
                        accent = AmberPending,
                        subtitle = "COD & unpaid orders",
                        modifier = Modifier.weight(1f)
                    )
                    KpiCard(
                        label = "Total Sales (All Time)",
                        value = formatMoney(analytics.totalSales, sym),
                        accent = MaterialTheme.colorScheme.onSurface,
                        subtitle = "Today: ${formatMoney(analytics.todaySales, sym)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Period Sales Matrix
        item(key = "sales_periods") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Sales Velocity", style = MaterialTheme.typography.titleMedium)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MiniMetric("Today", formatMoney(analytics.todaySales, sym))
                        MiniMetric("7 Days", formatMoney(analytics.weeklySales, sym))
                        MiniMetric("Month", formatMoney(analytics.monthlySales, sym))
                        MiniMetric("Year", formatMoney(analytics.yearlySales, sym))
                    }
                }
            }
        }

        // Revenue vs Profit Chart
        item(key = "revenue_profit_chart") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Revenue vs. Net Profit Trend", style = MaterialTheme.typography.titleMedium)
                        Text("Gold: Rev • Green: Profit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(12.dp))
                    RevenueProfitBarChart(
                        revenueSeries = analytics.revenueSeries,
                        profitSeries = analytics.profitSeries
                    )
                }
            }
        }

        // Order Pipeline + Customer Counts
        item(key = "orders_customers") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OrderPipelineRingChart(
                        pending = analytics.pendingOrders,
                        delivered = analytics.deliveredOrders,
                        cancelled = analytics.cancelledOrders
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Orders & Customers", style = MaterialTheme.typography.titleMedium)
                        Text("Total Orders: ${analytics.totalOrders}", style = MaterialTheme.typography.bodySmall)
                        Text("Delivered: ${analytics.deliveredOrders}", style = MaterialTheme.typography.bodySmall, color = EmeraldProfit)
                        Text("Pending Fulfillment: ${analytics.pendingOrders}", style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                        Text("Cancelled / Refunded: ${analytics.cancelledOrders}", style = MaterialTheme.typography.bodySmall, color = RubyDanger)
                        HorizontalDivider()
                        Text("Customers: ${analytics.totalCustomers} (New this month: ${analytics.newCustomers})", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Profit Calculation Breakdown
        item(key = "profit_breakdown") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Real Profit Formula Breakdown", style = MaterialTheme.typography.titleMedium)
                    BreakdownRow("Collected Revenue", formatMoney(analytics.filteredRevenue - analytics.pendingPaymentsAmount, sym), EmeraldProfit)
                    BreakdownRow("Product Cost (COGS)", "-${formatMoney(analytics.totalCogs, sym)}", RubyDanger)
                    BreakdownRow("Discounts Given", formatMoney(analytics.totalDiscounts, sym), AmberPending)
                    BreakdownRow("Shipping Collected", formatMoney(analytics.totalShippingCollected, sym))
                    BreakdownRow("Refunds Processed", "-${formatMoney(analytics.totalRefunds, sym)}", RubyDanger)
                    BreakdownRow("Allocated Operating Expense", "-${formatMoney(analytics.operatingExpense, sym)}", RubyDanger)
                    HorizontalDivider()
                    BreakdownRow(
                        "Estimated Net Profit",
                        formatMoney(analytics.estimatedProfit, sym),
                        if (analytics.estimatedProfit >= 0) EmeraldProfit else RubyDanger
                    )
                }
            }
        }

        // Low Stock Alert List
        item(key = "low_stock") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = AmberPending, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Low Stock Products (${analytics.lowStockProducts.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    if (analytics.lowStockProducts.isEmpty()) {
                        Text("All ${analytics.totalProducts} SKUs are well-stocked.", style = MaterialTheme.typography.bodySmall, color = EmeraldProfit)
                    } else {
                        analytics.lowStockProducts.forEach { prod ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prod.name, style = MaterialTheme.typography.bodyMedium)
                                Text("${prod.stock} left", style = MaterialTheme.typography.labelLarge, color = RubyDanger)
                            }
                        }
                    }
                }
            }
        }

        // Best Selling & Most Viewed
        item(key = "top_products") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Best Selling", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                        analytics.bestSellingProducts.forEach { p ->
                            Text("${p.name} (${p.soldCount} sold)", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Most Viewed", style = MaterialTheme.typography.titleSmall, color = ChampagneGold)
                        analytics.mostViewedProducts.forEach { p ->
                            Text("${p.name} (${p.viewCount} views)", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        }
                    }
                }
            }
        }
    }

    if (showCustomDaysDialog) {
        var daysText by remember { mutableStateOf(customDays.toString()) }
        AlertDialog(
            onDismissRequest = { showCustomDaysDialog = false },
            title = { Text("Custom Analytics Window") },
            text = {
                OutlinedTextField(
                    value = daysText,
                    onValueChange = { daysText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Number of days (1–365)") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSetCustomDays(daysText.toIntOrNull() ?: 14)
                        showCustomDaysDialog = false
                    }
                ) { Text("Apply") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomDaysDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    accent: Color,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = accent)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = ChampagneGold)
    }
}

@Composable
private fun BreakdownRow(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = color)
    }
}

@Composable
fun RestrictedAccessNotice(title: String, description: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
