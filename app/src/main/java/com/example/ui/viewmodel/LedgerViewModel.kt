package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DataGenerator
import com.example.data.local.LedgerDatabase
import com.example.data.model.*
import com.example.data.repository.CustomerBalanceSummary
import com.example.data.repository.LedgerRepository
import com.example.util.DateUtils
import com.example.util.LedgerItemWithBalance
import com.example.util.NotificationHelper
import com.example.util.PdfExporter
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

enum class CustomerFilter {
    ALL,
    YOU_WILL_GET, // Debit
    YOU_WILL_GIVE, // Credit
    SETTLED
}

enum class ReportPeriod {
    DAILY,
    WEEKLY,
    MONTHLY,
    ALL_TIME,
    CUSTOM
}

data class TransactionPreview(
    val customer: Customer,
    val transactionType: TransactionType,
    val amount: Double,
    val previousBalance: Double,
    val newBalance: Double
)

class LedgerViewModel(application: Application) : AndroidViewModel(application) {
    private val db = LedgerDatabase.getDatabase(application)
    val repository = LedgerRepository(db)

    // Current Auth User
    val currentProfile: StateFlow<UserProfile?> = repository.getProfileFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    // Businesses list
    private val _businesses = MutableStateFlow<List<Business>>(emptyList())
    val businesses: StateFlow<List<Business>> = _businesses.asStateFlow()

    // Selected Business
    private val _activeBusinessId = MutableStateFlow<String?>(null)
    val activeBusinessId: StateFlow<String?> = _activeBusinessId.asStateFlow()

