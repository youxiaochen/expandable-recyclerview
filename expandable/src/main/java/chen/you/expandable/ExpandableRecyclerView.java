package chen.you.expandable;

import android.content.Context;
import android.database.Observable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ConcatAdapter;
import androidx.recyclerview.widget.ExpandableViewHolderCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.ArrayList;
import java.util.List;

/**
 * author: you : 2020/10/19  作QQ/W 86207610
 * 原理同ExpandableListView, 增加RecyclerView的adapter方式及PositionMetadata缓存机制
 */
public class ExpandableRecyclerView extends RecyclerView {
    //Expandable核心处理类
    private ExpandableConnector mConnector;
    //ExpandableAdapter
    private ExpandableAdapter<? extends GroupViewHolder, ? extends ChildViewHolder> mAdapter;
    private final AdapterDataObserver mObserver = new ExpandableRecyclerViewDataObserver();
    //headerAdapter, 可计算HeaderCount
    private Adapter<? extends ViewHolder> mHeaderAdapter;
    //footerAdapter, 可计算FooterCount
    private Adapter<? extends ViewHolder> mFooterAdapter;

    public ExpandableRecyclerView(@NonNull Context context) {
        super(context);
    }

    public ExpandableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ExpandableRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //判断viewType是否为Group
    public static boolean isGroupViewType(int viewType) {
        return ExpandableConnector.isGroupViewType(viewType);
    }

    @Override
    public void setAdapter(@Nullable RecyclerView.Adapter adapter) {
        throw new RuntimeException("Use setAdapter(ExpandableAdapter adapter)");
    }

