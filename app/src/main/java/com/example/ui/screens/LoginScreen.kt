package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.BannerView
import com.example.ui.components.SgsTextField
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.Gold
import com.example.ui.theme.GoldLight
import com.example.ui.theme.Navy
import com.example.ui.theme.Navy2
import com.example.ui.theme.NavySurface
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.ScreenBg
import com.example.ui.theme.TextCream
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.BannerMessage
import com.example.ui.viewmodel.LoginUiState

@Composable
fun LoginScreen(
    state: LoginUiState,
    isDarkTheme: Boolean = true,
    biometricSupported: Boolean = false,
    onToggleTheme: () -> Unit = {},
    onBiometricLogin: () -> Unit = {},
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onRememberOfflineChange: (Boolean) -> Unit,
    onPreConnect: () -> Unit,
    onLogin: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var showPass by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ScreenBg, NavySurface, ScreenBg)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(20.dp),
            color = Navy,
            border = BorderStroke(1.5.dp, Gold.copy(alpha = 0.45f)),
            shadowElevation = 10.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hero School Banner Header
                Box(modifier = Modifier.fillMaxWidth().height(165.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_school_banner),
                        contentDescription = stringResource(R.string.school_name_mr),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(DeepNavy.copy(alpha = 0.45f), DeepNavy.copy(alpha = 0.92f))
                                )
                            )
                    )
                    Row(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                    ) {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("login_theme_button")
                        ) {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "थीम बदला",
                                tint = GoldLight
                            )
                        }
                        IconButton(
                            onClick = onOpenSettings,
                            modifier = Modifier.testTag("login_settings_button")
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "URL सेटिंग", tint = GoldLight)
                        }
                    }
                    Column(
                        modifier = Modifier.align(Alignment.BottomCenter).padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_school_emblem),
                            contentDescription = stringResource(R.string.school_name_mr),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(CircleShape)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stringResource(R.string.school_name_mr),
                            color = GoldLight,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "School Management System • V19.65",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onPreConnect,
                        enabled = !state.isConnecting,
                        border = BorderStroke(1.dp, RoyalBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("pre_connect_button")
                    ) {
                        if (state.isConnecting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = GoldLight)
                        } else {
                            Icon(Icons.Default.CloudDone, contentDescription = null, tint = GoldLight, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(
                            stringResource(R.string.connect_school),
                            color = TextCream,
                            fontSize = 12.5.sp,
                            maxLines = 1
                        )
                    }

                    BannerView(state.preConnectStatus)

                    SgsTextField(
                        value = state.username,
                        onValueChange = onUsernameChange,
                        label = stringResource(R.string.username_label),
                        placeholder = "user_1 / user_2 / teacher_1",
                        tag = "login_username_input"
                    )

                    SgsTextField(
                        value = state.password,
                        onValueChange = onPasswordChange,
                        label = stringResource(R.string.password_label),
                        placeholder = "Password",
                        tag = "login_password_input",
                        keyboardType = KeyboardType.Password,
                        visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPass = !showPass }) {
                                Icon(
                                    if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Password पहा",
                                    tint = Navy2
                                )
                            }
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = state.rememberOffline,
                            onCheckedChange = onRememberOfflineChange,
                            colors = CheckboxDefaults.colors(checkedColor = Gold, checkmarkColor = DeepNavy),
                            modifier = Modifier.testTag("remember_offline_checkbox")
                        )
                        Column(modifier = Modifier.padding(top = 8.dp)) {
                            Text(
                                stringResource(R.string.remember_offline_label),
                                color = TextCream,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                stringResource(R.string.remember_offline_hint),
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }

                    if (!state.loginError.isNullOrBlank()) {
                        BannerView(BannerMessage.Err(state.loginError))
                    }

                    Button(
                        onClick = onLogin,
                        enabled = !state.isLoggingIn,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold, contentColor = DeepNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("login_submit_button")
                    ) {
                        if (state.isLoggingIn) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp, color = DeepNavy)
                            Spacer(Modifier.size(8.dp))
                            Text("तपासत आहे...", fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(R.string.login_btn), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    if (biometricSupported && state.biometricQuickAvailable) {
                        OutlinedButton(
                            onClick = onBiometricLogin,
                            border = BorderStroke(1.2.dp, Gold),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("login_biometric_button")
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = GoldLight, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(8.dp))
                            Text(
                                "फिंगरप्रिंटने Login (${state.biometricQuickLabel})",
                                color = GoldLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Text(
                        "Software Developed by Tonape sir (SGS Highschool, Pachora)",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
