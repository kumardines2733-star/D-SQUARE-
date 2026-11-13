package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.Address
import com.example.data.model.CartItem
import com.example.data.model.Product
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class DSquareRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun customerProfileAndCart_authenticatedUser_succeeds() = runBlocking {
        val uid = signInTestUser("alice@dsquare.test")
        val customerRepo = CustomerRepository(firestore, auth)

        val profileRes = withTimeout(DEFAULT_TIMEOUT_MS) { customerRepo.ensureUserProfile() }
        assertTrue(profileRes.isSuccess)

        val product = Product(
            id = "prod_test_1",
            name = "Obsidian Tote",
            categoryName = "Fashion",
            price = 18999.0,
            costPrice = 9200.0,
            imageKey = "img_prod_fashion"
        )
        val cartRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.addToCart(product, "Obsidian / Gold", 2L)
        }
        assertTrue(cartRes.isSuccess)

        val cartItems = withTimeout(FLOW_TIMEOUT_MS) {
            customerRepo.observeCart(uid).first { it.isNotEmpty() }
        }
        assertEquals(1, cartItems.size)
        assertEquals(2L, cartItems.first().quantity)
    }

    @Test
    fun customerOrders_scopedToOwner_andBlocksUnauthenticated() = runBlocking {
        val aliceUid = signInTestUser("alice_orders@dsquare.test")
        val orderRepo = OrderRepository(firestore, auth)
        val customerRepo = CustomerRepository(firestore, auth)

        withTimeout(DEFAULT_TIMEOUT_MS) { customerRepo.ensureUserProfile() }
        val cartItem = CartItem(
            userId = aliceUid,
            productId = "prod_test_1",
            productName = "Obsidian Tote",
            variant = "Obsidian",
            unitPrice = 18999.0,
            costPrice = 9200.0,
            quantity = 1L,
            imageKey = "img_prod_fashion"
        )
        val orderRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            orderRepo.placeOrder(
                customerName = "Alice",
                customerEmail = "alice_orders@dsquare.test",
                customerPhone = "+91 9999999999",
                shippingAddress = "100 Luxury Lane, Mumbai 400001",
                cartItems = listOf(cartItem),
                appliedCoupon = null,
                discountAmount = 0.0,
                shippingFee = 0.0,
                taxAmount = 3419.82,
                paymentMethod = "UPI"
            )
        }
        assertTrue(orderRes.isSuccess)

        val aliceOrders = withTimeout(FLOW_TIMEOUT_MS) {
            orderRepo.observeCustomerOrders(aliceUid).first { it.isNotEmpty() }
        }
        assertTrue(aliceOrders.isNotEmpty())

        // Cross-user isolation check
        signInTestUser("bob_orders@dsquare.test")
        val bobQueryForAlice = try {
            withTimeout(FLOW_TIMEOUT_MS) {
                orderRepo.observeCustomerOrders(aliceUid).first()
            }
            false
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            fsEx?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
        }
        assertTrue("Expected PERMISSION_DENIED on cross-user order query", bobQueryForAlice)

        // Unauthenticated negative test
        auth.signOut()
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                orderRepo.observeCustomerOrders(aliceUid).first()
            }
            fail("Expected PERMISSION_DENIED when signed out")
        } catch (e: Exception) {
            val fsEx = e.unwrapFirestoreException()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
        }
    }

    @Test
    fun customerAddress_createAndObserve_succeeds() = runBlocking {
        val uid = signInTestUser("addr_user@dsquare.test")
        val customerRepo = CustomerRepository(firestore, auth)
        val saveRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            customerRepo.saveAddress(
                Address(
                    label = "Atelier Office",
                    recipientName = "Vikram Rao",
                    phone = "+91 9820012345",
                    street = "42 Marine Drive",
                    city = "Mumbai",
                    state = "Maharashtra",
                    postalCode = "400020",
                    country = "India",
                    isDefault = true
                )
            )
        }
        assertTrue(saveRes.isSuccess)
        val addresses = withTimeout(FLOW_TIMEOUT_MS) {
            customerRepo.observeAddresses(uid).first { it.isNotEmpty() }
        }
        assertEquals("Atelier Office", addresses.first().label)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
