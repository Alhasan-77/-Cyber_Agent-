package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.example.ui.components.GlobalExceptionHandler
import com.example.ui.theme.CyberBackground
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.CyberSurface
import com.example.ui.theme.CyberSurfaceElevated
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.GeminiBorderGradient
import com.example.ui.theme.GeminiSparkleGradient
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private fun getGoogleWebClientId(context: Context): String? {
    val resourceId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )
    return resourceId.takeIf { it != 0 }
        ?.let(context::getString)
        ?.takeIf { it.isNotBlank() }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Silent Auto-Sign-In on startup using Jetpack Credential Manager [GetGoogleIdOption].
 * Uses a single credential option per request for Android 16 (API 36) compatibility.
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: (String?) -> Unit = {},
    scope: CoroutineScope
) {
    val currentUserResult = runCatching { Firebase.auth.currentUser }
    currentUserResult.onFailure { e ->
        GlobalExceptionHandler.reportError(
            phase = "AutoSignIn Firebase.auth Check",
            throwable = e,
            isFatal = false
        )
        onUnauthenticated("تعذر تهيئة FirebaseAuth أثناء التحقق التلقائي: ${e.localizedMessage}")
        return
    }
    if (currentUserResult.getOrNull() != null) {
        onAuthSuccess()
        return
    }

    val clientId = getGoogleWebClientId(context)
    if (clientId == null) {
        onUnauthenticated("إعداد Google Sign-In غير مكتمل: المورد default_web_client_id غير موجود أو فارغ.")
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    val activityContext = context.findActivity() ?: context

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated("نوع بيانات الاعتماد المستلمة غير مدعوم (${credential.type}).")
            }
        } catch (e: NoCredentialException) {
            Log.i("Auth", "No pre-authorized credential found for auto sign-in; awaiting user tap.")
            onUnauthenticated(null)
        } catch (e: GetCredentialCancellationException) {
            Log.i("Auth", "Auto sign-in dismissed by user.")
            onUnauthenticated(null)
        } catch (e: Exception) {
            Log.w("Auth", "Auto sign-in initialization warning: ${e.message}", e)
            onUnauthenticated(null)
        }
    }
}

/**
 * Interactive Google Sign-In flow via Jetpack Credential Manager [GetSignInWithGoogleOption]
 * and Firebase Authentication [GoogleAuthProvider].
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = getGoogleWebClientId(context)
    if (clientId == null) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found or empty")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    val activityContext = context.findActivity() ?: context

    scope.launch {
        try {
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                Firebase.auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type: ${credential.type}")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            GlobalExceptionHandler.reportError(
                phase = "Interactive Google Sign-In",
                throwable = e,
                isFatal = false
            )
            val detail = e.localizedMessage ?: "Sign in failed"
            onAuthError("تعذر إتمام تسجيل الدخول بـ Google ($detail). تأكد من إضافة حساب Google في إعدادات الجهاز وتوفر اتصال الإنترنت.")
        }
    }
}

/**
 * Reusable Google Sign-In button component powered by Jetpack Credential Manager and Firebase Auth.
 */
@Composable
fun GoogleSignInButton(
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }

    Button(
        onClick = {
            isLoading = true
            onGoogleSignInClicked(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = {
                    isLoading = false
                    onAuthSuccess()
                },
                onAuthError = { errorMsg ->
                    isLoading = false
                    onAuthError(errorMsg)
                },
                scope = coroutineScope,
                onAuthCancelled = {
                    isLoading = false
                }
            )
        },
        enabled = !isLoading,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("google_sign_in_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(24.dp)
                    .testTag("google_sign_in_loading_indicator"),
                color = CyberBackground,
                strokeWidth = 2.5.dp
            )
        } else {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = CyberBackground
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(id = R.string.sign_in_google),
                style = MaterialTheme.typography.titleMedium,
                color = CyberBackground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Signs out the current user by clearing Credential Manager state inside [NonCancellable]
 * BEFORE calling [Firebase.auth.signOut], ensuring unmounting the calling composable
 * does not cancel credential cleanup.
 */
fun signOutUser(
    context: Context,
    onSignOutComplete: () -> Unit = {},
    scope: CoroutineScope
) {
    val credentialManager = CredentialManager.create(context)
    scope.launch {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}", e)
            } finally {
                runCatching { Firebase.auth.signOut() }
                onSignOutComplete()
            }
        }
    }
}

/**
 * Switches Google accounts by clearing cached Credential Manager state first and then
 * launching the interactive [GetSignInWithGoogleOption] account selector.
 */
fun switchGoogleAccount(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: (FirebaseUser?) -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w("Auth", "Failed to clear credential state prior to account switch: ${e.message}")
        }
        onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                onAuthSuccess(runCatching { Firebase.auth.currentUser }.getOrNull())
            },
            onAuthError = onAuthError,
            scope = scope
        )
    }
}

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = { diagnosticWarning ->
                if (!diagnosticWarning.isNullOrBlank()) {
                    errorMessage = diagnosticWarning
                }
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CyberBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("auth_screen_root"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gemini 4-point Sparkle Orb
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(GeminiSparkleGradient),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "مرحباً بك في Gemini Studio",
                style = MaterialTheme.typography.displayLarge.copy(
                    brush = GeminiSparkleGradient
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(id = R.string.studio_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = CyberTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Image(
                painter = painterResource(id = R.drawable.img_studio_hero),
                contentDescription = "Gemini Studio Banner",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(155.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, GeminiBorderGradient, RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberBorder, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CyberSurface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FeatureRowItem(
                        icon = Icons.Default.AutoAwesome,
                        title = "محادثة Gemini و ChatGPT والوسائط المتعددة",
                        subtitle = "تبديل فوري بين النماذج، توليد صور 4K وفيديو، ومحادثة صوتية حية"
                    )
                    FeatureRowItem(
                        icon = Icons.Default.Code,
                        title = "استوديو برمجة Full-Stack ومعاينة حية",
                        subtitle = "كتابة الكود، تشغيل المعاينة الفورية، واقتراحات Gemini الذكية"
                    )
                    FeatureRowItem(
                        icon = Icons.Default.Storage,
                        title = "مزامنة Google Cloud Firestore والمختبر الحيوي 3D",
                        subtitle = "حفظ المشاريع وتجارب طي البروتين وCRISPR تلقائياً في حسابك"
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("auth_error_banner"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            GoogleSignInButton(
                onAuthSuccess = {
                    errorMessage = null
                    onAuthSuccess()
                },
                onAuthError = { msg ->
                    errorMessage = msg
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudDone,
                    contentDescription = null,
                    tint = CyberEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(id = R.string.security_warning_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = CyberTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FeatureRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CyberSurfaceElevated)
                .border(1.dp, CyberBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = CyberCyan,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = CyberTextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CyberTextSecondary
            )
        }
    }
}
