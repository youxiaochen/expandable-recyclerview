package chen.you.expandabletest

import chen.you.expandabletest.extern.Expandable

/**
 *  author: you : 2021/1/6
 */
class BindingData(var name: String) : Expandable<String> {

    constructor(name: String, friendName: String, testSize: Int) : this(name) {
        for (i in 0 until testSize) {
            children.add("$friendName $i")
        }
    }

    override val children = ArrayList<String>()

    companion object {

        fun test0(): ArrayList<BindingData> {
            val testData = ArrayList<BindingData>()
            testData.add(BindingData("最近照片", "娜娜", 6))
            testData.add(BindingData("好友照片", "好友张三", 11))
            testData.add(BindingData("基友照片", "基友李四", 0))
            testData.add(BindingData("女友照片", "女友迪丽拉扎", 8))
            testData.add(BindingData("同事照片", "同事诗诗", 7))
            testData.add(BindingData("钓友照片", "钓友学友", 5))
            testData.add(BindingData("室友照片", "室友莲莲", 9))
            testData.add(BindingData("旅游照片", "旅游莲莲", 10))
            return testData
        }

        fun test1(): ArrayList<BindingData> {
            val testData = ArrayList<BindingData>()
            testData.add(BindingData("打鬼子照片", "打丸子", 6))
            testData.add(BindingData("打棒子照片", "打张娜拉", 10))
            return testData
        }
    }
}