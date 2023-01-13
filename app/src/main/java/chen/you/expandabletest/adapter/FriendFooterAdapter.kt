package chen.you.expandabletest.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import chen.you.expandabletest.R

/**
 *  author: you : 2021/1/6
 */
class FriendFooterAdapter : RecyclerView.Adapter<TestViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TestViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return TestViewHolder(inflater.inflate(R.layout.item_friend_footer, parent, false))
    }

    override fun onBindViewHolder(holder: TestViewHolder, position: Int) {
    }

    override fun getItemCount() = 1
}