package com.patipan.finance.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val INCOME = "INCOME"
const val EXPENSE = "EXPENSE"
const val TRANSFER = "TRANSFER"

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val color: Long,
    val initialCents: Long
)

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String,
    val emoji: String
)

@Entity(
    tableName = "transactions",
    indices = [Index("timestamp"), Index("walletId"), Index("categoryId")]
)
data class Txn(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val amountCents: Long,
    val categoryId: Long?,
    val walletId: Long,
    val toWalletId: Long?,
    val timestamp: Long,
    val detail: String,
    val note: String,
    val receiptPath: String?
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val categoryId: Long,
    val limitCents: Long
)
