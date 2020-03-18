/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.linphone.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.linphone.core.Address;
import org.linphone.core.tools.Log;

public class FileUtils {
    public static void deleteFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) return;
        File file = new File(filePath);
        if (file.exists()) {
            try {
                if (file.delete()) {
                    Log.i("[File Utils] File deleted: ", filePath);
                } else {
                    Log.e("[File Utils] Can't delete ", filePath);
                }
            } catch (Exception e) {
                Log.e("[File Utils] Can't delete ", filePath, ", exception: ", e);
            }
        } else {
            Log.e("[File Utils] File ", filePath, " doesn't exists");
        }
    }

    public static String getStorageDirectory(Context mContext) {
        File path = null;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.w("[File Utils] External storage is mounted");
            String directory = Environment.DIRECTORY_DOWNLOADS;
            path = mContext.getExternalFilesDir(directory);
        }

        if (path == null) {
            Log.w("[File Utils] Couldn't get external storage path, using internal");
            path = mContext.getFilesDir();
        }

        return path.getAbsolutePath();
    }

    public static String getRecordingsDirectory(Context mContext) {
        return getStorageDirectory(mContext);
    }

    @SuppressLint("SimpleDateFormat")
    public static String getCallRecordingFilename(Context context, Address address) {
        String fileName = getRecordingsDirectory(context) + "/";

        String name =
                address.getDisplayName() == null ? address.getUsername() : address.getDisplayName();
        fileName += name + "_";

        DateFormat format = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
        fileName += format.format(new Date()) + ".mkv";

        return fileName;
    }
}
