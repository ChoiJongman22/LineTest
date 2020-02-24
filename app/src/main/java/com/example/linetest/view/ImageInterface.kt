package com.example.linetest.view

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import java.io.InputStream

interface ImageInterface {
    fun setImageURI(uri: Uri)

    fun setBackgroundColor(color: Int)
    fun setOnClickListener(l: View.OnClickListener?)

    companion object {
        fun getImageOpt(context: Context, uri: Uri): BitmapFactory.Options {
            val opt = BitmapFactory.Options()
            opt.inJustDecodeBounds = true
            val istream: InputStream = context.contentResolver.openInputStream(uri)
            BitmapFactory.decodeStream(istream, null, opt)
            istream.close()
            return opt
        }
    }
}