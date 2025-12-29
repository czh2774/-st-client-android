package com.stproject.client.android

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

/**
 * Base class for Unit Tests involving Coroutines.
 */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class BaseUnitTest {

    protected val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setupCoroutines() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDownCoroutines() {
        Dispatchers.resetMain()
    }
}

