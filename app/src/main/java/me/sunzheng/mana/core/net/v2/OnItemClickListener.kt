package me.sunzheng.mana.core.net.v2

import android.view.View

interface OnItemClickListener {
    fun onItemClick(v: View, position: Int, id: Long, model: Any)
}