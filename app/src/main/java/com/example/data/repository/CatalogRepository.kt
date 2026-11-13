package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.data.model.StoreSettings
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

class CatalogRepository(
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

    fun observeCategories(): Flow<List<Category>> =
        db.collection("categories")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Category::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "categories")
            }

    fun observeProducts(): Flow<List<Product>> =
        db.collection("products")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Product::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.isFeatured }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "products")
            }

    fun observeBanners(): Flow<List<Banner>> =
        db.collection("banners")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Banner::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "banners")
            }

    fun observeCoupons(): Flow<List<Coupon>> =
        db.collection("coupons")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(Coupon::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "coupons")
            }

    fun observeStoreSettings(): Flow<StoreSettings> =
        db.collection("store_settings").document("main")
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(StoreSettings::class.java, ServerTimestampBehavior.ESTIMATE)
                    ?: StoreSettings(id = "main")
            }
            .catch { error ->
                logAndRethrow(error, OperationType.GET, "store_settings/main")
            }

    fun observeProductReviews(productId: String): Flow<List<ProductReview>> =
        db.collection("products").document(productId).collection("reviews")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(ProductReview::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { error ->
                logAndRethrow(error, OperationType.LIST, "products/$productId/reviews")
            }

    suspend fun saveCategory(category: Category): Result<String> = runCatching {
        requireUserId()
        val isNew = category.id.isBlank()
        val docId = if (isNew) "cat_${UUID.randomUUID().toString().take(8)}" else category.id
        val docRef = db.collection("categories").document(docId)
        val payload = mutableMapOf<String, Any>(
            "name" to category.name.trim(),
            "slug" to (category.slug.ifBlank { category.name.lowercase().replace(Regex("[^a-z0-9]+"), "-") }),
            "description" to category.description.trim(),
            "iconKey" to category.iconKey.ifBlank { "fashion" },
            "sortOrder" to category.sortOrder,
            "isActive" to category.isActive,
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
        handleFirestoreError(it, OperationType.WRITE, "categories")
    }

    suspend fun deleteCategory(categoryId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("categories").document(categoryId).delete().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "categories/$categoryId")
    }

    suspend fun saveProduct(product: Product): Result<String> = runCatching {
        requireUserId()
        val isNew = product.id.isBlank()
        val docId = if (isNew) "prod_${UUID.randomUUID().toString().take(8)}" else product.id
        val docRef = db.collection("products").document(docId)
        val payload = mutableMapOf<String, Any>(
            "name" to product.name.trim(),
            "sku" to product.sku.ifBlank { "DSQ-${docId.takeLast(5).uppercase()}" },
            "categoryId" to product.categoryId,
            "categoryName" to product.categoryName,
            "description" to product.description.trim(),
            "price" to product.price,
            "costPrice" to product.costPrice,
            "compareAtPrice" to product.compareAtPrice,
            "stock" to product.stock,
            "lowStockThreshold" to product.lowStockThreshold,
            "variants" to product.variants.ifEmpty { listOf("Signature") },
            "colors" to product.colors.ifEmpty { listOf("Obsidian") },
            "sizes" to product.sizes.ifEmpty { listOf("Standard") },
            "imageKey" to product.imageKey.ifBlank { "img_prod_fashion" },
            "imageUrls" to product.imageUrls,
            "ratingAvg" to product.ratingAvg,
            "reviewCount" to product.reviewCount,
            "soldCount" to product.soldCount,
            "viewCount" to product.viewCount,
            "isFeatured" to product.isFeatured,
            "isActive" to product.isActive,
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
        handleFirestoreError(it, OperationType.WRITE, "products")
    }

    suspend fun deleteProduct(productId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("products").document(productId).delete().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "products/$productId")
    }

    suspend fun incrementProductView(productId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("products").document(productId)
            .update("viewCount", FieldValue.increment(1L))
            .await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.UPDATE, "products/$productId")
    }

    suspend fun submitReview(
        productId: String,
        rating: Long,
        title: String,
        comment: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val userName = auth.currentUser?.displayName?.ifBlank { "Verified Buyer" } ?: "Verified Buyer"
        val reviewId = "rev_${UUID.randomUUID().toString().take(8)}"
        val reviewRef = db.collection("products").document(productId)
            .collection("reviews").document(reviewId)
        val payload = mapOf(
            "productId" to productId,
            "userId" to uid,
            "userName" to userName,
            "rating" to rating.coerceIn(1L, 5L),
            "title" to title.trim().take(120),
            "comment" to comment.trim().take(1000),
            "isApproved" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        reviewRef.set(payload).await()

        // Recalculate product ratingAvg & reviewCount
        val productSnap = db.collection("products").document(productId).get().await()
        val oldCount = productSnap.getLong("reviewCount") ?: 0L
        val oldAvg = productSnap.getDouble("ratingAvg") ?: 5.0
        val newCount = oldCount + 1L
        val newAvg = ((oldAvg * oldCount) + rating) / newCount.toDouble()
        db.collection("products").document(productId).update(
            mapOf(
                "reviewCount" to newCount,
                "ratingAvg" to (Math.round(newAvg * 10.0) / 10.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        reviewId
    }.onFailure {
        handleFirestoreError(it, OperationType.CREATE, "products/$productId/reviews")
    }

    suspend fun saveBanner(banner: Banner): Result<String> = runCatching {
        requireUserId()
        val isNew = banner.id.isBlank()
        val docId = if (isNew) "ban_${UUID.randomUUID().toString().take(8)}" else banner.id
        val docRef = db.collection("banners").document(docId)
        val payload = mutableMapOf<String, Any>(
            "title" to banner.title.trim(),
            "subtitle" to banner.subtitle.trim(),
            "badge" to banner.badge.trim(),
            "ctaText" to banner.ctaText.trim(),
            "targetCategoryId" to banner.targetCategoryId,
            "imageKey" to banner.imageKey.ifBlank { "img_hero_luxury" },
            "sortOrder" to banner.sortOrder,
            "isActive" to banner.isActive,
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
        handleFirestoreError(it, OperationType.WRITE, "banners")
    }

    suspend fun deleteBanner(bannerId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("banners").document(bannerId).delete().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "banners/$bannerId")
    }

    suspend fun saveCoupon(coupon: Coupon): Result<String> = runCatching {
        requireUserId()
        val isNew = coupon.id.isBlank()
        val docId = if (isNew) "cpn_${UUID.randomUUID().toString().take(8)}" else coupon.id
        val docRef = db.collection("coupons").document(docId)
        val payload = mutableMapOf<String, Any>(
            "code" to coupon.code.trim().uppercase(),
            "description" to coupon.description.trim(),
            "discountType" to coupon.discountType,
            "discountValue" to coupon.discountValue,
            "minOrderAmount" to coupon.minOrderAmount,
            "maxDiscount" to coupon.maxDiscount,
            "usageCount" to coupon.usageCount,
            "usageLimit" to coupon.usageLimit,
            "isActive" to coupon.isActive,
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
        handleFirestoreError(it, OperationType.WRITE, "coupons")
    }

    suspend fun deleteCoupon(couponId: String): Result<Unit> = runCatching {
        requireUserId()
        db.collection("coupons").document(couponId).delete().await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.DELETE, "coupons/$couponId")
    }

    suspend fun saveStoreSettings(settings: StoreSettings): Result<Unit> = runCatching {
        requireUserId()
        val payload = mapOf(
            "storeName" to settings.storeName.trim(),
            "tagline" to settings.tagline.trim(),
            "currencySymbol" to settings.currencySymbol.trim(),
            "defaultShippingFee" to settings.defaultShippingFee,
            "freeShippingThreshold" to settings.freeShippingThreshold,
            "taxRatePercent" to settings.taxRatePercent,
            "monthlyOperatingExpense" to settings.monthlyOperatingExpense,
            "supportEmail" to settings.supportEmail.trim(),
            "supportPhone" to settings.supportPhone.trim(),
            "announcementBar" to settings.announcementBar.trim(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        db.collection("store_settings").document("main").set(payload).await()
        Unit
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "store_settings/main")
    }

    suspend fun seedLuxuryStoreIfEmpty(isOwner: Boolean): Result<Boolean> = runCatching {
        if (!isOwner) return@runCatching false
        val existingCats = db.collection("categories").limit(1).get().await()
        if (!existingCats.isEmpty) return@runCatching false

        val categories = listOf(
            Category("cat_fashion", "Fashion & Clothing", "fashion-clothing", "Tailored outerwear, cashmere, and leather goods", "fashion", 1, true),
            Category("cat_electronics", "Electronics", "electronics", "Flagship acoustic audio and luxury smartwatches", "electronics", 2, true),
            Category("cat_mobile", "Mobile Accessories", "mobile-accessories", "MagSafe aramid cases and leather folios", "mobile", 3, true),
            Category("cat_home", "Home & Kitchen", "home-kitchen", "Artisanal espresso bars and stoneware", "home", 4, true),
            Category("cat_beauty", "Beauty & Personal Care", "beauty-personal-care", "Botanical serums and signature oud fragrances", "beauty", 5, true),
            Category("cat_footwear", "Footwear", "footwear", "Hand-stitched Italian leather sneakers and loafers", "footwear", 6, true),
            Category("cat_jewellery", "Jewellery", "jewellery", "18K champagne gold cuffs and solitaire pieces", "jewellery", 7, true),
            Category("cat_furniture", "Furniture", "furniture", "Sculptural travertine and walnut studio accents", "furniture", 8, true),
            Category("cat_fitness", "Sports & Fitness", "sports-fitness", "Precision weighted gear and studio mats", "fitness", 9, true),
            Category("cat_grocery", "Grocery", "grocery", "Single-origin reserve coffee and artisan pantry", "grocery", 10, true),
            Category("cat_toys", "Toys & Kids Products", "toys-kids", "Heirloom wooden architectural sets", "toys", 11, true)
        )

        val batch = db.batch()
        for (cat in categories) {
            batch.set(
                db.collection("categories").document(cat.id),
                mapOf(
                    "name" to cat.name,
                    "slug" to cat.slug,
                    "description" to cat.description,
                    "iconKey" to cat.iconKey,
                    "sortOrder" to cat.sortOrder,
                    "isActive" to cat.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        val products = listOf(
            Product("prod_tote", "Obsidian Pebbled Tote", "DSQ-FSH-101", "cat_fashion", "Fashion & Clothing",
                "Handcrafted full-grain Italian leather tote with brushed champagne-gold hardware and suede lining.",
                18999.0, 9200.0, 22999.0, 14, 5,
                listOf("Obsidian / Gold", "Ivory / Gold"), listOf("Obsidian", "Ivory"), listOf("One Size"),
                "img_prod_fashion", emptyList(), 4.9, 28, 42, 310, true, true),
            Product("prod_headphones", "Aurum ANC Studio Headphones", "DSQ-ELC-201", "cat_electronics", "Electronics",
                "Anodized matte black aluminum wireless headphones with champagne-gold acoustic rings and 48h battery.",
                24999.0, 12500.0, 29999.0, 9, 5,
                listOf("Obsidian Gold", "Slate Titanium"), listOf("Obsidian Gold", "Titanium"), listOf("Standard"),
                "img_prod_electronics", emptyList(), 4.9, 41, 67, 520, true, true),
            Product("prod_cuff", "18K Sculpted Gold Cuff", "DSQ-JWL-301", "cat_jewellery", "Jewellery",
                "Signature 18K champagne-gold hinged cuff bracelet paired with high-polish geometric facets.",
                32500.0, 17800.0, 36000.0, 6, 4,
                listOf("18K Champagne Gold", "Rose Vermeil"), listOf("Champagne Gold", "Rose Gold"), listOf("S/M", "M/L"),
                "img_prod_jewellery", emptyList(), 5.0, 19, 23, 275, true, true),
            Product("prod_chronograph", "D SQUARE Chrono Apex Watch", "DSQ-ELC-202", "cat_electronics", "Electronics",
                "Sapphire-crystal luxury smartwatch & hybrid chronograph with brushed obsidian link strap.",
                21499.0, 10800.0, 24999.0, 3, 5,
                listOf("44mm Obsidian", "40mm Champagne"), listOf("Obsidian", "Champagne"), listOf("44mm", "40mm"),
                "img_prod_electronics", emptyList(), 4.8, 34, 51, 440, true, true),
            Product("prod_pendant", "Hexagon Solitaire Pendant", "DSQ-JWL-302", "cat_jewellery", "Jewellery",
                "Architectural hexagonal bezel solitaire suspended on a diamond-cut 18K gold box chain.",
                14999.0, 7400.0, 17500.0, 11, 4,
                listOf("18K Gold / 18in", "18K Gold / 20in"), listOf("18K Gold"), listOf("18 in", "20 in"),
                "img_prod_jewellery", emptyList(), 4.9, 15, 30, 198, false, true),
            Product("prod_cashmere", "Ivory Cashmere Trench Wrap", "DSQ-FSH-102", "cat_fashion", "Fashion & Clothing",
                "Ultra-fine 100% Mongolian cashmere wrap scarf in warm alabaster ivory with hand-knotted fringe.",
                8499.0, 3600.0, 10999.0, 22, 6,
                listOf("Alabaster Ivory", "Onyx Black"), listOf("Ivory", "Onyx"), listOf("Free Size"),
                "img_prod_fashion", emptyList(), 4.8, 22, 38, 230, false, true),
            Product("prod_case", "Aramid Gold MagSafe Case", "DSQ-MOB-401", "cat_mobile", "Mobile Accessories",
                "Aerospace 600D aramid fiber protective case accented with a milled gold camera bezel.",
                3499.0, 1100.0, 4299.0, 40, 10,
                listOf("iPhone 16 Pro", "Galaxy S25 Ultra"), listOf("Carbon Black"), listOf("Pro", "Ultra"),
                "img_prod_electronics", emptyList(), 4.7, 56, 112, 610, false, true),
            Product("prod_oud", "Noir Reserve Oud Elixir 100ml", "DSQ-BTY-501", "cat_beauty", "Beauty & Personal Care",
                "Smoky saffron, roasted cardamom, and aged Cambodian oud bottled in weighted smoked crystal.",
                9999.0, 3400.0, 11999.0, 16, 5,
                listOf("100ml Extrait", "50ml Extrait"), listOf("Amber Noir"), listOf("100ml", "50ml"),
                "img_prod_fashion", emptyList(), 4.9, 31, 45, 290, true, true)
        )

        for (p in products) {
            batch.set(
                db.collection("products").document(p.id),
                mapOf(
                    "name" to p.name,
                    "sku" to p.sku,
                    "categoryId" to p.categoryId,
                    "categoryName" to p.categoryName,
                    "description" to p.description,
                    "price" to p.price,
                    "costPrice" to p.costPrice,
                    "compareAtPrice" to p.compareAtPrice,
                    "stock" to p.stock,
                    "lowStockThreshold" to p.lowStockThreshold,
                    "variants" to p.variants,
                    "colors" to p.colors,
                    "sizes" to p.sizes,
                    "imageKey" to p.imageKey,
                    "imageUrls" to p.imageUrls,
                    "ratingAvg" to p.ratingAvg,
                    "reviewCount" to p.reviewCount,
                    "soldCount" to p.soldCount,
                    "viewCount" to p.viewCount,
                    "isFeatured" to p.isFeatured,
                    "isActive" to p.isActive,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }

        batch.set(
            db.collection("banners").document("ban_flagship"),
            mapOf(
                "title" to "LUXURY REFINED",
                "subtitle" to "The Autumn/Winter Flagship Edit • Handcrafted accessories & precision tech",
                "badge" to "D SQUARE ATELIER",
                "ctaText" to "EXPLORE EDIT",
                "targetCategoryId" to "cat_fashion",
                "imageKey" to "img_hero_luxury",
                "sortOrder" to 1L,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.set(
            db.collection("coupons").document("cpn_dsquare15"),
            mapOf(
                "code" to "DSQUARE15",
                "description" to "15% Privilege Savings on orders above ₹5,000",
                "discountType" to "PERCENT",
                "discountValue" to 15.0,
                "minOrderAmount" to 5000.0,
                "maxDiscount" to 4000.0,
                "usageCount" to 14L,
                "usageLimit" to 500L,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.set(
            db.collection("coupons").document("cpn_gold1000"),
            mapOf(
                "code" to "GOLD1000",
                "description" to "Flat ₹1,000 off orders above ₹10,000",
                "discountType" to "FLAT",
                "discountValue" to 1000.0,
                "minOrderAmount" to 10000.0,
                "maxDiscount" to 1000.0,
                "usageCount" to 6L,
                "usageLimit" to 250L,
                "isActive" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        val defaultSettings = StoreSettings()
        batch.set(
            db.collection("store_settings").document("main"),
            mapOf(
                "storeName" to defaultSettings.storeName,
                "tagline" to defaultSettings.tagline,
                "currencySymbol" to defaultSettings.currencySymbol,
                "defaultShippingFee" to defaultSettings.defaultShippingFee,
                "freeShippingThreshold" to defaultSettings.freeShippingThreshold,
                "taxRatePercent" to defaultSettings.taxRatePercent,
                "monthlyOperatingExpense" to defaultSettings.monthlyOperatingExpense,
                "supportEmail" to defaultSettings.supportEmail,
                "supportPhone" to defaultSettings.supportPhone,
                "announcementBar" to defaultSettings.announcementBar,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )

        batch.commit().await()
        true
    }.onFailure {
        handleFirestoreError(it, OperationType.WRITE, "seed_catalog")
    }

    private fun logAndRethrow(error: Throwable, op: OperationType, path: String): Nothing {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser != null) {
            handleFirestoreError(exceptionToLog, op, path)
        }
        throw error
    }
}
