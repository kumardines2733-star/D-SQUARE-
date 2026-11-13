package com.example.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.R
import com.example.data.model.Product
import com.example.data.repository.CatalogRepository
import com.example.data.repository.CustomerRepository
import com.example.data.repository.OrderRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.DSquareCrestBadge
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AnalyticsDashboardScreen
import com.example.ui.screens.CartCheckoutScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.OrdersAccountScreen
import com.example.ui.screens.ProductDetailScreen
import com.example.ui.theme.ChampagneGold
import com.example.ui.viewmodel.AdminViewModel
import com.example.ui.viewmodel.StoreViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

enum class DSquareDestination(val label: String, val icon: ImageVector) {
    STORE("Store", Icons.Filled.Storefront),
    BAG("Bag", Icons.Filled.ShoppingBag),
    ACCOUNT("Account", Icons.Filled.Person),
    ANALYTICS("Analytics", Icons.Filled.Analytics),
    ADMIN("Admin", Icons.Filled.AdminPanelSettings)
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun DSquareAppNavigation(
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val auth = Firebase.auth
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            DSquareAuthenticatedShell(
                currentUserId = user.uid,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DSquareAuthenticatedShell(
    currentUserId: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val storeViewModel: StoreViewModel = viewModel(
        key = "store_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                StoreViewModel(
                    catalogRepo = CatalogRepository(db),
                    customerRepo = CustomerRepository(db),
                    orderRepo = OrderRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val adminViewModel: AdminViewModel = viewModel(
        key = "admin_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val db = FirebaseFirestore.getInstance(app.getString(R.string.firestore_database_id))
                AdminViewModel(
                    catalogRepo = CatalogRepository(db),
                    customerRepo = CustomerRepository(db),
                    orderRepo = OrderRepository(db),
                    currentUserId = currentUserId
                )
            }
        }
    )

    val accessControl by storeViewModel.accessControl.collectAsStateWithLifecycle()
    val categories by storeViewModel.categories.collectAsStateWithLifecycle()
    val allProducts by storeViewModel.allProducts.collectAsStateWithLifecycle()
    val filteredProducts by storeViewModel.filteredProducts.collectAsStateWithLifecycle()
    val banners by storeViewModel.banners.collectAsStateWithLifecycle()
    val coupons by storeViewModel.coupons.collectAsStateWithLifecycle()
    val storeSettings by storeViewModel.storeSettings.collectAsStateWithLifecycle()
    val filter by storeViewModel.filter.collectAsStateWithLifecycle()
    val profile by storeViewModel.userProfile.collectAsStateWithLifecycle()
    val addresses by storeViewModel.addresses.collectAsStateWithLifecycle()
    val cartItems by storeViewModel.cartItems.collectAsStateWithLifecycle()
    val wishlist by storeViewModel.wishlist.collectAsStateWithLifecycle()
    val notifications by storeViewModel.notifications.collectAsStateWithLifecycle()
    val customerOrders by storeViewModel.customerOrders.collectAsStateWithLifecycle()
    val appliedCoupon by storeViewModel.appliedCoupon.collectAsStateWithLifecycle()
    val isBusy by storeViewModel.isBusy.collectAsStateWithLifecycle()
    val storeMsg by storeViewModel.statusMessage.collectAsStateWithLifecycle()

    val adminOrders by adminViewModel.allOrders.collectAsStateWithLifecycle()
    val adminCustomers by adminViewModel.allCustomers.collectAsStateWithLifecycle()
    val staffMembers by adminViewModel.staffMembers.collectAsStateWithLifecycle()
    val auditLogs by adminViewModel.auditLogs.collectAsStateWithLifecycle()
    val analytics by adminViewModel.analytics.collectAsStateWithLifecycle()
    val selectedRange by adminViewModel.dateRange.collectAsStateWithLifecycle()
    val customDays by adminViewModel.customDays.collectAsStateWithLifecycle()
    val adminMsg by adminViewModel.adminMessage.collectAsStateWithLifecycle()

    var currentDest by remember { mutableStateOf(DSquareDestination.STORE) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(storeMsg) {
        if (storeMsg != null) {
            snackbarHostState.showSnackbar(storeMsg!!)
            storeViewModel.clearMessage()
        }
    }
    LaunchedEffect(adminMsg) {
        if (adminMsg != null) {
            snackbarHostState.showSnackbar(adminMsg!!)
            adminViewModel.clearMessage()
        }
    }

    val destinations = remember(accessControl.canAccessAdmin) {
        if (accessControl.canAccessAdmin) {
            DSquareDestination.entries
        } else {
            listOf(
                DSquareDestination.STORE,
                DSquareDestination.BAG,
                DSquareDestination.ACCOUNT,
                DSquareDestination.ADMIN
            )
        }
    }

    val cartBadgeCount = cartItems.sumOf { it.quantity }.toInt()

    // Full-screen Product Detail modal/route when a product is selected
    val activeDetail = selectedProduct?.let { sel ->
        allProducts.firstOrNull { it.id == sel.id } ?: sel
    }
    if (activeDetail != null) {
        ProductDetailScreen(
            product = activeDetail,
            currencySymbol = storeSettings.currencySymbol,
            isWishlisted = wishlist.any { it.productId == activeDetail.id },
            reviewsFlow = storeViewModel.observeProductReviews(activeDetail.id),
            onRecordView = { storeViewModel.recordProductView(activeDetail.id) },
            onBack = { selectedProduct = null },
            onToggleWishlist = { storeViewModel.toggleWishlist(activeDetail) },
            onAddToCart = { variant, qty -> storeViewModel.addToCart(activeDetail, variant, qty) },
            onBuyNow = { variant, qty ->
                storeViewModel.addToCart(activeDetail, variant, qty)
                selectedProduct = null
                currentDest = DSquareDestination.BAG
            },
            onSubmitReview = { rating, title, comment ->
                storeViewModel.submitReview(activeDetail.id, rating, title, comment)
            }
        )
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 720.dp

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DSquareCrestBadge()
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = storeSettings.storeName,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "Switch Theme",
                                tint = ChampagneGold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        destinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = {
                                    if (dest == DSquareDestination.BAG && cartBadgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$cartBadgeCount") } }) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail {
                        destinations.forEach { dest ->
                            NavigationRailItem(
                                selected = currentDest == dest,
                                onClick = { currentDest = dest },
                                icon = {
                                    if (dest == DSquareDestination.BAG && cartBadgeCount > 0) {
                                        BadgedBox(badge = { Badge { Text("$cartBadgeCount") } }) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    } else {
                                        Icon(dest.icon, contentDescription = dest.label)
                                    }
                                },
                                label = { Text(dest.label) }
                            )
                        }
                    }
                }

                val contentModifier = Modifier.weight(1f)
                when (currentDest) {
                    DSquareDestination.STORE -> HomeScreen(
                        products = filteredProducts,
                        categories = categories,
                        banners = banners,
                        wishlist = wishlist,
                        filter = filter,
                        storeSettings = storeSettings,
                        onSearchChange = storeViewModel::updateSearch,
                        onSelectCategory = storeViewModel::selectCategory,
                        onSortChange = storeViewModel::setSortOption,
                        onFilterApply = { inStock, maxPrice ->
                            storeViewModel.setInStockOnly(inStock)
                            storeViewModel.setMaxPrice(maxPrice)
                        },
                        onProductClick = { selectedProduct = it },
                        onToggleWishlist = storeViewModel::toggleWishlist,
                        onQuickAddToCart = { p ->
                            storeViewModel.addToCart(p, p.variants.firstOrNull() ?: "Signature", 1L)
                        },
                        modifier = contentModifier
                    )

                    DSquareDestination.BAG -> {
                        val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
                        val discount = storeViewModel.calculateDiscount(subtotal, appliedCoupon)
                        CartCheckoutScreen(
                            cartItems = cartItems,
                            addresses = addresses,
                            coupons = coupons,
                            appliedCoupon = appliedCoupon,
                            discountAmount = discount,
                            storeSettings = storeSettings,
                            isBusy = isBusy,
                            onUpdateQuantity = storeViewModel::updateCartQuantity,
                            onApplyCoupon = storeViewModel::applyCouponCode,
                            onRemoveCoupon = storeViewModel::removeCoupon,
                            onSaveAddress = storeViewModel::saveAddress,
                            onPlaceOrder = storeViewModel::placeOrder,
                            onNavigateToOrders = { currentDest = DSquareDestination.ACCOUNT },
                            modifier = contentModifier
                        )
                    }

                    DSquareDestination.ACCOUNT -> OrdersAccountScreen(
                        profile = profile,
                        accessControl = accessControl,
                        orders = customerOrders,
                        wishlist = wishlist,
                        products = allProducts,
                        addresses = addresses,
                        notifications = notifications,
                        currencySymbol = storeSettings.currencySymbol,
                        onUpdateProfile = storeViewModel::updateProfile,
                        onSaveAddress = storeViewModel::saveAddress,
                        onDeleteAddress = storeViewModel::deleteAddress,
                        onCancelOrder = storeViewModel::cancelOrder,
                        onRequestReturn = storeViewModel::requestReturn,
                        onMoveWishlistToCart = { p ->
                            storeViewModel.addToCart(p, p.variants.firstOrNull() ?: "Signature", 1L)
                        },
                        onRemoveWishlist = storeViewModel::toggleWishlist,
                        onMarkNotificationRead = storeViewModel::markNotificationRead,
                        onSignOut = {
                            signOutUser(credentialManager, scope)
                        },
                        modifier = contentModifier
                    )

                    DSquareDestination.ANALYTICS -> AnalyticsDashboardScreen(
                        accessControl = accessControl,
                        analytics = analytics,
                        selectedRange = selectedRange,
                        customDays = customDays,
                        storeSettings = storeSettings,
                        onSelectDateRange = adminViewModel::selectDateRange,
                        onSetCustomDays = adminViewModel::setCustomDays,
                        modifier = contentModifier
                    )

                    DSquareDestination.ADMIN -> AdminDashboardScreen(
                        accessControl = accessControl,
                        products = allProducts,
                        categories = categories,
                        orders = adminOrders,
                        coupons = coupons,
                        banners = banners,
                        customers = adminCustomers,
                        staffMembers = staffMembers,
                        auditLogs = auditLogs,
                        storeSettings = storeSettings,
                        adminViewModel = adminViewModel,
                        modifier = contentModifier
                    )
                }
            }
        }
    }
}
