package chen.you.expandabletest.extern

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import chen.you.expandable.ExpandableRecyclerView.ChildViewHolder

/**
 *  author: you : 2021/1/11
 */
class BindingChildViewHolder(itemView: View) : ChildViewHolder(itemView) {

    fun <T : ViewDataBinding> getBinding(): T = DataBindingUtil.getBinding<T>(itemView) as T
}