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

import com.nextgis.store.bindings.DirectoryEntry;

import static com.nextgis.libngui.util.ConstantsUI.*;


public class LocalResourceNativeListItem
        extends DirectoryEntry
{
    protected int     mItemType;
    protected boolean mSelectable;
    protected boolean mVisibleCheckBox;


    public LocalResourceNativeListItem(
            String baseName,
            String extension,
            int type,
            int itemType,
            boolean selectable,
            boolean forWritableFolder)
    {
        super(baseName, extension, type);
        init(itemType, selectable, forWritableFolder);
    }


    protected void init(
            int itemType,
            boolean selectable,
            boolean forWritableFolder)
    {
        mItemType = itemType;

        if (selectable) {
            if (!forWritableFolder) {
                mSelectable = true;
                mVisibleCheckBox = true;
            } else if (FILETYPE_FOLDER == mItemType) {
//                mSelectable = FileUtil.isDirectoryWritable(entry);
                mVisibleCheckBox = true;
            }
        } else {
            mSelectable = false;
            mVisibleCheckBox = false;
        }
    }


    public int getItemType()
    {
        return mItemType;
    }


    public boolean isSelectable()
    {
        return mSelectable;
    }


    public boolean isVisibleCheckBox()
    {
        return mVisibleCheckBox;
    }


    public static int getItemType(DirectoryEntry entry)
    {
        if (entry.isDirectory()) {
            return FILETYPE_FOLDER;
        }
        if (entry.getExtension().equals("zip")) {
            return FILETYPE_ZIP;
        }
        if (entry.getExtension().equals("ngfb")) {
            return FILETYPE_FB;
        }
        if (entry.getExtension().equals("geojson")) {
            return FILETYPE_GEOJSON;
        }
        if (entry.getExtension().equals("shp")) {
            return FILETYPE_SHP;
        }
        return FILETYPE_UNKNOWN;
    }
}
