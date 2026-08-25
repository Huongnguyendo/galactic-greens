package com.doquynhhuong.project.data

import android.content.Context
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

object DeliveryPhotoUploader {

    /**
     * Uploads [file] to Firebase Storage under
     * delivery_photos/{firebaseUid}/{ownerEmail}/{orderId}/{file.name} and appends the
     * download URL to the Firestore order document at
     * users/{ownerEmail}/orders/{orderId}. Returns the download URL.
     */
    suspend fun uploadPhoto(
        context: Context,
        file: File,
        ownerEmail: String,
        orderId: String
    ): String {
        require(ownerEmail.isNotBlank()) { "ownerEmail is required to upload photo" }
        require(orderId.isNotBlank()) { "orderId is required to upload photo" }
        require(file.isFile) { "Captured photo file does not exist: ${file.name}" }
        require(file.length() > 0L) { "Captured photo file is empty: ${file.name}" }

        val sanitizedEmail = ownerEmail.trim().lowercase()
        android.util.Log.d("DeliveryPhotoUploader", "Uploading photo: email=$sanitizedEmail, orderId=$orderId, file=${file.name}")

        val firebaseUser = Firebase.auth.currentUser
            ?: throw IllegalStateException("Sign in before uploading a delivery photo")
        require(firebaseUser.email?.trim()?.lowercase() == sanitizedEmail) {
            "The signed-in account does not own this order"
        }

        val storage = Firebase.storage
        // A local name can be reused after an app reinstall. Give every upload
        // a unique object name so an older Firebase photo is never overwritten.
        val objectName = "${UUID.randomUUID()}_${file.name}"
        val path = "delivery_photos/${firebaseUser.uid}/${sanitize(sanitizedEmail)}/$orderId/$objectName"
        val ref = storage.reference.child(path)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        val uploadSnapshot = try {
            val inputStream = requireNotNull(
                context.contentResolver.openInputStream(android.net.Uri.fromFile(file))
            ) { "Could not open captured photo: ${file.name}" }
            inputStream.use { stream ->
                ref.putStream(stream, metadata).await()
            }
        } catch (e: Exception) {
            throw IllegalStateException("Firebase Storage upload failed at $path: ${e.message}", e)
        }

        // Resolve the URL from the exact object reference returned by the
        // completed upload. Looking it up again through the original reference
        // can produce ERROR_OBJECT_NOT_FOUND with some Storage backends.
        val downloadUrl = try {
            uploadSnapshot.storage.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw IllegalStateException("Photo uploaded but its download URL could not be read: ${e.message}", e)
        }
        android.util.Log.d("DeliveryPhotoUploader", "Storage upload OK: $downloadUrl")

        val db = FirebaseFirestore.getInstance()
        val orderRef = db.collection("users")
            .document(sanitizedEmail)
            .collection("orders")
            .document(orderId)

        android.util.Log.d("DeliveryPhotoUploader", "Updating Firestore at: users/$sanitizedEmail/orders/$orderId")
        try {
            orderRef.update("photoUrls", FieldValue.arrayUnion(downloadUrl)).await()
        } catch (e: Exception) {
            throw IllegalStateException("Photo uploaded but the order could not be updated: ${e.message}", e)
        }
        android.util.Log.d("DeliveryPhotoUploader", "Firestore update OK")

        return downloadUrl
    }

    private fun sanitize(key: String): String =
        key.replace(Regex("[^A-Za-z0-9_-]"), "_")
}
