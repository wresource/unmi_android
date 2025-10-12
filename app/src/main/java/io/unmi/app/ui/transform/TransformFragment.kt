package io.unmi.app.ui.transform

import android.graphics.Rect
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.unmi.app.R
import io.unmi.app.databinding.FragmentTransformBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.text.InputType

/**
 * Fragment that demonstrates a responsive layout pattern where the format of the content
 * transforms depending on the size of the screen. Specifically this Fragment shows items in
 * the [RecyclerView] using LinearLayoutManager in a small screen
 * and shows items using GridLayoutManager in a large screen.
 */
class TransformFragment : Fragment() {

    private var _binding: FragmentTransformBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    // Add nullable view fields for add buttons
    private var btnAdd: View? = null
    private var fabAdd: View? = null

    private val viewModel: TransformViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private val adapter = DomainAdapter { domain ->
        val price = when {
            domain.endsWith(".com", true) -> 10
            domain.endsWith(".org", true) -> 10
            domain.endsWith(".net", true) -> 11
            else -> null
        }
        price?.let {
            Toast.makeText(requireContext(), "$domain renew price：${it} Dollar", Toast.LENGTH_SHORT).show()
        }
    }
    private lateinit var dbHelper: DomainDbHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = DomainDbHelper(requireContext().applicationContext)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransformBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        // Clear cached views to avoid leaks
        btnAdd = null
        fabAdd = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = binding.recyclerviewTransform
        recyclerView.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val position = vh.bindingAdapterPosition
                val name = adapter.currentList.getOrNull(position) ?: return
                lifecycleScope.launch(Dispatchers.IO) {
                    dbHelper.deleteByName(name)
                    withContext(Dispatchers.Main) { refreshList() }
                }
            }
        }).attachToRecyclerView(recyclerView)

        // 统一卡片上下间距（与 fragment_transform.xml 的 padding 配合）
        val spacing = (8 * resources.displayMetrics.density).toInt() // 8dp
        recyclerView.addItemDecoration(object : RecyclerView.ItemDecoration() {
            override fun getItemOffsets(outRect: Rect, v: View, parent: RecyclerView, state: RecyclerView.State) {
                outRect.top = spacing
                outRect.bottom = spacing
            }
        })

        // Safely find views for add buttons (may be absent in some layouts)
        btnAdd = view.findViewById(R.id.btnAddDomain)
        fabAdd = view.findViewById(R.id.fabAddDomain)
        btnAdd?.setOnClickListener { showAddDomainDialog() }
        fabAdd?.setOnClickListener { showAddDomainDialog() }

        refreshList()
    }

    private fun showAddDomainDialog() {
        val context = requireContext()
        val inputLayout = TextInputLayout(context).apply {
            hint = "please input a domain name like: example.com）"
            isHintEnabled = true
        }
        val edit = TextInputEditText(context).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            setSingleLine(true)
            gravity = Gravity.START
        }
        inputLayout.addView(edit)

        val container = FrameLayout(context).apply {
            val padding = (20 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, 0)
            addView(inputLayout)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("add domain")
            .setView(container)
            .setPositiveButton("add") { d, _ ->
                val name = edit.text?.toString()?.trim().orEmpty()
                if (name.isEmpty() || !name.contains('.')) {
                    Toast.makeText(context, "invalid domain name", Toast.LENGTH_SHORT).show()
                } else {
                    lifecycleScope.launch(Dispatchers.IO) {
                        dbHelper.insertOrIgnore(name.lowercase())
                        withContext(Dispatchers.Main) { refreshList() }
                    }
                }
                d.dismiss()
            }
            .setNegativeButton("cancel", null)
            .show()
    }

    private fun refreshList() {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = dbHelper.getAll()
            withContext(Dispatchers.Main) {
                render(data)
            }
        }
    }

    private fun render(domains: List<String>) {
        adapter.submitList(domains)
        val empty = domains.isEmpty()
        // 空列表：只显示中间“添加域名”按钮
        binding.recyclerviewTransform.isGone = empty
        btnAdd?.isVisible = empty
        fabAdd?.isVisible = !empty
    }
}

private const val DB_NAME = "domains.db"
private const val DB_VERSION = 1
private const val TBL = "domains"

class DomainDbHelper(ctx: Context) : SQLiteOpenHelper(ctx, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS $TBL (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 简单策略：如将来升级可做 ALTER；当前版本不需要
    }

    fun getAll(): List<String> {
        val list = mutableListOf<String>()
        readableDatabase.query(TBL, arrayOf("name"), null, null, null, null, "name ASC").use { c: Cursor ->
            while (c.moveToNext()) {
                list.add(c.getString(0))
            }
        }
        return list
    }

    fun insertOrIgnore(name: String) {
        val cv = ContentValues().apply { put("name", name) }
        try {
            writableDatabase.insertWithOnConflict(TBL, null, cv, SQLiteDatabase.CONFLICT_IGNORE)
        } catch (_: Exception) { }
    }

    fun deleteByName(name: String) {
        try {
            writableDatabase.delete(TBL, "name=?", arrayOf(name))
        } catch (_: Exception) { }
    }
}