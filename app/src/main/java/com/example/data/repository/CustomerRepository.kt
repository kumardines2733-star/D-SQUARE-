package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Address
import com.example.data.model.CartItem
import com.example.data.model.NotificationItem
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class CustomerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    suspend fun ensureUserProfile(): Result<Unit> = runCatching {
        val user = auth.currentUser ?: throw IllegalStateException("Not authenticated")
        val docRef = db.collection("users").document(user.uid)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mapOf(
                "userId" to user.uid,
                "displayName" to (user.displayName?.ifBlank { "D SQUARE Member" } ?: "D SQUARE Member"),
                "email" to (user.email ?: "member@dsquare.luxury"),
                "phone" to (user.phoneNumber ?: ""),
                "preferredPayment" to "UPI",
                "totalOrders" to 0L,
                "totalSpent" to 0.0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
        }
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "users")
    }

    fun observeProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logAndRethrow(error, OperationType.GET, "users/$userId", userId)
            }

    suspend fun updateProfile(displayName: String, phone: String, preferredPayment: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).update(
            mapOf(
                "displayName" to displayName.trim().take(80),
                "phone" to phone.trim().take(24),
                "preferredPayment" to preferredPayment.trim().take(30),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "users")
    }

    fun observeAddresses(userId: String): Flow<List<Address>> =
        db.collection("users").document(userId).collection("addresses")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Address::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.isDefault }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "users/$userId/addresses", userId)
            }

    suspend fun saveAddress(address: Address): Result<String> = runCatching {
        val uid = requireUserId()
        val isNew = address.id.isBlank()
        val docId = if (isNew) "addr_${UUID.randomUUID().toString().take(8)}" else address.id
        val docRef = db.collection("users").document(uid).collection("addresses").document(docId)
        val payload = mutableMapOf<String, Any>(
            "userId" to uid,
            "label" to address.label.trim().ifBlank { "Home" },
            "recipientName" to address.recipientName.trim(),
            "phone" to address.phone.trim(),
            "street" to address.street.trim(),
            "city" to address.city.trim(),
            "state" to address.state.trim(),
            "postalCode" to address.postalCode.trim(),
            "country" to address.country.trim().ifBlank { "India" },
            "isDefault" to address.isDefault,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (isNew) {
            payload["createdAt"] = FieldValue.serverTimestamp()
            docRef.set(payload).await()
        } else {
            docRef.update(payload).await()
        }
        docId
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "users/addresses")
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("addresses").document(addressId).delete().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "users/addresses/$addressId")
    }

    fun observeCart(userId: String): Flow<List<CartItem>> =
        db.collection("users").document(userId).collection("cart")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(CartItem::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "users/$userId/cart", userId)
            }

    suspend fun addToCart(product: Product, variant: String, qty: Long = 1L): Result<Unit> = runCatching {
        val uid = requireUserId()
        val safeVariant = variant.ifBlank { product.variants.firstOrNull() ?: "Standard" }
        val cartId = "${product.id}_${safeVariant.replace(Regex("[^a-zA-Z0-9]"), "")}".take(64)
        val docRef = db.collection("users").document(uid).collection("cart").document(cartId)
        val existing = docRef.get().await()
        if (existing.exists()) {
            val currentQty = existing.getLong("quantity") ?: 1L
            val nextQty = (currentQty + qty).coerceIn(1L, 99L)
            docRef.update(
                mapOf(
                    "quantity" to nextQty,
                    "unitPrice" to product.price,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "productId" to product.id,
                    "productName" to product.name,
                    "categoryName" to product.categoryName,
                    "variant" to safeVariant,
                    "unitPrice" to product.price,
                    "costPrice" to product.costPrice,
                    "quantity" to qty.coerceIn(1L, 99L),
                    "imageKey" to product.imageKey,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "users/cart")
    }

    suspend fun updateCartQuantity(cartItemId: String, newQuantity: Long): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("cart").document(cartItemId)
        if (newQuantity <= 0L) {
            docRef.delete().await()
        } else {
            docRef.update(
                mapOf(
                    "quantity" to newQuantity.coerceIn(1L, 99L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "users/cart/$cartItemId")
    }

    suspend fun clearCart(): Result<Unit> = runCatching {
        val uid = requireUserId()
        val items = db.collection("users").document(uid).collection("cart").get().await()
        val batch = db.batch()
        items.documents.forEach { batch.delete(it.reference) }
        batch.commit().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "users/cart")
    }

    fun observeWishlist(userId: String): Flow<List<WishlistItem>> =
        db.collection("users").document(userId).collection("wishlist")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(WishlistItem::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "users/$userId/wishlist", userId)
            }

    suspend fun toggleWishlist(product: Product, currentlyWishlisted: Boolean): Result<Boolean> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("wishlist").document(product.id)
        if (currentlyWishlisted) {
            docRef.delete().await()
            false
        } else {
            docRef.set(
                mapOf(
                    "userId" to uid,
                    "productId" to product.id,
                    "productName" to product.name,
                    "categoryName" to product.categoryName,
                    "price" to product.price,
                    "imageKey" to product.imageKey,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        }
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "users/wishlist/${product.id}")
    }

    fun observeNotifications(userId: String): Flow<List<NotificationItem>> =
        db.collection("users").document(userId).collection("notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(NotificationItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "users/$userId/notifications", userId)
            }

    suspend fun createNotification(targetUserId: String, title: String, message: String, type: String = "ORDER"): Result<Unit> = runCatching {
        requireUserId()
        val notifId = "ntf_${UUID.randomUUID().toString().take(8)}"
        db.collection("users").document(targetUserId).collection("notifications").document(notifId)
            .set(
                mapOf(
                    "userId" to targetUserId,
                    "title" to title.take(120),
                    "message" to message.take(400),
                    "type" to type,
                    "isRead" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "users/$targetUserId/notifications")
    }

    suspend fun markNotificationRead(notifId: String): Result<Unit> = runCatching {
        val uid = requireUserId()
        db.collection("users").document(uid).collection("notifications").document(notifId)
            .update(
                mapOf(
                    "isRead" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "users/notifications/$notifId")
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
