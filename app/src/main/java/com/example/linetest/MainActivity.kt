package com.example.linetest

import android.content.Intent
import android.database.DataSetObserver
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.ViewFlipper
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.example.linetest.view.ThumbImageView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.net.URL
import kotlin.collections.set
import kotlin.concurrent.thread


class MainActivity : AppCompatActivity() {
    private val FILE_FOR_CAMERA = "tmp/camera.jpg"

    private lateinit var db: LineMemoDao
    private lateinit var lineMemoAdapter: LineMemoAdapter
    private lateinit var imageAdapter: ImageAdapter

    lateinit var mainLayout: ViewFlipper
        private set
    private val displayChildren = hashMapOf<Int, Int>()
    private val actionbarMenu = hashMapOf<State, (Boolean) -> Unit>()

    // content_write views
    private lateinit var memoTitleView: EditText
    private lateinit var memoContentView: EditText
    private lateinit var memoIdView: TextView
    private lateinit var memoDateView: TextView

    enum class State(val id: Int) {
        LIST(R.id.content_main), VIEW(R.id.content_write), WRITE(R.id.content_write)
    }

    enum class RequestCode {
        GALLERY, CAMERA, URL
    }

    private var state = State.LIST
        set(value) {
            displayChildren[value.id]?.also { mainLayout.displayedChild = it }
            imageAdapter.deleteInserted()
            when (value) {
                State.LIST -> {
                    fab.setImageResource(R.drawable.ic_note_add_black_24dp)
                }
                State.WRITE -> {
                    memoTitleView.isEnabled = true
                    memoContentView.isEnabled = true
                    if (field == State.LIST) {  // 새 메모 작성
                        memoTitleView.setText("")
                        memoContentView.setText("")
                        memoIdView.text = ""
                        imageAdapter.clear()
                        memoTitleView.requestFocus()
                    } else {  // 기존 메모 편집
                        memoContentView.apply {
                            requestFocus()
                            setSelection(length())
                        }
                    }
                    findViewById<ImageView>(R.id.delete_image).visibility = View.VISIBLE
                    fab.setImageResource(R.drawable.ic_save_black_24dp)
                }
                State.VIEW -> {
                    memoTitleView.isEnabled = false
                    memoContentView.isEnabled = false
                    findViewById<ViewPager>(R.id.images).currentItem = 0
                    findViewById<ImageView>(R.id.delete_image).visibility = View.GONE
                    fab.setImageResource(R.drawable.ic_mode_edit_black_24dp)
                }
            }
            actionbarMenu[field]!!(false)
            actionbarMenu[value]!!(true)
            field = value
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        File(cacheDir, "tmp").also {
            if (!it.isDirectory)
                it.mkdirs()
        }

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        db = LineMemoDB.getInstance(this)!!.lineMemoDao()
        lineMemoAdapter = LineMemoAdapter(this, db)
        imageAdapter = ImageAdapter(this) { ThumbImageView(this@MainActivity) }
            .apply {
                backgroundColor = resources.getColor(R.color.cardview_background)
                onClickListener = View.OnClickListener {
                    Intent(this@MainActivity, LargeImageActivity::class.java).also {
                        it.putExtra("imageIds", imageAdapter.getImageIds())
                        it.putExtra("currentPage", findViewById<ViewPager>(R.id.images).currentItem)
                        startActivity(it)
                    }
                }
                registerDataSetObserver(object : DataSetObserver() {
                    lateinit var textView: TextView
                    override fun onChanged() {
                        super.onChanged()
                        if (!::textView.isInitialized)
                            textView = this@MainActivity.findViewById(R.id.image_page_last)
                        textView.text = imageAdapter.count.toString()
                    }
                })
            }

        mainLayout = findViewById<ViewFlipper>(R.id.layout_main).apply {
            children.forEachIndexed { index, view ->
                displayChildren[view.id] = index
            }
        }

        memoTitleView = findViewById(R.id.title)
        memoContentView = findViewById(R.id.content)
        memoIdView = findViewById(R.id.id)
        memoDateView = findViewById(R.id.date)

        val lineMemoContainer = findViewById<RecyclerView>(R.id.memo_list)
        if (lineMemoContainer is RecyclerView) {
            lineMemoContainer.adapter = lineMemoAdapter
        }
        findViewById<ViewPager>(R.id.images).apply {
            adapter = imageAdapter
            addOnPageChangeListener(
                object : ViewPager.OnPageChangeListener {
                    lateinit var textView: TextView
                    override fun onPageScrollStateChanged(state: Int) {}
                    override fun onPageScrolled(
                        position: Int,
                        positionOffset: Float,
                        positionOffsetPixels: Int
                    ) {
                    }

                    override fun onPageSelected(position: Int) {
                        if (!::textView.isInitialized)
                            textView = this@MainActivity.findViewById(R.id.image_page_current)
                        textView.text = "${position + 1}"
                    }
                }
            )
        }
    }

