/*
 * Project:  NextGIS Mobile
 * Purpose:  Mobile GIS for Android.
 * Author:   Dmitry Baryshnikov (aka Bishop), bishop.dev@gmail.com
 * Author:   NikitaFeodonit, nfeodonit@yandex.com
 * Author:   Stanislav Petriakov, becomeglory@gmail.com
 * ****************************************************************************
 * Copyright (c) 2012-2016 NextGIS, info@nextgis.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.nextgis.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.nextgis.libngui.R;
import com.nextgis.libngui.adapter.ListSelectorAdapter;
import com.nextgis.store.bindings.DirectoryContainer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.libngui.util.ConstantsUI.*;


/**
 * Adapter to represent folders and files
 */
public class LocalResourceNativeListAdapter
        extends ListSelectorAdapter
{
    protected PathAdapter                           mPathAdapter;
    protected OnChangePathListener                  mOnChangePathListener;
    protected LocalResourceNativeDirectoryContainer mContainer;


    public LocalResourceNativeListAdapter()
    {
        super();
    }


    public void setPathAdapter(
            LinearLayout linearLayout,
            String path)
    {
        mPathAdapter = new PathAdapter(linearLayout);
        mPathAdapter.setPath(path);
    }


    @Override
    protected int getItemViewResId()
    {
        return R.layout.item_local_resource;
    }


    @Override
    protected ListSelectorAdapter.ViewHolder getViewHolder(View itemView)
    {
        //final Context context = itemView.getContext();

        setOnItemClickListener(new ListSelectorAdapter.ViewHolder.OnItemClickListener()
        {
            @Override
            public void onItemClick(int position)
            {
                LocalResourceNativeListItem item = mContainer.getItems().get(position);
                int type = item.getItemType();
                if (FILETYPE_PARENT == type) {
                    setCurrentPath(mContainer.getParentPath());
                }
                if (FILETYPE_FOLDER == type) {
                    setCurrentPath(mContainer.getItemPath(position));
                }
            }
        });

        setOnItemLongClickListener(null);

        return new LocalResourceNativeListAdapter.ViewHolder(
                itemView, mSingleSelectable, mOnItemClickListener, mOnItemLongClickListener);
    }


    protected void setCurrentPath(String path)
    {
        if (null != mPathAdapter) {
            mPathAdapter.setPath(path);
        }

        if (null != mOnChangePathListener) {
            mOnChangePathListener.onChangePath(path);
        }
    }


//    @Override
//    public void setSelection(
//            int position,
//            boolean selection)
//    {
//        LocalResourceListNativeItem item = mResources.get(position);
//
//        switch (item.mType) {
//            case FILETYPE_FOLDER:
//            case FILETYPE_UNKNOWN:
//                selection = false;
//                break;
//        }
//
//        super.setSelection(position, selection);
//    }


    @Override
    public void onBindViewHolder(
            ListSelectorAdapter.ViewHolder holder,
            int position)
    {
        super.onBindViewHolder(holder, position);

        Context context = holder.mCheckBox.getContext();

        LocalResourceNativeListAdapter.ViewHolder viewHolder =
                (LocalResourceNativeListAdapter.ViewHolder) holder;
        LocalResourceNativeListItem item = mContainer.getItems().get(position);

        viewHolder.mFileIcon.setVisibility(View.VISIBLE);
        viewHolder.mFileName.setVisibility(View.VISIBLE);
        viewHolder.mFileDesc.setVisibility(View.VISIBLE);

        switch (item.mItemType) {
            case FILETYPE_PARENT:
                if (TextUtils.isEmpty(mContainer.getParentPath())) {
                    viewHolder.mFileIcon.setVisibility(View.GONE);
                    viewHolder.mFileName.setVisibility(View.GONE);
                    viewHolder.mFileDesc.setVisibility(View.GONE);
                } else {
                    viewHolder.mFileIcon.setImageDrawable(
                            ContextCompat.getDrawable(context, R.drawable.ic_folder));
                    viewHolder.mFileName.setText(context.getString(R.string.up_dots));
                    viewHolder.mFileDesc.setText(context.getString(R.string.up));
                }
                break;
            case FILETYPE_FOLDER:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_folder));
                viewHolder.mFileName.setText(item.getFullName());
                viewHolder.mFileDesc.setText(context.getString(R.string.folder));
                break;
            case FILETYPE_ZIP:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_zip));
                viewHolder.mFileName.setText(item.getFullName());
                viewHolder.mFileDesc.setText(context.getString(R.string.zip_file));
                break;
            case FILETYPE_FB:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_formbuilder));
                viewHolder.mFileName.setText(item.getFullName());
                viewHolder.mFileDesc.setText(context.getString(R.string.formbuilder_file));
                break;
            case FILETYPE_GEOJSON:
                viewHolder.mFileIcon.setImageDrawable(
                        ContextCompat.getDrawable(context, R.drawable.ic_geojson));
                viewHolder.mFileName.setText(item.getFullName());
                viewHolder.mFileDesc.setText(context.getString(R.string.geojson_file));
                break;
            case FILETYPE_SHP:
                viewHolder.mFileIcon.setImageDrawable( // TODO: icon for shp file
                        ContextCompat.getDrawable(context, R.drawable.ic_geojson));
                viewHolder.mFileName.setText(item.getFullName());
                viewHolder.mFileDesc.setText(context.getString(R.string.shape_file));
                break;
            case FILETYPE_UNKNOWN:
                viewHolder.mFileIcon.setImageDrawable( // TODO: icon for unknown file
                        ContextCompat.getDrawable(context, R.drawable.ic_formbuilder));
                viewHolder.mFileName.setText(item.getFullName());
                viewHolder.mFileDesc.setText(context.getString(R.string.unknown_file));
                break;
        }

        viewHolder.mCheckBox.setVisibility(item.isVisibleCheckBox() ? View.VISIBLE : View.GONE);
        viewHolder.mCheckBox.setEnabled(item.isSelectable());
    }


    public static class ViewHolder
            extends ListSelectorAdapter.ViewHolder
            implements View.OnClickListener
    {
        LinearLayout mItemLayout;
        ImageView    mFileIcon;
        TextView     mFileName;
        TextView     mFileDesc;


        public ViewHolder(
                View itemView,
                boolean singleSelectable,
                OnItemClickListener clickListener,
                OnItemLongClickListener longClickListener)
        {
            super(itemView, singleSelectable, clickListener, longClickListener);

            mItemLayout = (LinearLayout) itemView.findViewById(R.id.item_layout);
            mFileIcon = (ImageView) itemView.findViewById(R.id.file_icon);
            mFileName = (TextView) itemView.findViewById(R.id.file_name);
            mFileDesc = (TextView) itemView.findViewById(R.id.file_desc);
        }
    }


    public void setContainer(
            DirectoryContainer source,
            int itemTypeMask,
            boolean canWrite)
    {
        clearSelectionForAll();
        mContainer = new LocalResourceNativeDirectoryContainer(itemTypeMask, canWrite);
        mContainer.fillItems(source);
        notifyDataSetChanged();
    }


    public LocalResourceNativeDirectoryContainer getContainer()
    {
        return mContainer;
    }


    @Override
    public long getItemId(int position)
    {
        if (null == mContainer) {
            return super.getItemId(position);
        }
        return position;
    }


    @Override
    public int getItemCount()
    {
        if (null == mContainer) {
            return 0;
        }
        return mContainer.getItems().size();
    }


    public List<LocalResourceNativeListItem> getSelectedItems()
    {
        List<Integer> selIds = super.getSelectedItemsIds();
        List<LocalResourceNativeListItem> selRes = new ArrayList<>(selIds.size());
        for (Integer id : selIds) {
            selRes.add(mContainer.getItems().get(id));
        }
        return selRes;
    }


    public void setSelection(
            String path,
            boolean selection)
    {
        for (int i = 0, mResourcesSize = mContainer.getItems().size(); i < mResourcesSize; i++) {
            if (mContainer.getItemPath(i).equals(path)) {
                super.setSelection(i, selection);
            }
        }
    }


    public void setOnChangePathListener(OnChangePathListener listener)
    {
        mOnChangePathListener = listener;
    }


    public interface OnChangePathListener
    {
        void onChangePath(String path);
    }


    /**
     * A path view class. the path is a resources names divide by arrows in head of dialog. If user
     * click on name, the dialog follow the specified path.
     */
    public class PathAdapter
    {
        protected LinearLayout mLinearLayout;
        protected Context      mContext;


        public PathAdapter(LinearLayout linearLayout)
        {
            mLinearLayout = linearLayout;
            mContext = linearLayout.getContext();
        }


        public void setPath(String path)
        {
            if (null == mLinearLayout) {
                return;
            }
            mLinearLayout.removeAllViewsInLayout();

            File parent = new File(path);
            while (null != parent) {
                final String parentPath = parent.getPath();
                LayoutInflater inflater = LayoutInflater.from(mContext);

                TextView nameView =
                        (TextView) inflater.inflate(R.layout.item_path_name, mLinearLayout, false);
                nameView.setText(parent.getName());
                nameView.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        setCurrentPath(parentPath);
                    }
                });
                mLinearLayout.addView(nameView, 0);

                parent = parent.getParentFile();
                if (null != parent) {
                    ImageView imageView =
                            (ImageView) inflater.inflate(R.layout.item_path_icon, mLinearLayout,
                                    false);
                    mLinearLayout.addView(imageView, 0);
                }
            }
        }
    }
}
