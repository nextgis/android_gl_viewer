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

package com.nextgis.glviewer;

import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.nextgis.libngui.activity.NGActivity;
import com.nextgis.libngui.dialog.SelectLocalResourceDialog;
import com.nextgis.libngui.util.ConstantsUI;

import java.io.File;


public class MainActivity
        extends NGActivity
{
    protected Toolbar mToolbar;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
//            case R.id.action_select_resource:
//                selectResource();
//                return true;

            case R.id.action_select_file:
                selectFilesAndFolder(1);
                return true;

            case R.id.action_select_files:
                selectFilesAndFolder(2);
                return true;

            case R.id.action_select_folder:
                selectFilesAndFolder(3);
                return true;

            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void selectResource()
    {
        SelectLocalResourceDialog dialog = new SelectLocalResourceDialog();
        File path = new File("/sdcard");
        dialog.setPath(path);
        dialog.show(getSupportFragmentManager(), Constants.FRAGMENT_SELECT_RESOURCE);
    }

    protected void selectFilesAndFolder(int type)
    {
        SelectLocalResourceDialog dialog = new SelectLocalResourceDialog();
        File path = new File("/sdcard");
        dialog.setPath(path);

        switch (type) {
            case 1:
                dialog.setTypeMask(ConstantsUI.FILETYPE_ALL_FILE_TYPES);
                dialog.setCanSelectMultiple(false);
                break;
            case 2:
                dialog.setTypeMask(ConstantsUI.FILETYPE_ALL_FILE_TYPES);
                dialog.setCanSelectMultiple(true);
                break;
            case 3:
                dialog.setTypeMask(ConstantsUI.FILETYPE_FOLDER);
                dialog.setCanSelectMultiple(false);
                dialog.setWritable(true);
                break;
        }

        dialog.show(getSupportFragmentManager(), Constants.FRAGMENT_SELECT_RESOURCE);
    }
}
