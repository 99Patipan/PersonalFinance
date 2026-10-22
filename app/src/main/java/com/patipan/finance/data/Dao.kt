package com.patipan.finance.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    @Query("SELECT * FROM wallets ORDER BY id") fun walletsFlow(): Flow<List<Wallet>>
    @Query("SELECT * FROM categories ORDER BY id") fun categoriesFlow(): Flow<List<Category>>
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC, id DESC") fun txnsFlow(): Flow<List<Txn>>
    @Query("SELECT * FROM budgets") fun budgetsFlow(): Flow<List<Budget>>

    @Query("SELECT * FROM wallets") suspend fun walletsOnce(): List<Wallet>
    @Query("SELECT * FROM categories") suspend fun categoriesOnce(): List<Category>
    @Query("SELECT * FROM transactions ORDER BY timestamp") suspend fun txnsOnce(): List<Txn>
    @Query("SELECT * FROM budgets") suspend fun budgetsOnce(): List<Budget>

    @Insert suspend fun insertWallet(w: Wallet): Long
    @Update suspend fun updateWallet(w: Wallet)
    @Query("DELETE FROM wallets WHERE id = :id") suspend fun deleteWallet(id: Long)
    @Query("SELECT COUNT(*) FROM transactions WHERE walletId = :id OR toWalletId = :id")
    suspend fun txnCountForWallet(id: Long): Int

    @Insert suspend fun insertCategory(c: Category): Long
    @Update suspend fun updateCategory(c: Category)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun deleteCategory(id: Long)
    @Query("UPDATE transactions SET categoryId = NULL WHERE categoryId = :id") suspend fun clearCategoryOnTxns(id: Long)

    @Insert suspend fun insertTxn(t: Txn): Long
    @Update suspend fun updateTxn(t: Txn)
    @Query("DELETE FROM transactions WHERE id = :id") suspend fun deleteTxn(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertBudget(b: Budget)
    @Query("DELETE FROM budgets WHERE categoryId = :id") suspend fun deleteBudget(id: Long)

    @Query("DELETE FROM transactions") suspend fun clearTxns()
    @Query("DELETE FROM wallets") suspend fun clearWallets()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWallets(l: List<Wallet>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertCategories(l: List<Category>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTxns(l: List<Txn>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBudgets(l: List<Budget>)
}
