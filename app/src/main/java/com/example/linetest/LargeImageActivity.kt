package com.example.linetest

import android.app.Activity
import android.os.Bundle
import android.view.View
import androidx.viewpager.widget.ViewPager
import com.example.linetest.view.LargeImageView

class LargeImageActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_large_image)

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE

        val pager = findViewById<ViewPager>(R.id.large_image_pager)
        val imageAdapter = ImageAdapter(this) { LargeImageView(this) }
        pager.adapter = imageAdapter
        imageAdapter.addInitImage(intent.getStringExtra("imageIds").split(";"))
        pager.currentItem = intent.getIntExtra("currentPage", 0)
    }
}