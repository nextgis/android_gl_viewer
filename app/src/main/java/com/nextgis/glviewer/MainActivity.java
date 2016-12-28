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
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import com.nextgis.dialog.LocalResourceNativeSelectDialog;
import com.nextgis.libngui.activity.NGActivity;
import com.nextgis.libngui.dialog.LocalResourceSelectDialog;
import com.nextgis.libngui.util.ConstantsUI;

import java.io.File;


public class MainActivity
        extends NGActivity
{
    protected Toolbar     mToolbar;
    protected MapFragment mMapFragment;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);

        FragmentManager fm = getSupportFragmentManager();
        mMapFragment = (MapFragment) fm.findFragmentById(R.id.map_fragment);
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
            case R.id.action_open_file:
                openFile();
                return true;

            case R.id.action_select_file:
                selectFilesAndFolderNative(1);
                return true;

            case R.id.action_select_files:
                selectFilesAndFolderNative(2);
                return true;

            case R.id.action_select_folder:
                selectFilesAndFolderNative(3);
                return true;

            case R.id.action_select_folders:
                selectFilesAndFolderNative(4);
                return true;

            case R.id.action_settings:
                return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void openFile()
    {
        File path = getExternalFilesDir("");

        if (null == path) {
            Toast.makeText(this, "External storage is not mounted", Toast.LENGTH_LONG).show();
            return;
        }

        LocalResourceSelectDialog dialog = new LocalResourceSelectDialog();
        dialog.setPath(path);
        dialog.setTypeMask(ConstantsUI.FILETYPE_SHP);
//        LocalResourceNativeSelectDialog dialog = new LocalResourceNativeSelectDialog();
//        dialog.setPath(path.getAbsolutePath());
//        dialog.setItemTypeMask(ConstantsUI.FILETYPE_SHP);
        dialog.setCanSelectMultiple(false);
        dialog.setOnSelectionListener(mMapFragment);
        dialog.show(getSupportFragmentManager(), Constants.FRAGMENT_SELECT_RESOURCE);
    }


    protected void selectFilesAndFolderNative(int type)
    {
        File path = getExternalFilesDir("");

        if (null == path) {
            Toast.makeText(this, "External storage is not mounted", Toast.LENGTH_LONG).show();
            return;
        }

        LocalResourceNativeSelectDialog dialog = new LocalResourceNativeSelectDialog();
        dialog.setPath(path.getAbsolutePath());

        switch (type) {
            case 1:
                dialog.setItemTypeMask(ConstantsUI.FILETYPE_ALL_FILE_TYPES);
                dialog.setCanSelectMultiple(false);
                break;
            case 2:
                dialog.setItemTypeMask(ConstantsUI.FILETYPE_ALL_FILE_TYPES);
                dialog.setCanSelectMultiple(true);
                break;
            case 3:
                dialog.setItemTypeMask(ConstantsUI.FILETYPE_FOLDER);
                dialog.setCanSelectMultiple(false);
                break;
            case 4:
                dialog.setItemTypeMask(ConstantsUI.FILETYPE_FOLDER);
                dialog.setCanSelectMultiple(true);
                break;
        }

        dialog.show(getSupportFragmentManager(), Constants.FRAGMENT_SELECT_RESOURCE);
    }
}
