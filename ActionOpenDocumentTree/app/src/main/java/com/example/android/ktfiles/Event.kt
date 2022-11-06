/*
 * Copyright 2019 The Android Open Source Project
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

package com.example.android.ktfiles

import androidx.lifecycle.LiveData

/**
 * Used as a wrapper for data that is exposed via a [LiveData] that represents an event.
 *
 * @param content the [T] that we are constructed to hold.
 */
open class Event<out T>(private val content: T) {

    /**
     * Flag to prevent our [T] field [content] from being accessed more than once.
     */
    @Suppress("MemberVisibilityCanBePrivate") // I like to use kdoc [] references
    var hasBeenHandled: Boolean = false
        private set // Allow external read but not write

    /**
     * Returns the [T] field [content] and prevents its use again by setting our [Boolean] field
     * [hasBeenHandled] to `true`.
     *
     * @return the [T] field [content] we are holding or `null` if [hasBeenHandled] is `true`.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     *
     * @return the [T] field [content] we are holding.
     */
    @Suppress("unused") // Suggested change would make class less reusable
    fun peekContent(): T = content
}