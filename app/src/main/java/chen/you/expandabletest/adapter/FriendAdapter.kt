package chen.you.expandabletest.adapter

import android.animation.ObjectAnimator
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import chen.you.expandable.ExpandableRecyclerView
import chen.you.expandable.ExpandableRecyclerView.ExpandableAdapter
import chen.you.expandabletest.FriendData
import chen.you.expandabletest.R

/**
 *  author: you : 2021/1/6
 */
class FriendAdapter(context: Context, private val isGrid: Boolean = false) : ExpandableAdapter<FriendAdapter.GroupViewHolder, FriendAdapter.ChildViewHolder>() {
    private val inflater: LayoutInflater
    val data: MutableList<FriendData>

    init {
        inflater = LayoutInflater.from(context)
        data = if (isGrid) FriendData.test1() else FriendData.test0()
    }

    override fun getGroupCount(): Int = data.size

    override fun getChildCount(groupPos: Int): Int  = data[groupPos].friends.size

    override fun onCreateGroupViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val v = inflater.inflate(R.layout.item_friend_group, parent, false)
        return GroupViewHolder(v)
    }

    override fun getChildViewType(groupPos: Int, childPos: Int): Short {
        return 12
    }

    override fun onCreateChildViewHolder(parent: ViewGroup, viewType: Int): ChildViewHolder {
        val layoutRes = if (isGrid) R.layout.item_grid_child else R.layout.item_friend_child
        val v = inflater.inflate(layoutRes, parent, false)
        return ChildViewHolder(v).apply {
            itemView.setOnClickListener {
                childInfo?.also {
                    Toast.makeText(inflater.context, "GroupPos=${it.group.index}, ChildPos=${it.index}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onBindGroupViewHolder(vh: GroupViewHolder, groupPos: Int, isExpanded: Boolean) {
        val friend = data[groupPos]
        vh.tv.text = friend.name
        vh.tv2.text = "${friend.friends.size} 个"
        vh.iv.rotation = if (isExpanded) 90f else 0f
    }

    override fun onBindChildViewHolder(vh: ChildViewHolder, groupPos: Int, childPos: Int, isLastChild: Boolean) {
        vh.tv.text = data[groupPos].friends[childPos]
    }

    override fun onGroupStateChanged(vh: GroupViewHolder, groupPos: Int, isExpanded: Boolean) {
        if (isExpanded) { //展开
            ObjectAnimator.ofFloat(vh.iv, "rotation", 0f, 90f)
        } else {
            ObjectAnimator.ofFloat(vh.iv, "rotation", 90f, 0f)
        }.apply { duration = 300 }.start()
    }

    class GroupViewHolder(itemView: View) : ExpandableRecyclerView.GroupViewHolder(itemView) {
        val iv: ImageView
        val tv: TextView
        val tv2: TextView
        init {
            iv = itemView.findViewById(R.id.iv)
            tv = itemView.findViewById(R.id.tv)
            tv2 = itemView.findViewById(R.id.tv2)
        }
    }

    class ChildViewHolder(itemView: View) : ExpandableRecyclerView.ChildViewHolder(itemView) {
        val tv: TextView
        init {
            tv = itemView.findViewById(R.id.tv)
        }
    }
}