package com.example.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException
import org.json.JSONObject

enum class OperationType {
    GET, LIST, CREATE, UPDATE, DELETE
}

fun handleFirestoreError(exception: Exception, operation: OperationType, path: String) {
    val errorDetails = JSONObject().apply {
        put("operation", operation.name)
        put("path", path)
        put("message", exception.localizedMessage ?: "Unknown Firestore error")
        if (exception is FirebaseFirestoreException) {
            put("code", exception.code.name)
        }
    }
    Log.e("FirestoreError", errorDetails.toString(), exception)
}
