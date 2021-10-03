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

import android.widget.TextView

/**
 * A simple utility to bind a [TextView] with a [Contact].
 */
object ContactViewBinder {
    /**
     * Binds the [TextView] parameter [textView] with the specified [Contact] parameter [contact].
     * We set the `text of [textView] to the `name` property of [contact], then set the start Drawable
     * of [textView] to the Drawable whose resource ID is given by the `icon` property of [contact].
     *
     * @param contact  The [Contact].
     * @param textView The [TextView].
     */
    fun bind(contact: Contact, textView: TextView) {
        textView.text = contact.name
        textView.setCompoundDrawablesRelativeWithIntrinsicBounds(contact.icon, 0, 0, 0)
    }
}