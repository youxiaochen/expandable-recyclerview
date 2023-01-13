package chen.you.expandabletest

/**
 *  author: you : 2021/1/6
 */
class FriendData(var name: String) {

    constructor(name: String, friendName: String, testSize: Int) : this(name) {
        for (i in 0 until testSize) {
            friends.add("$friendName $i")
        }
    }

    val friends = ArrayList<String>()

    companion object {

        fun test0(): ArrayList<FriendData> {
            val testData = ArrayList<FriendData>()
            testData.add(FriendData("特别关心", "关心娜娜", 11))
            testData.add(FriendData("我的好友", "好友张三", 9))
            testData.add(FriendData("我的基友", "基友李四", 0))
            testData.add(FriendData("我的女友", "女友迪丽拉扎", 8))
            testData.add(FriendData("我的同事", "同事诗诗", 7))
            testData.add(FriendData("我的钓友", "钓友学友", 5))
            testData.add(FriendData("我的室友", "室友莲莲", 3))
            return testData
        }

        fun test1(): ArrayList<FriendData> {
            val testData = ArrayList<FriendData>()
            testData.add(FriendData("最近照片", "娜娜", 6))
            testData.add(FriendData("好友照片", "好友张三", 11))
            testData.add(FriendData("基友照片", "基友李四", 0))
            testData.add(FriendData("女友照片", "女友迪丽拉扎", 8))
            testData.add(FriendData("同事照片", "同事诗诗", 7))
            testData.add(FriendData("钓友照片", "钓友学友", 5))
            testData.add(FriendData("室友照片", "室友莲莲", 9))
            testData.add(FriendData("旅游照片", "旅游莲莲", 10))
            return testData
        }
    }
}