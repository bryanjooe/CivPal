package com.example.civic.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.Project
import com.example.civic.ui.theme.*

@Composable
fun ProjectCard(
    project: Project,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onToggleFollow: (() -> Unit)? = null
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    val cardModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable { onClick() }
    } else {
        modifier.fillMaxWidth()
    }

    Card(
        modifier = cardModifier,
        colors = CardDefaults.cardColors(containerColor = CivicSurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CivicSubtleBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Status Badge, Department Pill & Pin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(status = project.status)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = CivicSecondaryBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = project.department,
                            style = MaterialTheme.typography.labelSmall,
                            color = CivicDarkBlue,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (onToggleFollow != null) {
                        IconButton(
                            onClick = onToggleFollow,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (project.isFollowed) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Project",
                                tint = if (project.isFollowed) CivicPrimaryBlue else CivicTextSecondaryGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Title & Chevron Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CivicTextPrimaryDark,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = CivicPrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress Bar & Budget
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isTa) "முன்னேற்றம்: ${project.progressPercentage}%" else "Progress: ${project.progressPercentage}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = CivicTextPrimaryDark
                )
                Text(
                    text = "Budget: ₹${project.estimatedBudgetInLakhs.toInt()} L",
                    style = MaterialTheme.typography.labelSmall,
                    color = CivicPrimaryBlue,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { project.progressPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (project.progressPercentage >= 100) CivicSuccessGreen else CivicPrimaryBlue,
                trackColor = CivicSecondaryBg,
                drawStopIndicator = {}
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = project.district,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CivicTextSecondaryGray
                )

                Text(
                    text = project.municipalBody,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = CivicDarkBlue
                )
            }
        }
    }
}
