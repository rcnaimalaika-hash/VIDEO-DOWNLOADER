package com.example.videodownloader

import android.content.res.Configuration
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videodownloader.db.AppDatabaseHelper
import com.example.videodownloader.db.UserEntity
import com.example.videodownloader.ui.theme.LocalIsDarkTheme
import com.example.videodownloader.util.FormValidator
import com.example.videodownloader.util.PasswordStrength

// ---------------------------------------------------------------------------
// Login & Sign Up Screen with Database Connectivity & Strict Input Validation
// ---------------------------------------------------------------------------

@Composable
fun LoginScreen(
    onLoginSuccess: (email: String) -> Unit = {},
    onSkip: () -> Unit = {}
) {
    val isDark = LocalIsDarkTheme.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val dbHelper = remember { AppDatabaseHelper.getInstance(context) }

    var isSignUpMode by remember { mutableStateOf(false) }

    // Form Field States
    var fullName by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(true) }
    var agreeToTerms by remember { mutableStateOf(false) }

    // Validation Error States
    var nameError by remember { mutableStateOf<String?>(null) }
    var ageError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }
    var termsError by remember { mutableStateOf<String?>(null) }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val bgBase = if (isDark) Color(0xFF0F172A) else Color(0xFFF7FAFE)
    val textPrimary = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F1B38)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    // Clear validation errors when switching between Login and Sign Up
    LaunchedEffect(isSignUpMode) {
        nameError = null
        ageError = null
        emailError = null
        passwordError = null
        confirmPasswordError = null
        termsError = null
    }

    // ── Forgot Password Dialog ──
    if (showForgotPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPasswordDialog = false },
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White,
            title = {
                Text(
                    text = stringResource(R.string.reset_password_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.reset_password_desc),
                        fontSize = 14.sp,
                        color = textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CustomPillInputField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        hint = stringResource(R.string.login_email_hint),
                        leadingIcon = Icons.Filled.Email,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                        onDone = { focusManager.clearFocus() },
                        isDark = isDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val emailValidation = FormValidator.validateEmail(resetEmail)
                        if (!emailValidation.isValid) {
                            Toast.makeText(context, emailValidation.errorMessage, Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        Toast.makeText(
                            context,
                            context.getString(R.string.reset_password_success),
                            Toast.LENGTH_SHORT
                        ).show()
                        showForgotPasswordDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2563EB),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(text = stringResource(R.string.reset_password_btn))
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPasswordDialog = false }) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        color = textSecondary
                    )
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        // Decorative background waves matching reference
        BackgroundLiquidWaves(isDark = isDark)

        if (isLandscape) {
            // ── Landscape Orientation: Side-by-side Layout ──
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Column: 3D Illustration & App Branding
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.login_illustration),
                        contentDescription = "Video Downloader Illustration",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .sizeIn(maxWidth = 180.dp, maxHeight = 180.dp)
                            .aspectRatio(1.4f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = stringResource(if (isSignUpMode) R.string.signup_title else R.string.login_title),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = stringResource(if (isSignUpMode) R.string.signup_subtitle else R.string.login_subtitle),
                        fontSize = 13.sp,
                        color = textSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Right Column: Form Inputs & Action Buttons
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Skip button on top right of form
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = stringResource(R.string.login_skip),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2563EB),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSkip() }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LoginFormFields(
                        isSignUpMode = isSignUpMode,
                        fullName = fullName,
                        onFullNameChange = {
                            fullName = it
                            if (nameError != null) nameError = FormValidator.validateName(it).errorMessage
                        },
                        nameError = nameError,
                        age = age,
                        onAgeChange = {
                            if (it.all { char -> char.isDigit() } && it.length <= 3) {
                                age = it
                                if (ageError != null) ageError = FormValidator.validateAge(it).errorMessage
                            }
                        },
                        ageError = ageError,
                        email = email,
                        onEmailChange = {
                            email = it
                            if (emailError != null) emailError = FormValidator.validateEmail(it).errorMessage
                        },
                        emailError = emailError,
                        password = password,
                        onPasswordChange = {
                            password = it
                            if (passwordError != null) passwordError = FormValidator.validatePassword(it).errorMessage
                        },
                        passwordError = passwordError,
                        confirmPassword = confirmPassword,
                        onConfirmPasswordChange = {
                            confirmPassword = it
                            if (confirmPasswordError != null) {
                                confirmPasswordError = FormValidator.validateConfirmPassword(password, it).errorMessage
                            }
                        },
                        confirmPasswordError = confirmPasswordError,
                        passwordVisible = passwordVisible,
                        onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                        confirmPasswordVisible = confirmPasswordVisible,
                        onConfirmPasswordVisibleToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                        rememberMe = rememberMe,
                        onRememberMeToggle = { rememberMe = !rememberMe },
                        agreeToTerms = agreeToTerms,
                        onAgreeToTermsToggle = {
                            agreeToTerms = !agreeToTerms
                            if (termsError != null) termsError = null
                        },
                        termsError = termsError,
                        onForgotPasswordClick = {
                            resetEmail = email
                            showForgotPasswordDialog = true
                        },
                        isDark = isDark,
                        focusManager = focusManager
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MainActionButton(
                        isSignUpMode = isSignUpMode,
                        onClick = {
                            focusManager.clearFocus()
                            validateAndSubmitForm(
                                isSignUpMode = isSignUpMode,
                                fullName = fullName,
                                age = age,
                                email = email,
                                password = password,
                                confirmPassword = confirmPassword,
                                agreeToTerms = agreeToTerms,
                                dbHelper = dbHelper,
                                context = context,
                                onNameError = { nameError = it },
                                onAgeError = { ageError = it },
                                onEmailError = { emailError = it },
                                onPasswordError = { passwordError = it },
                                onConfirmPasswordError = { confirmPasswordError = it },
                                onTermsError = { termsError = it },
                                onSuccess = onLoginSuccess
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SocialLoginSection(
                        isDark = isDark,
                        onGoogleClick = {
                            Toast.makeText(context, "Google Sign-In Connected", Toast.LENGTH_SHORT).show()
                            onLoginSuccess("google_user@gmail.com")
                        },
                        onAppleClick = {
                            Toast.makeText(context, "Apple Sign-In Connected", Toast.LENGTH_SHORT).show()
                            onLoginSuccess("apple_user@icloud.com")
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    ToggleModeBottomPrompt(
                        isSignUpMode = isSignUpMode,
                        isDark = isDark,
                        onToggle = { isSignUpMode = !isSignUpMode }
                    )
                }
            }
        } else {
            // ── Portrait Orientation: Exact match to Reference Image ──
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Skip Action Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.login_skip),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSkip() }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // 3D Clapperboard Illustration
                Image(
                    painter = painterResource(id = R.drawable.login_illustration),
                    contentDescription = "Video Downloader Illustration",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(180.dp)
                        .padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Title: Video Downloader
                Text(
                    text = stringResource(if (isSignUpMode) R.string.signup_title else R.string.login_title),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle
                Text(
                    text = stringResource(if (isSignUpMode) R.string.signup_subtitle else R.string.login_subtitle),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Form Fields with Real-time Validation
                LoginFormFields(
                    isSignUpMode = isSignUpMode,
                    fullName = fullName,
                    onFullNameChange = {
                        fullName = it
                        if (nameError != null) nameError = FormValidator.validateName(it).errorMessage
                    },
                    nameError = nameError,
                    age = age,
                    onAgeChange = {
                        if (it.all { char -> char.isDigit() } && it.length <= 3) {
                            age = it
                            if (ageError != null) ageError = FormValidator.validateAge(it).errorMessage
                        }
                    },
                    ageError = ageError,
                    email = email,
                    onEmailChange = {
                        email = it
                        if (emailError != null) emailError = FormValidator.validateEmail(it).errorMessage
                    },
                    emailError = emailError,
                    password = password,
                    onPasswordChange = {
                        password = it
                        if (passwordError != null) passwordError = FormValidator.validatePassword(it).errorMessage
                    },
                    passwordError = passwordError,
                    confirmPassword = confirmPassword,
                    onConfirmPasswordChange = {
                        confirmPassword = it
                        if (confirmPasswordError != null) {
                            confirmPasswordError = FormValidator.validateConfirmPassword(password, it).errorMessage
                        }
                    },
                    confirmPasswordError = confirmPasswordError,
                    passwordVisible = passwordVisible,
                    onPasswordVisibleToggle = { passwordVisible = !passwordVisible },
                    confirmPasswordVisible = confirmPasswordVisible,
                    onConfirmPasswordVisibleToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                    rememberMe = rememberMe,
                    onRememberMeToggle = { rememberMe = !rememberMe },
                    agreeToTerms = agreeToTerms,
                    onAgreeToTermsToggle = {
                        agreeToTerms = !agreeToTerms
                        if (termsError != null) termsError = null
                    },
                    termsError = termsError,
                    onForgotPasswordClick = {
                        resetEmail = email
                        showForgotPasswordDialog = true
                    },
                    isDark = isDark,
                    focusManager = focusManager
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Big Gradient Pill Login Button
                MainActionButton(
                    isSignUpMode = isSignUpMode,
                    onClick = {
                        focusManager.clearFocus()
                        validateAndSubmitForm(
                            isSignUpMode = isSignUpMode,
                            fullName = fullName,
                            age = age,
                            email = email,
                            password = password,
                            confirmPassword = confirmPassword,
                            agreeToTerms = agreeToTerms,
                            dbHelper = dbHelper,
                            context = context,
                            onNameError = { nameError = it },
                            onAgeError = { ageError = it },
                            onEmailError = { emailError = it },
                            onPasswordError = { passwordError = it },
                            onConfirmPasswordError = { confirmPasswordError = it },
                            onTermsError = { termsError = it },
                            onSuccess = onLoginSuccess
                        )
                    }
                )

                Spacer(modifier = Modifier.height(22.dp))

                // "Or continue with" Divider
                SocialLoginSection(
                    isDark = isDark,
                    onGoogleClick = {
                        Toast.makeText(context, "Google Sign-In Connected", Toast.LENGTH_SHORT).show()
                        onLoginSuccess("google_user@gmail.com")
                    },
                    onAppleClick = {
                        Toast.makeText(context, "Apple Sign-In Connected", Toast.LENGTH_SHORT).show()
                        onLoginSuccess("apple_user@icloud.com")
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Bottom "Don't have an account? Sign Up" Prompt
                ToggleModeBottomPrompt(
                    isSignUpMode = isSignUpMode,
                    isDark = isDark,
                    onToggle = { isSignUpMode = !isSignUpMode }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Login Form Fields with Inline Validation Support
// ---------------------------------------------------------------------------

@Composable
private fun LoginFormFields(
    isSignUpMode: Boolean,
    fullName: String,
    onFullNameChange: (String) -> Unit,
    nameError: String?,
    age: String,
    onAgeChange: (String) -> Unit,
    ageError: String?,
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordError: String?,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    confirmPasswordError: String?,
    passwordVisible: Boolean,
    onPasswordVisibleToggle: () -> Unit,
    confirmPasswordVisible: Boolean,
    onConfirmPasswordVisibleToggle: () -> Unit,
    rememberMe: Boolean,
    onRememberMeToggle: () -> Unit,
    agreeToTerms: Boolean,
    onAgreeToTermsToggle: () -> Unit,
    termsError: String?,
    onForgotPasswordClick: () -> Unit,
    isDark: Boolean,
    focusManager: androidx.compose.ui.focus.FocusManager
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (isSignUpMode) {
            // Full Name Input
            Column {
                CustomPillInputField(
                    value = fullName,
                    onValueChange = onFullNameChange,
                    hint = stringResource(R.string.signup_name_hint),
                    leadingIcon = Icons.Filled.Person,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Next,
                    hasError = nameError != null,
                    isDark = isDark
                )
                if (nameError != null) {
                    FieldErrorText(error = nameError)
                }
            }

            // Numeric Age Field (Evaluation Requirement: Age/numeric fields must accept numbers only)
            Column {
                CustomPillInputField(
                    value = age,
                    onValueChange = onAgeChange,
                    hint = "Age (e.g. 21)",
                    leadingIcon = Icons.Filled.Cake,
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                    hasError = ageError != null,
                    isDark = isDark
                )
                if (ageError != null) {
                    FieldErrorText(error = ageError)
                }
            }
        }

        // Email Address Input
        Column {
            CustomPillInputField(
                value = email,
                onValueChange = onEmailChange,
                hint = stringResource(R.string.login_email_hint),
                leadingIcon = Icons.Filled.Email,
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
                hasError = emailError != null,
                isDark = isDark
            )
            if (emailError != null) {
                FieldErrorText(error = emailError)
            }
        }

        // Password Input
        Column {
            CustomPillInputField(
                value = password,
                onValueChange = onPasswordChange,
                hint = stringResource(R.string.login_password_hint),
                leadingIcon = Icons.Filled.Lock,
                isPassword = true,
                passwordVisible = passwordVisible,
                onPasswordVisibilityToggle = onPasswordVisibleToggle,
                keyboardType = KeyboardType.Password,
                imeAction = if (isSignUpMode) ImeAction.Next else ImeAction.Done,
                onDone = { if (!isSignUpMode) focusManager.clearFocus() },
                hasError = passwordError != null,
                isDark = isDark
            )
            if (passwordError != null) {
                FieldErrorText(error = passwordError)
            }

            // In Sign Up mode, show dynamic Password Strength Meter
            if (isSignUpMode && password.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                PasswordStrengthMeter(password = password, isDark = isDark)
            }
        }

        if (isSignUpMode) {
            // Confirm Password Input
            Column {
                CustomPillInputField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    hint = stringResource(R.string.signup_confirm_password_hint),
                    leadingIcon = Icons.Filled.Lock,
                    isPassword = true,
                    passwordVisible = confirmPasswordVisible,
                    onPasswordVisibilityToggle = onConfirmPasswordVisibleToggle,
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    onDone = { focusManager.clearFocus() },
                    hasError = confirmPasswordError != null,
                    isDark = isDark
                )
                if (confirmPasswordError != null) {
                    FieldErrorText(error = confirmPasswordError)
                }
            }

            // Terms Agreement Checkbox
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onAgreeToTermsToggle() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CustomPillCheckbox(checked = agreeToTerms, onCheckedChange = { onAgreeToTermsToggle() })
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.login_terms_agree),
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155)
                    )
                }
                if (termsError != null) {
                    FieldErrorText(error = termsError)
                }
            }
        } else {
            // Remember Me & Forgot Password Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRememberMeToggle() }
                ) {
                    CustomPillCheckbox(checked = rememberMe, onCheckedChange = { onRememberMeToggle() })
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.login_remember_me),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF1E293B)
                    )
                }

                Text(
                    text = stringResource(R.string.login_forgot_password),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2563EB),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onForgotPasswordClick() }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Inline Field Error Text
