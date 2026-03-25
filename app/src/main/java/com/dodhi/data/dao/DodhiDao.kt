package com.dodhi.data.dao

import androidx.room.*
import com.dodhi.data.model.Customer
import com.dodhi.data.model.DeliveryRecord
import com.dodhi.data.model.Payment
import com.dodhi.data.model.MilkSource
import kotlinx.coroutines.flow.Flow

@Dao
interface DodhiDao {
    @Query("SELECT * FROM customers")
    fun getAllCustomers(): Flow<List<Customer>>

    @Query("SELECT * FROM customers WHERE id = :id")
    fun getCustomerById(id: Long): Flow<Customer?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: Customer)

    @Delete
    suspend fun deleteCustomer(customer: Customer)

    @Query("SELECT * FROM delivery_records WHERE customerId = :customerId ORDER BY date DESC")
    fun getRecordsForCustomer(customerId: Long): Flow<List<DeliveryRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: DeliveryRecord)

    @Query("SELECT * FROM delivery_records WHERE date >= :start AND date <= :end")
    fun getRecordsInPeriod(start: Long, end: Long): Flow<List<DeliveryRecord>>

    // Payments
    @Query("SELECT * FROM payments WHERE customerId = :customerId ORDER BY date DESC")
    fun getPaymentsForCustomer(customerId: Long): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    // Milk Sources
    @Query("SELECT * FROM milk_sources ORDER BY date DESC")
    fun getAllMilkSources(): Flow<List<MilkSource>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilkSource(source: MilkSource)
    @Query("SELECT * FROM payments WHERE date BETWEEN :start AND :end")
    fun getPaymentsForCustomerInRange(start: Long, end: Long): Flow<List<Payment>>

    @Query("SELECT * FROM milk_sources WHERE date BETWEEN :start AND :end")
    fun getMilkSourcesInPeriod(start: Long, end: Long): Flow<List<MilkSource>>
}
