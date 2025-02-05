/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.example.android.sharingshortcuts

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder

/**
 * The dialog for selecting a contact to share the text with. This dialog is launched by
 * [SendMessageActivity] when the user taps on this sample's icon in the chooser instead
 * of one of the Direct Share contacts.
 */
class SelectContactActivity : Activity() {
    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_select_contact`. This
     * layout holds only a single [RecyclerView] as its root view.
     *
     * Next we initialize our [Intent] variable `val intent` to the [Intent] that started this
     * activity, and if the action of `intent` is not [ACTION_SELECT_CONTACT] we call [finish]
     * to close this activity and return. Otherwise we initialize our [RecyclerView] variable
     * `val recyclerView` by finding the view with ID `R.id.recycler_view` in our UI, then set
     * its [RecyclerView.Adapter] to our field [mContactAdapter], and set its [RecyclerView.LayoutManager]
     * to a new instance of [LinearLayoutManager].
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_contact)
        val intent: Intent = intent
        if (ACTION_SELECT_CONTACT != intent.action) {
            finish()
            return
        }
        // Set up the list of contacts
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = mContactAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    /**
     * The [RecyclerView.Adapter] that we use to bind the [Contact] objects in our dataset to the
     * views that are displayed within our [RecyclerView].
     */
    private val mContactAdapter: RecyclerView.Adapter<ContactViewHolder> =
        object : RecyclerView.Adapter<ContactViewHolder>() {
            /**
             * Called when [RecyclerView] needs a new [ContactViewHolder] of the given type to
             * represent an item. We initialize our [TextView] variable `val textView` to the
             * [View] that the [LayoutInflater] for the [Context] of our [ViewGroup] parameter
             * [parent] inflates from the layout file `R.layout.item_contact` using [parent] for
             * its `LayoutParams` without attaching to it. Finally we return a new instance of
             * [ContactViewHolder] constructed to use `textView` as its `itemView`.
             *
             * @param parent The [ViewGroup] into which the new [View] will be added after it is
             * bound to an adapter position.
             * @param viewType The view type of the new View.
             * @return A new [ContactViewHolder] that holds a [View] of the given view type.
             */
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): ContactViewHolder {
                val textView: TextView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_contact, parent, false) as TextView
                return ContactViewHolder(textView)
            }

            /**
             * Called by [RecyclerView] to display the data at the specified position. This method
             * should update the contents of the [ContactViewHolder.itemView] to reflect the item
             * at the given position. Note that unlike [ListView], [RecyclerView] will not call this
             * method again if the position of the item changes in the data set unless the item itself
             * is invalidated or the new position cannot be determined. For this reason, you should
             * only use the [position] in this method and should not keep a copy of it. If you need
             * the position of an item later on (e.g. in a click listener), use the method
             * [ViewHolder.getBindingAdapterPosition] which will have the updated adapter position.
             *
             * We initialize our [Contact] variable `val contact` to the [Contact] object at position
             * [position] in our [Contact.CONTACTS] dataset. Then we call our [ContactViewBinder.bind]
             * method to have it set the text of the [TextView] of the [ContactViewHolder.itemView]
             * of [holder] to the `name` property of `contact` and the start Drawable of that
             * [TextView] to the Drawable whose resource ID is the `icon` property of `contact`.
             * Finally we set the [View.OnClickListener] of that [TextView] to a lambda which:
             *  - initializes its [Intent] variable `val data` to a new instance.
             *  - adds the Adapter position of the item represented by [holder] as an extra to `data`
             *  under the key [Contact.ID].
             *  - calls [setResult] to set the result that our activity will return to its caller to
             *  [Activity.RESULT_OK], with `data` as the [Intent] to propagate back to the originating
             *  activity.
             *  - then calls [finish] to close this activity.
             *
             * @param holder The [ContactViewHolder] which should be updated to represent the
             * contents of the item at the given [position] in the data set.
             * @param position The position of the item within the adapter's data set.
             */
            override fun onBindViewHolder(
                holder: ContactViewHolder,
                position: Int
            ) {
                val contact: Contact = Contact.CONTACTS[position]
                ContactViewBinder.bind(contact, holder.itemView as TextView)
                holder.itemView.setOnClickListener {
                    val data = Intent()
                    data.putExtra(Contact.ID, holder.bindingAdapterPosition)
                    setResult(RESULT_OK, data)
                    finish()
                }
            }

            /**
             * Returns the total number of items in the data set held by the adapter. We just return
             * the size the [Contact.CONTACTS] array (which is our dataset).
             *
             * @return The total number of items in this adapter.
             */
            override fun getItemCount(): Int {
                return Contact.CONTACTS.size
            }
        }

    /**
     * The [ViewHolder] used to hold the [TextView] that displays a [Contact].
     */
    private class ContactViewHolder(textView: TextView) : ViewHolder(textView)

    companion object {
        /**
         * The action string for Intents.
         */
        const val ACTION_SELECT_CONTACT: String = "com.example.android.sharingshortcuts.intent.action.SELECT_CONTACT"
    }
}
