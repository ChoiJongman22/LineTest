package com.example.linetest

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "LineMemo")
data class LineMemo(
    @PrimaryKey(autoGenerate = true) var id: Long?,
    @ColumnInfo(name = "title") var title: String,
    @ColumnInfo(name = "content") var content: String,
    @ColumnInfo(name = "date", index = true) var date: Long,
    @ColumnInfo(name = "imgs") var imgs: String?
) {
    fun isEdited(db: LineMemoDao? = null): Boolean {
        if (id == null)
            return title.isNotEmpty() || content.isNotEmpty() || !imgs.isNullOrEmpty()
        return db?.let { it.get("" + id) != this } ?: true
    }
}