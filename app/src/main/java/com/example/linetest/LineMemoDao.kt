package com.example.linetest

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface LineMemoDao {
    @Query("SELECT * FROM LineMemo ORDER BY date DESC")
    fun getAll(): List<LineMemo>

    @Query("SELECT count(*) FROM LineMemo")
    fun getAllCount(): Int

    @Query("SELECT * FROM LineMemo WHERE id=:id")
    fun get(id: String): LineMemo

    @Query("SELECT * FROM LineMemo WHERE id=:id")
    fun get(id: Long): LineMemo

    @Query("SELECT * FROM LineMemo ORDER BY date DESC LIMIT :pos, 1")
    fun getByPos(pos: Int): LineMemo

    @Query("SELECT count(*) FROM LineMemo WHERE date>(SELECT date FROM linememo WHERE id=:id) ORDER BY date")
    fun getPos(id: Long): Int

    @Query("SELECT count(*) FROM LineMemo WHERE date>(SELECT date FROM linememo WHERE id=:id) ORDER BY date")
    fun getPos(id: String): Int

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    fun insert(memo: LineMemo): Long

    @Query("DELETE from LineMemo WHERE id=:id")
    fun delete(id: Long): Int

    @Query("DELETE from LineMemo WHERE id=:id")
    fun delete(id: String): Int
}