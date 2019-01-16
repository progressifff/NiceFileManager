package com.progressifff.nicefilemanager

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.progressifff.nicefilemanager.presenters.MainPresenter

class NavigationEntriesAdapter(val mainPresenter: MainPresenter) : RecyclerView.Adapter<NavigationEntriesAdapter.NavigationBarListViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NavigationBarListViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.navigation_bar_list_entry, parent, false)
        return NavigationBarListViewHolder(view)
    }

    override fun getItemCount(): Int {
        return mainPresenter.navigationEntriesCount
    }

    override fun onBindViewHolder(holder: NavigationBarListViewHolder, position: Int) {
        mainPresenter.onBindNavigationBarItem(position, holder)
    }

    inner class NavigationBarListViewHolder(view: View) : RecyclerView.ViewHolder(view), NavigationBarEntryView{
        private val listEntryTextView = view.findViewById<TextView>(R.id.navBarListEntryName)!!
        init {
            listEntryTextView.setOnClickListener {mainPresenter.onNavigationBarItemClicked(adapterPosition)}
        }

        override fun setName(entryName: String) {
            listEntryTextView.text = entryName
        }

        override fun getName(): String{
            return listEntryTextView.text.toString()
        }
    }

    interface NavigationBarEntryView {
        fun setName(entryName: String)
        fun getName(): String
    }
}