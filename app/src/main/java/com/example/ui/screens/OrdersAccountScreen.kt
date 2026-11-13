package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccessControlState
import com.example.data.model.Address
import com.example.data.model.NotificationItem
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.UserProfile
import com.example.data.model.WishlistItem
import com.example.ui.components.DSquareImage
import com.example.ui.components.formatMoney
import com.example.ui.theme.AmberPending
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RubyDanger

@Composable
fun OrdersAccountScreen(
    profile: UserProfile?,
    accessControl: AccessControlState,
    orders: List<Order>,
    wishlist: List<WishlistItem>,
    products: List<Product>,
    addresses: List<Address>,
    notifications: List<NotificationItem>,
    currencySymbol: String,
    onUpdateProfile: (String, String, String) -> Unit,
    onSaveAddress: (Address) -> Unit,
    onDeleteAddress: (String) -> Unit,
    onCancelOrder: (String, String) -> Unit,
    onRequestReturn: (String, String) -> Unit,
    onMoveWishlistToCart: (Product) -> Unit,
    onRemoveWishlist: (Product) -> Unit,
    onMarkNotificationRead: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Orders (${orders.size})", "Wishlist (${wishlist.size})", "Addresses", "Inbox (${notifications.count { !it.isRead }})")

    var invoiceOrder by remember { mutableStateOf<Order?>(null) }
    var actionOrder by remember { mutableStateOf<Pair<Order, Boolean>?>(null) } // true = cancel, false = return
    var showEditProfile by remember { mutableStateOf(false) }
    var showAddAddress by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // Luxury Profile Header
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = ChampagneGold.copy(alpha = 0.2f),
                    border = BorderStroke(1.5.dp, ChampagneGold),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(28.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.displayName ?: "D SQUARE Client",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = profile?.email ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = ChampagneGold.copy(alpha = 0.16f)
                    ) {
                        Text(
                            text = if (accessControl.isOwner) "OWNER SUPER-ADMIN" else accessControl.staffMember?.roleName?.uppercase() ?: "PRIVILEGE MEMBER",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChampagneGold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                IconButton(onClick = { showEditProfile = true }, modifier = Modifier.testTag("edit_profile_button")) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit profile")
                }
                IconButton(onClick = onSignOut, modifier = Modifier.testTag("sign_out_button")) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sign out", tint = RubyDanger)
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            tabs.forEachIndexed { idx, title ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        when (selectedTab) {
            0 -> {
                if (orders.isEmpty()) {
                    EmptyAccountPlaceholder("No Orders Yet", "Your placed D SQUARE orders, live tracking, and tax invoices will appear here.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(orders, key = { it.id }) { order ->
                            OrderTrackingCard(
                                order = order,
                                currencySymbol = currencySymbol,
                                onViewInvoice = { invoiceOrder = order },
                                onCancel = { actionOrder = order to true },
                                onReturn = { actionOrder = order to false }
                            )
                        }
                    }
                }
            }

            1 -> {
                if (wishlist.isEmpty()) {
                    EmptyAccountPlaceholder("Your Wishlist Is Empty", "Tap the heart icon on any luxury piece to save it here.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(wishlist, key = { it.id }) { item ->
                            val fullProd = products.firstOrNull { it.id == item.productId } ?: Product(
                                id = item.productId,
                                name = item.productName,
                                categoryName = item.categoryName,
                                price = item.price,
                                stock = 10L,
                                imageKey = item.imageKey
                            )
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DSquareImage(
                                        imageKey = item.imageKey,
                                        contentDescription = item.productName,
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.productName, style = MaterialTheme.typography.titleMedium)
                                        Text(formatMoney(item.price, currencySymbol), color = ChampagneGold, style = MaterialTheme.typography.labelLarge)
                                    }
                                    IconButton(onClick = { onMoveWishlistToCart(fullProd) }) {
                                        Icon(Icons.Filled.AddShoppingCart, contentDescription = "Move to bag", tint = ChampagneGold)
                                    }
                                    IconButton(onClick = { onRemoveWishlist(fullProd) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Button(
                            onClick = { showAddAddress = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("+ Add New Shipping Address")
                        }
                    }
                    items(addresses, key = { it.id }) { addr ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${addr.label} • ${addr.recipientName}", style = MaterialTheme.typography.titleMedium)
                                    Text("${addr.street}, ${addr.city}, ${addr.state} ${addr.postalCode}", style = MaterialTheme.typography.bodySmall)
                                    Text("Ph: ${addr.phone}", style = MaterialTheme.typography.labelSmall, color = ChampagneGold)
                                }
                                IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                    Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete address", tint = RubyDanger)
                                }
                            }
                        }
                    }
                }
            }

            3 -> {
                if (notifications.isEmpty()) {
                    EmptyAccountPlaceholder("No Notifications", "Order updates, tracking alerts, and private offers appear here.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = if (!notif.isRead) BorderStroke(1.dp, ChampagneGold) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onMarkNotificationRead(notif.id) }
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Filled.Notifications,
                                        contentDescription = null,
                                        tint = if (notif.isRead) MaterialTheme.colorScheme.onSurfaceVariant else ChampagneGold
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(notif.title, style = MaterialTheme.typography.titleSmall)
                                        Spacer(Modifier.height(2.dp))
                                        Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    invoiceOrder?.let { order ->
        InvoiceDialog(order = order, currencySymbol = currencySymbol, onDismiss = { invoiceOrder = null })
    }

    actionOrder?.let { (order, isCancel) ->
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { actionOrder = null },
            title = { Text(if (isCancel) "Cancel Order ${order.orderNumber}" else "Request Return & Refund") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text(if (isCancel) "Cancellation reason" else "Return reason") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isCancel) onCancelOrder(order.id, reason) else onRequestReturn(order.id, reason)
                        actionOrder = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RubyDanger, contentColor = Color.White)
                ) {
                    Text(if (isCancel) "Confirm Cancel" else "Submit Return")
                }
            },
            dismissButton = {
                TextButton(onClick = { actionOrder = null }) { Text("Back") }
            }
        )
    }

    if (showEditProfile) {
        var name by remember { mutableStateOf(profile?.displayName ?: "") }
        var phone by remember { mutableStateOf(profile?.phone ?: "") }
        var pay by remember { mutableStateOf(profile?.preferredPayment ?: "UPI") }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Update Profile") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Phone") }, singleLine = true)
                    OutlinedTextField(value = pay, onValueChange = { pay = it }, label = { Text("Preferred Payment (UPI/CARD/COD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(name, phone, pay)
                        showEditProfile = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text("Cancel") }
            }
        )
    }

    if (showAddAddress) {
        AddAddressDialog(
            onDismiss = { showAddAddress = false },
            onSave = {
                onSaveAddress(it)
                showAddAddress = false
            }
        )
    }
}

