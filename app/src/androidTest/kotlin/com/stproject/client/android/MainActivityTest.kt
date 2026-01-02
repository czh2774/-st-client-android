package com.stproject.client.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainActivityTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun showsAuthOrChatScreen() {
        val loginNodes = composeRule.onAllNodesWithTag("auth.login").fetchSemanticsNodes()
        if (loginNodes.isNotEmpty()) {
            composeRule.onNodeWithTag("auth.login").assertIsDisplayed()
        } else {
            composeRule.onNodeWithTag("chat.send").assertIsDisplayed()
        }
    }
}