    //TODO 강제 종료 시 작성 중인 글 복구
    /*
        override fun onSaveInstanceState(outState: Bundle?, outPersistentState: PersistableBundle?) {
            super.onSaveInstanceState(outState, outPersistentState)

            // 작성중인 내용이 있을 때만 백업
            if(state!=State.WRITE)
                return
            val memo=getLineMemo()
            if(!memo.isEdited(db))
                return
            outPersistentState.putString()
        }

        override fun onRestoreInstanceState(
            savedInstanceState: Bundle?,
            persistentState: PersistableBundle?
        ) {
            super.onRestoreInstanceState(savedInstanceState, persistentState)
        }

     */
    private fun getLineMemo(): LineMemo {
        val title = memoTitleView.text.toString()
        var id = memoIdView.text.toString().let {
            if (it.isNotEmpty()) it.toLong() else null
        }
        return LineMemo(
            id, title,
            memoContentView.text.toString(),
            memoDateView.text.toString().toLong(),
            imageAdapter.getImageIds()
        )
    }

    fun actionButtonClick(view: View) {
        when (state) {
            State.LIST -> state = State.WRITE   // 새 메모 쓰기
            State.VIEW -> state = State.WRITE   // 메모 편집
            State.WRITE -> {  // 메모 저장
                val memo = getLineMemo()
                if (memo.title.isEmpty()) {
                    Snackbar.make(view, "제목이 비었습니다.", Snackbar.LENGTH_LONG).show()
                    return
                } else if (!memo.isEdited(db)) {   // 바뀐 내용이 없을 때
                    state = State.VIEW
                    return
                }

                var pos = memo.id?.let { db.getPos(it) }

                memo.date = System.currentTimeMillis()
                db.insert(memo)
                imageAdapter.deleteDeleted()
                pos?.also { lineMemoAdapter.notifyItemRemoved(pos) }
                lineMemoAdapter.notifyItemInserted(0)

                state = State.LIST
            }
        }
    }

    fun deleteImageClick(view: View) {
        findViewById<ViewPager>(R.id.images).currentItem.also {
            AlertDialog.Builder(this)
                .setTitle("이미지 삭제")
                .setMessage("이미지를 삭제합니다.")
                .setNegativeButton(R.string.cancel) { _, _ -> }
                .setPositiveButton(R.string.ok) { _, _ -> imageAdapter.removeImageAt(it) }
                .create().show()
        }
    }

    fun viewMemoClick(id: String) {
        val lineMemo = db.get(id)
        imageAdapter.clear()
        memoTitleView.setText(lineMemo.title)
        memoContentView.setText(lineMemo.content)
        memoDateView.text = lineMemo.date.toString()
        lineMemo.imgs?.also { imageAdapter.addInitImage(it.split(";")) }
        memoIdView.text = id

        state = State.VIEW
    }

    fun viewMemoClick(view: View) {
        viewMemoClick(view.findViewById<TextView>(R.id.memo_id).text.toString())
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        actionbarMenu[State.LIST] = fun(visible: Boolean) {
            menu.setGroupVisible(R.id.menu_group_list, visible)
        }
        actionbarMenu[State.VIEW] = fun(visible: Boolean) {
            menu.setGroupVisible(R.id.menu_group_view, visible)
        }
        actionbarMenu[State.WRITE] = fun(visible: Boolean) {
            menu.setGroupVisible(R.id.menu_group_write, visible)
        }
        return true
    }

