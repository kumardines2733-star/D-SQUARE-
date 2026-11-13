package com.example.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Product
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RubyDanger
import com.example.ui.theme.WarmGoldHighlight
import java.text.NumberFormat
import java.util.Locale

@DrawableRes
fun productImageRes(imageKey: String): Int {
    return when {
        imageKey.contains("electronics", ignoreCase = true) -> R.drawable.img_prod_electronics
        imageKey.contains("jewellery", ignoreCase = true) -> R.drawable.img_prod_jewellery
        imageKey.contains("hero", ignoreCase = true) -> R.drawable.img_hero_luxury
        else -> R.drawable.img_prod_fashion
    }
}

fun formatMoney(amount: Double, symbol: String = "₹"): String {
    val formatter = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
        maximumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    }
    return "$symbol${formatter.format(amount)}"
}

@Composable
fun DSquareImage(
    imageKey: String,
    imageUrl: String? = null,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (!imageUrl.isNullOrBlank() && imageUrl.startsWith("http")) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            placeholder = painterResource(id = productImageRes(imageKey)),
            error = painterResource(id = productImageRes(imageKey))
        )
    } else {
        Image(
            painter = painterResource(id = productImageRes(imageKey)),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    }
}

@Composable
fun DSquareProductCard(
    product: Product,
    currencySymbol: String,
    isWishlisted: Boolean,
    onCardClick: () -> Unit,
    onToggleWishlist: () -> Unit,
    onQuickAddToCart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_card_${product.id}")
            .clickable(onClick = onCardClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.15f)
            ) {
                DSquareImage(
                    imageKey = product.imageKey,
                    imageUrl = product.imageUrls.firstOrNull(),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f))
                            )
                        )
                )

                // Featured or Low Stock Badge
                val badgeText = when {
                    product.stock <= 0L -> "SOLD OUT"
                    product.stock <= product.lowStockThreshold -> "ONLY ${product.stock} LEFT"
                    product.isFeatured -> "ATELIER EDIT"
                    else -> null
                }
                if (badgeText != null) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = if (product.stock <= 0L) RubyDanger else ObsidianBlack.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.7f))
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (product.stock <= 0L) Color.White else ChampagneGold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleWishlist,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .minimumInteractiveComponentSize()
                        .testTag("wishlist_toggle_${product.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = ObsidianBlack.copy(alpha = 0.65f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (isWishlisted) "Remove from wishlist" else "Save to wishlist",
                                tint = if (isWishlisted) ChampagneGold else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.categoryName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Rating",
                        tint = ChampagneGold,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${product.ratingAvg} (${product.reviewCount})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = formatMoney(product.price, currencySymbol),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (product.compareAtPrice > product.price) {
                            Text(
                                text = formatMoney(product.compareAtPrice, currencySymbol),
                                style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (product.stock > 0L) ChampagneGold else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = product.stock > 0L, onClick = onQuickAddToCart)
                            .testTag("quick_add_${product.id}")
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddShoppingCart,
                                contentDescription = "Add to bag",
                                tint = ObsidianBlack,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DSquareCrestBadge(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(ObsidianBlack)
            .border(1.5.dp, ChampagneGold, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "D²",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = ChampagneGold
        )
    }
}

@Composable
fun RevenueProfitBarChart(
    revenueSeries: List<Pair<String, Double>>,
    profitSeries: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val maxVal = (revenueSeries.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
        ) {
            val w = size.width
            val h = size.height
            val count = revenueSeries.size.coerceAtLeast(1)
            val groupWidth = w / count
            val barWidth = (groupWidth * 0.28f).coerceAtMost(28.dp.toPx())

            // Horizontal reference lines
            for (i in 0..3) {
                val y = h * (i / 3f)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            revenueSeries.forEachIndexed { idx, (_, rev) ->
                val prof = profitSeries.getOrNull(idx)?.second ?: 0.0
                val cx = groupWidth * idx + groupWidth / 2f
                val revHeight = ((rev / maxVal).toFloat() * (h - 12.dp.toPx())).coerceAtLeast(4.dp.toPx())
                val profHeight = ((prof / maxVal).toFloat() * (h - 12.dp.toPx())).coerceAtLeast(4.dp.toPx())

                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(WarmGoldHighlight, ChampagneGold)),
                    topLeft = Offset(cx - barWidth - 3.dp.toPx(), h - revHeight),
                    size = Size(barWidth, revHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = EmeraldProfit,
                    topLeft = Offset(cx + 3.dp.toPx(), h - profHeight),
                    size = Size(barWidth, profHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            revenueSeries.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OrderPipelineRingChart(
    pending: Int,
    delivered: Int,
    cancelled: Int,
    modifier: Modifier = Modifier
) {
    val total = (pending + delivered + cancelled).coerceAtLeast(1)
    val trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    Canvas(modifier = modifier.size(110.dp)) {
        val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = stroke
        )
        var start = -90f
        val deliveredSweep = (delivered.toFloat() / total) * 360f
        val pendingSweep = (pending.toFloat() / total) * 360f
        val cancelledSweep = (cancelled.toFloat() / total) * 360f

        if (deliveredSweep > 0f) {
            drawArc(EmeraldProfit, start, deliveredSweep, false, style = stroke)
            start += deliveredSweep
        }
        if (pendingSweep > 0f) {
            drawArc(ChampagneGold, start, pendingSweep, false, style = stroke)
            start += pendingSweep
        }
        if (cancelledSweep > 0f) {
            drawArc(RubyDanger, start, cancelledSweep, false, style = stroke)
        }
    }
}
