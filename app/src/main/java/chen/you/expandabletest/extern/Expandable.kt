package chen.you.expandabletest.extern

/**
 *  author: you : 2021/1/11
 */
interface Expandable<C> {

    val children: MutableList<C>

    fun size(): Int = children.size

    fun getChild(position: Int): C = children[position]

    fun addChild(item: C) {
        children.add(item)
    }

    fun addChild(position: Int, item: C) {
        children.add(position, item)
    }

    fun removeAt(position: Int): C = children.removeAt(position)
}