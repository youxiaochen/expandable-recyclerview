package chen.you.expandable;

import android.annotation.SuppressLint;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * author: you : 2020/10/22  作QQ/W 86207610
 * Expandable核心类
 */
final class ExpandableConnector extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
//    static final String TAG = ExpandableConnector.class.getSimpleName();
    //最大Capacity 64, 即为PositionMetadata缓存的大小
    static final int MAX_CAPACITY = 1 << 6;
    static final int DEF_CAPACITY = 1 << 4;
    //展开及叠起时防抖点击时间
    static final int CLICK_TIME = 500;

    //检测是否为GroupType的标记  二进制 0101010101010101
    static final int GROUP_TYPE_TAG = 0x5555;
    //右移后, viewType为short类型,16位
    static final int GROUP_TYPE_SR = 16;
    //校验ViewType是否为Group/Child  11111111 右侧为adapter getType 00000000 00000000
    static final int GROUP_TYPE_CHECK = GROUP_TYPE_TAG << GROUP_TYPE_SR;

    //ExpandableRecyclerView Adapter
    @NonNull
    final ExpandableRecyclerView.ExpandableAdapter mAdapter;
    //展开的组信息
    final ArrayList<GroupMetadata> groupMetadataList = new ArrayList<>();
    //记录下统共展开的Child的数量
    private int mTotalChildCount;
    //2的冥次容量后通过位移计算出position的索引数组下标, 原理参考ArrayDeque的索引index
    private final int capacityIndex;
    //缓存当前PositionMetadata的索引信息, RecyclerView在LayoutManager布局时会大量的调用getItemViewType,
    //特别是GridLayoutManager,StaggeredGridLayoutManager, 及onBindViewHolder时也会大量使用到PositionMetadata
    private final PositionMetadata[] positionPools;
    //展开与关闭时的刷新通知
    private final Object payloadObj = new Object();

    static boolean isGroupViewType(int viewType) {
        return (viewType >>> GROUP_TYPE_SR) == GROUP_TYPE_TAG;
    }

    //返回2的冥次处理
    static int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= MAX_CAPACITY) ? MAX_CAPACITY : n + 1;
    }

    ExpandableConnector(@NonNull ExpandableRecyclerView.ExpandableAdapter<?, ?> adapter) {
        this.mAdapter = adapter;
        int capacity = adapter.getPositionPoolSize();
        capacity = tableSizeFor(capacity);
        positionPools = new PositionMetadata[capacity];
        capacityIndex = capacity - 1;
        if (adapter.hasStableIds()) {
            setHasStableIds(true);
        }
    }

    //notifyData相关操作时刷新所有缓存的数据
    void refreshPools() {
        for (PositionMetadata metadata : positionPools) {
            if (metadata != null) metadata.resetState();
        }
    }

    /**
     * 是否保存展开的状态组
     */
    boolean saveExpandableState() {
        return mAdapter.saveExpandableState();
    }

    //状态恢复时
    void restoreGroupMetadataList(ArrayList<GroupMetadata> groupMetadataList) {
        if (groupMetadataList == null) return;
        this.groupMetadataList.clear();
        this.groupMetadataList.addAll(groupMetadataList);
        refreshGroupMetadataList(true);
    }

    /**
     * 刷新整个数据, 先前展开的组不变, 根据绑定的数据调整
     */
    @SuppressLint("NotifyDataSetChanged")
    @MainThread
    void refreshGroupMetadataList(boolean refreshPools) {
        int groupCount = mAdapter.getGroupCount();
        int totalChildCount = 0;
        if (mAdapter.groupCanClick()) {
            Iterator<GroupMetadata> iterator = groupMetadataList.iterator();
            int groupPosition;
            GroupMetadata leftGM = null;
            while (iterator.hasNext()) {
                GroupMetadata metadata = iterator.next();
                if (metadata.index >= groupCount) {
                    iterator.remove();
                    continue;
                }
                int childCount = mAdapter.getChildCount(metadata.index);
                if (leftGM == null) {
                    groupPosition = metadata.index;
                } else {
                    groupPosition = leftGM.lastChildPosition + (metadata.index - leftGM.index);
                }
                metadata.position = groupPosition;
                metadata.lastChildPosition = groupPosition + childCount;
                leftGM = metadata;
                totalChildCount += childCount;
            }
        } else { //全部展开的状态, groupMetadataList也为所有的组
            int i = 0;
            int groupPosition = 0;
            for (; i < groupCount && i < groupMetadataList.size(); i++) {
                GroupMetadata metadata = groupMetadataList.get(i);
                int childCount = mAdapter.getChildCount(i);

                metadata.index = i;
                metadata.position = groupPosition;
                metadata.lastChildPosition = (groupPosition += childCount);
                groupPosition++;
                totalChildCount += childCount;
            }
            if (groupCount > groupMetadataList.size()) {
                for (; i < groupCount; i++) {
                    int childCount = mAdapter.getChildCount(i);
                    groupMetadataList.add(new GroupMetadata(i, groupPosition, (groupPosition += childCount)));
                    groupPosition++;
                    totalChildCount += childCount;
                }
            } else {
                if (groupMetadataList.size() > i) {
                    groupMetadataList.subList(i, groupMetadataList.size()).clear();
                }
            }
        }
        mTotalChildCount = totalChildCount;
        if (refreshPools) refreshPools();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mAdapter.getGroupCount() + mTotalChildCount;
    }

    @Override
    public int getItemViewType(int position) {
        PositionMetadata metadata = findPositionMetadata(position);
        int viewType;
        if (metadata.isGroup()) {
            viewType = GROUP_TYPE_CHECK + mAdapter.getGroupViewType(metadata.index);
        } else {
            viewType = mAdapter.getChildViewType(metadata.groupMetadata.index, metadata.index);
        }
        return viewType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (isGroupViewType(viewType)) { //is GroupViewType
            int groupViewType = viewType - GROUP_TYPE_CHECK;
            ExpandableRecyclerView.GroupViewHolder gvh = mAdapter.onCreateGroupViewHolder(parent, groupViewType);
            gvh.mGroupViewType = groupViewType;
            if (mAdapter.groupCanClick()) {
                gvh.itemView.setOnClickListener(new View.OnClickListener() {
                    long lastClickTime;
                    @Override
                    public void onClick(View v) {
                        long currentTime = SystemClock.uptimeMillis();
                        if (currentTime - lastClickTime < CLICK_TIME) return;
                        lastClickTime = currentTime;
                        PositionMetadata metadata = findPositionMetadata(gvh.getBindingAdapterPosition());
                        int groupIndex = metadata.index;//要先记录,否则展开或闭合后会有变动
                        if (gvh.isExpanded()) {
                            if (collapseGroup(metadata)) {
                                gvh.mExpanded = false;
                                mAdapter.onGroupStateChanged(gvh, groupIndex, false);
                            }
                        } else {
                            if (expandGroup(metadata)) {
                                gvh.mExpanded = true;
                                mAdapter.onGroupStateChanged(gvh, groupIndex, true);
                            }
                        }
                    }
                });
            }
            return gvh;
        } else {
            ExpandableRecyclerView.ChildViewHolder cvh = mAdapter.onCreateChildViewHolder(parent, viewType);
            cvh.mChildViewType = viewType;
            return cvh;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty() && payloads.get(0) == payloadObj) {
            PositionMetadata metadata = findPositionMetadata(position);
            if (metadata.isGroup()) {
                ExpandableRecyclerView.GroupViewHolder gvh = (ExpandableRecyclerView.GroupViewHolder) holder;
                gvh.mExpanded = metadata.isExpanded();
            }
            return;
        }
        PositionMetadata metadata = findPositionMetadata(position);
        if (metadata.isGroup()) {
            ExpandableRecyclerView.GroupViewHolder gvh = (ExpandableRecyclerView.GroupViewHolder) holder;
            gvh.mExpanded = metadata.isExpanded();
            mAdapter.onBindGroupViewHolder(gvh, metadata.index, metadata.isExpanded(), payloads);
        } else {
            ExpandableRecyclerView.ChildViewHolder cvh = (ExpandableRecyclerView.ChildViewHolder) holder;
            mAdapter.onBindChildViewHolder(cvh, metadata.groupMetadata.index, metadata.index, metadata.isLastChild(), payloads);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        //nothing
    }

    @Override
    public long getItemId(int position) {
        PositionMetadata metadata = findPositionMetadata(position);
        if (metadata.isGroup()) {
            return mAdapter.getGroupItemId(metadata.index);
        } else {
            return mAdapter.getChildItemId(metadata.groupMetadata.index, metadata.index);
        }
    }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ExpandableRecyclerView.GroupViewHolder) {
            mAdapter.onGroupViewRecycled((ExpandableRecyclerView.GroupViewHolder) holder);
        } else if (holder instanceof ExpandableRecyclerView.ChildViewHolder) {
            mAdapter.onChildViewRecycled((ExpandableRecyclerView.ChildViewHolder) holder);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ExpandableRecyclerView.GroupViewHolder) {
            mAdapter.onGroupViewAttachedToWindow((ExpandableRecyclerView.GroupViewHolder) holder);
        } else if (holder instanceof ExpandableRecyclerView.ChildViewHolder) {
            mAdapter.onChildViewAttachedToWindow((ExpandableRecyclerView.ChildViewHolder) holder);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        if (holder instanceof ExpandableRecyclerView.GroupViewHolder) {
            mAdapter.onGroupViewDetachedFromWindow((ExpandableRecyclerView.GroupViewHolder) holder);
        } else if (holder instanceof ExpandableRecyclerView.ChildViewHolder) {
            mAdapter.onChildViewDetachedFromWindow((ExpandableRecyclerView.ChildViewHolder) holder);
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView rv) {
        mAdapter.onAttachedToExpandableRecyclerView((ExpandableRecyclerView) rv);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView rv) {
        mAdapter.onDetachedFromExpandableRecyclerView((ExpandableRecyclerView) rv);
        Arrays.fill(positionPools, null);
    }

    boolean isGroupType(int position) {
        PositionMetadata metadata = findPositionMetadata(position);
        return metadata.isGroup();
    }

    int getTotalChildCount() {
        return mTotalChildCount;
    }

    /**
     * 判断组是否已经展开
     * @param groupIndex 组的index position
     */
    boolean isGroupExpanded(int groupIndex) {
        if (groupIndex >= mAdapter.getGroupCount() || groupIndex < 0) return false;
        return findExpandedIndex(groupIndex) >= 0;
    }

    /**
     * 展开组
     * @param groupIndex 组的位置
     */
    boolean expandGroup(int groupIndex) {
        if (groupIndex >= mAdapter.getGroupCount() || groupIndex < 0) return false;
        PositionMetadata metadata = findGroupPositionMetadata(groupIndex);
        return expandGroup(metadata);
    }

    /**
     * 叠起组
     * @param groupIndex 组的位置
     */
    boolean collapseGroup(int groupIndex) {
        if (groupIndex >= mAdapter.getGroupCount() || groupIndex < 0) return false;
        return collapseGroupByIndex(findExpandedIndex(groupIndex));
    }

    //展开组
    boolean expandGroup(PositionMetadata metadata) {
        if (metadata.isExpanded()) return false;
        int expandChildCount = mAdapter.getChildCount(metadata.index);
        GroupMetadata insertMetadata = metadata.newGroup(expandChildCount);
        //将组信息添加进组数据中,并重新计算该组后的所有索引
        groupMetadataList.add(metadata.groupInsertIndex, insertMetadata);
        for (int i = metadata.groupInsertIndex + 1; i < groupMetadataList.size(); i++) {
            groupMetadataList.get(i).offset(expandChildCount);
        }
        mTotalChildCount += expandChildCount;

        notifyItemChanged(insertMetadata.position, payloadObj);
        refreshPools();
        if (expandChildCount > 0) {
            notifyItemRangeInserted(insertMetadata.position + 1, expandChildCount);
        }
        return true;
    }

    //叠起组
    boolean collapseGroup(PositionMetadata metadata) {
        if (metadata.isGroup()) {
            return collapseGroupByIndex(metadata.groupInsertIndex);
        }
        return false;
    }

    /**
     * 叠起组
     * @param groupInsertIndex 已经展开的组在groupMetadataList中的位置
     */
    private boolean collapseGroupByIndex(int groupInsertIndex) {
        if (groupInsertIndex < 0) return false;
        GroupMetadata rmMetadata = groupMetadataList.remove(groupInsertIndex);
        int rmChildCount = rmMetadata.childCount();
        for (int i = groupInsertIndex; i < groupMetadataList.size(); i++) {
            groupMetadataList.get(i).offset(-rmChildCount);
        }
        mTotalChildCount -= rmChildCount;

        notifyItemChanged(rmMetadata.position, payloadObj);
        refreshPools();
        if (rmChildCount > 0) {
            notifyItemRangeRemoved(rmMetadata.position + 1, rmChildCount);
        }
        return true;
    }

    /**
     * 先通过position索引位置查找出缓存的位置信息
     */
    @NonNull PositionMetadata findPositionMetadata(int position) {
        int index = position & capacityIndex;
        PositionMetadata metadata = positionPools[index];
        if (metadata != null && metadata.isSamePosition(position)) {
//            Log.d(TAG, "findPositionMetadata use cache------------------ " + position);
            return metadata;
        }
//        Log.d(TAG, "findPositionMetadata  <<<<<<<<<<<<<<<<< " + position);
        metadata = getPositionMetadataByPosition(position, metadata);
        positionPools[index] = metadata;
        return metadata;
    }

    /**
     * 通过adapter的position查找PositionMetadata
     * @param position adapter position
     */
    @MainThread
    @NonNull
    private PositionMetadata getPositionMetadataByPosition(int position, PositionMetadata sPool) {
        if (groupMetadataList.isEmpty()) { //都未展开时
            return PositionMetadata.obtainGroup(sPool, position, position, 0);
        }
        //二分查找
        int leftIndex = 0;
        int rightIndex = groupMetadataList.size() - 1;
        int midIndex = 0;
        GroupMetadata midGM;
        while (leftIndex <= rightIndex) {
            midIndex = (rightIndex - leftIndex) / 2 + leftIndex;
            midGM = groupMetadataList.get(midIndex);
            if (position == midGM.position) {
                return PositionMetadata.obtainGroup(sPool, midGM, midIndex);
            }
            if (position > midGM.position) {
                if (position <= midGM.lastChildPosition) {
                    int childIndex = position - (midGM.position + 1);
                    return PositionMetadata.obtainChild(sPool, midGM, childIndex, position);
                }
                leftIndex = midIndex + 1;
            } else {
                rightIndex = midIndex - 1;
            }
        }
        //展开列表里没找到的肯定就是未展开的组项
        if (leftIndex > midIndex) {
            GroupMetadata leftGM = groupMetadataList.get(leftIndex - 1);
            int groupIndex = (position - leftGM.lastChildPosition) + leftGM.index;
            return PositionMetadata.obtainGroup(sPool, groupIndex, position, leftIndex);
        } else {
            GroupMetadata rightGM = groupMetadataList.get(++rightIndex);
            int groupIndex = rightGM.index - (rightGM.position - position);
            return PositionMetadata.obtainGroup(sPool, groupIndex, position, rightIndex);
        }
    }

    /**
     * 查找GroupIndex对应的组信息
     */
    @NonNull PositionMetadata findGroupPositionMetadata(int groupIndex) {
        if (groupMetadataList.isEmpty()) {
            return obtainGroupPositionMetadata(groupIndex, groupIndex, 0);
        }
        //二分查找
        int leftIndex = 0;
        int rightIndex = groupMetadataList.size() - 1;
        int midIndex = 0;
        GroupMetadata midGM;
        while (leftIndex <= rightIndex) {
            midIndex = (rightIndex - leftIndex) / 2 + leftIndex;
            midGM = groupMetadataList.get(midIndex);
            if (groupIndex > midGM.index) {
                leftIndex = midIndex + 1;
            } else if (groupIndex < midGM.index) {
                rightIndex = midIndex - 1;
            } else {
                //找到索引,列表中存在的肯定就是已经展开了的
                return obtainGroupPositionMetadata(midGM, midIndex);
            }
        }
        if (leftIndex > midIndex) {
            GroupMetadata leftGM = groupMetadataList.get(leftIndex - 1);
            int position = leftGM.lastChildPosition + (groupIndex - leftGM.index);
            return obtainGroupPositionMetadata(groupIndex, position, leftIndex);
        } else {
            GroupMetadata rightGM = groupMetadataList.get(++rightIndex);
            int position = rightGM.position - (rightGM.index - groupIndex);
            return obtainGroupPositionMetadata(groupIndex, position, rightIndex);
        }
    }

    //先通过position查找缓存, 没有再生成
    @NonNull
    private PositionMetadata obtainGroupPositionMetadata(int groupIndex, int position, int groupInsertIndex) {
        int index = position & capacityIndex;
        PositionMetadata metadata = positionPools[index];
        if (metadata != null && metadata.isSamePosition(position) && metadata.isGroup()) {
//            Log.d(TAG, "obtainGroupPositionMetadata cache............... " + position);
            return metadata;
        }
//        Log.d(TAG, "findGroupPositionMetadata  ~~~~~~~~~~~~ " + position);
        metadata = PositionMetadata.obtainGroup(metadata, groupIndex, position, groupInsertIndex);
        positionPools[index] = metadata;
        return metadata;
    }

    //先通过GroupMetadata的position查找缓存, 没有再生成
    @NonNull
    private PositionMetadata obtainGroupPositionMetadata(GroupMetadata groupMetadata, int groupInsertIndex) {
        int index = groupMetadata.position & capacityIndex;
        PositionMetadata metadata = positionPools[index];
        if (metadata != null && metadata.isSamePosition(groupMetadata.position) && metadata.isGroup()) {
//            Log.d(TAG, "obtainGroupPositionMetadata cache............... " + groupMetadata.position);
            return metadata;
        }
//        Log.d(TAG, "findGroupPositionMetadata  ~~~~~~~~~~~~ " + groupMetadata.position);
        metadata = PositionMetadata.obtainGroup(metadata, groupMetadata, groupInsertIndex);
        positionPools[index] = metadata;
        return metadata;
    }

    /**
     * 根据组的索引查找展开的组数据在列表中的位置
     * @param groupIndex group index
     */
    private int findExpandedIndex(int groupIndex) {
        if (!groupMetadataList.isEmpty()) {//二分查找
            int leftIndex = 0;
            int rightIndex = groupMetadataList.size() - 1;
            int midIndex;
            GroupMetadata midGM;
            while (leftIndex <= rightIndex) {
                midIndex = (rightIndex - leftIndex) / 2 + leftIndex;
                midGM = groupMetadataList.get(midIndex);
                if (groupIndex > midGM.index) {
                    leftIndex = midIndex + 1;
                } else if (groupIndex < midGM.index) {
                    rightIndex = midIndex - 1;
                } else {//找到索引,列表中存在的肯定就是已经展开了的
                    return midIndex;
                }
            }
        }
        return -1;
    }

    /**
     * 刷新Group不刷新展开组的Child项
     */
    void groupRangeChanged(int groupPosStart, int itemCount, @Nullable Object payload) {
        if (groupMetadataList.isEmpty()) {
            notifyItemRangeChanged(groupPosStart, itemCount, payload);
            return;
        }
        int groupPosEnd = groupPosStart + itemCount - 1;
        GroupMetadata firstGM = groupMetadataList.get(0);
        int listStartPos = firstGM.index;
        if (listStartPos >= groupPosEnd) {
            notifyItemRangeChanged(groupPosStart, itemCount, payload);
            return;
        }
        int startPosition = groupPosStart;
        int startIndex = 0;
        if (groupPosStart > listStartPos) {
            GroupMetadata leftGM = firstGM;
            startIndex = 1;
            for (; startIndex < groupMetadataList.size(); startIndex++) {
                GroupMetadata metadata = groupMetadataList.get(startIndex);
                if (metadata.index >= groupPosStart) {
                    break;
                }
                leftGM = metadata;
            }
            startPosition = leftGM.lastChildPosition + (groupPosStart - leftGM.index);
        }
        int notifiedItemCount = 0;
        int notifyItemCount;
        for (; startIndex < groupMetadataList.size(); startIndex++) {
            GroupMetadata metadata = groupMetadataList.get(startIndex);
            if (metadata.index > groupPosEnd) break;
            if (metadata.childCount() == 0) {
                continue;
            }
            notifyItemCount = metadata.position - startPosition + 1;
            notifiedItemCount += notifyItemCount;
            notifyItemRangeChanged(startPosition, notifyItemCount, payload);
            startPosition = metadata.lastChildPosition + 1;
        }
        if (itemCount > notifiedItemCount) {
            notifyItemRangeChanged(startPosition, (itemCount - notifiedItemCount), payload);
        }
    }

    /**
     * group changed并刷新展开的组中的Child
     */
    void groupRangeChangedAndChild(int groupPosStart, int itemCount, @Nullable Object payload) {
        int groupPosEnd = groupPosStart + itemCount - 1;
        int totalCount = itemCount;
        GroupMetadata leftGM = null;
        for (int i = groupMetadataList.size() - 1; i >= 0; i--) {
            GroupMetadata metadata = groupMetadataList.get(i);
            if (metadata.index < groupPosStart) {
                leftGM = metadata;
                break;
            }
            if (metadata.index <= groupPosEnd) {
                totalCount += metadata.childCount();
            }
        }
        int startPosition = groupPosStart;
        if (leftGM != null) {//删除的起点左侧还有展开项
            startPosition = leftGM.lastChildPosition + (groupPosStart - leftGM.index);
        }
//        Log.d(TAG, "notify notifyItemRangeChanged " + startPosition +"  " + totalCount);
        notifyItemRangeChanged(startPosition, totalCount, payload);
    }

    /**
     * 添加组, 添加进的组是未展开, 需要展开时再调用{@link #expandGroup(int)}
     * 或者 {@link #groupExpandRangeInserted(int, int)}
     */
    void groupRangeInserted(int groupPosStart, int itemCount) {
        int backIndex = -1;//查找要插入的索引项的后面的组信息
        for (int i = 0; i < groupMetadataList.size(); i++) {
            GroupMetadata metadata = groupMetadataList.get(i);
            if (metadata.index >= groupPosStart) {
                backIndex = i;
                break;
            }
        }
        int startPosition;
        if (backIndex < 0) { //添加的位置处后边没有展开的组或者组空
            if (groupMetadataList.isEmpty()) {
                startPosition = groupPosStart;
            } else {
                GroupMetadata leftGM = groupMetadataList.get(groupMetadataList.size() - 1);
                startPosition = leftGM.lastChildPosition + (groupPosStart - leftGM.index);
            }
        } else {
            GroupMetadata rightGM = groupMetadataList.get(backIndex);
            startPosition = rightGM.position - (rightGM.index - groupPosStart);
            for (int i = backIndex; i < groupMetadataList.size(); i++) {
                GroupMetadata groupMetadata = groupMetadataList.get(i);
                groupMetadata.offset(itemCount);//后面的每组项偏移
                groupMetadata.offsetIndex(itemCount);
            }
        }
        refreshPools();
        notifyItemRangeInserted(startPosition, itemCount);
    }

    //添加组并展开所添加的组
    void groupExpandRangeInserted(int groupPosStart, int itemCount) {
        int backIndex = -1;//查找要插入的索引项的后面的组信息
        for (int i = 0; i < groupMetadataList.size(); i++) {
            GroupMetadata metadata = groupMetadataList.get(i);
            if (metadata.index >= groupPosStart) {
                backIndex = i;
                break;
            }
        }
        int startPosition;
        int totalChildCount = 0;
        int totalCount;
        if (backIndex < 0) { //添加的位置处后边没有展开的组或者展开的组list空
            if (groupMetadataList.isEmpty()) {
                startPosition = groupPosStart;
            } else {
                GroupMetadata leftGM = groupMetadataList.get(groupMetadataList.size() - 1);
                startPosition = leftGM.lastChildPosition + (groupPosStart - leftGM.index);
            }
            int position = startPosition;
            int groupIndex = groupPosStart;
            for (int i = 0; i < itemCount; i++) {
                int childCount = mAdapter.getChildCount(groupIndex);
                groupMetadataList.add(new GroupMetadata(groupIndex++, position, position += childCount));
                position++;
                totalChildCount += childCount;
            }
            totalCount = totalChildCount + itemCount;
        } else {
            GroupMetadata rightGM = groupMetadataList.get(backIndex);
            startPosition = rightGM.position - (rightGM.index - groupPosStart);

            int position = startPosition;
            int groupIndex = groupPosStart;
            for (int i = 0; i < itemCount; i++) {
                int childCount = mAdapter.getChildCount(groupIndex);
                groupMetadataList.add(backIndex++, new GroupMetadata(groupIndex++, position, position += childCount));
                position++;
                totalChildCount += childCount;
            }
            totalCount = totalChildCount + itemCount;
            for (int i = backIndex; i < groupMetadataList.size(); i++) {
                GroupMetadata groupMetadata = groupMetadataList.get(i);
                groupMetadata.offset(totalCount);//后面的每组项偏移
                groupMetadata.offsetIndex(itemCount);
            }
        }
        mTotalChildCount += totalChildCount;
        refreshPools();
        notifyItemRangeInserted(startPosition, totalCount);
    }

    /**
     * 移除组项
     * @param groupPosStart groupIndex
     * @param itemCount remove group count
     */
    void groupRangeRemoved(int groupPosStart, int itemCount) {
        int groupPosEnd = groupPosStart + itemCount - 1;
        Iterator<GroupMetadata> iterator = groupMetadataList.iterator();
        GroupMetadata rightGM = null;
        int totalChildCount = 0;
        int index = 0;
        while (iterator.hasNext()) {
            GroupMetadata metadata = iterator.next();
            if (metadata.index > groupPosEnd) {
                if (rightGM == null) rightGM = metadata;
                break;
            }
            if (metadata.index >= groupPosStart) {
                if (rightGM == null) rightGM = metadata;
                totalChildCount += metadata.childCount();
                iterator.remove();
            } else {
                index++;
            }
        }
        int startPosition = groupPosStart;
        if (rightGM != null) {
            startPosition = rightGM.position - (rightGM.index - groupPosStart);
        } else if (!groupMetadataList.isEmpty()){ //展开的组的后
            GroupMetadata leftGM = groupMetadataList.get(groupMetadataList.size() - 1);
            startPosition = leftGM.lastChildPosition + (groupPosStart - leftGM.index);
        }
        int totalCount = itemCount + totalChildCount;
        for (int i = index; i < groupMetadataList.size(); i++) {
            GroupMetadata metadata = groupMetadataList.get(i);
            metadata.offset(-totalCount);
            metadata.offsetIndex(-itemCount);
        }
//        Log.d(TAG, "notify groupRangeRemoved " + startPosition +"  " + totalCount);
        mTotalChildCount -= totalChildCount;
        refreshPools();
        notifyItemRangeRemoved(startPosition, totalCount);
    }

    //展开项里的child变动
    void childRangeChanged(int groupPos, int posStart, int itemCount, @Nullable Object payload) {
        int findIndex = findExpandedIndex(groupPos);
        if (findIndex < 0) return;
        GroupMetadata metadata = groupMetadataList.get(findIndex);
//        Log.d(TAG, "childRangeChanged " + groupPos + " -- " + posStart +" -- " + itemCount);
        notifyItemRangeChanged(metadata.position + 1 + posStart, itemCount, payload);
    }

    //展开项里的child插入
    void childRangeInserted(int groupPos, int posStart, int itemCount) {
        int findIndex = findExpandedIndex(groupPos);
        if (findIndex < 0) return;
        GroupMetadata metadata = groupMetadataList.get(findIndex);
        metadata.offsetChild(itemCount);
        for (int i = findIndex + 1; i < groupMetadataList.size(); i++) {
            groupMetadataList.get(i).offset(itemCount);
        }
        mTotalChildCount += itemCount;
//        Log.d(TAG, "childRangeInserted " + groupPos + " -- " + posStart +" -- " + itemCount);
        refreshPools();
        notifyItemRangeInserted(metadata.position + 1 + posStart, itemCount);
    }

    //展开项里的child移除
    void childRangeRemoved(int groupPos, int posStart, int itemCount) {
        int findIndex = findExpandedIndex(groupPos);
        if (findIndex < 0) return;
        GroupMetadata metadata = groupMetadataList.get(findIndex);
        metadata.offsetChild(-itemCount);
        for (int i = findIndex + 1; i < groupMetadataList.size(); i++) {
            groupMetadataList.get(i).offset(-itemCount);
        }
        mTotalChildCount -= itemCount;
//        Log.d(TAG, "childRangeRemoved " + groupPos + " -- " + posStart +" -- " + itemCount);
        refreshPools();
        notifyItemRangeRemoved(metadata.position + 1 + posStart, itemCount);
    }

    /**
     * 展开组的信息
     */
    static class GroupMetadata implements Parcelable {
        //在组中的 position
        int index;
        //在adapter position
        int position;
        //最后一child position
        int lastChildPosition;

        GroupMetadata(int index, int position, int lastChildPosition) {
            this.index = index;
            this.position = position;
            this.lastChildPosition = lastChildPosition;
        }

        protected GroupMetadata(Parcel in) {
            index = in.readInt();
            position = in.readInt();
            lastChildPosition = in.readInt();
        }

        /**
         * 在GroupMetadata列表中插入新的GroupMetadata后, 插入的索引后的都需要偏移position与lastChildPosition
         * @param offset 偏移量
         */
        void offset(int offset) {
            position += offset;
            lastChildPosition += offset;
        }

        //组中的位置偏移
        void offsetIndex(int offset) {
            index += offset;
        }

        //在该组中插入或者删除child
        void offsetChild(int childCount) {
            lastChildPosition += childCount;
        }

        //可通过最后一个child项计算出childCount
        int childCount() {
            return lastChildPosition - position;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(index);
            dest.writeInt(position);
            dest.writeInt(lastChildPosition);
        }

        public static final Creator<GroupMetadata> CREATOR = new Creator<GroupMetadata>() {
            @Override
            public GroupMetadata createFromParcel(Parcel in) {
                return new GroupMetadata(in);
            }

            @Override
            public GroupMetadata[] newArray(int size) {
                return new GroupMetadata[size];
            }
        };
    }

    /**
     * Position信息
     */
    static class PositionMetadata {
        /**
         * CHILD, GROUP两限制类型
         */
        final static int CHILD = 1;
        final static int GROUP = 2;
        //Group or Child
        int type;
        //组数据, 不null时即为child, type亦为CHILD
        GroupMetadata groupMetadata;
        //当前组中的索引, 可能是group也可能是child, groupMetadata != null时为child
        int index = RecyclerView.NO_POSITION;
        //当前adapter position, 可能是group也可能是child, groupMetadata != null时为child
        int position = RecyclerView.NO_POSITION;
        /**
         * 当为GROUP类型并需要展开时, 此值为需要往GroupMetadata插入的索引位置
         * 当需要叠起时, 此值为已经展开组的插入的索引位置
         */
        int groupInsertIndex = RecyclerView.NO_POSITION;

        private PositionMetadata() {
        }

        static PositionMetadata obtainGroup(PositionMetadata sPool, int index, int position, int groupInsertIndex) {
            if (sPool == null) {
                sPool = new PositionMetadata();
            } else {
                sPool.groupMetadata = null;
            }

            sPool.type = GROUP;
            sPool.index = index;
            sPool.position = position;
            sPool.groupInsertIndex = groupInsertIndex;
            return sPool;
        }

        static PositionMetadata obtainGroup(PositionMetadata sPool, GroupMetadata metadata, int groupInsertIndex) {
            if (sPool == null) {
                sPool = new PositionMetadata();
            }
            sPool.type = GROUP;
            sPool.index = metadata.index;
            sPool.position = metadata.position;
            sPool.groupMetadata = metadata;
            sPool.groupInsertIndex = groupInsertIndex;
            return sPool;
        }

        static PositionMetadata obtainChild(PositionMetadata sPool, GroupMetadata metadata, int index, int position) {
            if (sPool == null) {
                sPool = new PositionMetadata();
            } else {
                sPool.groupInsertIndex = RecyclerView.NO_POSITION;
            }

            sPool.type = CHILD;
            sPool.groupMetadata = metadata;
            sPool.index = index;
            sPool.position = position;
            return sPool;
        }

        private void resetState() {
            groupMetadata = null;
            type = 0;
            index = RecyclerView.NO_POSITION;
            position = RecyclerView.NO_POSITION;
            groupInsertIndex = RecyclerView.NO_POSITION;
        }

        boolean isGroup() {
            return type == GROUP;
        }

        boolean isLastChild() {
            return groupMetadata != null && groupMetadata.lastChildPosition == position;
        }

        boolean isExpanded() {
            return groupMetadata != null;
        }

        boolean isSamePosition(int position) {
            return this.position == position;
        }

        /**
         * 生成组数据
         * @param childCount 该index组中child的数量
         */
        GroupMetadata newGroup(int childCount) {
            return new GroupMetadata(index, position, position + childCount);
        }
    }
}
