package chen.you.expandabletest

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import chen.you.expandabletest.adapter.FriendAdapter
import chen.you.expandabletest.adapter.FriendFooterAdapter
import chen.you.expandabletest.adapter.FriendHeaderAdapter
import chen.you.expandabletest.databinding.FragmentFriendBinding

/**
 *  author: you : 2021/1/6
 */
class FriendFragment : Fragment(), View.OnClickListener {

    private lateinit var binding: FragmentFriendBinding
    private val adapter by lazy { FriendAdapter(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentFriendBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.erv.setAdapter(adapter, FriendHeaderAdapter(), FriendFooterAdapter())
        binding.erv.expandGroup(0)

        binding.bt0.setOnClickListener(this)
        binding.bt1.setOnClickListener(this)
        binding.bt2.setOnClickListener(this)
        binding.bt3.setOnClickListener(this)
        binding.bt4.setOnClickListener(this)
        binding.bt5.setOnClickListener(this)
        binding.bt6.setOnClickListener(this)
        binding.bt7.setOnClickListener(this)
        binding.bt8.setOnClickListener(this)
        binding.bt9.setOnClickListener(this)
        binding.bt10.setOnClickListener(this)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
        TAG = 0
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.bt0 -> testNotifyGroupChanged()
            R.id.bt1 -> testNotifyGroupRangeChanged()
            R.id.bt2 -> testNotifyGroupInserted()
            R.id.bt3 -> testNotifyGroupRangeInserted(false)
            R.id.bt4 -> testNotifyGroupRangeInserted(true)
            R.id.bt5 -> testNotifyGroupRemoved()
            R.id.bt6 -> testNotifyGroupRangeRemoved()
            R.id.bt7 -> testNotifyChildChanged()
            R.id.bt8 -> testNotifyChildInserted()
            R.id.bt9 -> testNotifyChildRangeInserted()
            R.id.bt10 -> testNotifyChildRangeRemoved()
        }
    }

    companion object {
        private var TAG = 0 //test
    }