@Composable
private fun OrderTrackingCard(
    order: Order,
    currencySymbol: String,
    onViewInvoice: () -> Unit,
    onCancel: () -> Unit,
    onReturn: () -> Unit
) {
    val steps = listOf("PLACED", "CONFIRMED", "PACKED", "SHIPPED", "DELIVERED")
    val currentIdx = steps.indexOf(order.orderStatus).let {
        if (order.orderStatus == "OUT_FOR_DELIVERY") 3 else it
    }
    val statusColor = when (order.orderStatus) {
        "DELIVERED" -> EmeraldProfit
        "CANCELLED", "REFUNDED", "RETURN_REQUESTED" -> RubyDanger
        else -> ChampagneGold
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.orderNumber, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Surface(shape = RoundedCornerShape(6.dp), color = statusColor.copy(alpha = 0.18f)) {
                    Text(
                        text = order.orderStatus.replace("_", " "),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Payment: ${order.paymentMethod} (${order.paymentStatus})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatMoney(order.totalAmount, currencySymbol), style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
            }

            if (order.trackingNumber.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${order.trackingCourier} • ${order.trackingNumber}", style = MaterialTheme.typography.labelMedium)
                }
            }

            if (currentIdx >= 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    steps.forEachIndexed { idx, step ->
                        val active = idx <= currentIdx
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (active) EmeraldProfit else MaterialTheme.colorScheme.outline),
                                contentAlignment = Alignment.Center
                            ) {
                                if (active) {
                                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = step.take(4),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onViewInvoice, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Invoice")
                }
                if (order.orderStatus in listOf("PLACED", "CONFIRMED")) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = RubyDanger)
                    }
                } else if (order.orderStatus == "DELIVERED") {
                    OutlinedButton(onClick = onReturn, modifier = Modifier.weight(1f)) {
                        Text("Return", color = AmberPending)
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceDialog(order: Order, currencySymbol: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("D SQUARE Tax Invoice") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Invoice #: INV-${order.orderNumber}", style = MaterialTheme.typography.labelLarge, color = ChampagneGold)
                Text("Customer: ${order.customerName}")
                Text("Email: ${order.customerEmail}", style = MaterialTheme.typography.bodySmall)
                Text("Ship To: ${order.shippingAddress}", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Items: ${order.itemsSummary}", style = MaterialTheme.typography.bodyMedium)
                HorizontalDivider()
                Text("Subtotal: ${formatMoney(order.subtotal, currencySymbol)}", style = MaterialTheme.typography.bodySmall)
                if (order.discountAmount > 0) {
                    Text("Discount (${order.couponCode}): -${formatMoney(order.discountAmount, currencySymbol)}", style = MaterialTheme.typography.bodySmall, color = EmeraldProfit)
                }
                Text("Shipping: ${formatMoney(order.shippingFee, currencySymbol)}", style = MaterialTheme.typography.bodySmall)
                Text("Tax / GST: ${formatMoney(order.taxAmount, currencySymbol)}", style = MaterialTheme.typography.bodySmall)
                Text("Total Paid: ${formatMoney(order.totalAmount, currencySymbol)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text("Payment: ${order.paymentMethod} • ${order.paymentStatus} (${order.paymentReference})", style = MaterialTheme.typography.labelSmall)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun EmptyAccountPlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
