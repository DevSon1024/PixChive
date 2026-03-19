package com.devson.pixchive.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Policy") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                Text(
                    text = "Our Commitment to Privacy",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "At PixChive, we believe your data is yours alone. We have designed our app from the ground up to respect your privacy and provide a clean, secure experience.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
            }

            item {
                PolicySection(
                    icon = Icons.Default.Lock,
                    title = "Completely Offline",
                    description = "PixChive is a 100% offline application. We do not use any cloud servers, and your images never leave your device. All processing and storage happen locally on your phone."
                )
            }

            item {
                PolicySection(
                    icon = Icons.Default.Info,
                    title = "No Data Sharing",
                    description = "We do not collect, store, or share any of your personal data, usage metrics, or images. Your private collection remains entirely private to you."
                )
            }

            item {
                PolicySection(
                    icon = Icons.Default.Info,
                    title = "User Data Privacy",
                    description = "The app requires storage permissions solely to read and display your local image and comic files. We do not access your contacts, location, or any other unrelated sensitive information."
                )
            }

            item {
                PolicySection(
                    icon = Icons.Default.CheckCircle,
                    title = "Clean User Experience",
                    description = "We are committed to providing a clean, ad-free, and distraction-free experience. You will never see ads or promotional content tracking your behavior within PixChive."
                )
            }
        }
    }
}

@Composable
private fun PolicySection(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(28.dp)
                .padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
