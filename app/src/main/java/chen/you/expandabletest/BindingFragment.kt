package chen.you.expandabletest

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import chen.you.expandabletest.adapter.BindingAdapter
import chen.you.expandabletest.adapter.FriendFooterAdapter
import chen.you.expandabletest.adapter.FriendHeaderAdapter
import chen.you.expandabletest.databinding.FragmentBindingBinding

/**
 *  author: you : 2021/1/11
 */
class BindingFragment : Fragment() {

    private lateinit var binding: FragmentBindingBinding
    private val adapter by lazy { BindingAdapter(requireContext()) }

    override fun onCreateView(inflater: LayoutInflater, c: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentBindingBinding.inflate(inflater, c, false)
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        binding.erv.setAdapter(adapter, FriendHeaderAdapter(), FriendFooterAdapter())

        binding.erv.postDelayed({
            run {
                adapter.setNewData(BindingData.test0())
            }
        }, 2000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.unbind()
    }

}