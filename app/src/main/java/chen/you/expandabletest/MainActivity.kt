package chen.you.expandabletest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import chen.you.expandabletest.databinding.ActMainBinding
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActMainBinding

    private val adapter by lazy { TabAdapter(this) }

    private lateinit var mediator: TabLayoutMediator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.act_main)
        binding.vp.adapter = adapter
        val strategy = TabLayoutMediator.TabConfigurationStrategy { tab, pos -> tab.text = adapter.getTitle(pos) }
        mediator = TabLayoutMediator(binding.tl, binding.vp, strategy)
        mediator.attach()
    }

    override fun onDestroy() {
        super.onDestroy()
        mediator.detach()
        binding.unbind()
    }

    private class TabAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {

        companion object {
            private val TITLES = arrayOf("普通分组", "Grid方式分组", "Binding的方式")
        }

        override fun getItemCount() = TITLES.size

        fun getTitle(position: Int) = TITLES[position]

        override fun createFragment(position: Int): Fragment {
            when (position) {
                0 -> return FriendFragment()
                1 -> return GridFragment()
                2 -> return BindingFragment()
            }
            return Fragment()
        }
    }

}