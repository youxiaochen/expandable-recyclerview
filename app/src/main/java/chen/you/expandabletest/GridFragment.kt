package chen.you.expandabletest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import chen.you.expandabletest.adapter.FriendAdapter
import chen.you.expandabletest.adapter.FriendFooterAdapter
import chen.you.expandabletest.adapter.FriendHeaderAdapter
import chen.you.expandabletest.databinding.FragmentGridBinding

/**
 *  author: you : 2021/1/6
 */
class GridFragment : Fragment() {

    private lateinit var binding: FragmentGridBinding
    private val adapter by lazy { FriendAdapter(requireContext(), true) }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentGridBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val glm = binding.erv.layoutManager as GridLayoutManager
        glm.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                if (position == 0 || position == glm.itemCount - 1) return 3
                return if (binding.erv.isGroupTypeByPosition(position)) 3 else 1
            }
        }

        binding.erv.setAdapter(adapter, FriendHeaderAdapter(), FriendFooterAdapter())
        if (savedInstanceState == null) binding.erv.expandGroup(0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
    }
}