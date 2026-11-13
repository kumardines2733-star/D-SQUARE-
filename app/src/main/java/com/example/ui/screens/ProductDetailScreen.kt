package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Product
import com.example.data.model.ProductReview
import com.example.ui.components.DSquareImage
import com.example.ui.components.formatMoney
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RubyDanger
import kotlinx.coroutines.flow.Flow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    currencySymbol: String,
    isWishlisted: Boolean,
    reviewsFlow: Flow<List<ProductReview>>,
    onRecordView: () -> Unit,
    onBack: () -> Unit,
    onToggleWishlist: () -> Unit,
    onAddToCart: (variant: String, qty: Long) -> Unit,
    onBuyNow: (variant: String, qty: Long) -> Unit,
    onSubmitReview: (rating: Long, title: String, comment: String) -> Unit
) {
    BackHandler(onBack = onBack)

    LaunchedEffect(product.id) {
        onRecordView()
    }

    val reviews by reviewsFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val allVariants = remember(product) {
        product.variants.ifEmpty { listOf("Signature Edition") }
    }
    var selectedVariant by remember(product.id) { mutableStateOf(allVariants.first()) }
    var quantity by remember(product.id) { mutableLongStateOf(1L) }
    var showReviewDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.categoryName, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("product_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onToggleWishlist, modifier = Modifier.testTag("detail_wishlist_button")) {
                        Icon(
                            imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = ChampagneGold
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "gallery") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(290.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    DSquareImage(
                        imageKey = product.imageKey,
                        imageUrl = product.imageUrls.firstOrNull(),
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize()
                    )
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        color = ObsidianBlack.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = "SKU: ${product.sku}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            item(key = "header") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = product.categoryName.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = ChampagneGold
                        )
                        Text(
                            text = if (product.stock > 0L) "${product.stock} in stock" else "Out of Stock",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (product.stock > 0L) EmeraldProfit else RubyDanger
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatMoney(product.price, currencySymbol),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        if (product.compareAtPrice > product.price) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = formatMoney(product.compareAtPrice, currencySymbol),
                                style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Filled.Star, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "${product.ratingAvg} (${product.reviewCount} reviews)",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item(key = "variants") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Select Variant", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        allVariants.forEach { variant ->
                            FilterChip(
                                selected = selectedVariant == variant,
                                onClick = { selectedVariant = variant },
                                label = { Text(variant) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ChampagneGold,
                                    selectedLabelColor = ObsidianBlack
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Quantity", style = MaterialTheme.typography.titleMedium)
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { if (quantity > 1L) quantity-- },
                                    modifier = Modifier.testTag("detail_qty_minus")
                                ) {
                                    Icon(Icons.Filled.Remove, contentDescription = "Decrease quantity")
                                }
                                Text(
                                    text = quantity.toString(),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                )
                                IconButton(
                                    onClick = { if (quantity < product.stock.coerceAtLeast(1L)) quantity++ },
                                    modifier = Modifier.testTag("detail_qty_plus")
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = "Increase quantity")
                                }
                            }
                        }
                    }
                }
            }

            item(key = "actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddToCart(selectedVariant, quantity) },
                        enabled = product.stock > 0L,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("detail_add_to_cart")
                    ) {
                        Icon(Icons.Filled.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Bag")
                    }
                    Button(
                        onClick = { onBuyNow(selectedVariant, quantity) },
                        enabled = product.stock > 0L,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampagneGold,
                            contentColor = ObsidianBlack
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("detail_buy_now")
                    ) {
                        Text("Buy Now", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            item(key = "description") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Atelier Craftsmanship & Details", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = product.description.ifBlank { "Curated luxury product exclusively stocked by D SQUARE." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item(key = "reviews_header") {
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Client Reviews (${reviews.size})", style = MaterialTheme.typography.titleLarge)
                    TextButton(
                        onClick = { showReviewDialog = true },
                        modifier = Modifier.testTag("write_review_button")
                    ) {
                        Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Write Review")
                    }
                }
            }

            if (reviews.isEmpty()) {
                item(key = "no_reviews") {
                    Text(
                        text = "Be the first D SQUARE client to review this piece.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(reviews, key = { it.id }) { rev ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(rev.title, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Star, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("${rev.rating}/5", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(rev.comment, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "By ${rev.userName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showReviewDialog) {
        var rating by remember { mutableLongStateOf(5L) }
        var title by remember { mutableStateOf("") }
        var comment by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReviewDialog = false },
            title = { Text("Write a Product Review") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Rating: ", style = MaterialTheme.typography.bodyMedium)
                        (1L..5L).forEach { star ->
                            IconButton(onClick = { rating = star }, modifier = Modifier.size(36.dp)) {
                                Icon(
                                    Icons.Filled.Star,
                                    contentDescription = "$star stars",
                                    tint = if (star <= rating) ChampagneGold else MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Headline") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = { Text("Your experience") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSubmitReview(rating, title, comment)
                        showReviewDialog = false
                    }
                ) { Text("Submit") }
            },
            dismissButton = {
                TextButton(onClick = { showReviewDialog = false }) { Text("Cancel") }
            }
        )
    }
}
