package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): UserProfile?

    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileByEmail(email: String): UserProfile?

    @Query("SELECT * FROM profiles WHERE mobile = :mobile LIMIT 1")
    suspend fun getProfileByMobile(mobile: String): UserProfile?

    @Query("SELECT * FROM profiles LIMIT 1")
    fun getFirstProfileFlow(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)

    @Update
    suspend fun updateProfile(profile: UserProfile)
}

@Dao
interface BusinessDao {
    @Query("SELECT * FROM businesses WHERE ownerId = :ownerId ORDER BY createdAt ASC")
    fun getBusinessesByOwner(ownerId: String): Flow<List<Business>>

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    suspend fun getBusinessById(id: String): Business?

    @Query("SELECT * FROM businesses WHERE id = :id LIMIT 1")
    fun getBusinessFlowById(id: String): Flow<Business?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBusiness(business: Business)

    @Update
    suspend fun updateBusiness(business: Business)

    @Delete
    suspend fun deleteBusiness(business: Business)
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers WHERE businessId = :businessId ORDER BY name ASC")
    fun getCustomersByBusiness(businessId: String): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    suspend fun getCustomerById(id: String): Customer?

    @Query("SELECT * FROM customers WHERE id = :id LIMIT 1")
    fun getCustomerFlowById(id: String): Flow<Customer?>

    @Query("SELECT * FROM customers WHERE mobile = :mobile LIMIT 1")
    suspend fun getCustomerByMobile(mobile: String): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Update
    suspend fun updateCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("DELETE FROM customers WHERE id = :id")
    suspend fun deleteCustomerById(id: String)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions WHERE businessId = :businessId ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsByBusiness(businessId: String): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY transactionDate ASC, createdAt ASC")
    fun getTransactionsByCustomerAsc(customerId: String): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId ORDER BY transactionDate DESC, createdAt DESC")
    fun getTransactionsByCustomerDesc(customerId: String): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM transactions WHERE customerId = :customerId")
    suspend fun getTransactionsForCustomerList(customerId: String): List<LedgerTransaction>

    @Query("SELECT * FROM transactions WHERE businessId = :businessId AND transactionDate >= :startDate AND transactionDate <= :endDate ORDER BY transactionDate DESC")
    fun getTransactionsByDateRange(businessId: String, startDate: Long, endDate: Long): Flow<List<LedgerTransaction>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun getTransactionById(id: String): LedgerTransaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: LedgerTransaction)

    @Update
    suspend fun updateTransaction(transaction: LedgerTransaction)

    @Delete
    suspend fun deleteTransaction(transaction: LedgerTransaction)

    @Query("DELETE FROM transactions WHERE customerId = :customerId")
    suspend fun deleteTransactionsByCustomer(customerId: String)
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE businessId = :businessId ORDER BY createdAt DESC")
    fun getNotificationsByBusiness(businessId: String): Flow<List<NotificationRecord>>

    @Query("SELECT * FROM notifications WHERE customerId = :customerId ORDER BY createdAt DESC")
    fun getNotificationsByCustomer(customerId: String): Flow<List<NotificationRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationRecord)

    @Update
    suspend fun updateNotification(notification: NotificationRecord)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotificationById(id: String)
}

@Dao
interface NotificationSettingsDao {
    @Query("SELECT * FROM notification_settings WHERE businessId = :businessId LIMIT 1")
    fun getSettingsByBusiness(businessId: String): Flow<NotificationSettings?>

    @Query("SELECT * FROM notification_settings WHERE businessId = :businessId LIMIT 1")
    suspend fun getSettingsDirect(businessId: String): NotificationSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: NotificationSettings)

    @Update
    suspend fun updateSettings(settings: NotificationSettings)
}
