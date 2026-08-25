package com.doquynhhuong.project.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Stores the "proof of delivery" photos a customer takes for an order.
 * Photos are staged on-device, in the app's private files
 * directory — app/files/delivery_photos/<orderKey>/photo_<n>.jpg — and are
 * uploaded by DeliveryPhotoUploader. Local copies remain available
 * for an immediate preview while Firebase-backed URLs are synced on the order.
 *
 * An order can have multiple photos (1-5 photos = 10 pts, 6-10 = 20 pts,
 * capped at 20 pts per order), so photos live in a per-order folder rather
 * than a single file.
 */
object DeliveryPhotoStore {

    private const val ROOT_FOLDER = "delivery_photos"

    private fun orderDir(context: Context, orderKey: String): File =
        File(File(context.filesDir, ROOT_FOLDER), sanitize(orderKey)).apply {
            if (!exists()) mkdirs()
        }

    /** All photo files for this order, oldest first (by file name index). */
    fun listPhotos(context: Context, orderKey: String): List<File> {
        if (orderKey.isBlank()) return emptyList()
        val dir = orderDir(context, orderKey)
        return dir.listFiles { f -> f.isFile && f.extension.equals("jpg", ignoreCase = true) }
            ?.sortedBy { file ->
                file.nameWithoutExtension.substringAfter("photo_").toIntOrNull() ?: Int.MAX_VALUE
            }
            ?: emptyList()
    }

    fun photoCount(context: Context, orderKey: String): Int = listPhotos(context, orderKey).size

    /** Deterministic next file for a new capture (photo_1.jpg, photo_2.jpg, ...). */
    fun fileForNextPhoto(context: Context, orderKey: String): File {
        val nextIndex = photoCount(context, orderKey) + 1
        return File(orderDir(context, orderKey), "photo_$nextIndex.jpg")
    }

    /** content:// Uri for an already-saved photo file (for display). */
    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun deleteAllPhotos(context: Context, orderKey: String) {
        if (orderKey.isBlank()) return
        orderDir(context, orderKey).deleteRecursively()
    }

    private fun sanitize(key: String): String =
        key.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
