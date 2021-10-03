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

/**
 * Provides the list of dummy contacts. This sample implements this as constants, but real-life apps
 * should use a database and such.
 *
 * @param name The name of the [Contact].
 */
class Contact(
    val name: String
) {
    /**
     * The resource ID of the icon used by all [Contact]s.
     *
     * @return The resource ID for the icon.
     */
    val icon: Int
        get() = R.mipmap.logo_avatar

    companion object {
        /**
         * The list of dummy contacts.
         */
        val CONTACTS = arrayOf(
            Contact("Tereasa"),
            Contact("Chang"),
            Contact("Kory"),
            Contact("Clare"),
            Contact("Landon"),
            Contact("Kyle"),
            Contact("Deana"),
            Contact("Daria"),
            Contact("Melisa"),
            Contact("Sammie"))

        /**
         * The contact ID.
         */
        const val ID = "contact_id"

        /**
         * Representative invalid contact ID.
         */
        const val INVALID_ID = -1

        /**
         * Finds a [Contact] specified by a contact ID.
         *
         * @param id The contact ID. This needs to be a valid ID.
         * @return A [Contact]
         */
        @JvmStatic
        fun byId(id: Int): Contact {
            return CONTACTS[id]
        }
    }
}