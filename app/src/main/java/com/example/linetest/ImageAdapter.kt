package com.example.linetest

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.example.linetest.view.ImageInterface
import java.io.File
import java.net.URL
import java.util.*
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class ImageAdapter(val context: Context, val makeView: () -> ImageInterface) : PagerAdapter() {
    val IMG_DIR = File(context.filesDir, context.getString(R.string.imgs_path))
    var backgroundColor: Int? = null
    var onClickListener: View.OnClickListener? = null

    data class ImageItem(val img: ImageInterface, val id: String)

    val insertedImageId = arrayListOf<String>()
    val deletedImageId = arrayListOf<String>()

    private val imageList = arrayListOf<ImageItem>()
    private val loadingUri =
        Uri.parse("android.resource://${context.packageName}/drawable/loading")

    init {
        if (!IMG_DIR.isDirectory)
            IMG_DIR.mkdirs()
    }

    fun getImageIds(): String? {
        if (imageList.size == 0)
            return null
        val ret = StringBuilder(imageList.size * (imageList[0].id.length + 1))
        imageList.forEach { ret.append(it.id).append(';') }
        ret.setLength(ret.length - 1)
        return ret.toString()
    }

    fun addImage(uri: Uri) {
        val file = makeNewFile()
        context.contentResolver.openInputStream(uri).copyTo(file.outputStream())
        addImageView(file.toUri(), file.name)
        notifyDataSetChanged()
        insertedImageId.add(file.name)
    }

    fun addImage(file: File) {
        var rFile = makeNewFile()
        if (!file.renameTo(rFile))
            file.copyTo(rFile)
        addImageView(rFile.toUri(), rFile.name)
        notifyDataSetChanged()
        insertedImageId.add(rFile.name)
    }

    fun addImage(url: URL) {
        val file = makeNewFile()
        addImageView(loadingUri, file.name)
        notifyDataSetChanged()

        thread(start = true) {
            val activity = context as Activity
            try {
                file.writeBytes(url.readBytes())
                if (!ImageInterface.getImageOpt(
                        context,
                        file.toUri()
                    )   //mime type을 알 수 없거나 image가 아닐 때
                        .outMimeType!!.startsWith("image")
                )
                    throw Exception()

                insertedImageId.add(file.name)
                activity.runOnUiThread {
                    imageList.find { it.id == file.name }?.img?.setImageURI(file.toUri())
                    notifyDataSetChanged()
                }
            } catch (e: Exception) {
                activity.runOnUiThread { removeImage(file.name) }
                file.delete()
            }
        }
    }

    fun addImage(id: String) {
        addImageView(File(IMG_DIR, id).toUri(), id)
        notifyDataSetChanged()
        insertedImageId.add(id)
    }

    fun addInitImage(ids: List<String>) {
        deleted = false
        deletedImageId.clear()
        insertedImageId.clear()
        ids.forEach { addImageView(File(IMG_DIR, it).toUri(), it) }
        notifyDataSetChanged()
    }

    private fun addImageView(uri: Uri, id: String) {
        val img = makeView()
        img.setImageURI(uri)
        backgroundColor?.also { img.setBackgroundColor(it) }
        onClickListener?.also { img.setOnClickListener(it) }
        imageList.add(ImageItem(img, id))
    }

    private fun makeNewFile(): File {
        val filename = UUID.randomUUID().toString()
        val file = File(IMG_DIR, filename)
        file.createNewFile()
        return file
    }

    fun clear() {
        imageList.clear()
        notifyDataSetChanged()
    }

    fun removeImageAt(position: Int): Boolean {
        return removeImage(imageList[position].id)
    }

    fun removeImage(id: String): Boolean {
        return imageList.find { it.id == id }?.let {
            imageList.remove(it)
            notifyDataSetChanged()
            deletedImageId.add(id)
            true
        } ?: false
    }

    private fun deleteImageFile(id: String) {
        File(IMG_DIR, id).also { file ->
            if (file.isFile)
                file.delete()
        }
    }

    private var deleted = false
    fun deleteInserted() {
        if (!deleted)
            insertedImageId.forEach { deleteImageFile(it) }
        deleted = true
    }

    fun deleteDeleted() {
        if (!deleted)
            deletedImageId.forEach { deleteImageFile(it) }
        deleted = true
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getItemPosition(obj: Any): Int {
        val pos = imageList.indexOf(obj)
        if (pos >= 0)
            return pos
        return POSITION_NONE
    }

    lateinit var viewPager: ViewPager
    lateinit var viewParent: View
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if (!::viewParent.isInitialized) {
            viewPager = container as ViewPager
            viewParent = container.parent as View
            if (imageList.size == 0) {
                viewParent.visibility = View.GONE
                notifyDataSetChanged()
                return imageList.getOrNull(0)?.img ?: container
            }
        }

        val img = imageList[position].img as View
        if (img.parent == null)
            container.addView(img)
        return img
    }

    override fun getCount(): Int {
        if (::viewParent.isInitialized) {
            if (imageList.size == 0) {
                viewParent.visibility = View.GONE
            } else {
                viewParent.visibility = View.VISIBLE
            }
        } else if (imageList.size == 0)
            return 1

        return imageList.size
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view.equals(`object`)
    }

    fun saveInstanceState(): HashMap<String, String> {
        val data = HashMap<String, String>()
        data["ImageList"] = imageList.run {
            var ret = StringBuilder()
            forEach { ret.append(it.id) }
            ret.toString()
        }
        data["insertedImageId"] = insertedImageId.joinToString(";")
        data["deletedImageId"] = deletedImageId.joinToString(";")
        return data
    }

    fun restoreInstanceState(data: HashMap<String, String>) {
        if (imageList.isEmpty() && data["ImageList"] != null) {
            addInitImage(data["imageList"]?.split(";")!!)
        }
        data["insertedImageId"]?.split(";")?.forEach { insertedImageId.add(it) }
        data["deletedImageId"]?.split(";")?.forEach { deletedImageId.add(it) }
    }
}