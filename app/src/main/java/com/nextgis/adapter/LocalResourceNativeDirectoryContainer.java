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

import com.nextgis.store.bindings.DirectoryContainer;
import com.nextgis.store.bindings.DirectoryEntry;
import com.nextgis.store.bindings.DirectoryEntryType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.nextgis.libngui.util.ConstantsUI.FILETYPE_FOLDER;
import static com.nextgis.libngui.util.ConstantsUI.FILETYPE_PARENT;


public class LocalResourceNativeDirectoryContainer
        extends DirectoryContainer
{
    protected List<LocalResourceNativeListItem> mItems;

    protected int     mItemTypeMask;
    protected boolean mCanWrite;


    public LocalResourceNativeDirectoryContainer(
            int itemTypeMask,
            boolean canWrite)
    {
        mCanWrite = canWrite;
        mItemTypeMask = itemTypeMask;
    }


    @Deprecated
    @Override
    public DirectoryEntry[] getEntries()
    {
        return null;
    }


    @Deprecated
    @Override
    public String getEntryPath(int index)
    {
        return null;
    }


    public List<LocalResourceNativeListItem> getItems()
    {
        return mItems;
    }


    public String getItemPath(int index)
    {
        return getPath() + File.separator + mItems.get(index).getFullName();
    }


    public void setCanWrite(boolean canWrite)
    {
        mCanWrite = canWrite;
    }


    public void setItemTypeMask(int itemTypeMask)
    {
        mItemTypeMask = itemTypeMask;
    }


    public void fillItems(DirectoryContainer source)
    {
        String parentPath = source.getParentPath();
        DirectoryEntry[] entries = source.getEntries();
        int entryCount = entries.length;
        int itemCount = (null == parentPath) ? entryCount : entryCount + 1;

        mParentPath = parentPath;
        mDirectoryName = source.getDirectoryName();
        mItems = new ArrayList<>(itemCount);

        if (null != parentPath) {
            LocalResourceNativeListItem item =
                    new LocalResourceNativeListItem(null, null, DirectoryEntryType.DET_DIRECTORY,
                            FILETYPE_PARENT, false, false);
            mItems.add(item);
        }

        for (int i = 0; i < entryCount; ++i) {
            DirectoryEntry entry = entries[i];
            int itemType = LocalResourceNativeListItem.getItemType(entry);
            boolean selectable = 0 != (mItemTypeMask & itemType);

            // always show folders
            if (selectable || itemType == FILETYPE_FOLDER) {
                LocalResourceNativeListItem item =
                        new LocalResourceNativeListItem(entry.getBaseName(), entry.getExtension(),
                                entry.getType(), itemType, selectable, mCanWrite);
                mItems.add(item);
            }
        }
    }
}
