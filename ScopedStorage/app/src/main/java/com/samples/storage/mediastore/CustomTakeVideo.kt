/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.storage.mediastore

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts

/**
 * Custom [ActivityResultContract] we use to have the camera application capture a video and store
 * it in the shared storage area. The system [ActivityResultContracts.CaptureVideo] returns `true`
 * if the result code is [Activity.RESULT_OK] whereas we return the [Uri] stored in the `data`
 * property of the [Intent] that is returned by the camera application. This is a content [Uri] that
 * points to the captured video that is stored in shared storage and we use `Glide` to display the
 * thumbnail of that video [Uri].
 */
class CustomTakeVideo : ActivityResultContract<Uri, Uri?>() {
    /**
     * Creates an [Intent] that can be used for [Activity.startActivityForResult] to launch the
     * camera application to capture a video and store it in the shared storage area file pointed
     * to by our content [Uri] parameter [input]. We create a new instance of [Intent] whose action
     * is [MediaStore.ACTION_VIDEO_CAPTURE] ("android.media.action.VIDEO_CAPTURE") and add our [Uri]
     * parameter [input] as an extra under the name [MediaStore.EXTRA_OUTPUT] ("output"), then
     * return this [Intent] to the caller.
     *
     * @param context the application [Context].
     * @param input a content [Uri] pointing to the file in shared storage where the camera app
     * should store the captured video. This is the argument passed to [ActivityResultLauncher.launch]
     * method of the [ActivityResultLauncher] used to launch us.
     * @return an [Intent] which will launch the camera application to have it capture a video and
     * store it in the shared storage area file pointed to by our [Uri] parameter [input]
     */
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_VIDEO_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
    }

    override fun getSynchronousResult(context: Context, input: Uri): SynchronousResult<Uri?>? {
        return null
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
        return if (intent == null || resultCode != Activity.RESULT_OK) {
            null
        } else {
            intent.data
        }
    }
}
