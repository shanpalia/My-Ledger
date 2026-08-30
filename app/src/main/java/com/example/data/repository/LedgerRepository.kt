package com.example.data.repository

import com.example.data.local.LedgerDatabase
import com.example.data.model.*
import com.example.util.LedgerItemWithBalance
import com.example.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.util.UUID

data class CustomerBalanceSummary(
    val customer: Customer,
    val totalDebit: Double,
    val totalCredit: Double,
    val netBalance: Double, // > 0 means customer owes money (Debit), < 0 means customer paid extra (Credit)
    val transactionCount: Int
)

data class BusinessSummary(
    val business: Business,
    val totalCustomers: Int,
    val totalDebit: Double,
    val totalCredit: Double,
    val totalOutstanding: Double, // Sum of positive net balances
    val todayTransactionsCount: Int,
    val todayTransactionsAmount: Double
)

class LedgerRepository(private val db: LedgerDatabase) {
    private val profileDao = db.profileDao()
    private val businessDao = db.businessDao()
    private val customerDao = db.customerDao()
    private val transactionDao = db.transactionDao()
    private val notificationDao = db.notificationDao()
    private val settingsDao = db.notificationSettingsDao()

    // --- Profile & Auth ---
    fun getProfileFlow(): Flow<UserProfile?> = profileDao.getFirstProfileFlow().flowOn(Dispatchers.IO)

    suspend fun getProfile(id: String): UserProfile? = withContext(Dispatchers.IO) {
        profileDao.getProfileById(id)
    }