    // ------------------------------    Group相关操作 --------------------------------------
    private fun testNotifyGroupChanged() {
        val name: String = binding.et0.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "请输入name", Toast.LENGTH_SHORT).show()
            return
        }
        val groupPos = getNum(binding.et1)
        if (groupPos >= adapter.data.size) {
            Toast.makeText(context, "groupPos >= data size ", Toast.LENGTH_SHORT).show()
            return
        }
        adapter.data[groupPos].name = name
        adapter.notifyGroupChanged(groupPos)
    }

    private fun testNotifyGroupRangeChanged() {
        val name: String = binding.et0.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "请输入name", Toast.LENGTH_SHORT).show()
            return
        }
        val groupPos = getNum(binding.et1)
        val itemCount = getNum(binding.et3)
        if (groupPos + itemCount > adapter.data.size) {
            Toast.makeText(context, "groupPos + itemCount > data size ", Toast.LENGTH_SHORT).show()
            return
        }
        for (i in 0 until itemCount) {
            adapter.data[groupPos + i].name = name + TAG++
        }
        adapter.notifyGroupRangeChanged(groupPos, itemCount)
    }

    private fun testNotifyGroupInserted() {
        val name: String = binding.et0.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "请输入name", Toast.LENGTH_SHORT).show()
            return
        }
        val groupPos = getNum(binding.et1)
        if (groupPos > adapter.data.size) {
            Toast.makeText(context, "groupPos > data size ", Toast.LENGTH_SHORT).show()
            return
        }
        adapter.data.add(groupPos, FriendData(name, "groupInsert", TAG++))
        adapter.notifyGroupInserted(groupPos)
    }

    private fun testNotifyGroupRangeInserted(expand: Boolean) {
        val name: String = binding.et0.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "请输入name", Toast.LENGTH_SHORT).show()
            return
        }
        val groupPos = getNum(binding.et1)
        if (groupPos > adapter.data.size) {
            Toast.makeText(context, "groupPos > data size ", Toast.LENGTH_SHORT).show()
            return
        }
        val itemCount = getNum(binding.et3)
        for (i in 0 until itemCount) {
            adapter.data.add(groupPos + i, FriendData(name, "groupInsert", TAG++))
        }
        adapter.notifyGroupRangeInserted(groupPos, itemCount, expand)
    }

    private fun testNotifyGroupRemoved() {
        val groupPos = getNum(binding.et1)
        if (groupPos >= adapter.data.size) {
            Toast.makeText(context, "groupPos >= data size ", Toast.LENGTH_SHORT).show()
            return
        }
        adapter.data.removeAt(groupPos)
        adapter.notifyGroupRemoved(groupPos)
    }

    private fun testNotifyGroupRangeRemoved() {
        val groupPos = getNum(binding.et1)
        val itemCount = getNum(binding.et3)
        if (groupPos + itemCount > adapter.data.size) {
            Toast.makeText(context, "groupPos + itemCount > data size ", Toast.LENGTH_SHORT).show()
            return
        }
        for (i in 0 until itemCount) {
            adapter.data.removeAt(groupPos)
        }
        adapter.notifyGroupRangeRemoved(groupPos, itemCount)
    }

    /// ------------------------------    Child  相关操作     ------------------------

    /// ------------------------------    Child  相关操作     ------------------------
    private fun testNotifyChildChanged() {
        val name: String = binding.et0.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "请输入name", Toast.LENGTH_SHORT).show()
            return
        }
        val groupPos = getNum(binding.et1)
        if (groupPos >= adapter.data.size) {
            Toast.makeText(context, "groupPos >= data size ", Toast.LENGTH_SHORT).show()
            return
        }
        val childPos = getNum(binding.et2)
        if (childPos >= adapter.data[groupPos].friends.size) {
            val text = "childPos >= data[$groupPos].friends.size"
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            return
        }
        val friendData: FriendData = adapter.data[groupPos]
        friendData.friends.removeAt(childPos)
        friendData.friends.add(childPos, name)
        adapter.notifyChildChanged(groupPos, childPos)
    }

    private fun testNotifyChildInserted() {
        val name: String = binding.et0.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "请输入name", Toast.LENGTH_SHORT).show()
            return
        }
        val groupPos = getNum(binding.et1)
        if (groupPos >= adapter.data.size) {
            Toast.makeText(context, "groupPos >= data size ", Toast.LENGTH_SHORT).show()
            return
        }
        val childPos = getNum(binding.et2)
        if (childPos > adapter.data[groupPos].friends.size) {
            val text = "childPos  > data[$groupPos].friends.size"
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            return
        }
        val friendData: FriendData = adapter.data[groupPos]
        friendData.friends.add(childPos, name + TAG++)
        adapter.notifyGroupChanged(groupPos)
        adapter.notifyChildInserted(groupPos, childPos)
    }

    private fun testNotifyChildRangeInserted() {
        val name: String = binding.et0.text.toString().trim()
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(context, "请输入name", Toast.LENGTH_SHORT).show()
            return
        }
        val groupPos = getNum(binding.et1)
        if (groupPos >= adapter.data.size) {
            Toast.makeText(context, "groupPos >= data size ", Toast.LENGTH_SHORT).show()
            return
        }
        val childPos = getNum(binding.et2)
        if (childPos > adapter.data[groupPos].friends.size) {
            val text = "childPos  > data[$groupPos].friends.size"
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            return
        }
        val itemCount = getNum(binding.et3)
        val friendData: FriendData = adapter.data[groupPos]
        for (i in 0 until itemCount) {
            friendData.friends.add(childPos + i, name + TAG++)
        }
        adapter.notifyGroupChanged(groupPos)
        adapter.notifyChildRangeInserted(groupPos, childPos, itemCount)
    }

    private fun testNotifyChildRangeRemoved() {
        val groupPos = getNum(binding.et1)
        if (groupPos >= adapter.data.size) {
            Toast.makeText(context, "groupPos >= data size ", Toast.LENGTH_SHORT).show()
            return
        }
        val childPos = getNum(binding.et2)
        val itemCount = getNum(binding.et3)
        val friendData: FriendData = adapter.data[groupPos]
        if (childPos + itemCount > friendData.friends.size) {
            val text = "childPos + itemCount > data[$groupPos].friends.size"
            Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
            return
        }
        for (i in 0 until itemCount) {
            friendData.friends.removeAt(childPos)
        }
        adapter.notifyGroupChanged(groupPos)
        adapter.notifyChildRangeRemoved(groupPos, childPos, itemCount)
    }

    private fun getNum(et: EditText): Int = try {
        et.text.toString().trim().toInt()
    } catch (e: Exception) {
        0
    }
}