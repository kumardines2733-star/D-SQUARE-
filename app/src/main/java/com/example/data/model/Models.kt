package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val phone: String = "",
    val preferredPayment: String = "UPI",
    val totalOrders: Long = 0L,
    val totalSpent: Double = 0.0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Address(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "Home",
    val recipientName: String = "",
    val phone: String = "",
    val street: String = "",
    val city: String = "",
    val state: String = "",
    val postalCode: String = "",
    val country: String = "India",
    val isDefault: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Category(
    @DocumentId val id: String = "",
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val iconKey: String = "fashion",
    val sortOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Product(
    @DocumentId val id: String = "",
    val name: String = "",
    val sku: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val costPrice: Double = 0.0,
    val compareAtPrice: Double = 0.0,
    val stock: Long = 0L,
    val lowStockThreshold: Long = 5L,
    val variants: List<String> = emptyList(),
    val colors: List<String> = emptyList(),
    val sizes: List<String> = emptyList(),
    val imageKey: String = "img_prod_fashion",
    val imageUrls: List<String> = emptyList(),
    val ratingAvg: Double = 4.8,
    val reviewCount: Long = 0L,
    val soldCount: Long = 0L,
    val viewCount: Long = 0L,
    val isFeatured: Boolean = false,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductReview(
    @DocumentId val id: String = "",
    val productId: String = "",
    val userId: String = "",
    val userName: String = "",
    val rating: Long = 5L,
    val title: String = "",
    val comment: String = "",
    val isApproved: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CartItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val categoryName: String = "",
    val variant: String = "Standard",
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val quantity: Long = 1L,
    val imageKey: String = "img_prod_fashion",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class WishlistItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val categoryName: String = "",
    val price: Double = 0.0,
    val imageKey: String = "img_prod_fashion",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class OrderLineItem(
    val productId: String = "",
    val productName: String = "",
    val variant: String = "",
    val qty: Long = 1L,
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val imageKey: String = "img_prod_fashion"
)

@IgnoreExtraProperties
data class Order(
    @DocumentId val id: String = "",
    val orderNumber: String = "",
    val userId: String = "",
    val customerName: String = "",
    val customerEmail: String = "",
    val customerPhone: String = "",
    val shippingAddress: String = "",
    val itemsSummary: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val subtotal: Double = 0.0,
    val discountAmount: Double = 0.0,
    val couponCode: String = "",
    val shippingFee: Double = 0.0,
    val taxAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalCost: Double = 0.0,
    val paymentMethod: String = "UPI",
    val paymentStatus: String = "PAID",
    val paymentReference: String = "",
    val orderStatus: String = "PLACED",
    val trackingNumber: String = "",
    val trackingCourier: String = "",
    val cancelReason: String = "",
    val returnReason: String = "",
    val refundAmount: Double = 0.0,
    val refundStatus: String = "NONE",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Coupon(
    @DocumentId val id: String = "",
    val code: String = "",
    val description: String = "",
    val discountType: String = "PERCENT",
    val discountValue: Double = 10.0,
    val minOrderAmount: Double = 0.0,
    val maxDiscount: Double = 500.0,
    val usageCount: Long = 0L,
    val usageLimit: Long = 100L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Banner(
    @DocumentId val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val badge: String = "",
    val ctaText: String = "SHOP NOW",
    val targetCategoryId: String = "",
    val imageKey: String = "img_hero_luxury",
    val sortOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StoreSettings(
    @DocumentId val id: String = "",
    val storeName: String = "D SQUARE",
    val tagline: String = "Curated Luxury & Modern Essentials",
    val currencySymbol: String = "₹",
    val defaultShippingFee: Double = 99.0,
    val freeShippingThreshold: Double = 2499.0,
    val taxRatePercent: Double = 18.0,
    val monthlyOperatingExpense: Double = 45000.0,
    val supportEmail: String = "concierge@dsquare.luxury",
    val supportPhone: String = "+91 1800 202 9900",
    val announcementBar: String = "COMPLIMENTARY EXPRESS SHIPPING ABOVE ₹2,499 • USE CODE DSQUARE15",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StaffMember(
    @DocumentId val id: String = "",
    val staffUid: String = "",
    val email: String = "",
    val displayName: String = "",
    val roleName: String = "Store Manager",
    val canManageProducts: Boolean = true,
    val canManageOrders: Boolean = true,
    val canManageCustomers: Boolean = false,
    val canManageCoupons: Boolean = false,
    val canManageContent: Boolean = false,
    val canViewAnalytics: Boolean = true,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "ORDER",
    val isRead: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorUid: String = "",
    val actorEmail: String = "",
    val actorRole: String = "OWNER",
    val action: String = "",
    val targetCollection: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class AdminRole {
    OWNER,
    STAFF,
    CUSTOMER
}

data class AccessControlState(
    val role: AdminRole = AdminRole.CUSTOMER,
    val staffMember: StaffMember? = null
) {
    val isOwner: Boolean get() = role == AdminRole.OWNER
    val canAccessAdmin: Boolean get() = role == AdminRole.OWNER || (role == AdminRole.STAFF && staffMember?.isActive == true)
    val canManageProducts: Boolean get() = isOwner || (staffMember?.isActive == true && staffMember.canManageProducts)
    val canManageOrders: Boolean get() = isOwner || (staffMember?.isActive == true && staffMember.canManageOrders)
    val canManageCustomers: Boolean get() = isOwner || (staffMember?.isActive == true && staffMember.canManageCustomers)
    val canManageCoupons: Boolean get() = isOwner || (staffMember?.isActive == true && staffMember.canManageCoupons)
    val canManageContent: Boolean get() = isOwner || (staffMember?.isActive == true && staffMember.canManageContent)
    val canViewAnalytics: Boolean get() = isOwner || (staffMember?.isActive == true && staffMember.canViewAnalytics)
}
