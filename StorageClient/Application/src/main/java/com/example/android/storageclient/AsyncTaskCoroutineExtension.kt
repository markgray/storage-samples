package com.example.android.storageclient

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Simple [CoroutineScope] extension function which fetches a [Bitmap] using the [CoroutineContext]
 * of [Dispatchers.IO] to execute the [doInBackground] lambda using its [Uri] parameter [uri] and
 * then calls the [onPostExecute] lambda with the [Bitmap] returned from [doInBackground] when the
 * background thread completes.
 *
 * @param doInBackground a lambda which takes a [Uri] and returns a [Bitmap].
 * @param onPostExecute a lambda which takes a [Bitmap] and returns nothing.
 * @param uri the [Uri] that will be passed to the [doInBackground] lambda.
 */
fun CoroutineScope.getBitmapAndDisplay(
    doInBackground: (uri: Uri) -> Bitmap,
    onPostExecute: (Bitmap) -> Unit,
    uri: Uri
) = launch {
    val result = withContext(Dispatchers.IO) { // runs in background thread without blocking the Main Thread
        doInBackground(uri)
    }
    onPostExecute(result)
}