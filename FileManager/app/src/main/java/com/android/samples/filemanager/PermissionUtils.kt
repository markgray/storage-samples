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
 * Launches an [Intent] which will start a Settings provider activity that will allow the user to
 * give us permission to access external storage. We branch on [Build.VERSION.SDK_INT] (the SDK
 * version of the software currently running on this hardware device):
 *  - greater than or equal to [Build.VERSION_CODES.R] (API 30) we call our [requestStoragePermissionApi30]
 *  method to have it build and launch an [Intent] that will have the settings provider allow the
 *  user to give us permission to access external storage on Android "R" and above.
 *  - less than [Build.VERSION_CODES.R] we start an activity using an [Intent] whose action is
 *  [Settings.ACTION_APPLICATION_DETAILS_SETTINGS] and whose [Intent] data [Uri] uses the scheme
 *  "package" and the scheme-specific-part is the name of this application's package (the resulting
 *  [Uri] is "package:com.android.samples.filemanager" in our case).
 *
 * It is called by the `onClickListener` of the button with ID [R.id.openSettingsButton] ("Open Settings")
 * in the UI of [SettingsActivity].
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

/**
 * Returns a [String] describing whether the shared/external storage media is a legacy view that
 * includes files not owned by the app ("true", "false" or "N/A"). Legacy view was first introduced
 * in Android "Q" so for "Q" and above we return the [String] value of the [Boolean] returned by the
 * [Environment.isExternalStorageLegacy] method to the caller, and for Android older than "Q" we
 * return our [String] constant [NOT_APPLICABLE] ("N/A").
 *
 * It is only called by our [SettingsActivity.getInfoList] method to be used as part of the info
 * displayed to the user when they click on our options menu.
 *
 * @return a [String] describing whether the shared/external storage media is a legacy view ("true",
 * "false" or "N/A").
 */
fun getLegacyStorageStatus(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Environment.isExternalStorageLegacy().toString()
    } else {
        NOT_APPLICABLE
    }
}

/**
 * Returns "true" or "false" [String] indicating whether permission to access external storage has
 * been granted or not. If [Build.VERSION.SDK_INT] (the SDK version of the software currently running
 * on this hardware device) is greater than or equal to [Build.VERSION_CODES.R] we return the string
 * value of the [Boolean] returned by our [checkStoragePermissionApi30] method, otherwise we return
 * the string value of the [Boolean] returned by our [checkStoragePermissionApi19] method.
 *
 * It is only called by our [SettingsActivity.getInfoList] method to be used as part of the info
 * displayed to the user when they click on our options menu.
 *
 * @param activity the [AppCompatActivity] to use to access resources and methods.
 * @return "true" or "false" [String] indicating whether permission to access external storage has
 * been granted or not.
 */
fun getPermissionStatus(activity: AppCompatActivity): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30(activity).toString()
    } else {
        checkStoragePermissionApi19(activity).toString()
    }
}

/**
 * Returns `true` if we have permission to access external storage, and `false` if we do not have
 * permission. If [Build.VERSION.SDK_INT] (the SDK version of the software currently running
 * on this hardware device) is greater than or equal to [Build.VERSION_CODES.R] we return the
 * [Boolean] returned by our [checkStoragePermissionApi30] method, otherwise we return the [Boolean]
 * returned by our [checkStoragePermissionApi19] method.
 *
 * Called by the `onResume` override of [FileExplorerActivity] to decide if the user needs to be
 * asked to grant us permission to access external storage (if we return `false`) or if the app
 * already has permission and can begin displaying the contents of external storage ((if we return
 * `true`).
 *
 * @param activity the [AppCompatActivity] to use to access app resources and methods.
 */
fun checkStoragePermission(activity: AppCompatActivity): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        checkStoragePermissionApi30(activity)
    } else {
        checkStoragePermissionApi19(activity)
    }
}

/**
 * Requests access to external storage using the methods which are appropriate for the version of
 * Android we are running on. If [Build.VERSION.SDK_INT] (the SDK version of the software currently
 * running on this hardware device) is greater than or equal to [Build.VERSION_CODES.R] we call our
 * method [requestStoragePermissionApi30] to have it ask for permission to access external storage
 * using the API for Android "R" and above, otherwise we call our method [requestStoragePermissionApi19]
 * to have it ask for permission to access external storage using the the API used for Android versions
 * older than "R".
 *
 * It is called from the `OnClickListener` of the button with ID [R.id.permissionButton] ("Give
 * Permission") in the UI for [FileExplorerActivity] and from the `OnClickListener` of the button
 * with ID [R.id.requestPermissionButton] ("Request Permission") in the UI for [SettingsActivity].
 *
 * @param activity the [AppCompatActivity] to use to access app resources and methods.
 */
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

