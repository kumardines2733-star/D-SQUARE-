package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Address
import com.example.data.model.CartItem
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.StoreSettings
import com.example.ui.components.DSquareImage
import com.example.ui.components.formatMoney
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ObsidianBlack

@Composable
fun CartCheckoutScreen(
    cartItems: List<CartItem>,
    addresses: List<Address>,
    coupons: List<Coupon>,
    appliedCoupon: Coupon?,
    discountAmount: Double,
    storeSettings: StoreSettings,
    isBusy: Boolean,
    onUpdateQuantity: (String, Long) -> Unit,
    onApplyCoupon: (String) -> Unit,
    onRemoveCoupon: () -> Unit,
    onSaveAddress: (Address) -> Unit,
    onPlaceOrder: (Address, String, (Order) -> Unit) -> Unit,
    onNavigateToOrders: () -> Unit,
    modifier: Modifier = Modifier
) {
    var couponInput by remember { mutableStateOf("") }
    var selectedPayment by remember { mutableStateOf("UPI") }
    var selectedAddressId by remember(addresses) {
        mutableStateOf(addresses.firstOrNull { it.isDefault }?.id ?: addresses.firstOrNull()?.id ?: "")
    }
    var showAddAddressDialog by remember { mutableStateOf(false) }
    var placedOrder by remember { mutableStateOf<Order?>(null) }

    val subtotal = cartItems.sumOf { it.unitPrice * it.quantity }
    val taxable = (subtotal - discountAmount).coerceAtLeast(0.0)
    val shippingFee = if (cartItems.isEmpty() || taxable >= storeSettings.freeShippingThreshold) 0.0 else storeSettings.defaultShippingFee
    val taxAmount = Math.round(taxable * (storeSettings.taxRatePercent / 100.0) * 100.0) / 100.0
    val grandTotal = (taxable + shippingFee + taxAmount).coerceAtLeast(0.0)
    val sym = storeSettings.currencySymbol

    if (cartItems.isEmpty() && placedOrder == null) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.ShoppingBag,
                contentDescription = null,
                tint = ChampagneGold,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text("Your Shopping Bag Is Empty", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Explore D SQUARE collections and add luxury pieces to begin checkout.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Text("Shopping Bag & Checkout", style = MaterialTheme.typography.headlineMedium)
        }

        items(cartItems, key = { it.id }) { item ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.productName, style = MaterialTheme.typography.titleMedium)
                        Text(item.variant, style = MaterialTheme.typography.bodySmall, color = ChampagneGold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = formatMoney(item.unitPrice * item.quantity, sym),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onUpdateQuantity(item.id, item.quantity - 1L) }) {
                            Icon(
                                if (item.quantity <= 1L) Icons.Filled.DeleteOutline else Icons.Filled.Remove,
                                contentDescription = "Decrease"
                            )
                        }
                        Text("${item.quantity}", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { onUpdateQuantity(item.id, item.quantity + 1L) }) {
                            Icon(Icons.Filled.Add, contentDescription = "Increase")
                        }
                    }
                }
            }
        }

        // Coupon section
        item(key = "coupons") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = ChampagneGold, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Promo & Privilege Codes", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = couponInput,
                            onValueChange = { couponInput = it },
                            placeholder = { Text("Enter coupon code") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("coupon_code_input")
                        )
                        Button(
                            onClick = { onApplyCoupon(couponInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                            modifier = Modifier.testTag("apply_coupon_button")
                        ) {
                            Text("Apply")
                        }
                    }
                    if (appliedCoupon != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Applied ${appliedCoupon.code} (-${formatMoney(discountAmount, sym)})",
                                color = EmeraldProfit,
                                style = MaterialTheme.typography.labelLarge
                            )
                            TextButton(onClick = onRemoveCoupon) { Text("Remove") }
                        }
                    }
                    val activeCoupons = coupons.filter { it.isActive }
                    if (activeCoupons.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(activeCoupons, key = { it.id }) { cpn ->
                                AssistChip(
                                    onClick = {
                                        couponInput = cpn.code
                                        onApplyCoupon(cpn.code)
                                    },
                                    label = { Text("${cpn.code} • ${cpn.description}") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Delivery Address
        item(key = "address") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Delivery Address", style = MaterialTheme.typography.titleMedium)
                        TextButton(
                            onClick = { showAddAddressDialog = true },
                            modifier = Modifier.testTag("checkout_add_address_button")
                        ) {
                            Text("+ Add New")
                        }
                    }
                    if (addresses.isEmpty()) {
                        Text(
                            "No saved address yet. Tap '+ Add New' to add your delivery address.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        addresses.forEach { addr ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAddressId = addr.id }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedAddressId == addr.id,
                                    onClick = { selectedAddressId = addr.id }
                                )
                                Column {
                                    Text("${addr.label} • ${addr.recipientName}", style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${addr.street}, ${addr.city}, ${addr.state} ${addr.postalCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Payment Method
        item(key = "payment") {
            val methods = listOf(
                "UPI" to "UPI (GPay, PhonePe, BHIM)",
                "CARD" to "Credit / Debit Card (Visa, Mastercard, RuPay)",
                "NET_BANKING" to "Net Banking (All Major Banks)",
                "RAZORPAY" to "Razorpay Express Checkout",
                "COD" to "Cash on Delivery (Pay at Doorstep)"
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    methods.forEach { (code, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPayment = code }
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPayment == code,
                                onClick = { selectedPayment = code }
                            )
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }

        // Summary & Checkout CTA
        item(key = "summary") {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, ChampagneGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryRow("Subtotal", formatMoney(subtotal, sym))
                    if (discountAmount > 0.0) {
                        SummaryRow("Coupon Discount", "-${formatMoney(discountAmount, sym)}", EmeraldProfit)
                    }
                    SummaryRow(
                        "Shipping",
                        if (shippingFee == 0.0) "COMPLIMENTARY" else formatMoney(shippingFee, sym)
                    )
                    SummaryRow("GST / Tax (${storeSettings.taxRatePercent.toInt()}%)", formatMoney(taxAmount, sym))
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Payable", style = MaterialTheme.typography.titleLarge)
                        Text(
                            formatMoney(grandTotal, sym),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = ChampagneGold
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    val chosenAddress = addresses.firstOrNull { it.id == selectedAddressId }
                    Button(
                        onClick = {
                            if (chosenAddress != null) {
                                onPlaceOrder(chosenAddress, selectedPayment) { order ->
                                    placedOrder = order
                                }
                            } else {
                                showAddAddressDialog = true
                            }
                        },
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("place_order_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = ObsidianBlack, strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = if (chosenAddress == null) "Add Delivery Address to Checkout" else "Place Order • ${formatMoney(grandTotal, sym)}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddAddressDialog) {
        AddAddressDialog(
            onDismiss = { showAddAddressDialog = false },
            onSave = { addr ->
                onSaveAddress(addr)
                showAddAddressDialog = false
            }
        )
    }

    placedOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { placedOrder = null },
            icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EmeraldProfit, modifier = Modifier.size(40.dp)) },
            title = { Text("Order Confirmed • ${order.orderNumber}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Thank you for shopping with D SQUARE.")
                    Text("Payment: ${order.paymentMethod} (${order.paymentStatus})", style = MaterialTheme.typography.bodySmall)
                    Text("Ref: ${order.paymentReference}", style = MaterialTheme.typography.bodySmall)
                    Text("Total: ${formatMoney(order.totalAmount, sym)}", style = MaterialTheme.typography.titleMedium)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        placedOrder = null
                        onNavigateToOrders()
                    }
                ) { Text("Track Order & Invoice") }
            },
            dismissButton = {
                OutlinedButton(onClick = { placedOrder = null }) { Text("Continue Shopping") }
            }
        )
    }
}

@Composable
private fun SummaryRow(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), color = valueColor)
    }
}

@Composable
fun AddAddressDialog(
    onDismiss: () -> Unit,
    onSave: (Address) -> Unit
) {
    var label by remember { mutableStateOf("Home") }
    var recipient by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Delivery Address") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label (Home / Work)") }, singleLine = true)
                OutlinedTextField(value = recipient, onValueChange = { recipient = it }, label = { Text("Recipient Full Name") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, singleLine = true)
                OutlinedTextField(value = street, onValueChange = { street = it }, label = { Text("Street / Apartment") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = state, onValueChange = { state = it }, label = { Text("State") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = postalCode, onValueChange = { postalCode = it }, label = { Text("Postal / PIN Code") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (recipient.isNotBlank() && street.isNotBlank() && city.isNotBlank() && phone.length >= 5) {
                        onSave(
                            Address(
                                label = label,
                                recipientName = recipient,
                                phone = phone,
                                street = street,
                                city = city,
                                state = state.ifBlank { "MH" },
                                postalCode = postalCode.ifBlank { "400001" },
                                country = "India",
                                isDefault = true
                            )
                        )
                    }
                }
            ) { Text("Save Address") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
