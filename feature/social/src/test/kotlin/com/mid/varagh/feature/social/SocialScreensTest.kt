package com.mid.varagh.feature.social

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mid.varagh.core.designsystem.theme.VaraghTheme
import com.mid.varagh.core.domain.VaraghException
import com.mid.varagh.core.model.BookMeta
import com.mid.varagh.core.model.FeedItem
import com.mid.varagh.core.model.FeedItemType
import com.mid.varagh.core.model.PublicReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fa")
class SocialScreensTest {

    @get:Rule
    val rule = createComposeRule()

    private var retries = 0
    private var signIns = 0
    private var submitted: Triple<String, String, String>? = null

    private fun feed(state: FeedUiState) = rule.setContent {
        VaraghTheme { FeedScreen(state, {}, {}, { signIns++ }, { retries++ }, {}) }
    }

    @Test
    fun signedOut_feedAsksToSignIn() {
        feed(FeedUiState(loading = false, signedIn = false))
        rule.onNodeWithTag("feed_sign_in").assertExists()
        rule.onNodeWithText("ورود").performClick()
        assertEquals(1, signIns)
    }

    @Test
    fun noServer_showsOfflineStateWithRetry() {
        feed(FeedUiState(loading = false, error = VaraghException.Network(IOException())))
        rule.onNodeWithTag("social_error").assertExists()
        rule.onNodeWithText("اتصال برقرار نیست").assertExists()
        rule.onNodeWithText("تلاش دوباره").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun feedItemsRender() {
        val reader = PublicReader("r1", "sara", "سارا", "", null, null, 3, false)
        feed(
            FeedUiState(
                loading = false,
                items = listOf(FeedItem("f1", reader, BookMeta("b1", "بوف کور", "هدایت", 120, null), FeedItemType.FINISHED, null, 0)),
            ),
        )
        rule.onNodeWithTag("feed_list").assertExists()
        rule.onNodeWithText("بوف کور").assertExists()
        rule.onNodeWithText("سارا این کتاب را تمام کرد").assertExists()
    }

    @Test
    fun login_isEnabledOnlyForValidInput_andShowsServerMessages() {
        rule.setContent {
            VaraghTheme {
                LoginScreen(
                    state = LoginUiState(error = VaraghException.Rejected(409, "این ایمیل قبلاً ثبت شده است")),
                    onBack = {},
                    onModeChange = {},
                    onSubmit = { e, p, u -> submitted = Triple(e, p, u) },
                )
            }
        }
        rule.onNodeWithText("این ایمیل قبلاً ثبت شده است").assertExists()
        rule.onNodeWithTag("login_submit").assertIsNotEnabled()
        rule.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("login_email"))).performTextInput("reader@example.com")
        rule.onNodeWithTag("login_password").performTextInput("short")
        rule.onNodeWithTag("login_submit").assertIsNotEnabled()
        rule.onNodeWithTag("login_password").performTextInput("-but-long-now")
        rule.onNodeWithTag("login_submit").assertIsEnabled().performClick()
        assertEquals("reader@example.com", submitted?.first)
    }

    @Test
    fun loginValidation() {
        assertTrue(LoginViewModel.isValid(false, "a@b.co", "12345678", ""))
        assertFalse(LoginViewModel.isValid(false, "not-an-email", "12345678", ""))
        assertFalse(LoginViewModel.isValid(true, "a@b.co", "12345678", "AB"))
        assertTrue(LoginViewModel.isValid(true, "a@b.co", "12345678", "amir.m_1"))
    }
}
