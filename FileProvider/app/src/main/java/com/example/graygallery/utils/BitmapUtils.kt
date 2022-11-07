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
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Rect
import com.example.graygallery.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Converts the full color [Bitmap] parameter [original] to a black and white [Bitmap]. Our `return`
 * statement calls a suspending block with the [Dispatchers.Default] coroutine context, suspends
 * until it completes, and returns the result. In the suspending block:
 *  - We initialize our [Int] variable `val height` to the `height` of [original].
 *  - We initialize our [Int] variable `val width` to the `width` of [original].
 *  - We initialize our [Bitmap] variable `val modifiedBitmap` to a `width` by `height` [Bitmap]
 *  which uses the bitmap config [Bitmap.Config.RGB_565].
 *  - We initialize our [Canvas] variable `val canvas` to a [Canvas] that uses `modifiedBitmap` as
 *  the [Bitmap] to draw into.
 *  - We initialize our [Paint] variable `val paint` to a new instance.
 *  - We initialize our [ColorMatrix] variable `val colorMatrix` and use the [apply] extension
 *  function to call its [ColorMatrix.setSaturation] method with a value of 0 to map all colors to
 *  gray scale.
 *  - We initialize our [ColorMatrixColorFilter] variable `val filter` to a new instance that uses
 *  `colorMatrix` to transform colors.
 *  - We set the `colorFilter` property of `paint` to `filter` (it will be used by `paint` to modify
 *  the color of each pixel drawn with the [Paint]).
 *  - We call the [Canvas.drawBitmap] method of `canvas` to have it draw our [Bitmap] parameter
 *  [original] into the `modifiedBitmap` [Bitmap] it is drawing to using `paint` as the [Paint].
 *  - The last line of the suspending block returns the [Bitmap] result `modifiedBitmap` to the
 *  coroutine continuation, and on resuming [applyGrayscaleFilter] returns this value to its caller.
 *
 * This is called by the [AppViewModel.saveImageFromCamera] method with the [Bitmap] which has been
 * taken using the camera.
 *
 * @param original the original full color [Bitmap] parameter.
 * @return a black and white version of [original].
 */
suspend fun applyGrayscaleFilter(original: Bitmap): Bitmap {
    return withContext(Dispatchers.Default) {
        val height: Int = original.height
        val width: Int = original.width

        val modifiedBitmap: Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        val canvas = Canvas(modifiedBitmap)
        val paint = Paint()
        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }
        val filter = ColorMatrixColorFilter(colorMatrix)

        paint.colorFilter = filter
        canvas.drawBitmap(original, null, Rect(0, 0, original.width, original.height), paint)

        modifiedBitmap
    }
}

/**
 * Convenience function to call the [BitmapFactory.decodeByteArray] method to decode an immutable
 * bitmap from the [ByteArray] parameter [byteArray].
 *
 * @param byteArray the [ByteArray] to decode into an immutable [Bitmap].
 * @return The decoded [Bitmap], or `null` if the image could not be decoded.
 */
@Suppress("unused") // Suggested change would make class less reusable
fun byteArrayToBitmap(byteArray: ByteArray): Bitmap? =
    BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
