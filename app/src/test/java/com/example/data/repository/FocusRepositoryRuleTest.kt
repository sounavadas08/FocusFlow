package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.AppLimit
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class FocusRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun getOrCreateProfile_authenticatedUser_createsAndReturnsProfile() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FocusRepository(firestore)

        val profile = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.getOrCreateProfile("Alice")
        }
        assertNotNull(profile)
        assertEquals("Alice", profile.displayName)
    }

    @Test
    fun saveAppLimit_authenticatedUser_persistsAndCanBeObserved() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = FocusRepository(firestore)

        val limit = AppLimit(
            id = "test_instagram",
            userId = uid,
            packageName = "com.instagram.android",
            appName = "Instagram",
            dailyBudgetMinutes = 25,
            blockReels = true,
            blockShorts = false,
            isBlocked = false,
            cooldownSeconds = 60
        )

        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveAppLimit(limit)
        }

        val observed = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.observeAppLimits().first { list -> list.any { it.packageName == "com.instagram.android" } }
        }
        assertTrue(observed.any { it.packageName == "com.instagram.android" && it.blockReels })
    }

    @Test
    fun observeAppLimits_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = FocusRepository(firestore)

        try {
            withTimeout(3000L) {
                repo.observeAppLimits().first()
            }
            fail("Expected exception when unauthenticated")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertTrue(e is IllegalStateException || firestoreException?.code == FirebaseFirestoreException.Code.PERMISSION_DENIED)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_rules@test.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
    }
}
