package com.progressifff.nicefilemanager

import android.graphics.drawable.Drawable
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.progressifff.nicefilemanager.views.FilesView
import com.progressifff.nicefilemanager.views.FilesListEntryView

class FilesListAdapter<ModelT, FileViewT : FilesView>(private val filesPresenter: AbstractFilesPresenter<ModelT, FileViewT>) :
        RecyclerView.Adapter<FilesListAdapter<ModelT, FileViewT>.ViewHolder>() {

    private lateinit var recyclerView: RecyclerView

    override fun getItemCount(): Int = filesPresenter.getFilesCount()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val filesListEntryView = if(recyclerView.layoutManager is GridLayoutManager){
            LayoutInflater.from(parent.context).inflate(R.layout.files_list_entry_card, parent, false)
        }
        else{
            LayoutInflater.from(parent.context).inflate(R.layout.files_list_entry, parent, false)
        }
        return ViewHolder(filesListEntryView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        filesPresenter.onBindFileListEntryView(position, holder)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    inner class ViewHolder(filesListEntryView : View) : RecyclerView.ViewHolder(filesListEntryView), FilesListEntryView{
        private val fileListEntryLayout: View = if(recyclerView.layoutManager is GridLayoutManager){
            filesListEntryView.findViewById(R.id.storageFileEntry)
        }
        else{
            filesListEntryView
        }

        private val fileNameView: TextView = fileListEntryLayout.findViewById(R.id.storageFileName)
        private val fileMoreBtn: ImageButton = fileListEntryLayout.findViewById(R.id.fileMoreBtn)
        private val fileModificationDate: TextView? = fileListEntryLayout.findViewById(R.id.fileModificationTime)
        private val fileImage: ImageView = fileListEntryLayout.findViewById(R.id.fileImage)

        init {
            fileListEntryLayout.setOnClickListener{
                val filesListState = if(recyclerView.layoutManager is LinearLayoutManager){
                    (recyclerView.layoutManager as LinearLayoutManager).onSaveInstanceState()!!
                }
                else{
                    (recyclerView.layoutManager as GridLayoutManager).onSaveInstanceState()!!
                }
                filesPresenter.onFilesListEntryClicked(adapterPosition, filesListState)
            }
            fileListEntryLayout.setOnLongClickListener{
                filesPresenter.onFileListItemLongClick(adapterPosition)
                return@setOnLongClickListener true
            }
            fileMoreBtn.setOnClickListener { filesPresenter.onFileListEntryMenuClicked(adapterPosition) }
        }

        override fun getImageView(): ImageView {
            return fileImage
        }

        override fun setFileImage(image: Drawable?) {
            fileImage.setImageDrawable(image)
        }

        override fun setSelected(isSelected: Boolean) {
            fileListEntryLayout.isSelected = isSelected
        }

        override fun setName(name: String) {
            fileNameView.text = name
        }

        override fun setModificationDate(date: String) {
            val text = "${itemView.context.getString(R.string.file_modified)} $date"
            fileModificationDate?.text = text
        }
    }
}