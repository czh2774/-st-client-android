package com.stproject.client.android

import com.stproject.client.android.core.testing.MainDispatcherRule
import org.junit.Rule

/**
 * Base class for Unit Tests involving Coroutines.
 */
abstract class BaseUnitTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
}

