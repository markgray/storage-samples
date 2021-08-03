/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.example.android.contentproviderpaging

import android.app.Activity
import android.content.ContentProvider
import androidx.loader.app.LoaderManager
import android.content.ContentResolver
import android.content.Context
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import android.database.Cursor
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.android.contentproviderpaging.ImageAdapter.ImageDocument
import java.util.concurrent.atomic.AtomicInteger

/**
 * Fragment that works as a client for accessing the DocumentsProvider
 * ([ImageProvider].
 */
class ImageClientFragment : Fragment() {
    /**
     * The [ImageAdapter] (custom [RecyclerView.Adapter] of [ImageViewHolder] objects) which feeds
     * views to our [RecyclerView].
     */
    private var mAdapter: ImageAdapter? = null
    /**
     * The [LinearLayoutManager] used by our [RecyclerView] as its [RecyclerView.LayoutManager].
     */
    private var mLayoutManager: LinearLayoutManager? = null
    /**
     * The [LoaderManager.LoaderCallbacks] callback interface we use to interact with the manager.
     */
    private val mLoaderCallback = LoaderCallback()

    /**
     * The offset position for the [ContentProvider] to use as the starting position to fetch
     * the images from.
     */
    private val mOffset = AtomicInteger(0)

    /**
     * Called to have the fragment instantiate its user interface view. This will be called between
     * [onCreate] and [onViewCreated]. It is recommended to only inflate the layout in this method
     * and move logic that operates on the returned [View] to [onViewCreated]. We return the [View]
     * that our [LayoutInflater] parameter [inflater] inflates from the layout file with resource ID
     * [R.layout.fragment_image_client] using our [ViewGroup] parameter [container] for `LayoutParams`
     * without attaching to it. This layout file consists of a vertical `LinearLayout` holding a
     * [Button] with the label "Show images" above a [RecyclerView].
     *
     * @param inflater The [LayoutInflater] object that can be used to inflate
     * any views in the fragment,
     * @param container If non-`null`, this is the parent view that the fragment's
     * UI will be attached to. The fragment should not add the view itself,
     * but this can be used to generate the `LayoutParams` of the view.
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_client, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state has been
     * restored in to the view. First we call our super's implementation of `onViewCreated`. We
     * initialize our [Activity] variable `val activity` to the [FragmentActivity] this fragment
     * is currently associated with, and we initialize our [RecyclerView] variable `val recyclerView`
     * to the [View] with ID [R.id.recyclerview].
     *
     * If our [LinearLayoutManager] field [mLayoutManager] is `null` we first initialize it to a
     * new instance of [LinearLayoutManager] before setting the [RecyclerView.LayoutManager] that
     * `recyclerView` will use to [mLayoutManager]. If our [ImageAdapter] field [mAdapter] is `null`
     * we first initialize it to a new instance of [ImageAdapter] before setting the
     * [RecyclerView.Adapter] of `recyclerView` that will provide child views on demand to
     * [mAdapter].
     *
     * Next we add an anonymous [RecyclerView.OnScrollListener] to `recyclerView` whose `onScrolled`
     * override will:
     *  - Initialize its [Int] variable `val lastVisiblePosition` to the adapter position of the
     *  last visible view that the [LinearLayoutManager.findLastVisibleItemPosition] method of
     *  [mLayoutManager] returns.
     *  - If `lastVisiblePosition` is greater than or equal to the number of images already fetched
     *  and added to the adapter [mAdapter] that its [ImageAdapter.getFetchedItemCount] returns we:
     *    - Initialize our [Int] variable `val pageId` to `lastVisiblePosition` divided by [LIMIT] (10)
     *    - Use an instance of [LoaderManager] to start or restart a new [Loader] whose ID is `pageId`
     *    with `null` for its args, and [mLoaderCallback] for its [LoaderManager.LoaderCallbacks]
     *
     * Next we initialize our [Button] variable `val showButton` to the view in our [View] parameter
     * [rootView] with resource ID [R.id.button_show] (labeled "Show images") and then set its
     * [View.OnClickListener] to a lambda which uses an instance of [LoaderManager] to start or
     * restart a new [Loader] whose ID is 0, with `null` for its args, and [mLoaderCallback] for its
     * [LoaderManager.LoaderCallbacks], and then sets the visibility of `showButton` to GONE.
     *
     * @param rootView The [View] returned by [onCreateView].
     * @param savedInstanceState If non-`null`, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(rootView: View, savedInstanceState: Bundle?) {
        super.onViewCreated(rootView, savedInstanceState)
        val activity: Activity? = activity
        val recyclerView = activity!!.findViewById<View>(R.id.recyclerview) as RecyclerView
        if (mLayoutManager == null) {
            mLayoutManager = LinearLayoutManager(activity)
        }
        recyclerView.layoutManager = mLayoutManager
        if (mAdapter == null) {
            mAdapter = ImageAdapter(activity)
        }
        recyclerView.adapter = mAdapter
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                val lastVisiblePosition = mLayoutManager!!.findLastVisibleItemPosition()
                if (lastVisiblePosition >= mAdapter!!.fetchedItemCount) {
                    Log.d(
                        TAG,
                        "Fetch new images. LastVisiblePosition: " + lastVisiblePosition
                            + ", NonEmptyItemCount: " + mAdapter!!.fetchedItemCount
                    )
                    val pageId = lastVisiblePosition / LIMIT
                    // Fetch new images once the last fetched item becomes visible
                    LoaderManager.getInstance(this@ImageClientFragment)
                        .restartLoader(pageId, null, mLoaderCallback)
                }
            }
        })
        val showButton = rootView.findViewById<Button>(R.id.button_show)
        showButton.setOnClickListener {
            LoaderManager.getInstance(this).restartLoader(0, null, mLoaderCallback)
            showButton.visibility = View.GONE
        }
    }

    private inner class LoaderCallback : LoaderManager.LoaderCallbacks<Cursor> {
        override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
            val activity: Activity? = this@ImageClientFragment.activity
            return object : CursorLoader(activity as Context) {
                override fun loadInBackground(): Cursor {
                    val bundle = Bundle()
                    bundle.putInt(ContentResolver.QUERY_ARG_OFFSET, mOffset.toInt())
                    bundle.putInt(ContentResolver.QUERY_ARG_LIMIT, LIMIT)
                    return activity!!.contentResolver
                        .query(ImageContract.CONTENT_URI, null, bundle, null)!!
                }
            }
        }

        override fun onLoadFinished(loader: Loader<Cursor>, cursor: Cursor) {
            val extras = cursor.extras
            val totalSize = extras.getInt(ContentResolver.EXTRA_SIZE)
            mAdapter!!.setTotalSize(totalSize)
            val beforeCount = mAdapter!!.fetchedItemCount
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(
                    cursor.getColumnIndex(
                        ImageContract.Columns.DISPLAY_NAME
                    )
                )
                val absolutePath = cursor.getString(
                    cursor.getColumnIndex(
                        ImageContract.Columns.ABSOLUTE_PATH
                    )
                )
                val imageDocument = ImageDocument()
                imageDocument.mAbsolutePath = absolutePath
                imageDocument.mDisplayName = displayName
                mAdapter!!.add(imageDocument)
            }
            val cursorCount = cursor.count
            if (cursorCount == 0) {
                return
            }
            val activity: Activity? = this@ImageClientFragment.activity
            mAdapter!!.notifyItemRangeChanged(beforeCount, cursorCount)
            val offsetSnapShot = mOffset.get()
            val message = activity!!.resources
                .getString(
                    R.string.fetched_images_out_of, offsetSnapShot + 1,
                    offsetSnapShot + cursorCount, totalSize
                )
            mOffset.addAndGet(cursorCount)
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }

        override fun onLoaderReset(loader: Loader<Cursor>) {}
    }

    companion object {
        /**
         * TAG used for logging.
         */
        private const val TAG = "ImageClientFragment"

        /**
         * The number of fetched images in a single query to the DocumentsProvider.
         */
        private const val LIMIT = 10
        fun newInstance(): ImageClientFragment {
            val args = Bundle()
            val fragment = ImageClientFragment()
            fragment.arguments = args
            return fragment
        }
    }
}