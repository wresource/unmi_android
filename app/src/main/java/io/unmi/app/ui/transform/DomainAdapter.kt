package io.unmi.app.ui.transform

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import io.unmi.app.R

class DomainAdapter(
    private val onClick: (String) -> Unit = {}
) : ListAdapter<String, DomainAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<String>() {
            override fun areItemsTheSame(old: String, new: String) = old == new
            override fun areContentsTheSame(old: String, new: String) = old == new
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        private val logo = v.findViewById<ImageView>(R.id.imgLogo)
        private val title = v.findViewById<TextView>(R.id.txtDomain)
        fun bind(domain: String) {
            title.text = domain
            logo.setImageResource(R.drawable.avatar_11) // 先用占位图
            itemView.setOnClickListener { onClick(domain) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_domain_card, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }
}