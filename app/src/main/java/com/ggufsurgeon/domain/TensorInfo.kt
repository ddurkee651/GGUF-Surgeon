package com.ggufsurgeon.domain

import android.os.Parcelable
import kotlinx.parcelize.Parcelize  // ✅ ADD THIS IMPORT

@Parcelize
data class TensorInfo(
    val name: String,
    val shape: List<Int>,
    val type: String,
    val bytes: Long
) : Parcelable
