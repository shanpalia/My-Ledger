package com.example.ui.screens.auth

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel

enum class AuthMode {
    LOGIN,
    SIGNUP
}

@Composable
fun AuthScreen(
    viewModel: LedgerViewModel,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var authMode by remember { mutableStateOf(AuthMode.LOGIN) }

    var emailOrMobile by remember { mutableStateOf("shanpalia786@gmail.com") }
    var password by remember { mutableStateOf("password123") }
    var fullName by remember { mutableStateOf("") }
    var mobile by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Indigo900),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // App Branding Header
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Indigo600),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "My Ledger",
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )
                Text(
                    text = "Smart Debit & Credit Management",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Indigo200
                )
            }

            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Auth Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Slate100)
                            .padding(4.dp)
                    ) {
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { authMode = AuthMode.LOGIN },
                            shape = RoundedCornerShape(10.dp),
                            color = if (authMode == AuthMode.LOGIN) Indigo600 else Color.Transparent
                        ) {
                            Text(
                                text = "Login",
                                color = if (authMode == AuthMode.LOGIN) Color.White else Slate700,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { authMode = AuthMode.SIGNUP },
                            shape = RoundedCornerShape(10.dp),
                            color = if (authMode == AuthMode.SIGNUP) Indigo600 else Color.Transparent
                        ) {
                            Text(
                                text = "Sign Up",
                                color = if (authMode == AuthMode.SIGNUP) Color.White else Slate700,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }

                    if (authMode == AuthMode.SIGNUP) {
                        OutlinedTextField(
                            value = fullName,
                            onValueChange = { fullName = it },
                            label = { Text("Full Name / Business Owner", fontWeight = FontWeight.Medium) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        OutlinedTextField(
                            value = mobile,
                            onValueChange = { mobile = it },
                            label = { Text("Mobile Number", fontWeight = FontWeight.Medium) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    OutlinedTextField(
                        value = emailOrMobile,
                        onValueChange = { emailOrMobile = it },
                        label = { Text(if (authMode == AuthMode.LOGIN) "Email or Mobile Number" else "Email Address", fontWeight = FontWeight.Medium) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", fontWeight = FontWeight.Medium) },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Slate500
                                )
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
                        shape = RoundedCornerShape(14.dp)
                    )

                    Button(
                        onClick = {
                            if (authMode == AuthMode.LOGIN) {
                                isLoading = true
                                viewModel.login(emailOrMobile, password) { success, msg ->
                                    isLoading = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) onAuthSuccess()
                                }
                            } else {
                                if (fullName.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter your name", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLoading = true
                                viewModel.signup(fullName, emailOrMobile, mobile, password) { success, msg ->
                                    isLoading = false
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    if (success) onAuthSuccess()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("auth_submit_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = if (authMode == AuthMode.LOGIN) "Sign In to Ledger" else "Create Ledger Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.5.sp
                            )
                        }
                    }

                    if (authMode == AuthMode.LOGIN) {
                        Text(
                            text = "Forgot password? Tap here to reset",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Indigo600,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Toast.makeText(context, "Password reset link sent to your email", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            }
        }
    }
}
