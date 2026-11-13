package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.Product
import com.example.data.model.StoreSettings
import com.example.data.model.WishlistItem
import com.example.ui.components.DSquareImage
import com.example.ui.components.DSquareProductCard
import com.example.ui.components.formatMoney
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.ObsidianBlack
import com.example.ui.viewmodel.CatalogUiFilter
import com.example.ui.viewmodel.ProductSortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    products: List<Product>,
    categories: List<Category>,
    banners: List<Banner>,
    wishlist: List<WishlistItem>,
    filter: CatalogUiFilter,
    storeSettings: StoreSettings,
    onSearchChange: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onSortChange: (ProductSortOption) -> Unit,
    onFilterApply: (inStockOnly: Boolean, maxPrice: Double?) -> Unit,
    onProductClick: (Product) -> Unit,
    onToggleWishlist: (Product) -> Unit,
    onQuickAddToCart: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    val wishIds = remember(wishlist) { wishlist.map { it.productId }.toSet() }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val columns = when {
            maxWidth >= 900.dp -> 4
            maxWidth >= 600.dp -> 3
            else -> 2
        }
        val rows = remember(products, columns) { products.chunked(columns) }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (storeSettings.announcementBar.isNotBlank()) {
                item(key = "announcement") {
                    Surface(
                        color = ChampagneGold,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = storeSettings.announcementBar,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = ObsidianBlack,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // Editorial Hero Banner
            item(key = "hero") {
                val activeBanner = banners.firstOrNull { it.isActive } ?: Banner(
                    title = "LUXURY REFINED",
                    subtitle = "Autumn/Winter Edit • Curated fashion, precision audio & fine jewellery",
                    badge = "D SQUARE ATELIER",
                    ctaText = "EXPLORE COLLECTION",
                    imageKey = "img_hero_luxury"
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(210.dp)
                        .clip(RoundedCornerShape(20.dp))
                ) {
                    DSquareImage(
                        imageKey = activeBanner.imageKey,
                        contentDescription = activeBanner.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        ObsidianBlack.copy(alpha = 0.92f),
                                        ObsidianBlack.copy(alpha = 0.55f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ChampagneGold.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, ChampagneGold)
                        ) {
                            Text(
                                text = activeBanner.badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = ChampagneGold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth(0.7f)) {
                            Text(
                                text = activeBanner.title,
                                style = MaterialTheme.typography.displaySmall,
                                color = Color.White,
                                maxLines = 2
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = activeBanner.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Button(
                            onClick = {
                                if (activeBanner.targetCategoryId.isNotBlank()) {
                                    onSelectCategory(activeBanner.targetCategoryId)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ChampagneGold,
                                contentColor = ObsidianBlack
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("hero_cta_button")
                        ) {
                            Text(
                                text = activeBanner.ctaText,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Search + Sort/Filter Controls
            item(key = "search_controls") {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = filter.searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search couture, audio, jewellery, SKU...") },
                        leadingIcon = {
                            Icon(Icons.Filled.Search, contentDescription = "Search products")
                        },
                        trailingIcon = {
                            if (filter.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("storefront_search_input")
                    )

                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { showSortMenu = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .testTag("sort_button")
                            ) {
                                Icon(Icons.Outlined.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = filter.sortOption.label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                ProductSortOption.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option.label) },
                                        onClick = {
                                            onSortChange(option)
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedButton(
                            onClick = { showFilterSheet = true },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("filter_button")
                        ) {
                            Icon(Icons.Filled.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (filter.inStockOnly || filter.maxPriceFilter != null) "Filtered" else "Filters")
                        }
                    }
                }
            }

            // Category Pills
            item(key = "categories") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "ALL") {
                        FilterChip(
                            selected = filter.selectedCategoryId == "ALL",
                            onClick = { onSelectCategory("ALL") },
                            label = { Text("All Collections") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneGold,
                                selectedLabelColor = ObsidianBlack
                            ),
                            modifier = Modifier.testTag("category_chip_all")
                        )
                    }
                    items(categories.filter { it.isActive }, key = { it.id }) { cat ->
                        FilterChip(
                            selected = filter.selectedCategoryId == cat.id,
                            onClick = { onSelectCategory(cat.id) },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ChampagneGold,
                                selectedLabelColor = ObsidianBlack
                            ),
                            modifier = Modifier.testTag("category_chip_${cat.id}")
                        )
                    }
                }
            }

            // Product Section Title
            item(key = "catalog_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Curated Collection",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${products.size} pieces",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (products.isEmpty()) {
                item(key = "empty_catalog") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Storefront,
                                contentDescription = null,
                                tint = ChampagneGold,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "No matching luxury pieces",
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Try clearing filters or ask the Store Owner to publish new arrivals from the Admin Console.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(rows) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEach { product ->
                            DSquareProductCard(
                                product = product,
                                currencySymbol = storeSettings.currencySymbol,
                                isWishlisted = wishIds.contains(product.id),
                                onCardClick = { onProductClick(product) },
                                onToggleWishlist = { onToggleWishlist(product) },
                                onQuickAddToCart = { onQuickAddToCart(product) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    if (showFilterSheet) {
        var inStock by remember { mutableStateOf(filter.inStockOnly) }
        var maxPrice by remember { mutableFloatStateOf((filter.maxPriceFilter ?: 40000.0).toFloat()) }
        var enablePriceCap by remember { mutableStateOf(filter.maxPriceFilter != null) }

        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Refine Collection", style = MaterialTheme.typography.headlineMedium)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { inStock = !inStock }
                ) {
                    Checkbox(checked = inStock, onCheckedChange = { inStock = it })
                    Text("Available In Stock Only", style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { enablePriceCap = !enablePriceCap }
                ) {
                    Checkbox(checked = enablePriceCap, onCheckedChange = { enablePriceCap = it })
                    Text(
                        text = if (enablePriceCap) "Max Price: ${formatMoney(maxPrice.toDouble(), storeSettings.currencySymbol)}" else "Set Maximum Price Cap",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (enablePriceCap) {
                    Slider(
                        value = maxPrice,
                        onValueChange = { maxPrice = it },
                        valueRange = 1000f..50000f
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onFilterApply(false, null)
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Reset")
                    }
                    Button(
                        onClick = {
                            onFilterApply(inStock, if (enablePriceCap) maxPrice.toDouble() else null)
                            showFilterSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ChampagneGold,
                            contentColor = ObsidianBlack
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply Filters")
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
