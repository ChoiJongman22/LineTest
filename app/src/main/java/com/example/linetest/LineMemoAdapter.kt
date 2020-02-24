package com.example.linetest

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.example.linetest.view.ThumbImageView
import java.io.File
import java.text.SimpleDateFormat

class LineMemoAdapter(val context: Context, val lineMemoDao: LineMemoDao) :
    RecyclerView.Adapter<LineMemoAdapter.Holder>() {
    val IMG_DIR = File(context.filesDir, context.getString(R.string.imgs_path))

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(context).inflate(R.layout.item_memo, parent, false))
    }

    override fun getItemCount(): Int {
        return lineMemoDao.getAllCount()
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(lineMemoDao.getByPos(position))
    }

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val id = view.findViewById<TextView>(R.id.memo_id)
        val thumb = view.findViewById<ThumbImageView>(R.id.memo_thumb)
        val title = view.findViewById<TextView>(R.id.memo_title)
        val content = view.findViewById<TextView>(R.id.memo_content)
        val date = view.findViewById<TextView>(R.id.memo_date)

        fun bind(lineMemo: LineMemo) {
            lineMemo.imgs?.split(";")?.first()?.also {
                thumb.apply {
                    setImageURI(File(IMG_DIR, it).toUri())
                    setBackgroundColor(resources.getColor(R.color.cardview_background))
                    visibility = View.VISIBLE
                }
            }
            id.text = "" + lineMemo.id
            title.text = lineMemo.title
            content.text = lineMemo.content.split("\n").joinToString(" ")
            date.text = dateFormat.format(lineMemo.date)
        }
    }
}