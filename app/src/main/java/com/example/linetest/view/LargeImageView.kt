package com.example.linetest.view

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.*
import com.github.chrisbanes.photoview.PhotoView


class LargeImageView : LinearLayout, ImageInterface {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    private val MIN_SCALE = 1f
    private val MAX_SCALE = 10f

    init {
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        gravity = Gravity.CENTER
    }

    override fun setImageURI(uri: Uri) {
        val opt = ImageInterface.getImageOpt(context, uri)
        addView(when (opt.outMimeType) {
            "image/gif" -> { // SubsamplingScaleImageView 는 gif 애니메이션 처리 못함
                PhotoView(context).apply {
                    layoutParams =
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    scaleType = ImageView.ScaleType.CENTER_INSIDE
                    minimumScale = MIN_SCALE
                    maximumScale = MAX_SCALE
                    Glide.with(context.applicationContext).load(uri).into(this)
                }
            }
            else -> {
                SubsamplingScaleImageView(context).apply {
                    layoutParams =
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    orientation = ORIENTATION_USE_EXIF
                    setMinimumScaleType(SCALE_TYPE_CUSTOM)
                    minScale = this@LargeImageView.MIN_SCALE
                    maxScale = this@LargeImageView.MAX_SCALE

                    // 이미지가 뷰 크기보다 클 경우, 화면 크기를 미니멈 크기로
                    setOnImageEventListener(object : DefaultOnImageEventListener() {
                        override fun onReady() {
                            if (width < opt.outWidth || height < opt.outHeight) {
                                setMinimumScaleType(SCALE_TYPE_CENTER_INSIDE)
                                resetScaleAndCenter()
                            }
                        }
                    })
                    setImage(ImageSource.uri(uri))
                }
            }
        })
    }
}