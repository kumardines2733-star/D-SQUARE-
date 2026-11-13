package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccessControlState
import com.example.data.model.AuditLog
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StaffMember
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class AnalyticsDateRange(val label: String) {
    TODAY("Today"),
    YESTERDAY("Yesterday"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    THIS_MONTH("This Month"),
    THIS_YEAR("This Year"),
    ALL_TIME("All Time"),
    CUSTOM("Custom")
}

data class BusinessAnalyticsSummary(
    val totalSales: Double = 0.0,
    val todaySales: Double = 0.0,
    val weeklySales: Double = 0.0,
    val monthlySales: Double = 0.0,
    val yearlySales: Double = 0.0,
    val filteredRevenue: Double = 0.0,
    val pendingPaymentsAmount: Double = 0.0,
    val totalDiscounts: Double = 0.0,
    val totalRefunds: Double = 0.0,
    val totalShippingCollected: Double = 0.0,
    val totalCogs: Double = 0.0,
    val operatingExpense: Double = 0.0,
    val estimatedProfit: Double = 0.0,
    val totalOrders: Int = 0,
    val pendingOrders: Int = 0,
    val deliveredOrders: Int = 0,
    val cancelledOrders: Int = 0,
    val totalCustomers: Int = 0,
    val newCustomers: Int = 0,
    val totalProducts: Int = 0,
    val lowStockProducts: List<Product> = emptyList(),
    val bestSellingProducts: List<Product> = emptyList(),
    val mostViewedProducts: List<Product> = emptyList(),
    val revenueSeries: List<Pair<String, Double>> = emptyList(),
    val profitSeries: List<Pair<String, Double>> = emptyList()
)

class AdminViewModel(
    private val catalogRepo: CatalogRepository,
    private val customerRepo: CustomerRepository,
    private val orderRepo: OrderRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _dateRange = MutableStateFlow(AnalyticsDateRange.THIS_MONTH)
    val dateRange: StateFlow<AnalyticsDateRange> = _dateRange.asStateFlow()

    private val _customDays = MutableStateFlow(14)
    val customDays: StateFlow<Int> = _customDays.asStateFlow()

    private val _adminMessage = MutableStateFlow<String?>(null)
    val adminMessage: StateFlow<String?> = _adminMessage.asStateFlow()

    val accessControl: StateFlow<AccessControlState> = orderRepo.observeAccessControl(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AccessControlState())

    val allOrders: StateFlow<List<Order>> =
        if (orderRepo.isVerifiedOwner()) {
            orderRepo.observeAllOrders().safeCatch(emptyList())
        } else {
            orderRepo.observeCustomerOrders(currentUserId).safeCatch(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allCustomers: StateFlow<List<UserProfile>> =
        if (orderRepo.isVerifiedOwner()) {
            orderRepo.observeAllCustomers().safeCatch(emptyList())
        } else {
            MutableStateFlow(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val staffMembers: StateFlow<List<StaffMember>> =
        if (orderRepo.isVerifiedOwner()) {
            orderRepo.observeStaff().safeCatch(emptyList())
        } else {
            MutableStateFlow(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> =
        if (orderRepo.isVerifiedOwner()) {
            orderRepo.observeAuditLogs().safeCatch(emptyList())
        } else {
            MutableStateFlow(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val products: StateFlow<List<Product>> = catalogRepo.observeProducts()
        .safeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val categories: StateFlow<List<Category>> = catalogRepo.observeCategories()
        .safeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val coupons: StateFlow<List<Coupon>> = catalogRepo.observeCoupons()
        .safeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val banners: StateFlow<List<Banner>> = catalogRepo.observeBanners()
        .safeCatch(emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val storeSettings: StateFlow<StoreSettings> = catalogRepo.observeStoreSettings()
        .safeCatch(StoreSettings())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StoreSettings())

    val analytics: StateFlow<BusinessAnalyticsSummary> = combine(
        allOrders,
        allCustomers,
        products,
        storeSettings,
        combine(_dateRange, _customDays) { r, d -> r to d }
    ) { orders, customers, prods, settings, (range, customDayWindow) ->
        computeAnalytics(orders, customers, prods, settings, range, customDayWindow)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BusinessAnalyticsSummary())

    fun clearMessage() {
        _adminMessage.value = null
    }

    fun selectDateRange(range: AnalyticsDateRange) {
        _dateRange.value = range
    }

    fun setCustomDays(days: Int) {
        _customDays.value = days.coerceIn(1, 365)
        _dateRange.value = AnalyticsDateRange.CUSTOM
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            catalogRepo.saveProduct(product)
                .onSuccess { id ->
                    logAction("SAVE_PRODUCT", "products", id, "Saved ${product.name} (${product.price})")
                    _adminMessage.value = "Product '${product.name}' saved"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to save product" }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            catalogRepo.deleteProduct(product.id)
                .onSuccess {
                    logAction("DELETE_PRODUCT", "products", product.id, "Deleted ${product.name}")
                    _adminMessage.value = "Deleted ${product.name}"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to delete product" }
        }
    }

    fun saveCategory(category: Category) {
        viewModelScope.launch {
            catalogRepo.saveCategory(category)
                .onSuccess { id ->
                    logAction("SAVE_CATEGORY", "categories", id, "Saved category ${category.name}")
                    _adminMessage.value = "Category '${category.name}' saved"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to save category" }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            catalogRepo.deleteCategory(category.id)
                .onSuccess {
                    logAction("DELETE_CATEGORY", "categories", category.id, "Deleted category ${category.name}")
                    _adminMessage.value = "Deleted ${category.name}"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to delete category" }
        }
    }

    fun updateOrder(
        order: Order,
        orderStatus: String,
        paymentStatus: String,
        trackingNumber: String,
        trackingCourier: String,
        refundStatus: String,
        refundAmount: Double
    ) {
        viewModelScope.launch {
            orderRepo.updateOrderByAdmin(
                orderId = order.id,
                orderStatus = orderStatus,
                paymentStatus = paymentStatus,
                trackingNumber = trackingNumber,
                trackingCourier = trackingCourier,
                refundStatus = refundStatus,
                refundAmount = refundAmount
            ).onSuccess {
                logAction(
                    "UPDATE_ORDER",
                    "orders",
                    order.id,
                    "${order.orderNumber}: status=$orderStatus, payment=$paymentStatus, refund=$refundStatus"
                )
                customerRepo.createNotification(
                    targetUserId = order.userId,
                    title = "Order ${order.orderNumber} • $orderStatus",
                    message = if (trackingNumber.isNotBlank()) {
                        "Your shipment is $orderStatus via $trackingCourier ($trackingNumber)."
                    } else {
                        "Your order status is now $orderStatus."
                    },
                    type = "ORDER"
                )
                _adminMessage.value = "Updated ${order.orderNumber}"
            }.onFailure {
                _adminMessage.value = it.localizedMessage ?: "Failed to update order"
            }
        }
    }

    fun saveCoupon(coupon: Coupon) {
        viewModelScope.launch {
            catalogRepo.saveCoupon(coupon)
                .onSuccess { id ->
                    logAction("SAVE_COUPON", "coupons", id, "Saved coupon ${coupon.code}")
                    _adminMessage.value = "Coupon ${coupon.code} saved"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to save coupon" }
        }
    }

    fun deleteCoupon(coupon: Coupon) {
        viewModelScope.launch {
            catalogRepo.deleteCoupon(coupon.id)
                .onSuccess {
                    logAction("DELETE_COUPON", "coupons", coupon.id, "Deleted coupon ${coupon.code}")
                    _adminMessage.value = "Deleted ${coupon.code}"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to delete coupon" }
        }
    }

    fun saveBanner(banner: Banner) {
        viewModelScope.launch {
            catalogRepo.saveBanner(banner)
                .onSuccess { id ->
                    logAction("SAVE_BANNER", "banners", id, "Saved banner ${banner.title}")
                    _adminMessage.value = "Banner updated"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to save banner" }
        }
    }

    fun saveStoreSettings(settings: StoreSettings) {
        viewModelScope.launch {
            catalogRepo.saveStoreSettings(settings)
                .onSuccess {
                    logAction("UPDATE_SETTINGS", "store_settings", "main", "Updated store configuration")
                    _adminMessage.value = "Business settings saved"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to save settings" }
        }
    }

    fun saveStaffMember(staff: StaffMember) {
        viewModelScope.launch {
            orderRepo.saveStaffMember(staff)
                .onSuccess {
                    logAction("SAVE_STAFF", "staff", staff.staffUid, "Assigned ${staff.roleName} to ${staff.email}")
                    _adminMessage.value = "Staff member '${staff.displayName}' saved"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to save staff" }
        }
    }

    fun deleteStaffMember(staff: StaffMember) {
        viewModelScope.launch {
            orderRepo.deleteStaffMember(staff.staffUid)
                .onSuccess {
                    logAction("REMOVE_STAFF", "staff", staff.staffUid, "Removed staff ${staff.email}")
                    _adminMessage.value = "Removed ${staff.displayName}"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to remove staff" }
        }
    }

    fun sendCustomerBroadcastNotification(targetUserId: String, title: String, message: String) {
        viewModelScope.launch {
            customerRepo.createNotification(targetUserId, title, message, "PROMO")
                .onSuccess {
                    logAction("SEND_NOTIFICATION", "notifications", targetUserId, title)
                    _adminMessage.value = "Notification sent"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Failed to send notification" }
        }
    }

    fun reseedCatalogIfOwner() {
        viewModelScope.launch {
            catalogRepo.seedLuxuryStoreIfEmpty(orderRepo.isVerifiedOwner())
                .onSuccess { seeded ->
                    _adminMessage.value = if (seeded) "D SQUARE flagship catalog initialized" else "Catalog already populated"
                }
                .onFailure { _adminMessage.value = it.localizedMessage ?: "Seed failed" }
        }
    }

    private suspend fun logAction(action: String, collection: String, targetId: String, details: String) {
        val roleLabel = if (accessControl.value.isOwner) "OWNER" else accessControl.value.staffMember?.roleName ?: "STAFF"
        orderRepo.recordAuditLog(roleLabel, action, collection, targetId, details)
    }

    private fun computeAnalytics(
        orders: List<Order>,
        customers: List<UserProfile>,
        prods: List<Product>,
        settings: StoreSettings,
        range: AnalyticsDateRange,
        customDayWindow: Int
    ): BusinessAnalyticsSummary {
        val nowMs = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfTodayMs = cal.timeInMillis
        val startOfYesterdayMs = startOfTodayMs - 86_400_000L
        val weekAgoMs = nowMs - 7L * 86_400_000L
        val thirtyDaysAgoMs = nowMs - 30L * 86_400_000L

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfMonthMs = monthCal.timeInMillis

        val yearCal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfYearMs = yearCal.timeInMillis

        fun orderTimeMs(order: Order): Long = (order.createdAt?.seconds ?: (nowMs / 1000L)) * 1000L

        val nonCancelled = orders.filter { it.orderStatus != "CANCELLED" && it.orderStatus != "REFUNDED" }
        val totalSales = nonCancelled.sumOf { it.totalAmount }
        val todaySales = nonCancelled.filter { orderTimeMs(it) >= startOfTodayMs }.sumOf { it.totalAmount }
        val weeklySales = nonCancelled.filter { orderTimeMs(it) >= weekAgoMs }.sumOf { it.totalAmount }
        val monthlySales = nonCancelled.filter { orderTimeMs(it) >= startOfMonthMs }.sumOf { it.totalAmount }
        val yearlySales = nonCancelled.filter { orderTimeMs(it) >= startOfYearMs }.sumOf { it.totalAmount }

        val rangeOrders = orders.filter { o ->
            val t = orderTimeMs(o)
            when (range) {
                AnalyticsDateRange.TODAY -> t >= startOfTodayMs
                AnalyticsDateRange.YESTERDAY -> t in startOfYesterdayMs until startOfTodayMs
                AnalyticsDateRange.LAST_7_DAYS -> t >= weekAgoMs
                AnalyticsDateRange.LAST_30_DAYS -> t >= thirtyDaysAgoMs
                AnalyticsDateRange.THIS_MONTH -> t >= startOfMonthMs
                AnalyticsDateRange.THIS_YEAR -> t >= startOfYearMs
                AnalyticsDateRange.ALL_TIME -> true
                AnalyticsDateRange.CUSTOM -> t >= (nowMs - customDayWindow.toLong() * 86_400_000L)
            }
        }

        val activeRangeOrders = rangeOrders.filter { it.orderStatus != "CANCELLED" && it.orderStatus != "REFUNDED" }
        val paidRevenue = activeRangeOrders.filter { it.paymentStatus == "PAID" }.sumOf { it.totalAmount }
        val pendingPayments = activeRangeOrders.filter { it.paymentStatus == "PENDING" }.sumOf { it.totalAmount }
        val totalDiscounts = activeRangeOrders.sumOf { it.discountAmount }
        val totalRefunds = rangeOrders.filter { it.refundStatus == "PROCESSED" || it.orderStatus == "REFUNDED" }
            .sumOf { if (it.refundAmount > 0) it.refundAmount else it.totalAmount }
        val totalShipping = activeRangeOrders.sumOf { it.shippingFee }
        val totalCogs = activeRangeOrders.sumOf { it.totalCost }

        val periodDays = when (range) {
            AnalyticsDateRange.TODAY, AnalyticsDateRange.YESTERDAY -> 1.0
            AnalyticsDateRange.LAST_7_DAYS -> 7.0
            AnalyticsDateRange.LAST_30_DAYS, AnalyticsDateRange.THIS_MONTH -> 30.0
            AnalyticsDateRange.THIS_YEAR -> 365.0
            AnalyticsDateRange.ALL_TIME -> 30.0
            AnalyticsDateRange.CUSTOM -> customDayWindow.toDouble()
        }
        val allocatedExpense = (settings.monthlyOperatingExpense / 30.0) * periodDays
        val grossRevenue = paidRevenue + pendingPayments
        val estimatedProfit = paidRevenue - totalCogs - totalRefunds - allocatedExpense

        val newCustomers = customers.count {
            val createdMs = (it.createdAt?.seconds ?: 0L) * 1000L
            createdMs >= startOfMonthMs
        }

        // Build 6-bucket series for visual charts
        val buckets = (5 downTo 0).map { offset ->
            val bucketEnd = nowMs - offset * 86_400_000L
            val bucketStart = bucketEnd - 86_400_000L
            val dayOrders = activeRangeOrders.filter {
                val t = orderTimeMs(it)
                t in bucketStart..bucketEnd
            }
            val rev = dayOrders.sumOf { it.totalAmount }
            val prof = (rev - dayOrders.sumOf { it.totalCost }).coerceAtLeast(0.0)
            val label = "D-${offset}"
            Triple(if (offset == 0) "Today" else label, rev, prof)
        }

        return BusinessAnalyticsSummary(
            totalSales = totalSales,
            todaySales = todaySales,
            weeklySales = weeklySales,
            monthlySales = monthlySales,
            yearlySales = yearlySales,
            filteredRevenue = grossRevenue,
            pendingPaymentsAmount = pendingPayments,
            totalDiscounts = totalDiscounts,
            totalRefunds = totalRefunds,
            totalShippingCollected = totalShipping,
            totalCogs = totalCogs,
            operatingExpense = allocatedExpense,
            estimatedProfit = estimatedProfit,
            totalOrders = rangeOrders.size,
            pendingOrders = rangeOrders.count { it.orderStatus in listOf("PLACED", "CONFIRMED", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY") },
            deliveredOrders = rangeOrders.count { it.orderStatus == "DELIVERED" },
            cancelledOrders = rangeOrders.count { it.orderStatus == "CANCELLED" || it.orderStatus == "REFUNDED" },
            totalCustomers = customers.size,
            newCustomers = newCustomers,
            totalProducts = prods.size,
            lowStockProducts = prods.filter { it.stock <= it.lowStockThreshold }.sortedBy { it.stock },
            bestSellingProducts = prods.sortedByDescending { it.soldCount }.take(5),
            mostViewedProducts = prods.sortedByDescending { it.viewCount }.take(5),
            revenueSeries = buckets.map { it.first to it.second },
            profitSeries = buckets.map { it.first to it.third }
        )
    }

    private fun <T> Flow<T>.safeCatch(fallback: T): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(fallback)
    }
}
