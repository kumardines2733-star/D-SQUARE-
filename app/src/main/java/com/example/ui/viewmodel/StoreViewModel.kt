package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccessControlState
import com.example.data.model.Address
import com.example.data.model.Banner
import com.example.data.model.CartItem
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
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

enum class ProductSortOption(val label: String) {
    FEATURED("Featured"),
    PRICE_LOW_HIGH("Price: Low to High"),
    PRICE_HIGH_LOW("Price: High to Low"),
    TOP_RATED("Top Rated"),
    BEST_SELLING("Best Selling")
}

data class CatalogUiFilter(
    val searchQuery: String = "",
    val selectedCategoryId: String = "ALL",
    val sortOption: ProductSortOption = ProductSortOption.FEATURED,
    val inStockOnly: Boolean = false,
    val maxPriceFilter: Double? = null
)

class StoreViewModel(
    private val catalogRepo: CatalogRepository,
    private val customerRepo: CustomerRepository,
    private val orderRepo: OrderRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _filter = MutableStateFlow(CatalogUiFilter())
    val filter: StateFlow<CatalogUiFilter> = _filter.asStateFlow()

    private val _appliedCoupon = MutableStateFlow<Coupon?>(null)
    val appliedCoupon: StateFlow<Coupon?> = _appliedCoupon.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val accessControl: StateFlow<AccessControlState> = orderRepo.observeAccessControl(currentUserId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AccessControlState())

    val categories: StateFlow<List<Category>> = catalogRepo.observeCategories()
        .safeCatch("categories", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allProducts: StateFlow<List<Product>> = catalogRepo.observeProducts()
        .safeCatch("products", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val filteredProducts: StateFlow<List<Product>> = combine(allProducts, _filter) { products, f ->
        products.filter { p ->
            p.isActive &&
                (f.selectedCategoryId == "ALL" || p.categoryId == f.selectedCategoryId) &&
                (!f.inStockOnly || p.stock > 0L) &&
                (f.maxPriceFilter == null || p.price <= f.maxPriceFilter) &&
                (f.searchQuery.isBlank() ||
                    p.name.contains(f.searchQuery, ignoreCase = true) ||
                    p.categoryName.contains(f.searchQuery, ignoreCase = true) ||
                    p.sku.contains(f.searchQuery, ignoreCase = true) ||
                    p.description.contains(f.searchQuery, ignoreCase = true))
        }.let { list ->
            when (f.sortOption) {
                ProductSortOption.FEATURED -> list.sortedByDescending { it.isFeatured }
                ProductSortOption.PRICE_LOW_HIGH -> list.sortedBy { it.price }
                ProductSortOption.PRICE_HIGH_LOW -> list.sortedByDescending { it.price }
                ProductSortOption.TOP_RATED -> list.sortedByDescending { it.ratingAvg }
                ProductSortOption.BEST_SELLING -> list.sortedByDescending { it.soldCount }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val banners: StateFlow<List<Banner>> = catalogRepo.observeBanners()
        .safeCatch("banners", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val coupons: StateFlow<List<Coupon>> = catalogRepo.observeCoupons()
        .safeCatch("coupons", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val storeSettings: StateFlow<StoreSettings> = catalogRepo.observeStoreSettings()
        .safeCatch("store_settings", StoreSettings())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), StoreSettings())

    val userProfile: StateFlow<UserProfile?> = customerRepo.observeProfile(currentUserId)
        .safeCatch("profile", null)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val addresses: StateFlow<List<Address>> = customerRepo.observeAddresses(currentUserId)
        .safeCatch("addresses", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val cartItems: StateFlow<List<CartItem>> = customerRepo.observeCart(currentUserId)
        .safeCatch("cart", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val wishlist: StateFlow<List<WishlistItem>> = customerRepo.observeWishlist(currentUserId)
        .safeCatch("wishlist", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<NotificationItem>> = customerRepo.observeNotifications(currentUserId)
        .safeCatch("notifications", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val customerOrders: StateFlow<List<Order>> = orderRepo.observeCustomerOrders(currentUserId)
        .safeCatch("orders", emptyList())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    init {
        viewModelScope.launch {
            customerRepo.ensureUserProfile()
            if (orderRepo.isVerifiedOwner()) {
                catalogRepo.seedLuxuryStoreIfEmpty(isOwner = true)
            }
        }
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    fun updateSearch(query: String) {
        _filter.value = _filter.value.copy(searchQuery = query)
    }

    fun selectCategory(categoryId: String) {
        _filter.value = _filter.value.copy(selectedCategoryId = categoryId)
    }

    fun setSortOption(option: ProductSortOption) {
        _filter.value = _filter.value.copy(sortOption = option)
    }

    fun setInStockOnly(inStockOnly: Boolean) {
        _filter.value = _filter.value.copy(inStockOnly = inStockOnly)
    }

    fun setMaxPrice(maxPrice: Double?) {
        _filter.value = _filter.value.copy(maxPriceFilter = maxPrice)
    }

    fun observeProductReviews(productId: String): Flow<List<ProductReview>> =
        catalogRepo.observeProductReviews(productId).safeCatch("reviews", emptyList())

    fun recordProductView(productId: String) {
        viewModelScope.launch {
            catalogRepo.incrementProductView(productId)
        }
    }

    fun addToCart(product: Product, variant: String, qty: Long = 1L) {
        if (product.stock <= 0L) {
            _statusMessage.value = "${product.name} is currently out of stock"
            return
        }
        viewModelScope.launch {
            customerRepo.addToCart(product, variant, qty)
                .onSuccess { _statusMessage.value = "Added ${product.name} to Bag" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to add to Bag" }
        }
    }

    fun updateCartQuantity(cartItemId: String, quantity: Long) {
        viewModelScope.launch {
            customerRepo.updateCartQuantity(cartItemId, quantity)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update cart" }
        }
    }

    fun toggleWishlist(product: Product) {
        val exists = wishlist.value.any { it.productId == product.id }
        viewModelScope.launch {
            customerRepo.toggleWishlist(product, exists)
                .onSuccess { added ->
                    _statusMessage.value = if (added) "Saved to Wishlist" else "Removed from Wishlist"
                }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Wishlist update failed" }
        }
    }

    fun applyCouponCode(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.isBlank()) {
            _appliedCoupon.value = null
            return
        }
        val subtotal = cartItems.value.sumOf { it.unitPrice * it.quantity }
        val match = coupons.value.firstOrNull { it.code.equals(code, ignoreCase = true) && it.isActive }
        when {
            match == null -> _statusMessage.value = "Coupon '$code' is invalid or expired"
            match.usageCount >= match.usageLimit -> _statusMessage.value = "Coupon usage limit reached"
            subtotal < match.minOrderAmount -> _statusMessage.value = "Minimum order ${storeSettings.value.currencySymbol}${match.minOrderAmount.toInt()} required"
            else -> {
                _appliedCoupon.value = match
                _statusMessage.value = "Applied ${match.code}"
            }
        }
    }

    fun removeCoupon() {
        _appliedCoupon.value = null
    }

    fun calculateDiscount(subtotal: Double, coupon: Coupon?): Double {
        if (coupon == null || !coupon.isActive || subtotal < coupon.minOrderAmount) return 0.0
        val raw = if (coupon.discountType == "PERCENT") {
            subtotal * (coupon.discountValue / 100.0)
        } else {
            coupon.discountValue
        }
        return raw.coerceAtMost(coupon.maxDiscount.takeIf { it > 0 } ?: raw).coerceAtMost(subtotal)
    }

    fun placeOrder(
        address: Address,
        paymentMethod: String,
        onOrderCreated: (Order) -> Unit
    ) {
        val items = cartItems.value
        if (items.isEmpty()) {
            _statusMessage.value = "Your shopping bag is empty"
            return
        }
        val profile = userProfile.value
        val settings = storeSettings.value
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val discount = calculateDiscount(subtotal, _appliedCoupon.value)
        val taxable = (subtotal - discount).coerceAtLeast(0.0)
        val shipping = if (taxable >= settings.freeShippingThreshold) 0.0 else settings.defaultShippingFee
        val tax = Math.round(taxable * (settings.taxRatePercent / 100.0) * 100.0) / 100.0
        val fullAddr = "${address.recipientName}, ${address.street}, ${address.city}, ${address.state} ${address.postalCode}, ${address.country} (Ph: ${address.phone})"

        viewModelScope.launch {
            _isBusy.value = true
            orderRepo.placeOrder(
                customerName = profile?.displayName ?: address.recipientName,
                customerEmail = profile?.email ?: "client@dsquare.luxury",
                customerPhone = address.phone,
                shippingAddress = fullAddr,
                cartItems = items,
                appliedCoupon = _appliedCoupon.value,
                discountAmount = discount,
                shippingFee = shipping,
                taxAmount = tax,
                paymentMethod = paymentMethod
            ).onSuccess { order ->
                customerRepo.clearCart()
                _appliedCoupon.value = null
                _statusMessage.value = "Order ${order.orderNumber} placed successfully"
                onOrderCreated(order)
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Checkout failed"
            }
            _isBusy.value = false
        }
    }

    fun submitReview(productId: String, rating: Long, title: String, comment: String) {
        if (title.isBlank() || comment.isBlank()) {
            _statusMessage.value = "Please enter both a review title and comment"
            return
        }
        viewModelScope.launch {
            catalogRepo.submitReview(productId, rating, title, comment)
                .onSuccess { _statusMessage.value = "Thank you for your review" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not submit review" }
        }
    }

    fun saveAddress(address: Address) {
        viewModelScope.launch {
            customerRepo.saveAddress(address)
                .onSuccess { _statusMessage.value = "Address saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save address" }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            customerRepo.deleteAddress(addressId)
                .onSuccess { _statusMessage.value = "Address removed" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to remove address" }
        }
    }

    fun updateProfile(displayName: String, phone: String, preferredPayment: String) {
        viewModelScope.launch {
            customerRepo.updateProfile(displayName, phone, preferredPayment)
                .onSuccess { _statusMessage.value = "Profile updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update profile" }
        }
    }

    fun cancelOrder(orderId: String, reason: String) {
        viewModelScope.launch {
            orderRepo.cancelOrderByCustomer(orderId, reason)
                .onSuccess { _statusMessage.value = "Order cancelled & refund requested" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Cancellation failed" }
        }
    }

    fun requestReturn(orderId: String, reason: String) {
        viewModelScope.launch {
            orderRepo.requestReturnByCustomer(orderId, reason)
                .onSuccess { _statusMessage.value = "Return & refund request submitted" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Return request failed" }
        }
    }

    fun markNotificationRead(notifId: String) {
        viewModelScope.launch {
            customerRepo.markNotificationRead(notifId)
        }
    }

    private fun <T> Flow<T>.safeCatch(tag: String, fallback: T): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        emit(fallback)
    }
}
