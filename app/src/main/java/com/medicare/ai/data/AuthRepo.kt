package com.medicare.ai.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.callbackFlow

class AuthRepo(private val dao: AppDao) {
    private val fb = FirebaseAuth.getInstance()
    val currentUser get() = fb.currentUser
    val currentUid get() = fb.currentUser?.uid

    val userFlow: Flow<FirebaseUser?> = callbackFlow {
        val l = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        fb.addAuthStateListener(l)
        awaitClose { fb.removeAuthStateListener(l) }
    }
    val uidFlow: Flow<String?> = userFlow.map { it?.uid }.distinctUntilChanged()

    suspend fun signUp(name: String, email: String, pw: String) {
        val r = fb.createUserWithEmailAndPassword(email, pw).await()
        r.user?.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build())?.await()
        dao.upsertUser(UserEntity(r.user!!.uid, email, name))
    }
    suspend fun login(email: String, pw: String) {
        val r = fb.signInWithEmailAndPassword(email, pw).await()
        dao.upsertUser(UserEntity(r.user!!.uid, email, r.user?.displayName.orEmpty()))
    }
    suspend fun resetPassword(email: String) { fb.sendPasswordResetEmail(email).await() }
    suspend fun token(): String? = fb.currentUser?.getIdToken(false)?.await()?.token
    fun logout() = fb.signOut()

    fun friendly(e: Throwable): String = when (e) {
        is FirebaseAuthWeakPasswordException -> "Password must be at least 6 characters."
        is FirebaseAuthInvalidUserException -> "No account found for this email."
        is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseNetworkException -> "No internet connection. Please try again."
        else -> e.message ?: "Something went wrong."
    }
}