/**
 * When running on hardware devices running software whose SDK version is API 30 or newer, this
 * method will check whether we have permission to access external storage and return `true` if
 * we do have permission, and `false` if we do not have permission. We initialize our [AppOpsManager]
 * variable `val appOps` to the handle to the [AppOpsManager] system-level service (used for runtime
 * permissions access control and tracking). Then we initialize our [Int] variable `val mode` to the
 * value returned by the [AppOpsManager.unsafeCheckOpNoThrow] method of `appOps` to check for the
 * operation [MANAGE_EXTERNAL_STORAGE_PERMISSION] (our hardcoded value of the `@SystemAPI` anotated
 * constant `AppOpsManager.OPSTR_MANAGE_EXTERNAL_STORAGE` which is used to check for permission to
 * manage external storage), the user id of our application, and our package name. Then we return
 * `true` if `mode` is equal to [AppOpsManager.MODE_ALLOWED] (the given caller is allowed to perform
 * the given operation) and `false` for any other value.
 *
 * @param activity the [AppCompatActivity] we should use to access app resources and methods.
 * @return `true` if we have permission to access external storage, and `false` if we do not.
 */
@RequiresApi(30)
fun checkStoragePermissionApi30(activity: AppCompatActivity): Boolean {
    val appOps: AppOpsManager = activity.getSystemService(AppOpsManager::class.java)
    val mode: Int = appOps.unsafeCheckOpNoThrow(
        MANAGE_EXTERNAL_STORAGE_PERMISSION,
        activity.applicationInfo.uid,
        activity.packageName
    )

    return mode == AppOpsManager.MODE_ALLOWED
}

/**
 * When running on hardware devices running software whose SDK version is API 30 or newer, this
 * method will launch an [Intent] with the action [Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION]
 * which will start a [Settings] activity that will allow the user to grant us permission to access
 * external storage if he wants to. We initialize our [Intent] variable `val intent` to a new instance
 * with the action [Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION] (show screen for controlling
 * which apps have access to manage external storage). Then we use the [AppCompatActivity.startActivity]
 * method of [activity] to launch `intent`.
 *
 * @param activity the [AppCompatActivity] we should use to access its `startActivity` method.
 */
@RequiresApi(30)
fun requestStoragePermissionApi30(activity: AppCompatActivity) {
    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
    // This used to be startActivityForResult, but onResume was used for the return from the
    // launched activity instead of onActivityResult, so startActivity is all that is needed
    activity.startActivity(intent)
}

/**
 * When running on hardware devices running software whose SDK version is older than API 30, this
 * method will check whether we have permission to access external storage and return `true` if
 * we do have permission, and `false` if we do not have permission. We initialize our [Int] variable
 * `val status` to the value returned by [ContextCompat.checkSelfPermission] when passed our
 * [AppCompatActivity] parameter [activity], and the [Manifest.permission.READ_EXTERNAL_STORAGE]
 * permission constant (allows an application to read from external storage). Then we return
 * `true` if `status` is equal to [PackageManager.PERMISSION_GRANTED] (returned if the permission
 * has been granted to the given package) and `false` for any other value.
 *
 * @param activity the [AppCompatActivity] we should use to access our apps resources.
 * @return `true` if we have permission to access external storage, and `false` if we do not.
 */
@RequiresApi(19)
fun checkStoragePermissionApi19(activity: AppCompatActivity): Boolean {
    val status: Int =
        ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)

    return status == PackageManager.PERMISSION_GRANTED
}

/**
 * When running on hardware devices running software whose SDK version is older than API 30, this
 * method will use the [ActivityCompat.requestPermissions] method to request that permission to read
 * external storage be granted to this application.
 *
 * @param activity the [AppCompatActivity] we should use to access app resources.
 */
@RequiresApi(19)
fun requestStoragePermissionApi19(activity: AppCompatActivity) {
    val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    ActivityCompat.requestPermissions(
        activity,
        permissions,
        READ_EXTERNAL_STORAGE_PERMISSION_REQUEST
    )
}
