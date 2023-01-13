package chen.you.expandabletest.extern

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import chen.you.expandable.ExpandableRecyclerView.GroupViewHolder

/**
 *  author: you : 2021/1/11
 */
class BindingGroupViewHolder(itemView: View) : GroupViewHolder(itemView) {

    fun <T : ViewDataBinding> getBinding(): T = DataBindingUtil.getBinding<T>(itemView) as T
}