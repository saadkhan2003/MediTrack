package com.meditrack.app.data.repository

import android.app.Activity
import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.meditrack.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isFeatureEnabled: Boolean = BuildConfig.FEATURE_FIREBASE

    private val firebaseAuth: FirebaseAuth? by lazy {
        if (!isFeatureEnabled) return@lazy null
        runCatching {
            FirebaseApp.initializeApp(context)
            FirebaseAuth.getInstance()
        }.getOrNull()
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    val isLoggedIn: Boolean
        get() = currentUser != null

    suspend fun signInWithEmail(email: String, password: String): Result<Unit> {
        if (!isFeatureEnabled) return Result.failure(IllegalStateException("Firebase feature is disabled"))
        val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase not initialized"))
        return runCatching {
            auth.signInWithEmailAndPassword(email, password).await()
        }
    }

    suspend fun registerWithEmail(email: String, password: String): Result<Unit> {
        if (!isFeatureEnabled) return Result.failure(IllegalStateException("Firebase feature is disabled"))
        val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase not initialized"))
        return runCatching {
            auth.createUserWithEmailAndPassword(email, password).await()
        }
    }

    suspend fun signInWithGoogle(idToken: String): Result<Unit> {
        if (!isFeatureEnabled) return Result.failure(IllegalStateException("Firebase feature is disabled"))
        val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase not initialized"))
        return runCatching {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            auth.signInWithCredential(credential).await()
        }
    }

    suspend fun signInWithGoogleInBrowser(activity: Activity): Result<Unit> {
        if (!isFeatureEnabled) return Result.failure(IllegalStateException("Firebase feature is disabled"))
        val auth = firebaseAuth ?: return Result.failure(IllegalStateException("Firebase not initialized"))
        return runCatching {
            val pending = auth.pendingAuthResult
            if (pending != null) {
                pending.await()
            } else {
                val provider = OAuthProvider.newBuilder("google.com").build()
                auth.startActivityForSignInWithProvider(activity, provider).await()
            }
        }
    }

    fun signOut() {
        firebaseAuth?.signOut()
    }
}