    public void setAdapter(@Nullable ExpandableAdapter<? extends GroupViewHolder, ? extends ChildViewHolder> adapter) {
        this.mHeaderAdapter = null;
        this.mFooterAdapter = null;
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
            mAdapter.onDetachedFromExpandableRecyclerView(this);
        }
        mAdapter = adapter;
        if (adapter != null) {
            mConnector = new ExpandableConnector(mAdapter);
            adapter.registerAdapterDataObserver(mObserver);
            adapter.onAttachedToExpandableRecyclerView(this);
            if (!adapter.groupCanClick()) {
                mConnector.refreshGroupMetadataList(false);
            }
        } else {
            mConnector = null;
        }
        super.setAdapter(mConnector);
    }

    public void setAdapter(@Nullable ExpandableAdapter<? extends GroupViewHolder, ? extends ChildViewHolder> adapter,
                           @Nullable Adapter<? extends ViewHolder> headerAdapter) {
        setAdapter(adapter, headerAdapter, null);
    }

    public void setAdapter(@Nullable ExpandableAdapter<? extends GroupViewHolder, ? extends ChildViewHolder> adapter,
                           @Nullable Adapter<? extends ViewHolder> headerAdapter,
                           @Nullable Adapter<? extends ViewHolder> footerAdapter) {
        setAdapter(ConcatAdapter.Config.DEFAULT, adapter, headerAdapter, footerAdapter);
    }

    public void setAdapter(@NonNull ConcatAdapter.Config config,
                           @Nullable ExpandableAdapter<? extends GroupViewHolder, ? extends ChildViewHolder> adapter,
                           @Nullable Adapter<? extends ViewHolder> headerAdapter,
                           @Nullable Adapter<? extends ViewHolder> footerAdapter) {
        if (headerAdapter == null && footerAdapter == null) {
            setAdapter(adapter);
            return;
        }
        if (mAdapter != null) {
            mAdapter.unregisterAdapterDataObserver(mObserver);
            mAdapter.onDetachedFromExpandableRecyclerView(this);
        }
        mAdapter = adapter;
        if (adapter != null) {
            this.mHeaderAdapter = headerAdapter;
            this.mFooterAdapter = footerAdapter;
            mConnector = new ExpandableConnector(mAdapter);
            adapter.registerAdapterDataObserver(mObserver);
            adapter.onAttachedToExpandableRecyclerView(this);
            if (!adapter.groupCanClick()) {
                mConnector.refreshGroupMetadataList(false);
            }

            ConcatAdapter concatAdapter = new ConcatAdapter(config);
            if (headerAdapter != null) {
                concatAdapter.addAdapter(headerAdapter);
            }
            concatAdapter.addAdapter(mConnector);
            if (footerAdapter != null) {
                concatAdapter.addAdapter(footerAdapter);
            }
            super.setAdapter(concatAdapter);
        } else {
            mConnector = null;
            this.mHeaderAdapter = null;
            this.mFooterAdapter = null;
            super.setAdapter(null);
        }
    }

    @Nullable
    public ExpandableAdapter<? extends GroupViewHolder, ? extends ChildViewHolder> getExpandableAdapter() {
        return mAdapter;
    }

    @Nullable
    public Adapter<? extends ViewHolder> getHeaderAdapter() {
        return mHeaderAdapter;
    }

    @Nullable
    public Adapter<? extends ViewHolder> getFooterAdapter() {
        return mFooterAdapter;
    }

    //HeaderCount
    public final int getHeaderCount() {
        if (mHeaderAdapter != null) return mHeaderAdapter.getItemCount();
        return 0;
    }

    //FooterCount
    public final int getFooterCount() {
        if (mFooterAdapter != null) return mFooterAdapter.getItemCount();
        return 0;
    }

    /**
     * 清除item Change时的闪烁效果
     */
    public final void clearChangeAnimations() {
        ItemAnimator animator = getItemAnimator();
        if (animator instanceof SimpleItemAnimator) {
            ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
        }
    }

    /**
     * 判断Adapter的position处是否为GroupViewType, 亦可是ConcatAdapter
     */
    public boolean isGroupTypeByPosition(int position) {
        return isGroupTypeByBindingPosition(position - getHeaderCount());
    }

    /**
     * 判断ExpandableAdapter的position处是否为GroupViewType
     * @param bindingPosition  ExpandableAdapter的position, ConcatAdapter时要 - getHeaderCount
     */
    public boolean isGroupTypeByBindingPosition(int bindingPosition) {
        if (mConnector != null && bindingPosition >= 0 && bindingPosition < mConnector.getItemCount()) {
            return mConnector.isGroupType(bindingPosition);
        }
        return false;
    }

    /**
     * 展开组
     * @param groupIndex 组的位置
     */
    public boolean expandGroup(int groupIndex) {
        if (mConnector != null) return mConnector.expandGroup(groupIndex);
        return false;
    }

    /**
     * 判断组是否已经展开
     * @param groupIndex 组的index position
     */
    public boolean isGroupExpanded(int groupIndex) {
        if (mConnector != null) return mConnector.isGroupExpanded(groupIndex);
        return false;
    }

    /**
     * 叠起组
     * @param groupIndex 组的位置
     */
    public boolean collapseGroup(int groupIndex) {
        if (mConnector != null) return mConnector.collapseGroup(groupIndex);
        return false;
    }

    //根据当前Adapter的Position获取GroupInfo, 不复用GroupInfo
    @Nullable
    public final GroupInfo findGroupInfoByPosition(int position) {
        return findGroupInfoByPosition(position, null);
    }

    /**
     * 根据当前Adapter的Position获取GroupInfo
     * @param position Adapter中的adapterPosition, 可是ExpandableAdapter亦可能是ConcatAdapter
     * @param groupInfo groupInfo重复使用
     */
    @Nullable
    public final GroupInfo findGroupInfoByPosition(int position, GroupInfo groupInfo) {
        return findGroupInfoByBindingPosition(position - getHeaderCount(), groupInfo);
    }

    //通过ExpandableAdapter的position查找组相关信息, ConcatAdapter时要 - headerCount不复用GroupInfo
    @Nullable
    public final GroupInfo findGroupInfoByBindingPosition(int bindingPosition) {
        return findGroupInfoByBindingPosition(bindingPosition, null);
    }

    /**
     * 通过ExpandableAdapter的position查找组相关信息, ConcatAdapter时要 - headerCount
     * @param bindingPosition ExpandableAdapter的position(即ViewHolder.getBindingAdapterPosition()), >= 0
     * @param groupInfo 重复使用GroupInfo
     */
    @MainThread
    @Nullable
    public final GroupInfo findGroupInfoByBindingPosition(int bindingPosition, GroupInfo groupInfo) {
        if (mConnector != null && bindingPosition >= 0 && bindingPosition < mConnector.getItemCount()) {
            ExpandableConnector.PositionMetadata metadata = mConnector.findPositionMetadata(bindingPosition);
            if (metadata.isGroup()) {
                if (groupInfo == null) groupInfo = new GroupInfo();
                return groupInfo.setPositionMetadata(metadata);
            }
        }
        return null;
    }

    //通过组的索引位置查找相关GroupInfo
    @Nullable
    public final GroupInfo findGroupInfoByIndex(int groupIndex) {
        return findGroupInfoByIndex(groupIndex, null);
    }

    /**
     * 通过组的索引位置查找相关GroupInfo
     */
    @Nullable
    public final GroupInfo findGroupInfoByIndex(int groupIndex, GroupInfo groupInfo) {
        if (mConnector != null && groupIndex >= 0 && groupIndex < mConnector.mAdapter.getGroupCount()) {
            ExpandableConnector.PositionMetadata metadata = mConnector.findGroupPositionMetadata(groupIndex);
            if (metadata.isGroup()) {
                if (groupInfo == null) groupInfo = new GroupInfo();
                return groupInfo.setPositionMetadata(metadata);
            }
        }
        return null;
    }

    //根据当前Adapter的Position获取ChildInfo, 不复用GroupInfo
    @Nullable
    public final ChildInfo findChildInfoByPosition(int position) {
        return findChildInfoByPosition(position, null);
    }

    /**
     * 根据当前Adapter的Position获取ChildInfo
     * @param position Adapter中的adapterPosition, 可是ExpandableAdapter亦可能是ConcatAdapter
     * @param childInfo childInfo重复使用
     */
    @Nullable
    public final ChildInfo findChildInfoByPosition(int position, ChildInfo childInfo) {
        return findChildInfoByBindingPosition(position - getHeaderCount(), childInfo);
    }

    //通过ExpandableAdapter的position查找Child相关信息, ConcatAdapter时要 - headerCount不复用ChildInfo
    @Nullable
    public final ChildInfo findChildInfoByBindingPosition(int bindingPosition) {
        return findChildInfoByBindingPosition(bindingPosition, null);
    }

    /**
     * 通过ExpandableAdapter的position查找Child相关信息, ConcatAdapter时要 - headerCount
     * @param bindingPosition ExpandableAdapter的position(即ViewHolder.getBindingAdapterPosition()) >= 0
     * @param childInfo 重复使用childInfo
     */
    @MainThread
    @Nullable
    public final ChildInfo findChildInfoByBindingPosition(int bindingPosition, ChildInfo childInfo) {
        if (mConnector != null && bindingPosition >= 0 && bindingPosition < mConnector.getItemCount()) {
            ExpandableConnector.PositionMetadata metadata = mConnector.findPositionMetadata(bindingPosition);
            if (!metadata.isGroup() && metadata.isExpanded()) {
                if (childInfo == null) childInfo = new ChildInfo();
                return childInfo.setPositionMetadata(metadata);
            }
        }
        return null;
    }

    /**
     * 通过groupIndex, childIndex查找信息, 要该groupIndex的组必须展开
     */
    @Nullable
    public final ChildInfo findChildInfoByIndex(int groupIndex, int childIndex) {
        return findChildInfoByIndex(groupIndex, childIndex, null);
    }

    /**
     * 通过groupIndex, childIndex查找信息, 要该groupIndex的组必须展开
     */
    @Nullable
    public final ChildInfo findChildInfoByIndex(int groupIndex, int childIndex, ChildInfo childInfo) {
        if (groupIndex < 0 || childIndex < 0 || mConnector == null) return null;
        if (groupIndex < mConnector.mAdapter.getGroupCount()) {
            ExpandableConnector.PositionMetadata group = mConnector.findGroupPositionMetadata(groupIndex);
            if (group.isGroup() && group.isExpanded() && childIndex < group.groupMetadata.childCount()) {
                if (childInfo == null) childInfo = new ChildInfo();
                return childInfo.setGroupPositionMetadata(group, childIndex);
            }
        }
        return null;
    }

    /**
     * 获取当前展开的Child总数, 需要在notifyData..之后获取
     */
    @MainThread
    public int getCurrentTotalChildCount() {
        if (mConnector != null) {
            return mConnector.getTotalChildCount();
        }
        return 0;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        if (mConnector != null && mConnector.saveExpandableState()) {
            return new SavedState(superState, mConnector.groupMetadataList);
        }
        return superState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        if (mConnector != null && mConnector.saveExpandableState()) {
            mConnector.restoreGroupMetadataList(savedState.groupMetadataList);
        }
    }

    /**
     * {@link #onSaveInstanceState()}, {@link #onRestoreInstanceState(Parcelable)}时状态保存
     */
    static class SavedState extends BaseSavedState {

        ArrayList<ExpandableConnector.GroupMetadata> groupMetadataList;

        SavedState(Parcelable superState, ArrayList<ExpandableConnector.GroupMetadata> groupMetadataList) {
            super(superState);
            this.groupMetadataList = groupMetadataList;
        }

        private SavedState(Parcel in) {
            super(in);
            groupMetadataList = new ArrayList<>();
            in.readList(groupMetadataList, ExpandableConnector.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeList(groupMetadataList);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class ExpandableRecyclerViewDataObserver extends AdapterDataObserver {
        @Override
        public void onChanged() {
            if (mConnector != null) {
                mConnector.refreshGroupMetadataList(true);
            }
        }

        @Override
        public void onGroupRangeChanged(int groupPosStart, int itemCount, boolean childChanged, @Nullable Object payload) {
            if (mConnector != null) {
                if (childChanged) {
                    mConnector.groupRangeChangedAndChild(groupPosStart, itemCount, payload);
                } else {
                    mConnector.groupRangeChanged(groupPosStart, itemCount, payload);
                }
            }
        }

        @Override
        public void onGroupRangeInserted(int groupPosStart, int itemCount, boolean expand) {
            if (mConnector != null) {
                if (expand) {
                    mConnector.groupExpandRangeInserted(groupPosStart, itemCount);
                } else {
                    mConnector.groupRangeInserted(groupPosStart, itemCount);
                }
            }
        }

        @Override
        public void onGroupRangeRemoved(int groupPosStart, int itemCount) {
            if (mConnector != null) {
                mConnector.groupRangeRemoved(groupPosStart, itemCount);
            }
        }

        @Override
        public void onChildRangeChanged(int groupPos, int posStart, int itemCount, @Nullable Object payload) {
            if (mConnector != null) {
                mConnector.childRangeChanged(groupPos, posStart, itemCount, payload);
            }
        }

        @Override
        public void onChildRangeInserted(int groupPos, int posStart, int itemCount) {
            if (mConnector != null) {
                mConnector.childRangeInserted(groupPos, posStart, itemCount);
            }
        }

        @Override
        public void onChildRangeRemoved(int groupPos, int posStart, int itemCount) {
            if (mConnector != null) {
                mConnector.childRangeRemoved(groupPos, posStart, itemCount);
            }
        }
    }

    /*--------------------------------  inner classes  --------------------------------*/

    /**
     * Group的相关信息, 亦可重复使用
     */
    public final static class GroupInfo implements Parcelable {
        //该Group所在的组的位置
        int index = RecyclerView.NO_POSITION;
        /**
         * 该Group所在的adapter中的位置, 也就是ViewHolder.getBindingAdapterPosition()
         * ConcatAdapter时需要注意Header的count导致的position偏移
         */
        int position = RecyclerView.NO_POSITION;
        //是否已经展开
        boolean isExpanded = false;

        //需要大量查找相关信息时, 可使用此方式
        public static GroupInfo obtainEmpty() {
            return new GroupInfo();
        }

        private GroupInfo() {}

        private GroupInfo(Parcel in) {
            index = in.readInt();
            position = in.readInt();
            isExpanded = in.readByte() != 0;
        }

        public int getIndex() {
            return index;
        }

        public int getPosition() {
            return position;
        }

        public boolean isExpanded() {
            return isExpanded;
        }

        /**
         * pool重复利用对象
         */
        private GroupInfo setPositionMetadata(ExpandableConnector.PositionMetadata metadata) {
            this.index = metadata.index;
            this.position = metadata.position;
            this.isExpanded = metadata.isExpanded();
            return this;
        }

        /**
         * pool重复利用对象
         */
        private void setGroupMetadata(ExpandableConnector.GroupMetadata groupMetadata) {
            this.index = groupMetadata.index;
            this.position = groupMetadata.position;
            this.isExpanded = true;
        }

        @Override
        public String toString() {
            return "GroupInfo{index=" + index + ", position=" + position + ", isExpanded=" + isExpanded + '}';
        }

        public static final Creator<GroupInfo> CREATOR = new Creator<GroupInfo>() {
            @Override
            public GroupInfo createFromParcel(Parcel in) {
                return new GroupInfo(in);
            }

            @Override
            public GroupInfo[] newArray(int size) {
                return new GroupInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(index);
            dest.writeInt(position);
            dest.writeByte((byte) (isExpanded ? 1 : 0));
        }
    }

    /**
     * Child的相关信息
     */
    public final static class ChildInfo implements Parcelable {
        //该Child所在的组的信息
        public final GroupInfo group;
        //该Child所在的Group所在的组的位置
        int index = RecyclerView.NO_POSITION;
        /**
         * 该Child所在的adapter中的位置, 也就是ViewHolder.getBindingAdapterPosition()
         * ConcatAdapter时需要注意Header的count导致的position偏移
         */
        int position = RecyclerView.NO_POSITION;
        //是否为最后一项
        boolean isLastChild = false;

        //需要大量查找相关信息时, 可使用此方式
        public static ChildInfo obtainEmpty() {
            return new ChildInfo();
        }

        private ChildInfo() {
            group = new GroupInfo();
        }

        private ChildInfo(Parcel in) {
            group = in.readParcelable(GroupInfo.class.getClassLoader());
            index = in.readInt();
            position = in.readInt();
            isLastChild = in.readByte() != 0;
        }

        public int getIndex() {
            return index;
        }

        public int getPosition() {
            return position;
        }

        public boolean isLastChild() {
            return isLastChild;
        }

        private ChildInfo setPositionMetadata(ExpandableConnector.PositionMetadata metadata) {
            this.group.setGroupMetadata(metadata.groupMetadata);
            this.index = metadata.index;
            this.position = metadata.position;
            this.isLastChild = metadata.isLastChild();
            return this;
        }

        private ChildInfo setGroupPositionMetadata(ExpandableConnector.PositionMetadata group, int childIndex) {
            this.group.setGroupMetadata(group.groupMetadata);
            this.index = childIndex;
            this.position = group.position + childIndex + 1;
            this.isLastChild = group.groupMetadata.lastChildPosition == this.position;
            return this;
        }

        @Override
        public String toString() {
            return "ChildInfo{group=" + group + ", index=" + index +
                    ", position=" + position + ", isLastChild=" + isLastChild + '}';
        }

        public static final Creator<ChildInfo> CREATOR = new Creator<ChildInfo>() {
            @Override
            public ChildInfo createFromParcel(Parcel in) {
                return new ChildInfo(in);
            }

            @Override
            public ChildInfo[] newArray(int size) {
                return new ChildInfo[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(group, flags);
            dest.writeInt(index);
            dest.writeInt(position);
            dest.writeByte((byte) (isLastChild ? 1 : 0));
        }
    }

    /**
     * GroupViewHolder, 包含当前组的位置是否展开, groupViewType
     */
    public static abstract class GroupViewHolder extends ViewHolder {
        //当前组的viewType即adapter里获取的viewType, 与getItemViewType()获取的是代理后的viewType不同,
        int mGroupViewType = RecyclerView.INVALID_TYPE;
        //是否已经展开组, notifyInsert,remove都不会影响此是否展开
        boolean mExpanded = false;

        public GroupViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public final int getGroupViewType() {
            return mGroupViewType;
        }

        public final boolean isExpanded() {
            return mExpanded;
        }

        /**
         * 注意 此方法与getBindingAdapterPosition一样需要遍历查找, 不复用GroupInfo
         * 当需要大量查找信息时, 当使用{@link ExpandableRecyclerView#findGroupInfoByBindingPosition(int, GroupInfo)}
         */
        public final GroupInfo getGroupInfo() {
            int bindingAdapterPosition = getBindingAdapterPosition();
            if (bindingAdapterPosition >= 0) {
                RecyclerView rv = ExpandableViewHolderCompat.getOwnerRecyclerView(this);
                if (rv instanceof ExpandableRecyclerView) {
                    return ((ExpandableRecyclerView) rv).findGroupInfoByBindingPosition(bindingAdapterPosition);
                }
            }
            return null;
        }
    }

    /**
     * ChildViewHolder包含ChildInfo方法
     */
    public abstract static class ChildViewHolder extends RecyclerView.ViewHolder {
        //当前Child的viewType即adapter里获取的viewType, 在ConcatAdapter中获取的可能不一致, 不受其影响
        int mChildViewType = RecyclerView.INVALID_TYPE;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public int getChildViewType() {
            return mChildViewType;
        }

        /**
         * 注意 此方法与getBindingAdapterPosition一样需要遍历查找, 不复用ChildInfo
         * 当需要大量查找信息时, 当使用{@link ExpandableRecyclerView#findChildInfoByBindingPosition(int, ChildInfo)}
         */
        public final ChildInfo getChildInfo() {
            int bindingAdapterPosition = getBindingAdapterPosition();
            if (bindingAdapterPosition >= 0) {
                RecyclerView rv = ExpandableViewHolderCompat.getOwnerRecyclerView(this);
                if (rv instanceof ExpandableRecyclerView) {
                    return ((ExpandableRecyclerView) rv).findChildInfoByBindingPosition(bindingAdapterPosition);
                }
            }
            return null;
        }
    }

    public abstract static class ExpandableAdapter<GVH extends GroupViewHolder, CVH extends ChildViewHolder> {

        private final AdapterDataObservable mObservable = new AdapterDataObservable();

        public void registerAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            mObservable.registerObserver(observer);
        }

        public void unregisterAdapterDataObserver(@NonNull AdapterDataObserver observer) {
            mObservable.unregisterObserver(observer);
        }

        public abstract int getGroupCount();

        public abstract int getChildCount(int groupPos);

        /**
         *  {@link ExpandableConnector#getItemViewType(int)}
         */
        public short getGroupViewType(int groupPos) {
            return 0;
        }

        /**
         * {@link ExpandableConnector#getItemViewType(int)}
         */
        public short getChildViewType(int groupPos, int childPos) {
            return 0;
        }

        @NonNull public abstract GVH onCreateGroupViewHolder(@NonNull ViewGroup parent, int viewType);

        @NonNull public abstract CVH onCreateChildViewHolder(@NonNull ViewGroup parent, int viewType);

        public void onBindGroupViewHolder(@NonNull GVH vh, int groupPos, boolean isExpanded,
                                          @NonNull List<Object> payloads) {
            onBindGroupViewHolder(vh, groupPos, isExpanded);
        }

        public abstract void onBindGroupViewHolder(@NonNull GVH vh, int groupPos, boolean isExpanded);

        public void onBindChildViewHolder(@NonNull CVH vh, int groupPos, int childPos, boolean isLastChild,
                                          @NonNull List<Object> payloads) {
            onBindChildViewHolder(vh, groupPos, childPos, isLastChild);
        }

        public abstract void onBindChildViewHolder(@NonNull CVH vh, int groupPos, int childPos, boolean isLastChild);

        //true时关联getGroupItemId, getChildItemId
        public boolean hasStableIds() {
            return false;
        }

        public int getGroupItemId(int groupPos) {
            return (int) NO_ID;
        }

        public int getChildItemId(int groupPos, int childPos) {
            return (int) NO_ID;
        }

        /**
         * 组可以点击, 影响展开或者叠起
         * @return 默认true,  false时全部展开且不能叠起
         */
        public boolean groupCanClick() {
            return true;
        }

        /**
         * 是否保存展开时的组状态{@link #onSaveInstanceState()}
         * @return true,  false时可以自己保存展开组的相关信息
         */
        public boolean saveExpandableState() {
            return  true;
        }

        /**
         * {@link ExpandableConnector#getItemViewType(int)},{@link ExpandableConnector#onBindViewHolder(ViewHolder, int)}
         * 特别在GridLayoutManager,瀑布流布局时会大量查找PositionMetadata信息, 此方法即是缓存PositionMetadata的数量大小,
         * 通常大概为界面显示的ViewHolder数量(转为2的冥次)即可
         */
        public int getPositionPoolSize() {
            return ExpandableConnector.DEF_CAPACITY;
        }

        /**
         * 叠起或者展开, 可在此方法中处理展开与叠起时的箭头动画, 必须groupCanClick true, 亦可将此当OnGroupClickListener
         * 如此组不可点击时, 还需要点击事件可在onCreateGroupViewHolder中设置点击事件
         */
        public void onGroupStateChanged(@NonNull GVH vh, int groupPos, boolean isExpanded) {}

        public void onGroupViewRecycled(@NonNull GVH holder) {}

        public void onChildViewRecycled(@NonNull CVH holder) {}

        /**
         * 在StaggeredGridLayoutManager中,要某种类型FullSpan可在此方法中实现
         */
        public void onGroupViewAttachedToWindow(@NonNull GVH holder) {
        }

        public void onGroupViewDetachedFromWindow(@NonNull GVH holder) {}

        public void onChildViewAttachedToWindow(@NonNull CVH holder) {}

        public void onChildViewDetachedFromWindow(@NonNull CVH holder) {}

        public void onAttachedToExpandableRecyclerView(@NonNull ExpandableRecyclerView erv) {
        }

        public void onDetachedFromExpandableRecyclerView(@NonNull ExpandableRecyclerView erv) {}

        public final void notifyDataSetChanged() {
            mObservable.notifyChanged();
        }

        /**------------------------------ Group notifyChanged ------------------------------*/

        //刷新Group,Child不刷新
        public final void notifyGroupChanged(int groupPos) {
            mObservable.onGroupRangeChanged(groupPos, 1);
        }

        //刷新Group,如果Group展开,Child也会刷新
        public final void notifyGroupChangedAndChild(int groupPos) {
            mObservable.onGroupRangeChangedAndChild(groupPos, 1);
        }

        //刷新Group,Child不刷新
        public final void notifyGroupChanged(int groupPos, @Nullable Object payload) {
            mObservable.onGroupRangeChanged(groupPos, 1, false, payload);
        }

        //刷新Group,如果Group展开,Child也会刷新
        public final void notifyGroupChangedAndChild(int groupPos, @Nullable Object payload) {
            mObservable.onGroupRangeChanged(groupPos, 1, true, payload);
        }

        //刷新Group,Child不刷新
        public final void notifyGroupRangeChanged(int groupPosStart, int itemCount) {
            mObservable.onGroupRangeChanged(groupPosStart, itemCount);
        }

        /**
         * 刷新Group,如果Group展开,Child也会刷新
         */
        public final void notifyGroupRangeChangedAndChild(int groupPosStart, int itemCount) {
            mObservable.onGroupRangeChangedAndChild(groupPosStart, itemCount);
        }

        /**
         * 刷新Group,Child不刷新
         */
        public final void notifyGroupRangeChanged(int groupPosStart, int itemCount, @Nullable Object payload) {
            mObservable.onGroupRangeChanged(groupPosStart, itemCount, false, payload);
        }

        /**
         * 刷新Group,如果Group展开,Child也会刷新
         */
        public final void notifyGroupRangeChangedAndChild(int groupPosStart, int itemCount, @Nullable Object payload) {
            mObservable.onGroupRangeChanged(groupPosStart, itemCount, true, payload);
        }

        //插入组不展开
        public final void notifyGroupInserted(int groupPos) {
            notifyGroupInserted(groupPos, false);
        }

        /**
         * 插入组
         * @param expand 是否展开
         */
        public final void notifyGroupInserted(int groupPos, boolean expand) {
            mObservable.onGroupRangeInserted(groupPos, 1, expand);
        }

        /**
         * 插入组, 新插入的组是不展开状态
         */
        public final void notifyGroupRangeInserted(int groupPosStart, int itemCount) {
            notifyGroupRangeInserted(groupPosStart, itemCount, false);
        }

        /**
         * 插入组
         * @param expand 是否展开
         */
        public final void notifyGroupRangeInserted(int groupPosStart, int itemCount, boolean expand) {
            mObservable.onGroupRangeInserted(groupPosStart, itemCount, expand);
        }

        public final void notifyGroupRemoved(int groupPos) {
            mObservable.onGroupRangeRemoved(groupPos, 1);
        }

        /**
         * 组的删除,连带child
         */
        public final void notifyGroupRangeRemoved(int groupPosStart, int itemCount) {
            mObservable.onGroupRangeRemoved(groupPosStart, itemCount);
        }

        /**------------------------------ Child notifyChanged ------------------------------*/

        public final void notifyChildChanged(int groupPos, int childPos) {
            mObservable.onChildRangeChanged(groupPos, childPos, 1);
        }

        public final void notifyChildChanged(int groupPos, int childPos, @Nullable Object payload) {
            mObservable.onChildRangeChanged(groupPos, childPos, 1, payload);
        }

        /**
         * 刷新组里child, 组是已经展开的
         */
        public final void notifyChildRangeChanged(int groupPos, int childPosStart, int itemCount) {
            mObservable.onChildRangeChanged(groupPos, childPosStart, itemCount);
        }

        public final void notifyChildRangeChanged(int groupPos, int childPosStart, int itemCount, @Nullable Object payload) {
            mObservable.onChildRangeChanged(groupPos, childPosStart, itemCount, payload);
        }

        public final void notifyChildInserted(int groupPos, int childPos) {
            mObservable.onChildRangeInserted(groupPos, childPos, 1);
        }

        /**
         * 展开的组中Child的插入
         */
        public final void notifyChildRangeInserted(int groupPos, int childPosStart, int itemCount) {
            mObservable.onChildRangeInserted(groupPos, childPosStart, itemCount);
        }

        public final void notifyChildRemoved(int groupPos, int childPos) {
            mObservable.onChildRangeRemoved(groupPos, childPos, 1);
        }

        /**
         * 展开组中Child的移除
         */
        public final void notifyChildRangeRemoved(int groupPos, int childPosStart, int itemCount) {
            mObservable.onChildRangeRemoved(groupPos, childPosStart, itemCount);
        }
    }

    static class AdapterDataObservable extends Observable<AdapterDataObserver> {

        public void notifyChanged() {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged();
            }
        }

        public void onGroupRangeChanged(int groupPosStart, int itemCount) {
            onGroupRangeChanged(groupPosStart, itemCount, false, null);
        }

        //group改变并附带group中的child
        public void onGroupRangeChangedAndChild(int groupPosStart, int itemCount) {
            onGroupRangeChanged(groupPosStart, itemCount, true, null);
        }

        public void onGroupRangeChanged(int groupPosStart, int itemCount, boolean childChanged, @Nullable Object payload) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onGroupRangeChanged(groupPosStart, itemCount, childChanged, payload);
            }
        }

        public void onChildRangeChanged(int groupPos, int posStart, int itemCount) {
            onChildRangeChanged(groupPos, posStart, itemCount, null);
        }

        public void onChildRangeChanged(int groupPos, int posStart, int itemCount, @Nullable Object payload) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChildRangeChanged(groupPos, posStart, itemCount, payload);
            }
        }

        public void onGroupRangeInserted(int groupPosStart, int itemCount, boolean expand) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onGroupRangeInserted(groupPosStart, itemCount, expand);
            }
        }

        public void onChildRangeInserted(int groupPos, int posStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChildRangeInserted(groupPos, posStart, itemCount);
            }
        }

        public void onGroupRangeRemoved(int groupPosStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onGroupRangeRemoved(groupPosStart, itemCount);
            }
        }

        public void onChildRangeRemoved(int groupPos, int posStart, int itemCount) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChildRangeRemoved(groupPos, posStart, itemCount);
            }
        }
    }

    /**
     * 适配器观察者
     */
    public static abstract class AdapterDataObserver {

        public void onChanged() {
        }

        /**
         * Group notifyChanged
         * @param childChanged 是否包含刷新组中的Child, false只刷新Group
         */
        public void onGroupRangeChanged(int groupPosStart, int itemCount, boolean childChanged, @Nullable Object payload) {
        }

        public void onGroupRangeInserted(int groupPosStart, int itemCount, boolean expand) {
        }

        public void onGroupRangeRemoved(int groupPosStart, int itemCount) {
        }

        public void onChildRangeChanged(int groupPos, int posStart, int itemCount, @Nullable Object payload) {
        }

        public void onChildRangeInserted(int groupPos, int posStart, int itemCount) {
        }

        public void onChildRangeRemoved(int groupPos, int posStart, int itemCount) {
        }
    }
}
