package com.example.civic.ui.navigation

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.civic.data.CivicDataRepository

/** Shared dialog overlays for error reporting and password recovery. */
@Composable
internal fun BackendStatus() {
    val repo = CivicDataRepository
    val tamil = repo.currentLanguage == com.example.civic.data.AppLanguage.TAMIL
    var password by remember { mutableStateOf("") }

    repo.error?.let { message ->
        AlertDialog(
            onDismissRequest = { repo.clearError() },
            title = { Text(if (tamil) "கோரிக்கை நிலை" else "Request status") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { repo.clearError() }) {
                    Text(if (tamil) "சரி" else "OK")
                }
            }
        )
    }

    if (repo.recoverPassword) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(if (tamil) "புதிய கடவுச்சொல்" else "Set your password") },
            text = {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(if (tamil) "கடவுச்சொல்" else "New password") },
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            },
            confirmButton = {
                Button(onClick = { repo.launchOperation { repo.setPassword(password); password = "" } }) {
                    Text(if (tamil) "சேமி" else "Save password")
                }
            }
        )
    }
}
