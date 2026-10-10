package com.example.civic.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.ComplaintStatus
import com.example.civic.data.models.ProjectStatus
import com.example.civic.ui.theme.*

@Composable
fun StatusBadge(status: ProjectStatus) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val (bgColor, textColor) = when (status) {
        ProjectStatus.ONGOING -> CivicPrimaryBlue.copy(alpha = 0.12f) to CivicPrimaryBlue
        ProjectStatus.COMPLETED -> CivicSuccessContainer to CivicSuccessGreen
        ProjectStatus.DELAYED -> CivicWarningContainer to CivicWarningAmber
        ProjectStatus.PLANNED -> CivicSecondaryBg to CivicTextSecondary
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = if (isTa) status.labelTa else status.labelEn,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun ComplaintStatusBadge(status: ComplaintStatus) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    val (bgColor, textColor) = when (status) {
        ComplaintStatus.PENDING -> CivicWarningContainer to CivicWarningAmber
        ComplaintStatus.UNDER_REVIEW -> CivicPrimaryBlue.copy(alpha = 0.12f) to CivicPrimaryBlue
        ComplaintStatus.ACTION_PROPOSED -> Color(0xFFF3E8FF) to Color(0xFF7E22CE)
        ComplaintStatus.RESOLVED -> CivicSuccessContainer to CivicSuccessGreen
        ComplaintStatus.REJECTED -> CivicErrorContainerRes to CivicErrorRedRes
        ComplaintStatus.ESCALATED_OVERDUE -> CivicErrorContainerRes to CivicErrorRedRes
        ComplaintStatus.AWAITING_CITIZEN_INFO -> CivicWarningContainer to CivicWarningAmber
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            if (status == ComplaintStatus.ESCALATED_OVERDUE) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = if (isTa) status.labelTa else status.labelEn,
                color = textColor,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun CivicSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder, color = CivicTextSecondaryGray, fontSize = 13.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CivicPrimaryBlue) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = CivicTextSecondaryGray)
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = CivicSurfaceWhite,
            focusedContainerColor = CivicSurfaceWhite,
            unfocusedBorderColor = CivicSubtleBorder,
            focusedBorderColor = CivicPrimaryBlue
        )
    )
}

@Composable
fun FilterChipRow(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(options) { option ->
            val isSelected = selectedOption == option
            FilterChip(
                selected = isSelected,
                onClick = { onOptionSelected(option) },
                label = { Text(option, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = CivicPrimaryBlue,
                    selectedLabelColor = Color.White,
                    containerColor = CivicSurfaceWhite,
                    labelColor = CivicTextPrimaryDark
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = CivicSubtleBorder,
                    selectedBorderColor = CivicPrimaryBlue
                )
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, CivicSubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = CivicTextSecondaryGray,
                    fontWeight = FontWeight.Medium
                )
                Surface(
                    color = containerColor,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = CivicTextPrimaryDark
            )
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmText: String = "Confirm",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CivicPrimaryBlue)
            ) {
                Text(confirmText, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, CivicSubtleBorder)
            ) {
                Text(dismissText)
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = CivicSurfaceWhite
    )
}

@Composable
fun EmptyStateView(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, CivicSubtleBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                color = CivicPrimaryBlue.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = CivicPrimaryBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = CivicTextPrimaryDark
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = CivicTextSecondaryGray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ExpandableDescription(
    description: String,
    label: String = "Description",
    initialExpanded: Boolean = false,
    modifier: Modifier = Modifier
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = if (isTa) "விவரமான அறிக்கை" else label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = CivicPrimaryBlue
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isExpanded) (if (isTa) "மறை" else "Hide") else (if (isTa) "விவரம் காண்க" else "Show description"),
                    fontSize = 12.sp,
                    color = CivicPrimaryBlue,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = CivicPrimaryBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = CivicTextSecondaryGray,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}
