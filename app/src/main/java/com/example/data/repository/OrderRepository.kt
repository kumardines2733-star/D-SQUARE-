package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.AccessControlState
import com.example.data.model.AdminRole
import com.example.data.model.AuditLog
import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.StaffMember
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class OrderRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    companion object {
        const val OWNER_EMAIL = "deepakdhudiya152@gmail.com"
    }

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun isVerifiedOwner(): Boolean {
        val user = auth.currentUser ?: return false
        return user.email.equals(OWNER_EMAIL, ignoreCase = true) && user.isEmailVerified
    }

    fun observeAccessControl(uid: String): Flow<AccessControlState> = flow {
        if (isVerifiedOwner()) {
            emit(AccessControlState(role = AdminRole.OWNER, staffMember = null))
        } else {
            db.collection("staff").document(uid)
                .snapshots()
                .map { snap ->
                    val staff = snap.toObject(StaffMember::class.java, ServerTimestampBehavior.ESTIMATE)
                    if (staff != null && staff.isActive) {
                        AccessControlState(role = AdminRole.STAFF, staffMember = staff)
                    } else {
                        AccessControlState(role = AdminRole.CUSTOMER, staffMember = null)
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(AccessControlState(role = AdminRole.CUSTOMER, staffMember = null))
                }
                .collect { emit(it) }
        }
    }

    fun observeCustomerOrders(userId: String): Flow<List<Order>> =
        db.collection("orders")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Order::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "orders", userId)
            }

    fun observeAllOrders(): Flow<List<Order>> =
        db.collection("orders")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Order::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "orders", auth.currentUser?.uid ?: "")
            }

    fun observeAllCustomers(): Flow<List<UserProfile>> =
        db.collection("users")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.totalSpent }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "users", auth.currentUser?.uid ?: "")
            }

    fun observeStaff(): Flow<List<StaffMember>> =
        db.collection("staff")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(StaffMember::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "staff", auth.currentUser?.uid ?: "")
            }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        db.collection("audit_logs")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(AuditLog::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "audit_logs", auth.currentUser?.uid ?: "")
            }

    suspend fun placeOrder(
        customerName: String,
        customerEmail: String,
        customerPhone: String,
        shippingAddress: String,
        cartItems: List<CartItem>,
        appliedCoupon: Coupon?,
        discountAmount: Double,
        shippingFee: Double,
        taxAmount: Double,
        paymentMethod: String
    ): Result<Order> = runCatching {
        val uid = requireUserId()
        require(cartItems.isNotEmpty()) { "Cart cannot be empty" }

        val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
        val totalCost = cartItems.sumOf { it.costPrice * it.quantity }
        val totalAmount = (subtotal - discountAmount + shippingFee + taxAmount).coerceAtLeast(0.0)
        val orderId = "ord_${UUID.randomUUID().toString().take(10)}"
        val orderNumber = "DSQ-${System.currentTimeMillis().toString().takeLast(6)}"
        val isCod = paymentMethod == "COD"
        val paymentStatus = if (isCod) "PENDING" else "PAID"
        val paymentRef = if (isCod) "COD-COLLECT" else "${paymentMethod}-${UUID.randomUUID().toString().take(6).uppercase()}"

        val lineMaps = cartItems.take(25).map { item ->
            mapOf(
                "productId" to item.productId,
                "productName" to item.productName,
                "variant" to item.variant,
                "qty" to item.quantity,
                "unitPrice" to item.unitPrice,
                "costPrice" to item.costPrice,
                "imageKey" to item.imageKey
            )
        }
        val summary = cartItems.joinToString(", ") { "${it.productName} ×${it.quantity}" }.take(280)

        val orderPayload = mapOf(
            "orderNumber" to orderNumber,
            "userId" to uid,
            "customerName" to customerName.ifBlank { "D SQUARE Client" },
            "customerEmail" to customerEmail.ifBlank { "client@dsquare.luxury" },
            "customerPhone" to customerPhone,
            "shippingAddress" to shippingAddress,
            "itemsSummary" to summary,
            "items" to lineMaps,
            "subtotal" to subtotal,
            "discountAmount" to discountAmount,
            "couponCode" to (appliedCoupon?.code ?: ""),
            "shippingFee" to shippingFee,
            "taxAmount" to taxAmount,
            "totalAmount" to totalAmount,
            "totalCost" to totalCost,
            "paymentMethod" to paymentMethod,
            "paymentStatus" to paymentStatus,
            "paymentReference" to paymentRef,
            "orderStatus" to "PLACED",
            "trackingNumber" to "",
            "trackingCourier" to "",
            "cancelReason" to "",
            "returnReason" to "",
            "refundAmount" to 0.0,
            "refundStatus" to "NONE",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        db.collection("orders").document(orderId).set(orderPayload).await()

        // Update stock & soldCount safely for each item
        for (item in cartItems) {
            val prodRef = db.collection("products").document(item.productId)
            val snap = prodRef.get().await()
            if (snap.exists()) {
                val currStock = snap.getLong("stock") ?: 0L
                val currSold = snap.getLong("soldCount") ?: 0L
                val nextStock = (currStock - item.quantity).coerceAtLeast(0L)
                prodRef.update(
                    mapOf(
                        "stock" to nextStock,
                        "soldCount" to (currSold + item.quantity),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }

        // Increment coupon usage if applied
        if (appliedCoupon != null && appliedCoupon.id.isNotBlank()) {
            db.collection("coupons").document(appliedCoupon.id).update(
                mapOf(
                    "usageCount" to (appliedCoupon.usageCount + 1L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // Update customer totals
        val userRef = db.collection("users").document(uid)
        val userSnap = userRef.get().await()
        if (userSnap.exists()) {
            val currOrders = userSnap.getLong("totalOrders") ?: 0L
            val currSpent = userSnap.getDouble("totalSpent") ?: 0.0
            userRef.update(
                mapOf(
                    "totalOrders" to (currOrders + 1L),
                    "totalSpent" to (currSpent + totalAmount),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        // Create order confirmation notification
        val notifId = "ntf_${UUID.randomUUID().toString().take(8)}"
        db.collection("users").document(uid).collection("notifications").document(notifId)
            .set(
                mapOf(
                    "userId" to uid,
                    "title" to "Order $orderNumber Confirmed",
                    "message" to "Thank you! Your order for $summary is placed and being prepared by D SQUARE Atelier.",
                    "type" to "ORDER",
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

        Order(
            id = orderId,
            orderNumber = orderNumber,
            userId = uid,
            customerName = customerName,
            customerEmail = customerEmail,
            customerPhone = customerPhone,
            shippingAddress = shippingAddress,
            itemsSummary = summary,
            items = lineMaps,
            subtotal = subtotal,
            discountAmount = discountAmount,
            couponCode = appliedCoupon?.code ?: "",
            shippingFee = shippingFee,
            taxAmount = taxAmount,
            totalAmount = totalAmount,
            totalCost = totalCost,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            paymentReference = paymentRef,
            orderStatus = "PLACED"
        )
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "orders")
    }

    suspend fun cancelOrderByCustomer(orderId: String, reason: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("orders").document(orderId).update(
            mapOf(
                "orderStatus" to "CANCELLED",
                "cancelReason" to reason.ifBlank { "Cancelled by customer" }.take(300),
                "refundStatus" to "REQUESTED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "orders/$orderId")
    }

    suspend fun requestReturnByCustomer(orderId: String, reason: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("orders").document(orderId).update(
            mapOf(
                "orderStatus" to "RETURN_REQUESTED",
                "returnReason" to reason.ifBlank { "Return requested" }.take(300),
                "refundStatus" to "REQUESTED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "orders/$orderId")
    }

    suspend fun updateOrderByAdmin(
        orderId: String,
        orderStatus: String,
        paymentStatus: String,
        trackingNumber: String,
        trackingCourier: String,
        refundStatus: String,
        refundAmount: Double
    ): Result<Unit> = runCatching {
        requireUserId()
        db.collection("orders").document(orderId).update(
            mapOf(
                "orderStatus" to orderStatus,
                "paymentStatus" to paymentStatus,
                "trackingNumber" to trackingNumber,
                "trackingCourier" to trackingCourier,
                "refundStatus" to refundStatus,
                "refundAmount" to refundAmount,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "orders/$orderId")
    }

    suspend fun saveStaffMember(staff: StaffMember): Result<Unit> = runCatching {
        requireUserId()
        val uid = staff.staffUid.trim()
        require(uid.isNotBlank()) { "Staff UID is required" }
        val docRef = db.collection("staff").document(uid)
        val snap = docRef.get().await()
        val payload = mutableMapOf<String, Any>(
            "staffUid" to uid,
            "email" to staff.email.trim(),
            "displayName" to staff.displayName.trim(),
            "roleName" to staff.roleName.trim(),
            "canManageProducts" to staff.canManageProducts,
            "canManageOrders" to staff.canManageOrders,
            "canManageCustomers" to staff.canManageCustomers,
            "canManageCoupons" to staff.canManageCoupons,
            "canManageContent" to staff.canManageContent,
            "canViewAnalytics" to staff.canViewAnalytics,
            "isActive" to staff.isActive,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!snap.exists()) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(payload).await()
        }
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "staff")
    }

    suspend fun deleteStaffMember(staffUid: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("staff").document(staffUid).delete().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "staff/$staffUid")
    }

    suspend fun recordAuditLog(
        actorRole: String,
        action: String,
        targetCollection: String,
        targetId: String,
        details: String
    ): Result<Unit> = runCatching {
        val user = auth.currentUser ?: return@runCatching Unit
        val logId = "log_${UUID.randomUUID().toString().take(10)}"
        db.collection("audit_logs").document(logId).set(
            mapOf(
                "actorUid" to user.uid,
                "actorEmail" to (user.email ?: "unknown@dsquare.luxury"),
                "actorRole" to actorRole,
                "action" to action.take(80),
                "targetCollection" to targetCollection.take(60),
                "targetId" to targetId.take(128),
                "details" to details.take(400),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "audit_logs")
    }

    private fun logAndRethrow(error: Throwable, op: OperationType, path: String, uid: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }
}
