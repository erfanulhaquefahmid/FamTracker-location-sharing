package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.UserRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UserConsentTest {

    private lateinit var database: AppDatabase
    private lateinit var userRepository: UserRepository

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        userRepository = UserRepository(database.userDao())
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun userConsentFlow_persistsConsentWithoutFamilyId() = runBlocking {
        // User signs in (no family yet)
        val profile = userRepository.getOrCreateDefaultUser("Alex")
        assertFalse("New user must not have consented yet", profile.hasConsentedToTracking)

        // User taps 'I agree — Continue to FamTrack'
        userRepository.updateConsent(profile.uid, true)

        val updatedProfile = userRepository.getCurrentUser()
        assertTrue("User consent must be persisted even before family creation", updatedProfile?.hasConsentedToTracking == true)
    }
}
