/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.graygallery.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import okhttp3.ResponseBody
import java.io.InputStream

/**
 * Convenience method to call the [BitmapFactory.decodeStream] method to decode the [InputStream]
 * returned by the [ResponseBody.byteStream] method of our parameter [responseBody] into a [Bitmap]
 * and return it to the caller.
 *
 * @param responseBody the [ResponseBody] whose [ResponseBody.byteStream] method's [InputStream] we
 * are to decode into a [Bitmap].
 * @return the [Bitmap] decoded from the [InputStream] returned by the [ResponseBody.byteStream]
 * method of our parameter [responseBody]
 */
@Suppress("unused") // Suggested change would make class less reusable
fun getBitmapFromResponseBody(responseBody: ResponseBody): Bitmap =
    BitmapFactory.decodeStream(responseBody.byteStream())