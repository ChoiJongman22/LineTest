package com.example.linetest.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import com.bumptech.glide.Glide

class ThumbImageView : ImageView, ImageInterface {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    override fun setImageURI(uri: Uri) {
        Glide.with(context.applicationContext).load(uri).into(this)
    }
}