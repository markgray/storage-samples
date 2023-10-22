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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat

/**
 * Provides the Sharing Shortcuts items to the system.
 *
 * Use the [ShortcutManagerCompat] to make it work on older Android versions
 * without any extra work needed.
 *
 * Interactions with the ShortcutManager API can happen on any thread.
 */
class SharingShortcutsManager {
    /**
     * Publish the list of dynamics shortcuts that will be used in Direct Share.
     *
     * For each shortcut, we specify the categories that it will be associated to,
     * the intent that will trigger when opened as a static launcher shortcut,
     * and the Shortcut ID between other things.
     *
     * The Shortcut ID that we specify in the [ShortcutInfoCompat.Builder] constructor will
     * be received in the intent as [Intent.EXTRA_SHORTCUT_ID].
     *
     * In this code sample, this method is completely static. We are always setting the same sharing
     * shortcuts. In a real-world example, we would replace existing shortcuts depending on
     * how the user interacts with the app as often as we want to.
     *
     * First we initialize our [ArrayList] of [ShortcutInfoCompat] variable `val shortcuts` to a new
     * instance (a [ShortcutInfoCompat] is a support library "Helper for accessing features in
     * `ShortcutInfo`"). Then we initialize our [MutableSet] of [String] variable `val contactCategories`
     * to a new instance of [HashSet] and add our [CATEGORY_TEXT_SHARE_TARGET] constant [String] to
     * it (this is used in our resource file xml/shortcuts.xml as the `<category>` element of the
     * `<share-target>` for [SendMessageActivity] that also specifies that that activity accepts
     * data of type android:mimeType="text/plain" in the `<data>` element of that `<share-target>`).
     *
     * Next we loop over `id` from 0 until [MAX_SHORTCUTS] (4) and:
     *  - we initialize our [Contact] variable `val contact` to the [Contact] with contact ID `id`
     *  that is returned by the [Contact.byId] method.
     *  - we initialize our [Intent] variable `val staticLauncherShortcutIntent` to a new instance
     *  whose action is [Intent.ACTION_DEFAULT] (synonym for [Intent.ACTION_VIEW], the "standard"
     *  action that is performed on a piece of data).
     *  - we add a [ShortcutInfoCompat] to `shortcuts` that we build using a [ShortcutInfoCompat.Builder]
     *  for our [Context] parameter [context] and the [String] value of `id` as the shortcut ID.
     *      - we set the short title of the shortcut to the `name` property  of `contact`.
     *      - we set the icon of the shortcut to the [IconCompat] that the [IconCompat.createWithResource]
     *      method creates from the resource ID given in the `icon` property of `contact`.
     *      - we set the intent of the shortcut to `staticLauncherShortcutIntent`.
     *      - we set the shortcut to be "Long Lived" (if it is long lived it can be cached by various
     *      system services even after it has been "unpublished" as a dynamic shortcut).
     *      - we set the categories for the shortcut to `contactCategories` (Launcher apps may use
     *      this information to categorize shortcuts, and it is used by the system to associate a
     *      published Sharing Shortcut with supported mimeTypes).
     *      - we associate a [Person] with the shortcut that we build to use the `name` property of
     *      `contact` as the name to use for display.
     *      - Finally we build the [ShortcutInfoCompat.Builder] into a [ShortcutInfoCompat].
     *
     * When done with the loop we call the [ShortcutManagerCompat.addDynamicShortcuts] method with
     * our [Context] parameter [context] and `shortcuts` to publish the list of dynamic shortcuts.
     *
     * @param context the [Context] of [MainActivity], used to retrieve resources, access system
     * services, and identify this app.
     */
    @SuppressLint("ReportShortcutUsage")
    fun pushDirectShareTargets(context: Context) {
        val shortcuts = ArrayList<ShortcutInfoCompat>()

        // Category that our sharing shortcuts will be assigned to
        val contactCategories: MutableSet<String> = HashSet()
        contactCategories.add(CATEGORY_TEXT_SHARE_TARGET)

        // Adding maximum number of shortcuts to the list
        for (id: Int in 0 until MAX_SHORTCUTS) {
            val contact: Contact = Contact.byId(id)

            // Item that will be sent if the shortcut is opened as a static launcher shortcut
            val staticLauncherShortcutIntent = Intent(Intent.ACTION_DEFAULT)

            // Creates a new Sharing Shortcut and adds it to the list
            // The id passed in the constructor will become EXTRA_SHORTCUT_ID in the received Intent
            shortcuts.add(ShortcutInfoCompat.Builder(context, id.toString())
                .setShortLabel(contact.name)
                // Icon that will be displayed in the share target
                .setIcon(IconCompat.createWithResource(context, contact.icon))
                .setIntent(staticLauncherShortcutIntent)
                // Make this sharing shortcut cached by the system
                // Even if it is unpublished, it can still appear on the sharesheet
                .setLongLived(true)
                .setCategories(contactCategories)
                // Person objects are used to give better suggestions
                .setPerson(Person.Builder()
                    .setName(contact.name)
                    .build())
                .build()
            )
        }
        ShortcutManagerCompat.addDynamicShortcuts(context, shortcuts)
    }

    /**
     * Remove all dynamic shortcuts. We just call [ShortcutManagerCompat.removeAllDynamicShortcuts]
     * with our [Context] parameter [context] which will delete all dynamic shortcuts from this app.
     * Note that if a shortcut is set as long-lived, it may still be available in the system as a
     * cached shortcut even after being removed from the list of dynamic shortcuts.
     */
    @Suppress("unused") // Suggested change would make class less reusable
    fun removeAllDirectShareTargets(context: Context) {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }

    companion object {
        /**
         * Define maximum number of shortcuts.
         * Don't add more than [ShortcutManagerCompat.getMaxShortcutCountPerActivity].
         */
        private const val MAX_SHORTCUTS = 4

        /**
         * Category name defined in res/xml/shortcuts.xml that accepts data of type text/plain
         * and will trigger [SendMessageActivity].
         */
        private const val CATEGORY_TEXT_SHARE_TARGET =
            "com.example.android.sharingshortcuts.category.TEXT_SHARE_TARGET"
    }
}