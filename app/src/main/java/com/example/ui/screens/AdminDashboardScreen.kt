package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AccessControlState
import com.example.data.model.AuditLog
import com.example.data.model.Banner
import com.example.data.model.Category
import com.example.data.model.Coupon
import com.example.data.model.Order
import com.example.data.model.Product
import com.example.data.model.StaffMember
import com.example.data.model.StoreSettings
import com.example.data.model.UserProfile
import com.example.ui.components.formatMoney
import com.example.ui.theme.ChampagneGold
import com.example.ui.theme.EmeraldProfit
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.RubyDanger
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(
    accessControl: AccessControlState,
    products: List<Product>,
    categories: List<Category>,
    orders: List<Order>,
    coupons: List<Coupon>,
    banners: List<Banner>,
    customers: List<UserProfile>,
    staffMembers: List<StaffMember>,
    auditLogs: List<AuditLog>,
    storeSettings: StoreSettings,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    if (!accessControl.canAccessAdmin) {
        RestrictedAccessNotice(
            title = "Owner & Staff Admin Portal",
            description = "Full business control is locked to the Store Owner (deepakdhudiya152@gmail.com) and active Role-Based Staff accounts created by the Owner."
        )
        return
    }

    val tabs = listOf("Products", "Categories", "Orders & Refunds", "Coupons", "Content & Settings", "Customers", "Staff RBAC", "Audit Logs")
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var editingOrder by remember { mutableStateOf<Order?>(null) }
    var editingCoupon by remember { mutableStateOf<Coupon?>(null) }
    var editingStaff by remember { mutableStateOf<StaffMember?>(null) }
    val sym = storeSettings.currencySymbol

    Column(modifier = modifier.fillMaxSize()) {
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("D SQUARE Command Center", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = if (accessControl.isOwner) "Super-Admin Owner Access" else "Staff Role: ${accessControl.staffMember?.roleName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChampagneGold
                    )
                }
                if (accessControl.isOwner && products.isEmpty()) {
                    Button(
                        onClick = { adminViewModel.reseedCatalogIfOwner() },
                        colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack)
                    ) {
                        Text("Seed Catalog")
                    }
                }
            }
        }

        ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
            tabs.forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, style = MaterialTheme.typography.labelLarge) }
                )
            }
        }

        when (selectedTab) {
            // Products CRUD
            0 -> {
                if (!accessControl.canManageProducts) {
                    RestrictedAccessNotice("Products Permission Required", "Your staff role does not permit product edits.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = {
                                    val firstCat = categories.firstOrNull()
                                    editingProduct = Product(
                                        categoryId = firstCat?.id ?: "cat_fashion",
                                        categoryName = firstCat?.name ?: "Fashion & Clothing",
                                        lowStockThreshold = 5L,
                                        stock = 10L
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_add_product_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add New Product")
                            }
                        }
                        items(products, key = { it.id }) { prod ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(prod.name, style = MaterialTheme.typography.titleMedium)
                                        Text("${prod.categoryName} • SKU: ${prod.sku}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(
                                            "Price: ${formatMoney(prod.price, sym)} | Cost: ${formatMoney(prod.costPrice, sym)} | Stock: ${prod.stock}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (prod.stock <= prod.lowStockThreshold) RubyDanger else ChampagneGold
                                        )
                                    }
                                    IconButton(onClick = { editingProduct = prod }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit product")
                                    }
                                    IconButton(onClick = { adminViewModel.deleteProduct(prod) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete product", tint = RubyDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Categories CRUD
            1 -> {
                if (!accessControl.canManageProducts) {
                    RestrictedAccessNotice("Categories Permission Required", "Your staff role does not permit category edits.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { editingCategory = Category(sortOrder = (categories.size + 1).toLong()) },
                                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Unlimited Category")
                            }
                        }
                        items(categories, key = { it.id }) { cat ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cat.name, style = MaterialTheme.typography.titleMedium)
                                        Text(cat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { editingCategory = cat }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit category")
                                    }
                                    IconButton(onClick = { adminViewModel.deleteCategory(cat) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete category", tint = RubyDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Orders & Refunds
            2 -> {
                if (!accessControl.canManageOrders) {
                    RestrictedAccessNotice("Orders Permission Required", "Your staff role does not permit order management.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(orders, key = { it.id }) { ord ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${ord.orderNumber} • ${ord.customerName}", style = MaterialTheme.typography.titleMedium)
                                        Text(ord.itemsSummary, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            "${ord.orderStatus} | Pay: ${ord.paymentStatus} (${ord.paymentMethod}) | ${formatMoney(ord.totalAmount, sym)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ChampagneGold
                                        )
                                        if (ord.refundStatus != "NONE") {
                                            Text("Refund: ${ord.refundStatus} (${formatMoney(ord.refundAmount, sym)})", style = MaterialTheme.typography.labelSmall, color = RubyDanger)
                                        }
                                    }
                                    Button(onClick = { editingOrder = ord }) {
                                        Text("Manage")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Coupons
            3 -> {
                if (!accessControl.canManageCoupons) {
                    RestrictedAccessNotice("Coupons Permission Required", "Your staff role does not permit coupon management.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { editingCoupon = Coupon() },
                                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Create Promotional Coupon")
                            }
                        }
                        items(coupons, key = { it.id }) { cpn ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${cpn.code} (${if (cpn.isActive) "Active" else "Paused"})", style = MaterialTheme.typography.titleMedium, color = ChampagneGold)
                                        Text(cpn.description, style = MaterialTheme.typography.bodySmall)
                                        Text("Used ${cpn.usageCount}/${cpn.usageLimit}", style = MaterialTheme.typography.labelSmall)
                                    }
                                    IconButton(onClick = { editingCoupon = cpn }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit coupon")
                                    }
                                    IconButton(onClick = { adminViewModel.deleteCoupon(cpn) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete coupon", tint = RubyDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Content & Business Settings
            4 -> {
                if (!accessControl.canManageContent) {
                    RestrictedAccessNotice("Content Permission Required", "Your staff role does not permit business settings edits.")
                } else {
                    BusinessSettingsEditor(
                        settings = storeSettings,
                        banner = banners.firstOrNull(),
                        onSaveSettings = { adminViewModel.saveStoreSettings(it) },
                        onSaveBanner = { adminViewModel.saveBanner(it) }
                    )
                }
            }

            // Customers CRM
            5 -> {
                if (!accessControl.isOwner) {
                    RestrictedAccessNotice("Owner CRM Access", "Full customer directory listing is restricted to the Store Owner.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(customers, key = { it.id }) { cust ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cust.displayName, style = MaterialTheme.typography.titleMedium)
                                        Text(cust.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("Orders: ${cust.totalOrders} • Spent: ${formatMoney(cust.totalSpent, sym)}", style = MaterialTheme.typography.labelMedium, color = ChampagneGold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            adminViewModel.sendCustomerBroadcastNotification(
                                                cust.userId,
                                                "D SQUARE Private Offer",
                                                "Enjoy 15% privilege savings with code DSQUARE15 on your next order."
                                            )
                                        }
                                    ) {
                                        Text("Notify")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Staff RBAC (Owner Only)
            6 -> {
                if (!accessControl.isOwner) {
                    RestrictedAccessNotice("Owner Only", "Only the Store Owner can create or modify staff RBAC permissions.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Button(
                                onClick = { editingStaff = StaffMember() },
                                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("+ Assign Staff Member & Permissions")
                            }
                        }
                        items(staffMembers, key = { it.id }) { staff ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${staff.displayName} • ${staff.roleName}", style = MaterialTheme.typography.titleMedium)
                                        Text("${staff.email} (UID: ${staff.staffUid})", style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            listOfNotNull(
                                                if (staff.canManageProducts) "Products" else null,
                                                if (staff.canManageOrders) "Orders" else null,
                                                if (staff.canManageCoupons) "Coupons" else null,
                                                if (staff.canManageContent) "Content" else null,
                                                if (staff.canViewAnalytics) "Analytics" else null
                                            ).joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = EmeraldProfit
                                        )
                                    }
                                    IconButton(onClick = { editingStaff = staff }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit staff")
                                    }
                                    IconButton(onClick = { adminViewModel.deleteStaffMember(staff) }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete staff", tint = RubyDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Immutable Audit Trail (Owner Only)
            7 -> {
                if (!accessControl.isOwner) {
                    RestrictedAccessNotice("Owner Only", "Activity and staff audit logs are restricted to the Store Owner.")
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(auditLogs, key = { it.id }) { log ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("${log.action} • ${log.targetCollection}", style = MaterialTheme.typography.labelLarge, color = ChampagneGold)
                                    Text(log.details, style = MaterialTheme.typography.bodySmall)
                                    Text("By ${log.actorEmail} (${log.actorRole})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editingProduct?.let { prod ->
        ProductEditorDialog(
            product = prod,
            categories = categories,
            onDismiss = { editingProduct = null },
            onSave = {
                adminViewModel.saveProduct(it)
                editingProduct = null
            }
        )
    }

    editingCategory?.let { cat ->
        CategoryEditorDialog(
            category = cat,
            onDismiss = { editingCategory = null },
            onSave = {
                adminViewModel.saveCategory(it)
                editingCategory = null
            }
        )
    }

    editingOrder?.let { ord ->
        OrderAdminDialog(
            order = ord,
            onDismiss = { editingOrder = null },
            onSave = { status, payStatus, tracking, courier, refStatus, refAmt ->
                adminViewModel.updateOrder(ord, status, payStatus, tracking, courier, refStatus, refAmt)
                editingOrder = null
            }
        )
    }

    editingCoupon?.let { cpn ->
        CouponEditorDialog(
            coupon = cpn,
            onDismiss = { editingCoupon = null },
            onSave = {
                adminViewModel.saveCoupon(it)
                editingCoupon = null
            }
        )
    }

    editingStaff?.let { staff ->
        StaffEditorDialog(
            staff = staff,
            onDismiss = { editingStaff = null },
            onSave = {
                adminViewModel.saveStaffMember(it)
                editingStaff = null
            }
        )
    }
}

@Composable
private fun BusinessSettingsEditor(
    settings: StoreSettings,
    banner: Banner?,
    onSaveSettings: (StoreSettings) -> Unit,
    onSaveBanner: (Banner) -> Unit
) {
    var announcement by remember(settings) { mutableStateOf(settings.announcementBar) }
    var shippingFee by remember(settings) { mutableStateOf(settings.defaultShippingFee.toString()) }
    var freeThreshold by remember(settings) { mutableStateOf(settings.freeShippingThreshold.toString()) }
    var taxRate by remember(settings) { mutableStateOf(settings.taxRatePercent.toString()) }
    var monthlyExpense by remember(settings) { mutableStateOf(settings.monthlyOperatingExpense.toString()) }

    var bannerTitle by remember(banner) { mutableStateOf(banner?.title ?: "LUXURY REFINED") }
    var bannerSubtitle by remember(banner) { mutableStateOf(banner?.subtitle ?: "The Flagship Atelier Collection") }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Storefront & Financial Settings", style = MaterialTheme.typography.titleLarge)
        }
        item {
            OutlinedTextField(value = announcement, onValueChange = { announcement = it }, label = { Text("Announcement Ticker") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = shippingFee, onValueChange = { shippingFee = it }, label = { Text("Shipping Fee") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = freeThreshold, onValueChange = { freeThreshold = it }, label = { Text("Free Ship Threshold") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = taxRate, onValueChange = { taxRate = it }, label = { Text("Tax / GST %") }, modifier = Modifier.weight(1f))
                OutlinedTextField(value = monthlyExpense, onValueChange = { monthlyExpense = it }, label = { Text("Monthly OpEx") }, modifier = Modifier.weight(1f))
            }
        }
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        settings.copy(
                            announcementBar = announcement,
                            defaultShippingFee = shippingFee.toDoubleOrNull() ?: settings.defaultShippingFee,
                            freeShippingThreshold = freeThreshold.toDoubleOrNull() ?: settings.freeShippingThreshold,
                            taxRatePercent = taxRate.toDoubleOrNull() ?: settings.taxRatePercent,
                            monthlyOperatingExpense = monthlyExpense.toDoubleOrNull() ?: settings.monthlyOperatingExpense
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = ChampagneGold, contentColor = ObsidianBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Business Settings")
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Hero Banner Content", style = MaterialTheme.typography.titleLarge)
        }
        item {
            OutlinedTextField(value = bannerTitle, onValueChange = { bannerTitle = it }, label = { Text("Banner Headline") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(value = bannerSubtitle, onValueChange = { bannerSubtitle = it }, label = { Text("Banner Subtitle") }, modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedButton(
                onClick = {
                    val base = banner ?: Banner(id = "ban_flagship", badge = "D SQUARE ATELIER", ctaText = "EXPLORE EDIT", imageKey = "img_hero_luxury", sortOrder = 1L, isActive = true)
                    onSaveBanner(base.copy(title = bannerTitle, subtitle = bannerSubtitle))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Update Homepage Banner")
            }
        }
    }
}

@Composable
private fun ProductEditorDialog(
    product: Product,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    var name by remember { mutableStateOf(product.name) }
    var sku by remember { mutableStateOf(product.sku) }
    var price by remember { mutableStateOf(if (product.price > 0) product.price.toString() else "") }
    var cost by remember { mutableStateOf(if (product.costPrice > 0) product.costPrice.toString() else "") }
    var stock by remember { mutableStateOf(product.stock.toString()) }
    var desc by remember { mutableStateOf(product.description) }
    var selectedCatId by remember { mutableStateOf(product.categoryId) }
    var imageKey by remember { mutableStateOf(product.imageKey) }
    var featured by remember { mutableStateOf(product.isFeatured) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (product.id.isBlank()) "New Product" else "Edit Product") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, singleLine = true)
                OutlinedTextField(value = sku, onValueChange = { sku = it }, label = { Text("SKU") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Selling Price") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, label = { Text("Cost Price") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = stock, onValueChange = { stock = it }, label = { Text("Stock Qty") }, singleLine = true)
                OutlinedTextField(value = imageKey, onValueChange = { imageKey = it }, label = { Text("Image Key (img_prod_fashion/electronics/jewellery)") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = featured, onCheckedChange = { featured = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Featured on Home")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cat = categories.firstOrNull { it.id == selectedCatId } ?: categories.firstOrNull()
                    if (name.isNotBlank()) {
                        onSave(
                            product.copy(
                                name = name,
                                sku = sku,
                                categoryId = cat?.id ?: product.categoryId,
                                categoryName = cat?.name ?: product.categoryName,
                                price = price.toDoubleOrNull() ?: 999.0,
                                costPrice = cost.toDoubleOrNull() ?: 500.0,
                                stock = stock.toLongOrNull() ?: 10L,
                                description = desc,
                                imageKey = imageKey,
                                isFeatured = featured
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CategoryEditorDialog(category: Category, onDismiss: () -> Unit, onSave: (Category) -> Unit) {
    var name by remember { mutableStateOf(category.name) }
    var desc by remember { mutableStateOf(category.description) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (category.id.isBlank()) "New Category" else "Edit Category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, maxLines = 2)
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onSave(category.copy(name = name, description = desc)) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun OrderAdminDialog(
    order: Order,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, Double) -> Unit
) {
    var status by remember { mutableStateOf(order.orderStatus) }
    var payStatus by remember { mutableStateOf(order.paymentStatus) }
    var tracking by remember { mutableStateOf(order.trackingNumber) }
    var courier by remember { mutableStateOf(order.trackingCourier.ifBlank { "BlueDart Luxury Air" }) }
    var refundStatus by remember { mutableStateOf(order.refundStatus) }
    var refundAmount by remember { mutableStateOf(if (order.refundAmount > 0) order.refundAmount.toString() else order.totalAmount.toString()) }

    val statuses = listOf("PLACED", "CONFIRMED", "PACKED", "SHIPPED", "OUT_FOR_DELIVERY", "DELIVERED", "CANCELLED", "REFUNDED")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage ${order.orderNumber}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tap Status:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.take(4).forEach { st ->
                        OutlinedButton(onClick = { status = st }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text(st.take(5), color = if (status == st) ChampagneGold else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    statuses.drop(4).forEach { st ->
                        OutlinedButton(onClick = { status = st }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                            Text(st.take(5), color = if (status == st) ChampagneGold else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
                OutlinedTextField(value = status, onValueChange = { status = it }, label = { Text("Order Status") }, singleLine = true)
                OutlinedTextField(value = payStatus, onValueChange = { payStatus = it }, label = { Text("Payment Status (PAID/PENDING/REFUNDED)") }, singleLine = true)
                OutlinedTextField(value = courier, onValueChange = { courier = it }, label = { Text("Courier") }, singleLine = true)
                OutlinedTextField(value = tracking, onValueChange = { tracking = it }, label = { Text("Tracking Number") }, singleLine = true)
                OutlinedTextField(value = refundStatus, onValueChange = { refundStatus = it }, label = { Text("Refund Status (NONE/REQUESTED/APPROVED/PROCESSED)") }, singleLine = true)
                OutlinedTextField(value = refundAmount, onValueChange = { refundAmount = it }, label = { Text("Refund Amount") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(status, payStatus, tracking, courier, refundStatus, refundAmount.toDoubleOrNull() ?: 0.0)
                }
            ) { Text("Update Order") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun CouponEditorDialog(coupon: Coupon, onDismiss: () -> Unit, onSave: (Coupon) -> Unit) {
    var code by remember { mutableStateOf(coupon.code) }
    var desc by remember { mutableStateOf(coupon.description) }
    var type by remember { mutableStateOf(coupon.discountType) }
    var value by remember { mutableStateOf(coupon.discountValue.toString()) }
    var minOrder by remember { mutableStateOf(coupon.minOrderAmount.toString()) }
    var active by remember { mutableStateOf(coupon.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Coupon Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Coupon Code") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true)
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Type (PERCENT or FLAT)") }, singleLine = true)
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("Discount Value") }, singleLine = true)
                OutlinedTextField(value = minOrder, onValueChange = { minOrder = it }, label = { Text("Min Order Amount") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = active, onCheckedChange = { active = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Active")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank()) {
                        onSave(
                            coupon.copy(
                                code = code,
                                description = desc.ifBlank { "Special Privilege Discount" },
                                discountType = if (type.uppercase() == "FLAT") "FLAT" else "PERCENT",
                                discountValue = value.toDoubleOrNull() ?: 10.0,
                                minOrderAmount = minOrder.toDoubleOrNull() ?: 0.0,
                                isActive = active
                            )
                        )
                    }
                }
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun StaffEditorDialog(staff: StaffMember, onDismiss: () -> Unit, onSave: (StaffMember) -> Unit) {
    var uid by remember { mutableStateOf(staff.staffUid) }
    var name by remember { mutableStateOf(staff.displayName) }
    var email by remember { mutableStateOf(staff.email) }
    var role by remember { mutableStateOf(staff.roleName) }
    var pProducts by remember { mutableStateOf(staff.canManageProducts) }
    var pOrders by remember { mutableStateOf(staff.canManageOrders) }
    var pCoupons by remember { mutableStateOf(staff.canManageCoupons) }
    var pContent by remember { mutableStateOf(staff.canManageContent) }
    var pAnalytics by remember { mutableStateOf(staff.canViewAnalytics) }
    var active by remember { mutableStateOf(staff.isActive) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Staff RBAC Permissions") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(value = uid, onValueChange = { uid = it }, label = { Text("Firebase User UID") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Staff Name") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Staff Google Email") }, singleLine = true)
                OutlinedTextField(value = role, onValueChange = { role = it }, label = { Text("Role Title") }, singleLine = true)
                PermCheck("Manage Products & Categories", pProducts) { pProducts = it }
                PermCheck("Manage Orders & Refunds", pOrders) { pOrders = it }
                PermCheck("Manage Coupons", pCoupons) { pCoupons = it }
                PermCheck("Manage Banners & Settings", pContent) { pContent = it }
                PermCheck("View Analytics Dashboard", pAnalytics) { pAnalytics = it }
                PermCheck("Staff Account Active", active) { active = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (uid.isNotBlank() && email.isNotBlank() && name.isNotBlank()) {
                        onSave(
                            staff.copy(
                                staffUid = uid,
                                displayName = name,
                                email = email,
                                roleName = role,
                                canManageProducts = pProducts,
                                canManageOrders = pOrders,
                                canManageCoupons = pCoupons,
                                canManageContent = pContent,
                                canViewAnalytics = pAnalytics,
                                isActive = active
                            )
                        )
                    }
                }
            ) { Text("Save Staff") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun PermCheck(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onChange)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}
