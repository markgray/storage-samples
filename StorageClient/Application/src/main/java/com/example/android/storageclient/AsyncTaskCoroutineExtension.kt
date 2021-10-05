package com.example.android.storageclient

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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