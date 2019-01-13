package com.progressifff.filemanager

import android.graphics.Rect
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import java.lang.AssertionError

class RecyclerViewGridLayoutDecoration : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        super.getItemOffsets(outRect, view, parent, state)
        val fileCardWidth = App.get().resources.getDimensionPixelSize(R.dimen.file_card_width)
        val columnsCount = (parent.layoutManager as GridLayoutManager).spanCount

        if(columnsCount == 0) {
            throw AssertionError("Null columns count")
        }

        val columnWidth = parent.width / columnsCount
        val spacing = (columnWidth - fileCardWidth) / 2
        outRect.left = spacing
        outRect.right = spacing
        outRect.top = spacing
        outRect.bottom = spacing
    }
}