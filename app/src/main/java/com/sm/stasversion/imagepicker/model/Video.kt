package com.sm.stasversion.imagepicker.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File

/**
 * A Video that was picked by the user.
 */
@Parcelize
data class Video(override val id: Long, override val name: String, override val path: String, override var correction: String, override val crop: String, override var position: Long): Asset, Parcelable {

    val thumbnailUri: Uri get() = Uri.fromFile(File(path))
}