package chen.you.expandabletest.extern

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import chen.you.expandable.ExpandableRecyclerView.ChildInfo
import chen.you.expandable.ExpandableRecyclerView.ExpandableAdapter
import chen.you.expandable.ExpandableRecyclerView.GroupInfo

/**
 *  author: you : 2021/1/11
 *  示例封装类
 */
abstract class BindingExpandableAdapter<T : Expandable<C>, C>(context: Context, initData: List<T>? = null)
    : ExpandableAdapter<BindingGroupViewHolder, BindingChildViewHolder>() {

    private val mInflater: LayoutInflater = LayoutInflater.from(context)

    private val mData = ArrayList<T>()
    //对外只提供List
    val data: List<T> = mData

    //adapter view click call back
    private var onGroupClickListener: OnGroupItemClickListener? = null

    //adapter view click call back
    private var onChildClickListener: OnChildItemClickListener? = null

    init {
        initData?.also { mData.addAll(it) }
    }

    override fun getGroupCount(): Int = mData.size

    override fun getChildCount(groupPos: Int): Int = mData[groupPos].size()

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): BindingGroupViewHolder {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(mInflater, getGroupLayoutResId(viewType), parent, false)
        return BindingGroupViewHolder(binding.root).apply {
            onGroupViewHolderCreated(this, viewType)
            if (!groupCanClick()) {
                onGroupClickListener?.also {
                    itemView.setOnClickListener { v -> it.onGroupItemClick(v, groupInfo) }
                }
            }
        }
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): BindingChildViewHolder {
        val binding = DataBindingUtil.inflate<ViewDataBinding>(mInflater, getChildLayoutResId(viewType), parent, false)
        return BindingChildViewHolder(binding.root).apply {
            onChildViewHolderCreated(this, viewType)
            onChildClickListener?.also {
                itemView.setOnClickListener { v -> it.onChildItemClick(v, childInfo) }
            }
        }
    }

    override fun onBindGroupViewHolder(vh: BindingGroupViewHolder, groupPos: Int, isExpanded: Boolean) {
        vh.getBinding<ViewDataBinding>().setVariable(getGroupVariableId(vh.itemViewType), getGroupItem(groupPos))
    }

    override fun onBindChildViewHolder(vh: BindingChildViewHolder, groupPos: Int, childPos: Int, isLastChild: Boolean) {
        vh.getBinding<ViewDataBinding>().setVariable(getChildVariableId(vh.itemViewType), getChildItem(groupPos, childPos))
    }

    @LayoutRes
    protected abstract fun getGroupLayoutResId(viewType: Int): Int

    @LayoutRes
    protected abstract fun getChildLayoutResId(viewType: Int): Int

    protected open fun onGroupViewHolderCreated(vh: BindingGroupViewHolder, viewType: Int) = Unit

    protected open fun onChildViewHolderCreated(vh: BindingChildViewHolder, viewType: Int) = Unit

    protected abstract fun getGroupVariableId(viewType: Int): Int

    protected abstract fun getChildVariableId(viewType: Int): Int

    fun getGroupItem(groupPos: Int): T = mData[groupPos]

    fun getChildItem(groupPos: Int, childPos: Int): C = mData[groupPos].getChild(childPos)

    fun isEmpty(): Boolean = mData.isEmpty()

    fun setNewData(newData: List<T>) {
        if (mData.isEmpty()) {
            if (newData.isNotEmpty()) {
                mData.addAll(newData)
                notifyGroupRangeInserted(0, newData.size)
            }
        } else {
            mData.clear()
            mData.addAll(newData)
            notifyDataSetChanged()
        }
    }

    fun addData(addData: List<T>, expandChild: Boolean = false) {
        if (addData.isEmpty()) return
        val positionStart = mData.size
        mData.addAll(addData)
        notifyGroupRangeInserted(positionStart, addData.size, expandChild)
    }

    fun removeAll() {
        if (mData.isEmpty()) return
        val itemCount = mData.size
        mData.clear()
        notifyGroupRangeRemoved(0, itemCount)
    }

    fun addGroupItem(groupPos: Int, item: T, expandChild: Boolean) {
        if (groupPos > mData.size || groupPos < 0) return
        if (groupPos == mData.size) mData.add(item) else mData.add(groupPos, item)
        notifyGroupInserted(groupPos, expandChild)
    }

    fun removeGroupItem(groupPos: Int) {
        if (groupPos >= mData.size || groupPos < 0) return
        mData.removeAt(groupPos)
        notifyGroupRemoved(groupPos)
    }

    fun addChildItem(groupPos: Int, childPos: Int, item: C) {
        if (groupPos > mData.size || groupPos < 0 || childPos < 0) return
        val group = mData[groupPos]
        if (childPos > group.size()) return
        if (childPos == group.size()) group.addChild(item) else group.addChild(childPos, item)
        notifyChildInserted(groupPos, childPos)
    }

    fun removeChildItem(groupPos: Int, childPos: Int) {
        if (groupPos >= mData.size || groupPos < 0 || childPos < 0) return
        val group = mData[groupPos]
        if (childPos >= group.size()) return
        group.removeAt(childPos)
        notifyChildRemoved(groupPos, childPos)
    }

    fun setOnGroupItemClickListener(onGroupClickListener: OnGroupItemClickListener?) {
        this.onGroupClickListener = onGroupClickListener
    }

    fun setOnChildItemClickListener(onChildClickListener: OnChildItemClickListener?) {
        this.onChildClickListener = onChildClickListener
    }

    /**
     * 在adapter设置 groupCanClick false时, 监听才有效,
     * 否则只需要重写 {@see onGroupStateChanged}
     */
    interface OnGroupItemClickListener {

        fun onGroupItemClick(v: View, info: GroupInfo?)
    }

    interface OnChildItemClickListener {

        fun onChildItemClick(v: View, info: ChildInfo?)
    }
}