package androidx.recyclerview.widget;

/**
 * author: you : 2020/12/27
 */
public final class ExpandableViewHolderCompat {

    private ExpandableViewHolderCompat(){}

    public static RecyclerView getOwnerRecyclerView(RecyclerView.ViewHolder vh) {
        return vh.mOwnerRecyclerView;
    }
}
