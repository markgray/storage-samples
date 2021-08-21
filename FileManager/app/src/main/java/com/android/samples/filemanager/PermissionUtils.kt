/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:JvmName("PermissionUtils")

package com.android.samples.filemanager

import android.Manifest
import android.app.AppOpsManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * This is the value of the [String] constant `AppOpsManager.OPSTR_MANAGE_EXTERNAL_STORAGE` which
 * is the permission name for access to external storage on Android "R" and above. It is anotated
 * with `@SystemAPI` at the moment which is why we have to use hardcoded value here. The use of
 * this is a bit odd: it appears that while [AppOpsManager] uses this when checking whether we have
 * the manage external storage permission the settings app uses on Android "R" the [String] constant
 * "android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION" when we want to ask for the permission and
 * the AndroidManifest permission name for Android "R" is "android.permission.MANAGE_EXTERNAL_STORAGE"
 */
const val MANAGE_EXTERNAL_STORAGE_PERMISSION = "android:manage_external_storage"
/**
 * Our [getLegacyStorageStatus] method returns this [String] when running on Android builds older
 * than "Q" since legacy view of the shared/external storage media was introduced by "Q" (stands
 * for "Not Applicable" of course). On "Q" and above [getLegacyStorageStatus] returns the string
 * value of the [Boolean] value returned by the [Environment.isExternalStorageLegacy] method (which
 * will be `true` if `requestLegacyExternalStorage` is requested in the app's manifest.
 */
const val NOT_APPLICABLE = "N/A"

/**
 * Returns the name of the permission used to request access to external storage, which depends on the
 * version of Android that we are running on. It is only called by our [SettingsActivity.getInfoList]
 * method to be used as part of the info displayed to the user when they click on our options menu.
 * If the device we are running on is Android "R" or above we return our [String] constant
 * [MANAGE_EXTERNAL_STORAGE_PERMISSION], otherwise we return the [Manifest.permission.READ_EXTERNAL_STORAGE]
 * system constant ("android.permission.READ_EXTERNAL_STORAGE").
 *
 * @return the [String] naming the permission (according to the [AppOpsManager]) for the version of
 * Android we are running on.
 */
fun getStoragePermissionName(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        MANAGE_EXTERNAL_STORAGE_PERMISSION
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

/**
 * Launches an [Intent] which will start a Settings provider activity that will ask the user to give
 * us permission to access external storage.
 *
 * @param activity the [AppCompatActivity] we can use to access application info and call its method
 * [AppCompatActivity.startActivity] to start another activity.
 */
fun openPermissionSettings(activity: AppCompatActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        requestStoragePermissionApi30(activity)
    } else {
        activity.startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null)
            )
        )
    }
}

fun getLegacyStorageStatus(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Environment.isExternalStorageLegacy().toString()
    } else {
        NOT_APPLICABLE
    }
}

fun getPermissionStatus(activity: AppCompatActivity): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30(activity).toString()
    } else {
        checkStoragePermissionApi19(activity).toString()
    }
}

fun checkStoragePermission(activity: AppCompatActivity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30(activity)
    } else {
        checkStoragePermissionApi19(activity)
    }
}

fun requestStoragePermission(activity: AppCompatActivity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        requestStoragePermissionApi30(activity)
    }
    // If you want to see the default storage behavior on Android Q once the permission is granted
    // Set the "requestLegacyExternalStorage" flag in the AndroidManifest.xml file to false
    else {
        requestStoragePermissionApi19(activity)
    }
}

@RequiresApi(30)
fun checkStoragePermissionApi30(activity: AppCompatActivity): Boolean {
    val appOps = activity.getSystemService(AppOpsManager::class.java)
    val mode = appOps.unsafeCheckOpNoThrow(
        MANAGE_EXTERNAL_STORAGE_PERMISSION,
        activity.applicationInfo.uid,
        activity.packageName
    )

    return mode == AppOpsManager.MODE_ALLOWED
}

@RequiresApi(30)
fun requestStoragePermissionApi30(activity: AppCompatActivity) {
    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    // This used to be startActivityForResult, but onResume was used for the return from the
    // launched activity instead of onActivityResult, so startActivity is all that is needed
    activity.startActivity(intent)
}

@RequiresApi(19)
fun checkStoragePermissionApi19(activity: AppCompatActivity): Boolean {
    val status =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)

    return status == PackageManager.PERMISSION_GRANTED
}

@RequiresApi(19)
fun requestStoragePermissionApi19(activity: AppCompatActivity) {
    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ActivityCompat.requestPermissions(
        activity,
        permissions,
        READ_EXTERNAL_STORAGE_PERMISSION_REQUEST
    )
}