    suspend fun saveProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        profileDao.insertProfile(profile)
    }

    suspend fun updateProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        profileDao.updateProfile(profile)
    }

    // --- Business ---
    fun getBusinessesByOwner(ownerId: String): Flow<List<Business>> =
        businessDao.getBusinessesByOwner(ownerId).flowOn(Dispatchers.IO)

    fun getBusinessFlow(businessId: String): Flow<Business?> =
        businessDao.getBusinessFlowById(businessId).flowOn(Dispatchers.IO)

    suspend fun getBusiness(businessId: String): Business? = withContext(Dispatchers.IO) {
        businessDao.getBusinessById(businessId)
    }

    suspend fun saveBusiness(business: Business) = withContext(Dispatchers.IO) {
        businessDao.insertBusiness(business)
        // Ensure default settings exist
        if (settingsDao.getSettingsDirect(business.id) == null) {
            settingsDao.insertSettings(
                NotificationSettings(
                    id = UUID.randomUUID().toString(),
                    businessId = business.id
                )
            )
        }
    }

    suspend fun updateBusiness(business: Business) = withContext(Dispatchers.IO) {
        businessDao.updateBusiness(business)
    }

    suspend fun deleteBusiness(business: Business) = withContext(Dispatchers.IO) {
        businessDao.deleteBusiness(business)
    }

    // --- Customers ---
    fun getCustomersByBusiness(businessId: String): Flow<List<Customer>> =
        customerDao.getCustomersByBusiness(businessId).flowOn(Dispatchers.IO)

    fun getCustomerFlow(customerId: String): Flow<Customer?> =
        customerDao.getCustomerFlowById(customerId).flowOn(Dispatchers.IO)

    suspend fun getCustomer(customerId: String): Customer? = withContext(Dispatchers.IO) {
        customerDao.getCustomerById(customerId)
    }

    suspend fun saveCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) = withContext(Dispatchers.IO) {
        customerDao.updateCustomer(customer)
    }

    suspend fun deleteCustomer(customerId: String) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransactionsByCustomer(customerId)
        customerDao.deleteCustomerById(customerId)
    }

    // --- Transactions & Balance Calculation ---
    fun getTransactionsByBusiness(businessId: String): Flow<List<LedgerTransaction>> =
        transactionDao.getTransactionsByBusiness(businessId).flowOn(Dispatchers.IO)

    fun getTransactionsByCustomerAsc(customerId: String): Flow<List<LedgerTransaction>> =
        transactionDao.getTransactionsByCustomerAsc(customerId).flowOn(Dispatchers.IO)

    fun getTransactionsByCustomerDesc(customerId: String): Flow<List<LedgerTransaction>> =
        transactionDao.getTransactionsByCustomerDesc(customerId).flowOn(Dispatchers.IO)

    fun getTransactionsByDateRange(businessId: String, start: Long, end: Long): Flow<List<LedgerTransaction>> =
        transactionDao.getTransactionsByDateRange(businessId, start, end).flowOn(Dispatchers.IO)

    /**
     * Compute running balance securely for a customer.
     * Opening balance + sum(Debits) - sum(Credits).
     */
    suspend fun calculateCustomerBalance(customerId: String): CustomerBalanceSummary = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(customerId)
            ?: return@withContext CustomerBalanceSummary(
                customer = Customer(id = customerId, businessId = "", name = "Unknown", mobile = ""),
                totalDebit = 0.0,
                totalCredit = 0.0,
                netBalance = 0.0,
                transactionCount = 0
            )

        val txs = transactionDao.getTransactionsForCustomerList(customerId)

        var totalDebit = if (customer.openingBalanceType == BalanceType.DEBIT.name) customer.openingBalance else 0.0
        var totalCredit = if (customer.openingBalanceType == BalanceType.CREDIT.name) customer.openingBalance else 0.0

        for (tx in txs) {
            if (tx.transactionType == TransactionType.DEBIT.name) {
                totalDebit += tx.amount
            } else {
                totalCredit += tx.amount
            }
        }

        val netBalance = totalDebit - totalCredit

        CustomerBalanceSummary(
            customer = customer,
            totalDebit = totalDebit,
            totalCredit = totalCredit,
            netBalance = netBalance,
            transactionCount = txs.size
        )
    }

    suspend fun getCustomerLedgerItemsWithBalance(customerId: String): List<LedgerItemWithBalance> = withContext(Dispatchers.IO) {
        val customer = customerDao.getCustomerById(customerId) ?: return@withContext emptyList()
        val txs = transactionDao.getTransactionsForCustomerList(customerId).sortedWith(
            compareBy<LedgerTransaction> { it.transactionDate }.thenBy { it.createdAt }
        )

        var runningBalance = if (customer.openingBalanceType == BalanceType.DEBIT.name) customer.openingBalance else -customer.openingBalance
        val result = mutableListOf<LedgerItemWithBalance>()

        for (tx in txs) {
            val isDebit = tx.transactionType == TransactionType.DEBIT.name
            val debitAmt = if (isDebit) tx.amount else 0.0
            val creditAmt = if (!isDebit) tx.amount else 0.0

            runningBalance += if (isDebit) tx.amount else -tx.amount

            result.add(
                LedgerItemWithBalance(
                    transaction = tx,
                    debitAmount = debitAmt,
                    creditAmount = creditAmt,
                    runningBalance = runningBalance
                )
            )
        }

        result
    }

    /**
     * Add transaction, validate, update ledger, and generate dynamic notification record.
     */
    suspend fun insertTransaction(
        tx: LedgerTransaction,
        customer: Customer,
        business: Business
    ): NotificationRecord? = withContext(Dispatchers.IO) {
        // 1. Insert transaction safely
        transactionDao.insertTransaction(tx)

        // 2. Recalculate balance
        val summary = calculateCustomerBalance(customer.id)

        // 3. Check notification settings
        val settings = settingsDao.getSettingsDirect(business.id)
            ?: NotificationSettings(id = UUID.randomUUID().toString(), businessId = business.id)

        val template = if (tx.transactionType == TransactionType.DEBIT.name) {
            settings.debitTemplate
        } else {
            settings.creditTemplate
        }

        val messageText = NotificationHelper.buildMessage(
            template = template,
            shopName = business.businessName,
            customerName = customer.name,
            amount = tx.amount,
            balance = summary.netBalance,
            transactionDate = tx.transactionDate,
            transactionType = tx.transactionType
        )

        val channel = when {
            settings.whatsappEnabled -> NotificationChannel.WHATSAPP.name
            settings.smsEnabled -> NotificationChannel.SMS.name
            else -> NotificationChannel.PUSH.name
        }

        val notifRecord = NotificationRecord(
            id = UUID.randomUUID().toString(),
            businessId = business.id,
            customerId = customer.id,
            transactionId = tx.id,
            channel = channel,
            title = "🏪 ${business.businessName}",
            message = messageText,
            status = NotificationStatus.SENT.name,
            sentAt = System.currentTimeMillis()
        )

        notificationDao.insertNotification(notifRecord)
        notifRecord
    }

    suspend fun deleteTransaction(tx: LedgerTransaction) = withContext(Dispatchers.IO) {
        transactionDao.deleteTransaction(tx)
    }

    // --- Notifications & Settings ---
    fun getNotificationsByBusiness(businessId: String): Flow<List<NotificationRecord>> =
        notificationDao.getNotificationsByBusiness(businessId).flowOn(Dispatchers.IO)

    fun getNotificationSettings(businessId: String): Flow<NotificationSettings?> =
        settingsDao.getSettingsByBusiness(businessId).flowOn(Dispatchers.IO)

    suspend fun saveNotificationSettings(settings: NotificationSettings) = withContext(Dispatchers.IO) {
        settingsDao.insertSettings(settings)
    }

    suspend fun retryNotification(notifId: String) = withContext(Dispatchers.IO) {
        // Mark as sent / update sentAt
        // Can be queried or refreshed
    }

    suspend fun insertNotification(record: NotificationRecord) = withContext(Dispatchers.IO) {
        notificationDao.insertNotification(record)
    }
}
