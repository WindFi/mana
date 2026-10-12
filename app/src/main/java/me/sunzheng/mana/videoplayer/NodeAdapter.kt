package me.sunzheng.mana.videoplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import me.sunzheng.mana.R
import me.sunzheng.mana.core.net.v2.routing.BackendDto

/** 节点列表的一行（FR-2）。 */
sealed interface NodeItem {

    /** 首项固定为"自动（由服务器选择）"。 */
    data object Auto : NodeItem

    /** 地区分组标题（不可点）。 */
    data class Group(val title: String) : NodeItem

    data class Backend(
        val backend: BackendDto,
        val isCurrent: Boolean,
        val enabled: Boolean
    ) : NodeItem
}

/**
 * 节点列表适配器（FR-2）：按 region 分组、置灰不可选项、标注"当前"、离线提示。
 * 复用播放器既有的 `ListView` + `item_*` 布局范式，不引入新的设计元素。
 */
class NodeAdapter(
    private val context: Context,
    private val onSelected: (NodeItem) -> Unit
) : BaseAdapter() {

    private val items = ArrayList<NodeItem>()

    fun submit(newItems: List<NodeItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /** 当前生效节点在列表中的位置（用于 ListView 的选中态），没有则返回 -1。 */
    fun indexOfCurrent(): Int = items.indexOfFirst { it is NodeItem.Backend && it.isCurrent }

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): NodeItem = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = TYPE_COUNT

    override fun getItemViewType(position: Int): Int =
        if (items[position] is NodeItem.Group) TYPE_GROUP else TYPE_ITEM

    override fun areAllItemsEnabled(): Boolean = false

    override fun isEnabled(position: Int): Boolean = when (val item = items[position]) {
        is NodeItem.Backend -> item.enabled
        is NodeItem.Auto -> true
        is NodeItem.Group -> false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflater = LayoutInflater.from(context)
        return when (val item = items[position]) {
            is NodeItem.Group -> {
                val view = convertView?.takeIf { it.findViewById<TextView>(R.id.group_title) != null }
                    ?: inflater.inflate(R.layout.item_node_group, parent, false)
                view.findViewById<TextView>(R.id.group_title).text = item.title
                view
            }

            is NodeItem.Auto -> {
                val view = itemView(convertView, inflater, parent)
                view.findViewById<TextView>(R.id.title).text = context.getString(R.string.node_auto)
                view.findViewById<TextView>(R.id.subtitle).text = ""
                view.contentDescription = context.getString(R.string.node_auto)
                view
            }

            is NodeItem.Backend -> {
                val view = itemView(convertView, inflater, parent)
                val backend = item.backend
                view.findViewById<TextView>(R.id.title).text = backend.displayLabel
                view.findViewById<TextView>(R.id.subtitle).text = subtitle(backend, item.isCurrent)
                view.contentDescription = itemDescription(backend, item.isCurrent)
                view.isEnabled = item.enabled
                view
            }
        }
    }

    private fun itemView(
        convertView: View?,
        inflater: LayoutInflater,
        parent: ViewGroup?
    ): View = convertView?.takeIf { it.findViewById<TextView>(R.id.subtitle) != null }
        ?: inflater.inflate(R.layout.item_node_listview, parent, false)

    /** 副文案：地区 + 状态 + （当前 / 可能被服务器回退）。 */
    private fun subtitle(backend: BackendDto, isCurrent: Boolean): String {
        val parts = ArrayList<String>(4)
        backend.region?.takeIf { it.isNotBlank() }?.let { parts.add(it) }
        parts.add(statusText(backend))
        if (isCurrent) parts.add(context.getString(R.string.node_current))
        if (backend.isOffline) parts.add(context.getString(R.string.node_offline_hint))
        return parts.joinToString(" · ")
    }

    /** 未知 availability 取值按原样展示（未决-1：客户端必须容忍未知值）。 */
    private fun statusText(backend: BackendDto): String = when (backend.availability) {
        BackendDto.AVAILABILITY_HEALTHY -> context.getString(R.string.node_status_healthy)
        BackendDto.AVAILABILITY_OFFLINE -> context.getString(R.string.node_status_offline)
        null, "" -> context.getString(R.string.node_status_unknown)
        else -> backend.availability!!
    }

    /** 无障碍：节点名 + 地区 + 状态 + 是否当前（§7.4）。 */
    private fun itemDescription(backend: BackendDto, isCurrent: Boolean): String =
        context.getString(
            R.string.node_item_content_description,
            backend.displayLabel,
            backend.region ?: context.getString(R.string.node_status_unknown),
            statusText(backend),
            if (isCurrent) context.getString(R.string.node_current) else ""
        )

    fun dispatchSelection(position: Int) {
        val item = items.getOrNull(position) ?: return
        if (item is NodeItem.Group) return
        onSelected(item)
    }

    companion object {
        private const val TYPE_ITEM = 0
        private const val TYPE_GROUP = 1
        private const val TYPE_COUNT = 2

        /**
         * 组装列表（FR-2）：首项"自动"，其后按 region 分组；无 region 的归入"其他"。
         * 分组顺序按地区码排序，"其他"放最后。
         */
        fun buildItems(
            backends: List<BackendDto>,
            currentBackendId: String?,
            cachedLabel: String?,
            otherGroupLabel: String
        ): List<NodeItem> {
            val items = ArrayList<NodeItem>()
            if (cachedLabel != null) items.add(NodeItem.Group(cachedLabel))
            items.add(NodeItem.Auto)
            val grouped = LinkedHashMap<String, MutableList<BackendDto>>()
            backends.forEach { backend ->
                val key = backend.region?.takeIf { it.isNotBlank() } ?: otherGroupLabel
                grouped.getOrPut(key) { ArrayList() }.add(backend)
            }
            grouped.keys.sortedWith(compareBy({ it == otherGroupLabel }, { it })).forEach { region ->
                items.add(NodeItem.Group(region))
                grouped.getValue(region).forEach { backend ->
                    items.add(
                        NodeItem.Backend(
                            backend = backend,
                            isCurrent = backend.id == currentBackendId,
                            enabled = backend.isSelectable
                        )
                    )
                }
            }
            return items
        }
    }
}