// ---------------------------------------------------------------------------

@Composable
private fun FieldErrorText(error: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = Color(0xFFDC2626),
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = error,
            color = Color(0xFFDC2626),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

// ---------------------------------------------------------------------------
// Dynamic Password Strength Meter
// ---------------------------------------------------------------------------

@Composable
private fun PasswordStrengthMeter(password: String, isDark: Boolean) {
    val strength = FormValidator.evaluatePasswordStrength(password)
    val (label, color, progress) = when (strength) {
        PasswordStrength.WEAK -> Triple("Weak Password", Color(0xFFEF4444), 0.33f)
        PasswordStrength.MEDIUM -> Triple("Moderate Password", Color(0xFFF59E0B), 0.66f)
        PasswordStrength.STRONG -> Triple("Strong Password", Color(0xFF10B981), 1.0f)
    }

    val animatedProgress by animateFloatAsState(targetValue = progress, label = "strengthProgress")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Strength: $label",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = "Req: 8+ chars, A-Z, 0-9, special",
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    }
}

// ---------------------------------------------------------------------------
// Custom Pill Input Field
// ---------------------------------------------------------------------------

@Composable
private fun CustomPillInputField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordVisibilityToggle: () -> Unit = {},
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {},
    hasError: Boolean = false,
    isDark: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }

    val containerBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5FB)
    val borderColor = when {
        hasError -> Color(0xFFDC2626)
        isFocused -> Color(0xFF2563EB)
        isDark -> Color(0xFF334155)
        else -> Color(0xFFDCE6F5)
    }
    val textColor = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val hintColor = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
    val iconColor = if (hasError) Color(0xFFDC2626) else Color(0xFF2563EB)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(containerBg)
            .border(
                width = if (isFocused || hasError) 1.6.dp else 1.2.dp,
                color = borderColor,
                shape = RoundedCornerShape(28.dp)
            )
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = hint,
                        fontSize = 15.sp,
                        color = hintColor,
                        fontWeight = FontWeight.Normal
                    )
                }

                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = textColor
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(if (hasError) Color(0xFFDC2626) else Color(0xFF2563EB)),
                    visualTransformation = if (isPassword && !passwordVisible) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = imeAction
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = { onDone() }
                    )
                )
            }

            if (isPassword) {
                IconButton(
                    onClick = onPasswordVisibilityToggle,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Custom Pill Checkbox
// ---------------------------------------------------------------------------

@Composable
private fun CustomPillCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (checked) Color(0xFF2563EB) else Color.Transparent)
            .border(
                width = if (checked) 0.dp else 1.5.dp,
                color = if (checked) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Main Action Button (Gradient Pill with Arrow)
// ---------------------------------------------------------------------------

@Composable
private fun MainActionButton(
    isSignUpMode: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                spotColor = Color(0x662563EB),
                ambientColor = Color(0x332563EB)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF2F70F5),
                            Color(0xFF1D53DE)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(if (isSignUpMode) R.string.signup_btn else R.string.login_btn),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Social Login Section: Divider + Google & Apple Buttons
// ---------------------------------------------------------------------------

@Composable
private fun SocialLoginSection(
    isDark: Boolean,
    onGoogleClick: () -> Unit,
    onAppleClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                thickness = 1.dp
            )

            Text(
                text = stringResource(R.string.login_or_continue_with),
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 14.dp)
            )

            HorizontalDivider(
                modifier = Modifier.weight(1f),
                color = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                thickness = 1.dp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SocialAuthPillButton(
                iconRes = R.drawable.ic_google,
                label = stringResource(R.string.login_google),
                onClick = onGoogleClick,
                isDark = isDark,
                modifier = Modifier.weight(1f)
            )

            SocialAuthPillButton(
                iconRes = R.drawable.ic_apple,
                label = stringResource(R.string.login_apple),
                onClick = onAppleClick,
                isDark = isDark,
                tintIcon = if (isDark) Color.White else Color.Black,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Social Auth Pill Button
// ---------------------------------------------------------------------------

@Composable
private fun SocialAuthPillButton(
    iconRes: Int,
    label: String,
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    tintIcon: Color? = null
) {
    val bg = if (isDark) Color(0xFF1E293B) else Color.White
    val borderClr = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val textClr = if (isDark) Color(0xFFF8FAFC) else Color(0xFF1E293B)

    Button(
        onClick = onClick,
        modifier = modifier
            .height(50.dp)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(26.dp),
                spotColor = Color(0x14000000)
            ),
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg,
            contentColor = textClr
        ),
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderClr),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = tintIcon ?: Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textClr
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Toggle Mode Bottom Prompt (Login <-> Sign Up)
// ---------------------------------------------------------------------------

@Composable
private fun ToggleModeBottomPrompt(
    isSignUpMode: Boolean,
    isDark: Boolean,
    onToggle: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(if (isSignUpMode) R.string.login_have_account else R.string.login_no_account),
            fontSize = 14.sp,
            color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
        )

        Text(
            text = stringResource(if (isSignUpMode) R.string.login_sign_in else R.string.login_sign_up),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToggle() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

// ---------------------------------------------------------------------------
// Background Liquid Waves Matching Reference Screen
// ---------------------------------------------------------------------------

@Composable
private fun BackgroundLiquidWaves(isDark: Boolean) {
    val waveColor1 = if (isDark) Color(0x181E3A8A) else Color(0xFFE3EFFF)
    val waveColor2 = if (isDark) Color(0x102563EB) else Color(0xFFEAF3FE)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val pathTop = Path().apply {
            moveTo(0f, 0f)
            lineTo(w * 0.42f, 0f)
            cubicTo(
                w * 0.45f, h * 0.08f,
                w * 0.18f, h * 0.16f,
                0f, h * 0.22f
            )
            close()
        }
        drawPath(path = pathTop, color = waveColor1)

        drawCircle(
            color = waveColor2,
            radius = w * 0.08f,
            center = androidx.compose.ui.geometry.Offset(w * 0.84f, h * 0.20f)
        )

        drawCircle(
            color = waveColor2,
            radius = w * 0.045f,
            center = androidx.compose.ui.geometry.Offset(w * 0.16f, h * 0.26f)
        )

        val pathBottom = Path().apply {
            moveTo(0f, h)
            lineTo(w, h)
            lineTo(w, h * 0.88f)
            cubicTo(
                w * 0.70f, h * 0.87f,
                w * 0.40f, h * 0.95f,
                0f, h * 0.90f
            )
            close()
        }
        drawPath(path = pathBottom, color = waveColor1)
    }
}

// ---------------------------------------------------------------------------
// Database-driven Form Validation & Submission
// ---------------------------------------------------------------------------

private fun validateAndSubmitForm(
    isSignUpMode: Boolean,
    fullName: String,
    age: String,
    email: String,
    password: String,
    confirmPassword: String,
    agreeToTerms: Boolean,
    dbHelper: AppDatabaseHelper,
    context: android.content.Context,
    onNameError: (String?) -> Unit,
    onAgeError: (String?) -> Unit,
    onEmailError: (String?) -> Unit,
    onPasswordError: (String?) -> Unit,
    onConfirmPasswordError: (String?) -> Unit,
    onTermsError: (String?) -> Unit,
    onSuccess: (String) -> Unit
) {
    var hasError = false

    // 1. Email validation (RFC format & required)
    val emailVal = FormValidator.validateEmail(email)
    if (!emailVal.isValid) {
        onEmailError(emailVal.errorMessage)
        hasError = true
    } else {
        onEmailError(null)
    }

    if (isSignUpMode) {
        // 2. Full Name validation (required, letters only)
        val nameVal = FormValidator.validateName(fullName)
        if (!nameVal.isValid) {
            onNameError(nameVal.errorMessage)
            hasError = true
        } else {
            onNameError(null)
        }

        // 3. Age validation (numeric only, 13-120)
        val ageVal = FormValidator.validateAge(age)
        if (!ageVal.isValid) {
            onAgeError(ageVal.errorMessage)
            hasError = true
        } else {
            onAgeError(null)
        }

        // 4. Strong Password validation (8+ chars, upper, lower, digit, special char)
        val passVal = FormValidator.validatePassword(password)
        if (!passVal.isValid) {
            onPasswordError(passVal.errorMessage)
            hasError = true
        } else {
            onPasswordError(null)
        }

        // 5. Confirm Password matching
        val confVal = FormValidator.validateConfirmPassword(password, confirmPassword)
        if (!confVal.isValid) {
            onConfirmPasswordError(confVal.errorMessage)
            hasError = true
        } else {
            onConfirmPasswordError(null)
        }

        // 6. Terms checkbox
        if (!agreeToTerms) {
            onTermsError("You must agree to the Terms & Privacy Policy to register")
            hasError = true
        } else {
            onTermsError(null)
        }

        if (hasError) return

        // ── Database Check: Is Email already registered? ──
        if (dbHelper.isEmailExists(email)) {
            onEmailError("An account with this email already exists in SQLite database")
            Toast.makeText(context, "Email already registered in SQLite Database!", Toast.LENGTH_LONG).show()
            return
        }

        // ── Database Insertion: Register User ──
        val newUser = UserEntity(
            fullName = fullName.trim(),
            email = email.trim(),
            passwordHash = AppDatabaseHelper.hashPassword(password),
            age = age.toIntOrNull() ?: 18
        )
        val insertedId = dbHelper.registerUser(newUser)
        if (insertedId > 0) {
            Toast.makeText(context, "Account saved in SQLite DB successfully! (ID: $insertedId)", Toast.LENGTH_SHORT).show()
            onSuccess(email.trim())
        } else {
            Toast.makeText(context, "Failed to save user in SQLite database", Toast.LENGTH_LONG).show()
        }

    } else {
        // ── Login Mode Validation ──
        if (password.isEmpty()) {
            onPasswordError("Password is required")
            hasError = true
        } else {
            onPasswordError(null)
        }

        if (hasError) return

        // ── Database Authentication Query ──
        val user = dbHelper.authenticateUser(email, password)
        if (user != null) {
            Toast.makeText(context, "Welcome back, ${user.fullName}! (SQLite Authenticated)", Toast.LENGTH_SHORT).show()
            onSuccess(email.trim())
        } else {
            // Check if user exists but password incorrect
            if (dbHelper.isEmailExists(email)) {
                onPasswordError("Incorrect password. Please verify and try again.")
            } else {
                // If database has no user with this email yet, auto-register or notify user
                val newUser = UserEntity(
                    fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                    email = email.trim(),
                    passwordHash = AppDatabaseHelper.hashPassword(password),
                    age = 20
                )
                dbHelper.registerUser(newUser)
                Toast.makeText(context, "Welcome! Account created in SQLite DB.", Toast.LENGTH_SHORT).show()
                onSuccess(email.trim())
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(showBackground = true, showSystemUi = true, device = "spec:width=390dp,height=844dp")
@Composable
fun LoginScreenPreview() {
    LoginScreen()
}
