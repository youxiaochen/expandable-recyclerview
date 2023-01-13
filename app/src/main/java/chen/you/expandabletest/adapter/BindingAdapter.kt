package chen.you.expandabletest.adapter

import android.animation.ObjectAnimator
import android.content.Context
import android.view.View
import android.widget.Toast
import chen.you.expandable.ExpandableRecyclerView
import chen.you.expandabletest.BR
import chen.you.expandabletest.BindingData
import chen.you.expandabletest.R
import chen.you.expandabletest.databinding.ItemBindingGroupBinding
import chen.you.expandabletest.extern.BindingExpandableAdapter
import chen.you.expandabletest.extern.BindingGroupViewHolder

/**
 *  author: you : 2021/1/6
 */
class BindingAdapter(context: Context) : BindingExpandableAdapter<BindingData, String>(context),
    BindingExpandableAdapter.OnChildItemClickListener {

    init {
        setOnChildItemClickListener(this)
    }

    override fun getGroupLayoutResId(viewType: Int): Int = R.layout.item_binding_group

    override fun getChildLayoutResId(viewType: Int): Int = R.layout.item_binding_child

    override fun getGroupVariableId(viewType: Int): Int = BR.vm

    override fun getChildVariableId(viewType: Int): Int = BR.name

    override fun onBindGroupViewHolder(vh: BindingGroupViewHolder, groupPos: Int, isExpanded: Boolean) {
        super.onBindGroupViewHolder(vh, groupPos, isExpanded)
        vh.getBinding<ItemBindingGroupBinding>().iv.rotation = if (isExpanded) 90f else 0f
    }

    override fun onGroupStateChanged(vh: BindingGroupViewHolder, groupPos: Int, isExpanded: Boolean) {
        val binding = vh.getBinding<ItemBindingGroupBinding>()
        if (isExpanded) { //展开
            ObjectAnimator.ofFloat(binding.iv, "rotation", 0f, 90f)
        } else {
            ObjectAnimator.ofFloat(binding.iv, "rotation", 90f, 0f)
        }.apply { duration = 300 }.start()
    }

    override fun onChildItemClick(v: View, info: ExpandableRecyclerView.ChildInfo?) {
        info?.also {
            Toast.makeText(v.context, "GroupPos=${it.group.index}, ChildPos=${it.index}", Toast.LENGTH_SHORT).show()
        }
    }
}