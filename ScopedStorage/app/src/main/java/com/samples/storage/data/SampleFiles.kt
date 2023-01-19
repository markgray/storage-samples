/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samples.storage.data

/**
 * List of remote sample files to be used in the different samples
 */
@Suppress("MemberVisibilityCanBePrivate") // I like to use kdoc [] references
object SampleFiles {
    /**
     * URL's of sample image files that can be downloaded
     */
    val images: List<String> = listOf(
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Balcony%20Toss/card.jpg",
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Dance%20Search/card.jpg",
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Extra%20Spicy/card.jpg",
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Get%20Your%20Money's%20Worth/card.jpg"
    )

    /**
     * URL's of sample video files that can be downloaded
     */
    val video: List<String> = listOf(
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Balcony%20Toss.mp4",
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Dance%20Search.mp4",
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Extra%20Spicy.mp4",
        "https://storage.googleapis.com/android-tv/Sample%20videos/Demo%20Slam/Google%20Demo%20Slam_%20Get%20Your%20Money's%20Worth.mp4"
    )

    /**
     * [List] of all the sample files contained in both [images] and [video] lists.
     */
    @Suppress("unused") // Suggested change would make class less reusable
    val media: List<String> = images + video

    /**
     * List of sample markdown files that can be downloaded.
     */
    val texts: List<String> = listOf(
        "https://raw.githubusercontent.com/android/storage-samples/main/README.md",
        "https://raw.githubusercontent.com/android/security-samples/main/README.md"
    )

    /**
     * List of sample pdf files that can be downloaded.
     */
    val documents: List<String> = listOf(
        "https://developer.android.com/images/jetpack/compose/compose-testing-cheatsheet.pdf",
        "https://developer.android.com/images/training/dependency-injection/hilt-annotations.pdf",
        "https://android.github.io/android-test/downloads/espresso-cheat-sheet-2.1.0.pdf"
    )

    /**
     * List of sample zip archives that can be downloaded.
     */
    val archives: List<String> = listOf(
        "https://github.com/android/storage-samples/archive/refs/heads/main.zip",
        "https://github.com/android/security-samples/archive/refs/heads/main.zip"
    )

    /**
     * [List] of [String]'s which includes all the entries in [texts], [documents] and [archives]
     */
    val nonMedia: List<String> = texts + documents + archives
}
