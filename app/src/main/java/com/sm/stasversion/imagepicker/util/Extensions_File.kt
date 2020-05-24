package com.sm.stasversion.imagepicker.util

import java.io.File
import java.net.URLConnection

val File.isImageFile: Boolean get() = URLConnection.guessContentTypeFromName(this.absolutePath).startsWith("image")

val File.isVideoFile: Boolean get() = URLConnection.guessContentTypeFromName(this.absolutePath).startsWith("video")