    val activeBusiness: StateFlow<Business?> = combine(businesses, _activeBusinessId) { list, id ->
        list.find { it.id == id } ?: list.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Customers for active business
    private val _customers = MutableStateFlow<List<Customer>>(emptyList())
    val customers: StateFlow<List<Customer>> = _customers.asStateFlow()

    // Customer balance summaries
    private val _customerSummaries = MutableStateFlow<Map<String, CustomerBalanceSummary>>(emptyMap())
    val customerSummaries: StateFlow<Map<String, CustomerBalanceSummary>> = _customerSummaries.asStateFlow()

    // Transactions for active business
    private val _transactions = MutableStateFlow<List<LedgerTransaction>>(emptyList())
    val transactions: StateFlow<List<LedgerTransaction>> = _transactions.asStateFlow()

    // Notifications for active business
    private val _notifications = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val notifications: StateFlow<List<NotificationRecord>> = _notifications.asStateFlow()

    // Notification settings for active business
    private val _notificationSettings = MutableStateFlow<NotificationSettings?>(null)
    val notificationSettings: StateFlow<NotificationSettings?> = _notificationSettings.asStateFlow()

    // Active Customer for Ledger View
    private val _selectedCustomerId = MutableStateFlow<String?>(null)
    val selectedCustomerId: StateFlow<String?> = _selectedCustomerId.asStateFlow()

    val selectedCustomer: StateFlow<Customer?> = combine(customers, _selectedCustomerId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _selectedCustomerLedgerItems = MutableStateFlow<List<LedgerItemWithBalance>>(emptyList())
    val selectedCustomerLedgerItems: StateFlow<List<LedgerItemWithBalance>> = _selectedCustomerLedgerItems.asStateFlow()

    // Filters & Search
    val customerSearchQuery = MutableStateFlow("")
    val customerFilter = MutableStateFlow(CustomerFilter.ALL)

    val filteredCustomers: StateFlow<List<CustomerBalanceSummary>> = combine(
        customers,
        _customerSummaries,
        customerSearchQuery,
        customerFilter
    ) { custList, summaries, query, filter ->
        custList.mapNotNull { summaries[it.id] }
            .filter { summary ->
                val matchesQuery = summary.customer.name.contains(query, ignoreCase = true) ||
                        summary.customer.mobile.contains(query, ignoreCase = true) ||
                        summary.customer.address.contains(query, ignoreCase = true)

                val matchesFilter = when (filter) {
                    CustomerFilter.ALL -> true
                    CustomerFilter.YOU_WILL_GET -> summary.netBalance > 0.001
                    CustomerFilter.YOU_WILL_GIVE -> summary.netBalance < -0.001
                    CustomerFilter.SETTLED -> kotlin.math.abs(summary.netBalance) <= 0.001
                }
                matchesQuery && matchesFilter
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI Message / Toast Flow
    private val _uiEvent = MutableSharedFlow<String>()
    val uiEvent: SharedFlow<String> = _uiEvent.asSharedFlow()

    // Security & App Settings
    val isAppLocked = MutableStateFlow(false)
    val isDarkMode = MutableStateFlow(false)
    val appLanguage = MutableStateFlow("English")

    init {
        viewModelScope.launch {
            // Seed initial data if empty
            DataGenerator.populateInitialDataIfEmpty(db)
            observeData()
        }
    }

    private fun observeData() {
        viewModelScope.launch {
            currentProfile.collectLatest { profile ->
                val ownerId = profile?.id ?: DataGenerator.DEFAULT_USER_ID
                repository.getBusinessesByOwner(ownerId).collectLatest { bizList ->
                    _businesses.value = bizList
                    if (_activeBusinessId.value == null || bizList.none { it.id == _activeBusinessId.value }) {
                        _activeBusinessId.value = bizList.firstOrNull()?.id
                    }
                }
            }
        }

        viewModelScope.launch {
            _activeBusinessId.collectLatest { bizId ->
                if (bizId != null) {
                    launch {
                        repository.getCustomersByBusiness(bizId).collectLatest { custList ->
                            _customers.value = custList
                            recalculateAllCustomerBalances(custList)
                        }
                    }
                    launch {
                        repository.getTransactionsByBusiness(bizId).collectLatest { txList ->
                            _transactions.value = txList
                            recalculateAllCustomerBalances(_customers.value)
                        }
                    }
                    launch {
                        repository.getNotificationsByBusiness(bizId).collectLatest { notifs ->
                            _notifications.value = notifs
                        }
                    }
                    launch {
                        repository.getNotificationSettings(bizId).collectLatest { settings ->
                            _notificationSettings.value = settings
                        }
                    }
                }
            }
        }

        viewModelScope.launch {
            selectedCustomerId.collectLatest { custId ->
                if (custId != null) {
                    loadCustomerLedger(custId)
                } else {
                    _selectedCustomerLedgerItems.value = emptyList()
                }
            }
        }
    }

    fun switchBusiness(businessId: String) {
        _activeBusinessId.value = businessId
        _selectedCustomerId.value = null
    }

    fun selectCustomerForLedger(customerId: String) {
        _selectedCustomerId.value = customerId
        loadCustomerLedger(customerId)
    }

    fun loadCustomerLedger(customerId: String) {
        viewModelScope.launch {
            val items = repository.getCustomerLedgerItemsWithBalance(customerId)
            _selectedCustomerLedgerItems.value = items
        }
    }

    private suspend fun recalculateAllCustomerBalances(customers: List<Customer>) {
        val map = mutableMapOf<String, CustomerBalanceSummary>()
        for (c in customers) {
            map[c.id] = repository.calculateCustomerBalance(c.id)
        }
        _customerSummaries.value = map
    }

    // --- Business Actions ---
    fun createBusiness(
        name: String,
        address: String,
        city: String,
        state: String,
        pinCode: String,
        mobile: String,
        email: String,
        gstNumber: String,
        upiId: String,
        logoUrl: String = ""
    ) {
        viewModelScope.launch {
            val ownerId = currentProfile.value?.id ?: DataGenerator.DEFAULT_USER_ID
            val newBiz = Business(
                id = UUID.randomUUID().toString(),
                ownerId = ownerId,
                businessName = name.trim(),
                logoUrl = logoUrl,
                address = address.trim(),
                city = city.trim(),
                state = state.trim(),
                country = "India",
                pinCode = pinCode.trim(),
                mobile = mobile.trim(),
                email = email.trim(),
                gstNumber = gstNumber.trim(),
                upiId = upiId.trim()
            )
            repository.saveBusiness(newBiz)
            _activeBusinessId.value = newBiz.id
            _uiEvent.emit("Business '${newBiz.businessName}' created successfully!")
        }
    }

    fun updateBusinessProfile(business: Business) {
        viewModelScope.launch {
            repository.updateBusiness(business)
            _uiEvent.emit("Business profile updated successfully!")
        }
    }

    // --- Customer Actions ---
    fun addCustomer(
        name: String,
        mobile: String,
        alternateMobile: String = "",
        address: String = "",
        openingBalance: Double = 0.0,
        openingBalanceType: BalanceType = BalanceType.DEBIT,
        notes: String = ""
    ) {
        val bizId = _activeBusinessId.value ?: return
        viewModelScope.launch {
            val customer = Customer(
                id = UUID.randomUUID().toString(),
                businessId = bizId,
                name = name.trim(),
                mobile = mobile.trim(),
                alternateMobile = alternateMobile.trim(),
                address = address.trim(),
                openingBalance = openingBalance,
                openingBalanceType = openingBalanceType.name,
                notes = notes.trim()
            )
            repository.saveCustomer(customer)
            _uiEvent.emit("Customer '${customer.name}' added successfully!")
        }
    }

    fun updateCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.updateCustomer(customer)
            _uiEvent.emit("Customer details updated!")
        }
    }

    fun deleteCustomer(customerId: String) {
        viewModelScope.launch {
            repository.deleteCustomer(customerId)
            if (_selectedCustomerId.value == customerId) {
                _selectedCustomerId.value = null
            }
            _uiEvent.emit("Customer and associated entries deleted.")
        }
    }

    // --- Transaction Preview & Insertion ---
    suspend fun getTransactionPreview(
        customerId: String,
        type: TransactionType,
        amount: Double
    ): TransactionPreview? {
        val customer = customers.value.find { it.id == customerId } ?: return null
        val currentSummary = repository.calculateCustomerBalance(customerId)
        val prevBalance = currentSummary.netBalance
        val newBalance = if (type == TransactionType.DEBIT) {
            prevBalance + amount
        } else {
            prevBalance - amount
        }
        return TransactionPreview(
            customer = customer,
            transactionType = type,
            amount = amount,
            previousBalance = prevBalance,
            newBalance = newBalance
        )
    }

    fun addTransaction(
        customerId: String,
        type: TransactionType,
        amount: Double,
        description: String,
        paymentMode: PaymentMode,
        referenceNumber: String = "",
        transactionDate: Long = System.currentTimeMillis(),
        onSuccess: (NotificationRecord?) -> Unit = {}
    ) {
        val biz = activeBusiness.value ?: return
        val customer = customers.value.find { it.id == customerId } ?: return

        viewModelScope.launch {
            val tx = LedgerTransaction(
                id = UUID.randomUUID().toString(),
                businessId = biz.id,
                customerId = customerId,
                transactionType = type.name,
                amount = amount,
                description = description.trim(),
                paymentMode = paymentMode.displayName,
                referenceNumber = referenceNumber.trim(),
                transactionDate = transactionDate,
                createdBy = currentProfile.value?.fullName ?: "Owner"
            )

            val notif = repository.insertTransaction(tx, customer, biz)
            loadCustomerLedger(customerId)
            _uiEvent.emit("${if (type == TransactionType.DEBIT) "Debit entry" else "Payment entry"} saved successfully!")
            onSuccess(notif)
        }
    }

    fun deleteTransaction(transaction: LedgerTransaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            loadCustomerLedger(transaction.customerId)
            _uiEvent.emit("Transaction deleted.")
        }
    }

    // --- Notification Settings & Templates ---
    fun updateNotificationSettings(settings: NotificationSettings) {
        viewModelScope.launch {
            repository.saveNotificationSettings(settings)
            _notificationSettings.value = settings
            _uiEvent.emit("Notification settings saved!")
        }
    }

    fun resendNotification(notification: NotificationRecord, applicationContext: Application) {
        viewModelScope.launch {
            val customer = customers.value.find { it.id == notification.customerId }
            val phone = customer?.mobile ?: ""
            if (notification.channel == NotificationChannel.WHATSAPP.name) {
                NotificationHelper.openWhatsApp(applicationContext, phone, notification.message)
            } else {
                NotificationHelper.openSms(applicationContext, phone, notification.message)
            }
        }
    }

    // --- PDF Export & Print ---
    fun generateCustomerPdf(customerId: String, onGenerated: (File) -> Unit) {
        val biz = activeBusiness.value ?: return
        val customer = customers.value.find { it.id == customerId } ?: return

        viewModelScope.launch {
            val items = repository.getCustomerLedgerItemsWithBalance(customerId)
            val summary = repository.calculateCustomerBalance(customerId)

            val pdf = PdfExporter.generateLedgerPdf(
                context = getApplication(),
                business = biz,
                customer = customer,
                items = items,
                totalDebit = summary.totalDebit,
                totalCredit = summary.totalCredit,
                netBalance = summary.netBalance
            )

            if (pdf != null) {
                onGenerated(pdf)
            } else {
                _uiEvent.emit("Failed to create PDF statement.")
            }
        }
    }

    // --- Authentication Actions ---
    fun login(emailOrMobile: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val profile = db.profileDao().getProfileByEmail(emailOrMobile.trim())
                ?: db.profileDao().getProfileByMobile(emailOrMobile.trim())
            if (profile != null) {
                onResult(true, "Welcome back, ${profile.fullName}!")
            } else {
                // Auto create or sample profile
                val newP = UserProfile(
                    id = UUID.randomUUID().toString(),
                    fullName = "Shop Owner",
                    email = if (emailOrMobile.contains("@")) emailOrMobile else "owner@ledger.in",
                    mobile = if (!emailOrMobile.contains("@")) emailOrMobile else "9876543210"
                )
                db.profileDao().insertProfile(newP)
                onResult(true, "Signed in as ${newP.fullName}")
            }
        }
    }

    fun signup(fullName: String, email: String, mobile: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val newP = UserProfile(
                id = UUID.randomUUID().toString(),
                fullName = fullName.trim(),
                email = email.trim(),
                mobile = mobile.trim(),
                passwordHash = pass
            )
            db.profileDao().insertProfile(newP)
            onResult(true, "Account created successfully!")
        }
    }

    fun setPinLock(pin: String?) {
        viewModelScope.launch {
            val p = currentProfile.value ?: return@launch
            val updated = p.copy(pinLock = pin)
            db.profileDao().updateProfile(updated)
            _uiEvent.emit(if (pin != null) "PIN Lock enabled" else "PIN Lock disabled")
        }
    }
}
