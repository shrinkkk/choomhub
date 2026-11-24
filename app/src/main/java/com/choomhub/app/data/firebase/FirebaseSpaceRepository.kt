package com.choomhub.app.data.firebase

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.choomhub.app.core.firebase.FirebaseConfig
import com.choomhub.app.domain.model.Account
import com.choomhub.app.domain.model.InviteCodes
import com.choomhub.app.domain.model.SpaceState
import com.choomhub.app.domain.repository.SpaceException
import com.choomhub.app.domain.repository.SpaceRepository
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Firebase Auth + Firestore implementation of the shared space.
 * Data model and the rules that guard it: firebase/firestore.rules.
 */
@Singleton
class FirebaseSpaceRepository @Inject constructor() : SpaceRepository {

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeSpace(): Flow<SpaceState> = authUser().flatMapLatest { account ->
        if (account == null) {
            flowOf(SpaceState.SignedOut)
        } else {
            db.collection(USERS).document(account.uid).observe()
                .map { it?.getString("coupleId") }
                .distinctUntilChanged()
                .flatMapLatest { coupleId ->
                    if (coupleId == null) {
                        flowOf(SpaceState.NoSpace(account))
                    } else {
                        db.collection(COUPLES).document(coupleId).observe().map { snap ->
                            spaceStateFrom(account, coupleId, snap?.get("members") as? List<*>, snap?.getString("inviteCode"))
                        }
                    }
                }
        }
    }.distinctUntilChanged()

    private fun authUser(): Flow<Account?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { fa ->
            trySend(fa.currentUser?.let { Account(it.uid, it.email) })
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }.distinctUntilChanged()

    override suspend fun signInWithGoogle(activityContext: Context): Result<Unit> = runCatching {
        val option = GetSignInWithGoogleOption.Builder(FirebaseConfig.webClientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = try {
            CredentialManager.create(activityContext).getCredential(activityContext, request).credential
        } catch (e: GetCredentialCancellationException) {
            throw SpaceException("Sign-in was cancelled.")
        } catch (e: NoCredentialException) {
            throw SpaceException("No Google account found on this phone. Add one in Settings → Accounts, then try again.")
        } catch (e: GetCredentialException) {
            throw SpaceException("Couldn't open Google sign-in. Check that the phone has a Google account and is online.")
        }
        if (credential !is CustomCredential || credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            throw SpaceException("Google didn't return a usable sign-in.")
        }
        val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        Unit
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    /** Called with an application or activity context by the UI layer when signing out fully. */
    suspend fun clearCredentials(context: Context) {
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    override suspend fun startSpace(): Result<Unit> = runCatching {
        val uid = requireUid()
        val couple = db.collection(COUPLES).document()
        val code = InviteCodes.generate()
        db.batch()
            .set(couple, mapOf("members" to listOf(uid), "createdBy" to uid, "createdAt" to FieldValue.serverTimestamp(), "inviteCode" to code))
            .set(db.collection(INVITES).document(code), newInvite(couple.id, uid))
            .set(db.collection(USERS).document(uid), mapOf("coupleId" to couple.id, "updatedAt" to FieldValue.serverTimestamp()))
            .commit()
            .await()
        Unit
    }.mapFailure("Couldn't start your space. Check your connection and try again.")

    override suspend fun joinSpace(code: String): Result<Unit> = runCatching {
        val uid = requireUid()
        val normalized = InviteCodes.normalize(code)
        if (!InviteCodes.isWellFormed(normalized)) throw SpaceException("Codes are ${InviteCodes.LENGTH} letters and numbers.")
        val inviteRef = db.collection(INVITES).document(normalized)
        val invite = try {
            inviteRef.get(Source.SERVER).await()
        } catch (e: FirebaseFirestoreException) {
            throw SpaceException("You need to be online to join.")
        }
        if (!invite.exists()) throw SpaceException("That code doesn't match any space.")
        if (invite.getString("createdBy") == uid) throw SpaceException("That's your own code. Your partner enters it on their phone.")
        if (invite.get("usedBy") != null) throw SpaceException("That code has already been used.")
        val expires = invite.getTimestamp("expiresAt")
        if (expires == null || expires.toDate().before(Date())) throw SpaceException("That code has expired. Ask for a new one.")
        val coupleId = invite.getString("coupleId") ?: throw SpaceException("That code is broken. Ask for a new one.")
        db.batch()
            .update(inviteRef, "usedBy", uid)
            .update(db.collection(COUPLES).document(coupleId), "members", FieldValue.arrayUnion(uid))
            .set(db.collection(USERS).document(uid), mapOf("coupleId" to coupleId, "updatedAt" to FieldValue.serverTimestamp()))
            .commit()
            .await()
        Unit
    }.mapFailure("Couldn't join. The code may be used up or the space may be full.")

    override suspend fun refreshInvite(): Result<Unit> = runCatching {
        val uid = requireUid()
        val coupleId = db.collection(USERS).document(uid).get().await().getString("coupleId")
            ?: throw SpaceException("You don't have a space yet.")
        val code = InviteCodes.generate()
        db.batch()
            .set(db.collection(INVITES).document(code), newInvite(coupleId, uid))
            .update(db.collection(COUPLES).document(coupleId), "inviteCode", code)
            .commit()
            .await()
        Unit
    }.mapFailure("Couldn't make a new code. Check your connection and try again.")

    private fun requireUid(): String = auth.currentUser?.uid ?: throw SpaceException("Sign in first.")

    private fun newInvite(coupleId: String, uid: String) = mapOf(
        "coupleId" to coupleId,
        "createdBy" to uid,
        "usedBy" to null,
        "expiresAt" to Timestamp(Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(INVITE_HOURS))),
    )

    private fun <T> Result<T>.mapFailure(fallback: String): Result<T> =
        recoverCatching { throw if (it is SpaceException) it else SpaceException(fallback) }

    companion object {
        const val USERS = "users"
        const val COUPLES = "couples"
        const val INVITES = "invites"
        const val INVITE_HOURS = 48L

        /** Pure mapping from the couple document to a [SpaceState]; unit-tested. */
        fun spaceStateFrom(account: Account, coupleId: String, members: List<*>?, inviteCode: String?): SpaceState {
            val uids = members?.filterIsInstance<String>().orEmpty()
            val partner = uids.firstOrNull { it != account.uid }
            return when {
                account.uid !in uids -> SpaceState.NoSpace(account)
                partner != null -> SpaceState.Paired(account, coupleId, partner)
                else -> SpaceState.Waiting(account, coupleId, inviteCode.orEmpty())
            }
        }
    }
}
