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
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.pm.ShortcutManagerCompat

/**
 * Provides the UI for sharing a text with a [Contact].
 */
class SendMessageActivity : Activity() {
    /**
     * The text to share.
     */
    private var mBody: String? = null

    /**
     * The ID of the contact to share the text with.
     */
    private var mContactId = 0

    // View references.
    private lateinit var mTextContactName: TextView
    private lateinit var mTextMessageBody: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_message)
        setTitle(R.string.sending_message)
        // View references.
        mTextContactName = findViewById(R.id.contact_name)
        mTextMessageBody = findViewById(R.id.message_body)
        // Handle the share Intent.
        val handled = handleIntent(intent)
        if (!handled) {
            finish()
            return
        }
        // Bind event handlers.
        findViewById<View>(R.id.send).setOnClickListener(mOnClickListener)
        // Set up the UI.
        prepareUi()
        // The contact ID will not be passed on when the user clicks on the app icon rather than any
        // of the Direct Share icons. In this case, we show another dialog for selecting a contact.
        if (mContactId == Contact.INVALID_ID) {
            selectContact()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQUEST_SELECT_CONTACT -> {
                if (resultCode == RESULT_OK) {
                    mContactId = data.getIntExtra(Contact.ID, Contact.INVALID_ID)
                }
                // Give up sharing the send_message if the user didn't choose a contact.
                if (mContactId == Contact.INVALID_ID) {
                    finish()
                    return
                }
                prepareUi()
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    /**
     * Handles the passed [Intent]. This method can only handle intents for sharing a plain
     * text. [mBody] and [mContactId] are modified accordingly.
     *
     * @param intent The [Intent].
     * @return true if the `intent` is handled properly.
     */
    private fun handleIntent(intent: Intent): Boolean {
        if (Intent.ACTION_SEND == intent.action && "text/plain" == intent.type) {
            mBody = intent.getStringExtra(Intent.EXTRA_TEXT)
            // The intent comes from Direct share
            mContactId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
                && intent.hasExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)) {
                val shortcutId = intent.getStringExtra(ShortcutManagerCompat.EXTRA_SHORTCUT_ID)
                Integer.valueOf(shortcutId!!)
            } else {
                // The text was shared and the user chose our app
                Contact.INVALID_ID
            }
            return true
        }
        return false
    }

    /**
     * Sets up the UI.
     */
    private fun prepareUi() {
        if (mContactId != Contact.INVALID_ID) {
            val contact = Contact.byId(mContactId)
            ContactViewBinder.bind(contact, mTextContactName)
        }
        mTextMessageBody.text = mBody
    }

    /**
     * Delegates selection of a [Contact] to [SelectContactActivity].
     */
    private fun selectContact() {
        val intent = Intent(this, SelectContactActivity::class.java)
        intent.action = SelectContactActivity.ACTION_SELECT_CONTACT
        startActivityForResult(intent, REQUEST_SELECT_CONTACT)
    }

    private val mOnClickListener = View.OnClickListener { view ->
        when (view.id) {
            R.id.send -> send()
        }
    }

    /**
     * Pretends to send the text to the contact. This only shows a dummy message.
     */
    private fun send() {
        Toast.makeText(this,
            getString(R.string.message_sent, mBody, Contact.byId(mContactId).name),
            Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        /**
         * The request code for [SelectContactActivity]. This is used when the user doesn't
         * select any of Direct Share icons.
         */
        private const val REQUEST_SELECT_CONTACT = 1
    }
}