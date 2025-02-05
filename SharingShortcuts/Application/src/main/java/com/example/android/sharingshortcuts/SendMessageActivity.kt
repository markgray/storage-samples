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
import android.widget.Button
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
    private var mContactId: Int = 0

    /**
     * The [TextView] in our UI with the ID `R.id.contact_name`. It holds the name of the contact
     * that the message is being sent to.
     */
    private lateinit var mTextContactName: TextView

    /**
     * The [TextView] in our UI with the ID `R.id.message_body`. It holds the message that is being
     * sent to the contact.
     */
    private lateinit var mTextMessageBody: TextView

    /**
     * Called when the activity is starting. First we call our super's implementation of `onCreate`,
     * then we set our content view to our layout file `R.layout.activity_send_message` and call the
     * [setTitle] method to change the title of this activity to "Sending a message". We initialize
     * our [TextView] field [mTextContactName] by finding the view with the ID `R.id.contact_name`
     * and our [TextView] field [mTextMessageBody] by finding the view with the ID `R.id.message_body`.
     * We initialize our [Boolean] variable `val handled` to the value returned by our [handleIntent]
     * method when passed the [Intent] that started this activity (it returns `true` if it was able
     * to use the [Intent] to set the text of our [mBody] field and the value of our contact ID field
     * [mContactId], and `false` if it was not able to). If `handled` is `false` we just call the
     * [finish] method to close our activity and return from [onCreate. Otherwise we set the
     * [View.OnClickListener] for the view with ID `R.id.send` to our field [mOnClickListener] and
     * call our [prepareUi] method to set up our UI.
     *
     * @param savedInstanceState we do not override [onSaveInstanceState] so do not use.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_send_message)
        setTitle(R.string.sending_message)
        // View references.
        mTextContactName = findViewById(R.id.contact_name)
        mTextMessageBody = findViewById(R.id.message_body)
        // Handle the share Intent.
        val handled: Boolean = handleIntent(intent)
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

    /**
     * Called when an activity you launched exits, giving you the [requestCode] you started it with,
     * the [resultCode] it returned, and any additional data from it. The [resultCode] will be
     * [Activity.RESULT_CANCELED] if the activity explicitly returned that, didn't return any
     * result, or crashed during its operation.
     *
     * This is called when we return from [SelectContactActivity] which is started for its result by
     * our [selectContact] method. [selectContact] is called from our [onCreate] override if our
     * [handleIntent] method was unable to find the value for the contact ID in the [Intent] that
     * started this activity. [SelectContactActivity] presents a UI that allows the user to select
     * a contact when this activity is launched without using one of our Direct Share icons (the
     * Direct Share icons will provide the contact ID in the [Intent] that launches us if they are
     * clicked).
     *
     * When our [requestCode] parameter is [REQUEST_SELECT_CONTACT] we check if our [resultCode] is
     * [Activity.RESULT_OK] and if it is we set our [mContactId] field to the [Int] that is stored
     * in our [Intent] parameter [data] under the key [Contact.ID] defaulting to [Contact.INVALID_ID]
     * if there is no such extra in [data]. If [mContactId] is [Contact.INVALID_ID] we call [finish]
     * to close this activity and return (we give up sharing the send_message if the user didn't
     * choose a contact). Otherwise we call our [prepareUi] method to have it update our UI with the
     * contact ID value.
     *
     * If our [requestCode] parameter is NOT [REQUEST_SELECT_CONTACT] we call our super's
     * implementation of `onActivityResult`.
     *
     * @param requestCode The integer request code originally supplied to [startActivityForResult],
     * allowing you to identify who this result came from.
     * @param resultCode The integer result code returned by the child activity through its
     * [setResult], either [Activity.RESULT_CANCELED] or [Activity.RESULT_OK].
     * @param data An [Intent], which can return result data to the caller (various data can be
     * attached as [Intent] "extras").
     */
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
     * Handles the contents of the [Intent] that started us. This method can only handle intents for
     * sharing plain text. [mBody] and [mContactId] are modified accordingly.
     *
     * If the `action` of our [Intent] parameter [intent] is [Intent.ACTION_SEND] and the MIME type
     * of [intent] is "text/plain" we initialize our [String] field [mBody] to the extra stored in
     * [intent] under the key [Intent.EXTRA_TEXT], then when [Build.VERSION.SDK_INT] is greater than
     * or equal to [Build.VERSION_CODES.P] and [intent] has an extra stored under the key
     * [ShortcutManagerCompat.EXTRA_SHORTCUT_ID] we initialize our [String] variable `val shortcutId`
     * to the value stored under the key [ShortcutManagerCompat.EXTRA_SHORTCUT_ID] and set our field
     * [mContactId] to the integer value of `shortcutId`. Otherwise we set [mContactId] to
     * [Contact.INVALID_ID]. In either case we return `true` to our caller.
     *
     *  If the `action` of our [Intent] parameter [intent] is NOT [Intent.ACTION_SEND] or the MIME
     *  type of [intent] is NOT "text/plain" we return `false` to the caller.
     *
     * @param intent The [Intent] that started this activity.
     * @return `true` if the [Intent] parameter [intent] is handled properly.
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
     * Sets up the UI to display the [Contact] associated with the contact ID in our [mContactId]
     * field, and the message stored in our [String] field [mBody].
     *
     * If our [mContactId] field is not [Contact.INVALID_ID] we initialize our [Contact] variable
     * `val contact` to the [Contact] whose ID is [mContactId], then we call the [ContactViewBinder.bind]
     * method to have it set the text of our [TextView] field [mTextContactName] to the `name` property
     * of `contact` and set the start Drawable of [mTextContactName] to the resource ID stored in the
     * `icon` property of `contact`.
     *
     * Finally we set the text of the [TextView] field [mTextMessageBody] to our [String] field [mBody].
     */
    private fun prepareUi() {
        if (mContactId != Contact.INVALID_ID) {
            val contact: Contact = Contact.byId(mContactId)
            ContactViewBinder.bind(contact, mTextContactName)
        }
        mTextMessageBody.text = mBody
    }

    /**
     * Delegates selection of a [Contact] to [SelectContactActivity] by starting it for its result.
     * We initialize our [Intent] variable `val intent` to an instance intended to start the class
     * [SelectContactActivity], and set the `action` of `intent` to the action string for Intents
     * [SelectContactActivity.ACTION_SELECT_CONTACT] that [SelectContactActivity] honors. Finally
     * we start the activity selected by `intent` with [REQUEST_SELECT_CONTACT] as the `requestCode`
     * that will be returned to our [onActivityResult] override.
     */
    private fun selectContact() {
        val intent = Intent(this, SelectContactActivity::class.java)
        intent.action = SelectContactActivity.ACTION_SELECT_CONTACT
        startActivityForResult(intent, REQUEST_SELECT_CONTACT)
    }

    /**
     * The [View.OnClickListener] for the "Send" [Button] in our UI, resource ID `R.id.send`.
     * When the `id` property of the [View] that was clicked is `R.id.send` we call our [send]
     * method to have it pretend to send the text to the contact.
     */
    private val mOnClickListener = View.OnClickListener { view: View ->
        when (view.id) {
            R.id.send -> send()
        }
    }

    /**
     * Pretends to send the text to the contact. This only toasts a dummy message and calls [finish]
     * to close the activity.
     */
    private fun send() {
        Toast.makeText(
            this,
            getString(R.string.message_sent, mBody, Contact.byId(mContactId).name),
            Toast.LENGTH_SHORT
        ).show()

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