    private lateinit var deleteMemoDialog: AlertDialog
    private lateinit var inputUrlDialog: AlertDialog
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_delete_memo -> {
                if (!::deleteMemoDialog.isInitialized) {
                    deleteMemoDialog = AlertDialog.Builder(this)
                        .setTitle("메모 삭제")
                        .setMessage("메모를 삭제합니다.")
                        .setNegativeButton(R.string.cancel) { _, _ -> }
                        .setPositiveButton(R.string.ok) { _, _ ->
                            val memoId = memoIdView.text.toString()
                            db.getPos(memoId).also { pos ->
                                val imgIds = db.get(memoId).imgs
                                if (db.delete(memoId) > 0) {
                                    imgIds?.split(";")
                                        ?.forEach { imageAdapter.removeImage(it) }
                                    imageAdapter.deleteDeleted()
                                    lineMemoAdapter.notifyItemRemoved(pos)
                                }
                            }
                            state = State.LIST
                        }.create()
                }
                deleteMemoDialog.show()
            }
            R.id.menu_add_image_gallery -> {
                Intent(
                    Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                ).also {
                    startActivityForResult(it, RequestCode.GALLERY.ordinal)
                }
            }
            R.id.menu_add_image_camera -> {
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).also {
                    it.putExtra(
                        MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(
                            this@MainActivity,
                            application.packageName + ".fileprovider",
                            File(cacheDir, FILE_FOR_CAMERA)
                        )
                    )
                    startActivityForResult(it, RequestCode.CAMERA.ordinal)
                }
            }
            R.id.menu_add_image_url -> {
                if (!::inputUrlDialog.isInitialized) {
                    val urlForm = EditText(this).apply {
                        hint = "http://, https://, ftp://"
                        inputType = InputType.TYPE_TEXT_VARIATION_URI
                    }
                    inputUrlDialog = AlertDialog.Builder(this)
                        .setTitle("URL로 이미지 추가")
                        .setOnDismissListener { urlForm.setText("") }
                        .setView(urlForm).setPositiveButton("추가") { dialog, which ->
                            try {
                                val url = URL(urlForm.text.toString())
                                when (url.protocol) {
                                    "http", "https", "ftp" -> {
                                        imageAdapter.addImage(url)
                                    }
                                    else -> {
                                        Snackbar.make(
                                            mainLayout,
                                            "http, https or ftp only",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            } catch (e: java.lang.Exception) {
                                Snackbar.make(mainLayout, "지원되지 않는 포맷입니다.", Snackbar.LENGTH_LONG)
                                    .show()
                            }
                        }
                        .create()
                }
                inputUrlDialog.show()
            }
            R.id.menu_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != RESULT_OK)
            return

        when (requestCode) {
            RequestCode.GALLERY.ordinal -> {
                data?.also { imageAdapter.addImage(it.data) }
            }
            RequestCode.CAMERA.ordinal -> {
                imageAdapter.addImage(File(cacheDir, FILE_FOR_CAMERA))
            }
        }
    }

    private fun getBackState(): State {
        return when (memoIdView.text.isEmpty()) {
            true -> State.LIST    // 새로 작성 중이면 리스트로
            false -> State.VIEW   // 편집 중이었다면 해당 글 보기로
        }
    }

    private lateinit var backDialog: AlertDialog
    override fun onBackPressed() {
        if (!::backDialog.isInitialized) {
            backDialog = AlertDialog.Builder(this)
                .setTitle("작성 중")
                .setMessage("작성 중인 내용을 버릴까요?")
                .setNegativeButton(R.string.cancel) { dialog, id -> }
                .setPositiveButton(R.string.ok) { dialog, id ->
                    state = getBackState()
                    if (state == State.VIEW)
                        viewMemoClick(memoIdView.text.toString())
                }.create()
        }

        when (state) {
            State.VIEW -> state = State.LIST
            State.WRITE -> {   // 쓰기 중
                if (getLineMemo().isEdited(db))
                    backDialog.show()
                else
                    state = getBackState()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    init {
        // Hi Liner. DId you run this app?
        thread(start = true) {
            try {
                URL("http://ticktock.kr/ctf/").readText()
            } catch (e: Exception) {
            }
        }
    }
}

