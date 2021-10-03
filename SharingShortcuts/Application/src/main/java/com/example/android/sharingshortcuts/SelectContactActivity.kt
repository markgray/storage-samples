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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
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
     * then we set our content view to our layout file [R.layout.activity_select_contact]. This
     * layout holds only a single [RecyclerView] as its root view.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_contact)
        val intent = intent
        if (ACTION_SELECT_CONTACT != intent.action) {
            finish()
            return
        }
        // Set up the list of contacts
        val recyclerView: RecyclerView = findViewById(R.id.recycler_view)
        recyclerView.adapter = mContactAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private val mContactAdapter: RecyclerView.Adapter<ContactViewHolder> =
        object : RecyclerView.Adapter<ContactViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): ContactViewHolder {
                val textView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_contact, parent, false) as TextView
                return ContactViewHolder(textView)
            }

            override fun onBindViewHolder(
                holder: ContactViewHolder,
                position: Int
            ) {
                val contact = Contact.CONTACTS[position]
                ContactViewBinder.bind(contact, holder.itemView as TextView)
                holder.itemView.setOnClickListener {
                    val data = Intent()
                    data.putExtra(Contact.ID, holder.bindingAdapterPosition)
                    setResult(RESULT_OK, data)
                    finish()
                }
            }

            override fun getItemCount(): Int {
                return Contact.CONTACTS.size
            }
        }

    private class ContactViewHolder(textView: TextView) : ViewHolder(textView)
    companion object {
        /**
         * The action string for Intents.
         */
        const val ACTION_SELECT_CONTACT = "com.example.android.sharingshortcuts.intent.action.SELECT_CONTACT"
    }
}