package com.example.data.local

import com.example.data.model.*
import java.util.UUID

object DataGenerator {
    const val DEFAULT_USER_ID = "user_default_owner"
    const val BIZ_HAFSA_ID = "biz_hafsa_traders"
    const val BIZ_GAMA_ID = "biz_gama_earthmovers"
    const val BIZ_KHWAZA_ID = "biz_khwaza_collection"

    suspend fun populateInitialDataIfEmpty(db: LedgerDatabase) {
        val profileDao = db.profileDao()
        val existingProfile = profileDao.getProfileById(DEFAULT_USER_ID)
        if (existingProfile != null) return

        // 1. Create Default Profile
        val user = UserProfile(
            id = DEFAULT_USER_ID,
            fullName = "Shan Palia",
            email = "shanpalia786@gmail.com",
            mobile = "9876543210",
            passwordHash = "password123",
            createdAt = System.currentTimeMillis() - 86400000L * 30
        )
        profileDao.insertProfile(user)

        // 2. Create Businesses
        val businessDao = db.businessDao()
        val settingsDao = db.notificationSettingsDao()

        val hafsaBiz = Business(
            id = BIZ_HAFSA_ID,
            ownerId = DEFAULT_USER_ID,
            businessName = "Hafsa Traders",
            logoUrl = "",
            address = "Main Market Road",
            city = "Palia Kalan",
            state = "Uttar Pradesh",
            country = "India",
            pinCode = "262902",
            mobile = "9876543210",
            email = "contact@hafsatraders.in",
            gstNumber = "09AAAAA0000A1Z5",
            upiId = "hafsa@upi",
            createdAt = System.currentTimeMillis() - 86400000L * 25
        )

        val gamaBiz = Business(
            id = BIZ_GAMA_ID,
            ownerId = DEFAULT_USER_ID,
            businessName = "New Gama Earthmovers",
            logoUrl = "",
            address = "Industrial Area, Bypass Road",
            city = "Lakhimpur",
            state = "Uttar Pradesh",
            country = "India",
            pinCode = "262701",
            mobile = "9876598765",
            email = "gama.earthmovers@gmail.com",
            gstNumber = "09BBBBB1111B1Z2",
            upiId = "gama@upi",
            createdAt = System.currentTimeMillis() - 86400000L * 20
        )

        val khwazaBiz = Business(
            id = BIZ_KHWAZA_ID,
            ownerId = DEFAULT_USER_ID,
            businessName = "Khwaza Hasan Collection",
            logoUrl = "",
            address = "Cloth Market, Chandni Chowk",
            city = "Delhi",
            state = "Delhi",
            country = "India",
            pinCode = "110006",
            mobile = "9811122233",
            email = "khwaza.collection@yahoo.com",
            gstNumber = "07CCCCC2222C1Z8",
            upiId = "khwaza@upi",
            createdAt = System.currentTimeMillis() - 86400000L * 15
        )

        businessDao.insertBusiness(hafsaBiz)
        businessDao.insertBusiness(gamaBiz)
        businessDao.insertBusiness(khwazaBiz)

        listOf(BIZ_HAFSA_ID, BIZ_GAMA_ID, BIZ_KHWAZA_ID).forEach { bizId ->
            settingsDao.insertSettings(
                NotificationSettings(
                    id = UUID.randomUUID().toString(),
                    businessId = bizId,
                    pushEnabled = true,
                    whatsappEnabled = true,
                    smsEnabled = true
                )
            )
        }

        // 3. Customers for Hafsa Traders
        val customerDao = db.customerDao()
        val txDao = db.transactionDao()
        val notifDao = db.notificationDao()

        val custRahul = Customer(
            id = "cust_rahul",
            businessId = BIZ_HAFSA_ID,
            name = "Rahul Sharma",
            mobile = "9876512345",
            address = "Station Road, Palia",
            openingBalance = 10000.0,
            openingBalanceType = BalanceType.DEBIT.name,
            notes = "Wholesale Grocery Client"
        )
        val custAman = Customer(
            id = "cust_aman",
            businessId = BIZ_HAFSA_ID,
            name = "Aman Verma",
            mobile = "9876523456",
            address = "Bazaar Gate, Palia",
            openingBalance = 5000.0,
            openingBalanceType = BalanceType.DEBIT.name,
            notes = "Regular Kirana shop buyer"
        )
        val custRizwan = Customer(
            id = "cust_rizwan",
            businessId = BIZ_HAFSA_ID,
            name = "Mohd Rizwan",
            mobile = "9876534567",
            address = "Civil Lines, Palia",
            openingBalance = 2500.0,
            openingBalanceType = BalanceType.CREDIT.name,
            notes = "Advance deposit account"
        )
        val custDeepak = Customer(
            id = "cust_deepak",
            businessId = BIZ_HAFSA_ID,
            name = "Deepak Gupta",
            mobile = "9876545678",
            address = "Bus Stand Complex, Palia",
            openingBalance = 15000.0,
            openingBalanceType = BalanceType.DEBIT.name,
            notes = "Bulk wheat dealer"
        )

        customerDao.insertCustomer(custRahul)
        customerDao.insertCustomer(custAman)
        customerDao.insertCustomer(custRizwan)
        customerDao.insertCustomer(custDeepak)

        // Customer for New Gama Earthmovers
        val custVikram = Customer(
            id = "cust_vikram",
            businessId = BIZ_GAMA_ID,
            name = "Vikram Singh (Contractor)",
            mobile = "9988776655",
            address = "Highway Project Site 4",
            openingBalance = 20000.0,
            openingBalanceType = BalanceType.DEBIT.name,
            notes = "Excavator rental"
        )
        customerDao.insertCustomer(custVikram)

        // 4. Initial Transactions for Hafsa Traders
        val now = System.currentTimeMillis()

        // Rahul: Opening 10000 Debit
        // Aug 30: Debit 5500 (Sugar & Oil purchase)
        val tx1 = LedgerTransaction(
            id = "tx_1",
            businessId = BIZ_HAFSA_ID,
            customerId = "cust_rahul",
            transactionType = TransactionType.DEBIT.name,
            amount = 5500.0,
            description = "Sugar 50kg & Mustard Oil 15L",
            paymentMode = PaymentMode.OTHER.displayName,
            referenceNumber = "INV-1042",
            transactionDate = now - 86400000L * 2,
            createdBy = "Shan Palia"
        )
        // Aug 31: Payment Received 3000 Credit
        val tx2 = LedgerTransaction(
            id = "tx_2",
            businessId = BIZ_HAFSA_ID,
            customerId = "cust_rahul",
            transactionType = TransactionType.CREDIT.name,
            amount = 3000.0,
            description = "UPI Partial Payment",
            paymentMode = PaymentMode.UPI.displayName,
            referenceNumber = "UPI98237419",
            transactionDate = now - 86400000L * 1,
            createdBy = "Shan Palia"
        )
        txDao.insertTransaction(tx1)
        txDao.insertTransaction(tx2)

        // Aman: Debit 3500, Credit 2000
        val tx3 = LedgerTransaction(
            id = "tx_3",
            businessId = BIZ_HAFSA_ID,
            customerId = "cust_aman",
            transactionType = TransactionType.DEBIT.name,
            amount = 3500.0,
            description = "Pulses & Spices delivery",
            paymentMode = PaymentMode.CASH.displayName,
            referenceNumber = "INV-1088",
            transactionDate = now - 86400000L * 3,
            createdBy = "Shan Palia"
        )
        val tx4 = LedgerTransaction(
            id = "tx_4",
            businessId = BIZ_HAFSA_ID,
            customerId = "cust_aman",
            transactionType = TransactionType.CREDIT.name,
            amount = 2000.0,
            description = "Cash Received at shop",
            paymentMode = PaymentMode.CASH.displayName,
            referenceNumber = "RCP-441",
            transactionDate = now - 86400000L * 1,
            createdBy = "Shan Palia"
        )
        txDao.insertTransaction(tx3)
        txDao.insertTransaction(tx4)

        // Gama Earthmovers Transaction
        val txGama1 = LedgerTransaction(
            id = "tx_gama_1",
            businessId = BIZ_GAMA_ID,
            customerId = "cust_vikram",
            transactionType = TransactionType.DEBIT.name,
            amount = 5000.0,
            description = "JCB 8 Hours leveling work",
            paymentMode = PaymentMode.OTHER.displayName,
            referenceNumber = "LOG-88",
            transactionDate = now - 86400000L * 1,
            createdBy = "Shan Palia"
        )
        txDao.insertTransaction(txGama1)

        // Notifications
        notifDao.insertNotification(
            NotificationRecord(
                id = UUID.randomUUID().toString(),
                businessId = BIZ_HAFSA_ID,
                customerId = "cust_rahul",
                transactionId = "tx_2",
                channel = NotificationChannel.WHATSAPP.name,
                title = "🏪 Hafsa Traders",
                message = "₹3,000 payment has been received.\nCurrent Balance: ₹12,500\nThank You\nHafsa Traders",
                status = NotificationStatus.SENT.name,
                sentAt = now - 86400000L * 1
            )
        )
        notifDao.insertNotification(
            NotificationRecord(
                id = UUID.randomUUID().toString(),
                businessId = BIZ_GAMA_ID,
                customerId = "cust_vikram",
                transactionId = "tx_gama_1",
                channel = NotificationChannel.WHATSAPP.name,
                title = "🏪 New Gama Earthmovers",
                message = "₹5,000 debit entry has been added.\nCurrent Balance: ₹25,000\nThank You\nNew Gama Earthmovers",
                status = NotificationStatus.SENT.name,
                sentAt = now - 86400000L * 1
            )
        )
    }
}
