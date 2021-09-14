/*
 * Copyright (C) 2020 The Android Open Source Project
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

package com.android.samples.safdemos.imagepicker

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.android.samples.safdemos.databinding.FragmentImagePickerBinding

const val IMAGE_MIME_TYPE = "image/*"

class ImagePickerFragment : Fragment() {
    private lateinit var binding: FragmentImagePickerBinding

    private val resultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Uri? = result.data?.data
                data?.let { uri: Uri ->
                    activity?.let {
                        binding.preview.setImageBitmap(
                            BitmapFactory.decodeStream(it.contentResolver.openInputStream(uri))
                        )
                    }
                }
            }
        }

    @Suppress("RedundantNullableReturnType")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentImagePickerBinding.inflate(layoutInflater)

        binding.selectImageButton.setOnClickListener {
            selectImage()
        }

        return binding.root
    }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = IMAGE_MIME_TYPE
        }

        resultLauncher.launch(intent)
    }

}
