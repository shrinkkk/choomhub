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
                            spaceStateFrom(
                                account,
                                coupleId,
                                snap?.get("members") as? List<*>,
                                snap?.getString("inviteCode"),
                                snap?.get(IDS) as? Map<*, *>,
                            )
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
        // Coming back to a space someone left: you take over their place (and everything in it),
        // whichever Google account you're on now.
        val vacancy = invite.getString(VACANCY)
        val coupleRef = db.collection(COUPLES).document(coupleId)
        val joining = if (vacancy != null) {
            mapOf("members" to FieldValue.arrayUnion(uid), "$IDS.$uid" to vacancy, VACANCY to FieldValue.delete())
        } else {
            mapOf("members" to FieldValue.arrayUnion(uid))
        }
        db.batch()
            .update(inviteRef, "usedBy", uid)
            .update(coupleRef, joining)
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
        // If someone left, the code carries their place so whoever comes back takes it over.
        val vacancy = db.collection(COUPLES).document(coupleId).get().await().getString(VACANCY)
        db.batch()
            .set(db.collection(INVITES).document(code), newInvite(coupleId, uid, vacancy))
            .update(db.collection(COUPLES).document(coupleId), "inviteCode", code)
            .commit()
            .await()
        Unit
    }.mapFailure("Couldn't make a new code. Check your connection and try again.")

    override suspend fun leaveSpace(): Result<Unit> = runCatching {
        val uid = requireUid()
        val coupleId = try {
            db.collection(USERS).document(uid).get(Source.SERVER).await().getString("coupleId")
        } catch (e: FirebaseFirestoreException) {
            throw SpaceException("You need to be online to leave.")
        } ?: throw SpaceException("You're not in a space.")
        val coupleRef = db.collection(COUPLES).document(coupleId)
        val couple = coupleRef.get(Source.SERVER).await()
        val members = (couple.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
        val meRef = db.collection(USERS).document(uid)
        val unlink = mapOf("coupleId" to null, "updatedAt" to FieldValue.serverTimestamp())
        if (members.any { it != uid }) {
            // They stay; your place is kept for you (points, names, the cat's coins...), and
            // their phone makes a fresh code for you to come back with.
            val memberId = (couple.get(IDS) as? Map<*, *>)?.get(uid) as? String ?: uid
            db.batch()
                .update(coupleRef, mapOf("members" to FieldValue.arrayRemove(uid), VACANCY to memberId, "inviteCode" to ""))
                .set(meRef, unlink)
                .commit()
                .await()
        } else {
            // The last one out: everything in the space is deleted for good.
            coupleRef.update(CLOSING, true).await()
            SpaceWipe.deleteEverything(db, coupleRef)
            db.batch().delete(coupleRef).set(meRef, unlink).commit().await()
        }
        Unit
    }.mapFailure("Couldn't leave. Check your connection and try again.")

    private fun requireUid(): String = auth.currentUser?.uid ?: throw SpaceException("Sign in first.")

    private fun newInvite(coupleId: String, uid: String, vacancy: String? = null) = buildMap {
        put("coupleId", coupleId)
        put("createdBy", uid)
        put("usedBy", null)
        put("expiresAt", Timestamp(Date(System.currentTimeMillis() + TimeUnit.HOURS.toMillis(INVITE_HOURS))))
        if (vacancy != null) put(VACANCY, vacancy)
    }

    private fun <T> Result<T>.mapFailure(fallback: String): Result<T> =
        recoverCatching { throw if (it is SpaceException) it else SpaceException(fallback) }

    companion object {
        const val USERS = "users"
        const val COUPLES = "couples"
        const val INVITES = "invites"
        const val INVITE_HOURS = 48L

        /** couples/{id}.ids: {account uid: member id} for anyone who came back on another account. */
        const val IDS = "ids"

        /** couples/{id}.vacancy: the member id of whoever left, kept for them to come back to. */
        const val VACANCY = "vacancy"

        /** couples/{id}.closing: set by the last one out while everything is deleted. */
        const val CLOSING = "closing"

        /** Pure mapping from the couple document to a [SpaceState]; unit-tested. */
        fun spaceStateFrom(account: Account, coupleId: String, members: List<*>?, inviteCode: String?, ids: Map<*, *>? = null): SpaceState {
            val uids = members?.filterIsInstance<String>().orEmpty()
            fun memberId(uid: String) = ids?.get(uid) as? String ?: uid
            val partner = uids.firstOrNull { it != account.uid }
            return when {
                account.uid !in uids -> SpaceState.NoSpace(account)
                partner != null -> SpaceState.Paired(account, coupleId, memberId(partner), memberId(account.uid))
                else -> SpaceState.Waiting(account, coupleId, inviteCode.orEmpty(), memberId(account.uid))
            }
        }
    }
}
