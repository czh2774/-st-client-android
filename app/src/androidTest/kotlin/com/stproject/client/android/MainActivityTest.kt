package com.stproject.client.android

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import org.junit.Rule
import org.junit.Test

class MainActivityTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

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
