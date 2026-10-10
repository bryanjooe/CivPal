package com.example.civic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.civic.data.AppLanguage
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.UserRole
import com.example.civic.ui.navigation.Screen

data class DrawerMenuItem(
    val titleEn: String,
    val titleTa: String,
    val screen: Screen,
    val icon: ImageVector
)

@Composable
fun CivicDrawerContent(
    currentRole: UserRole,
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    val isTa = CivicDataRepository.currentLanguage == AppLanguage.TAMIL

    val items = when (currentRole) {
        UserRole.OFFICER -> listOf(
            DrawerMenuItem("Official Workspace", "அதிகாரப்பூர்வ தளம்", Screen.OfficialDashboard, Icons.Default.Dashboard),
            DrawerMenuItem("Smart Action Inbox", "நடவடிக்கை பெட்டி", Screen.ActionInbox, Icons.Default.Inbox),
            DrawerMenuItem("My Projects", "எனது திட்டங்கள்", Screen.OfficerMyProjects, Icons.Default.Folder),
            DrawerMenuItem("Register Project", "புதிய திட்டம் பதிவு", Screen.RegisterProject, Icons.Default.AddCircle),
            DrawerMenuItem("Verify Progress", "முன்னேற்ற சரிபார்ப்பு", Screen.ProgressVerification, Icons.Default.Verified),
            DrawerMenuItem("Officer Handover", "அலுவலர் ஒப்படைப்பு", Screen.HandoverDashboard, Icons.Default.SwapHoriz),
            DrawerMenuItem("Public Surface View", "பொதுமக்கள் பக்கம்", Screen.CitizenDashboard, Icons.Default.Public)
        )
        UserRole.COORDINATOR -> listOf(
            DrawerMenuItem("Official Workspace", "அதிகாரப்பூர்வ தளம்", Screen.OfficialDashboard, Icons.Default.Dashboard),
            DrawerMenuItem("Smart Action Inbox", "நடவடிக்கை பெட்டி", Screen.ActionInbox, Icons.Default.Inbox),
            DrawerMenuItem("Department Projects", "துறை திட்டங்கள்", Screen.DepartmentProjects, Icons.Default.AccountTree),
            DrawerMenuItem("Conflict Detection", "முரண்பாடுகள் கண்டறிதல்", Screen.ConflictDetection, Icons.Default.Warning),
            DrawerMenuItem("Shared Schedule", "பகிரப்பட்ட காலஅட்டவணை", Screen.SharedSchedule, Icons.Default.DateRange),
            DrawerMenuItem("Resolution History", "தீர்வு வரலாறு", Screen.ResolutionHistory, Icons.Default.History),
            DrawerMenuItem("Reports", "அறிக்கைகள்", Screen.CoordinatorReports, Icons.Default.Assessment),
            DrawerMenuItem("Public Surface View", "பொதுமக்கள் பக்கம்", Screen.CitizenDashboard, Icons.Default.Public)
        )
        UserRole.CONTRACTOR -> listOf(
            DrawerMenuItem("Official Workspace", "அதிகாரப்பூர்வ தளம்", Screen.OfficialDashboard, Icons.Default.Dashboard),
            DrawerMenuItem("Smart Action Inbox", "நடவடிக்கை பெட்டி", Screen.ActionInbox, Icons.Default.Inbox),
            DrawerMenuItem("Assigned Contracts", "ஒப்படைக்கப்பட்ட பணிகள்", Screen.ContractorAssignedProjects, Icons.Default.Assignment),
            DrawerMenuItem("Submit Progress", "முன்னேற்றம் சமர்ப்பி", Screen.SubmitProgress, Icons.Default.CloudUpload),
            DrawerMenuItem("Deadline Revision", "காலக்கெடு திருத்தம்", Screen.DeadlineRevision, Icons.Default.Update),
            DrawerMenuItem("Public Surface View", "பொதுமக்கள் பக்கம்", Screen.CitizenDashboard, Icons.Default.Public)
        )
        UserRole.ADMINISTRATOR -> listOf(
            DrawerMenuItem("Official Workspace", "அதிகாரப்பூர்வ தளம்", Screen.OfficialDashboard, Icons.Default.Dashboard),
            DrawerMenuItem("Smart Action Inbox", "நடவடிக்கை பெட்டி", Screen.ActionInbox, Icons.Default.Inbox),
            DrawerMenuItem("User Management", "பயனர் நிர்வாகம்", Screen.UserManagement, Icons.Default.People),
            DrawerMenuItem("Department Management", "துறை நிர்வாகம்", Screen.DepartmentManagement, Icons.Default.Business),
            DrawerMenuItem("Jurisdictions", "அதிகார வரம்புகள்", Screen.Jurisdictions, Icons.Default.LocationCity),
            DrawerMenuItem("Officer Handover Continuity", "அலுவலர் ஒப்படைப்பு", Screen.HandoverDashboard, Icons.Default.SwapHoriz),
            DrawerMenuItem("Audit Log", "தணிக்கைப் பதிவு", Screen.AuditLog, Icons.Default.ReceiptLong),
            DrawerMenuItem("Settings", "அமைப்புகள்", Screen.AdminSettings, Icons.Default.Settings),
            DrawerMenuItem("Public Surface View", "பொதுமக்கள் பக்கம்", Screen.CitizenDashboard, Icons.Default.Public)
        )
        UserRole.CITIZEN -> listOf(
            DrawerMenuItem("Public Dashboard", "முகப்பு", Screen.CitizenDashboard, Icons.Default.Home),
            DrawerMenuItem("Public Projects", "பொது திட்டங்கள்", Screen.PublicProjects, Icons.Default.List),
            DrawerMenuItem("Project Map", "வரைபடம்", Screen.ProjectMap, Icons.Default.Map),
            DrawerMenuItem("Submit Complaint", "புகார் அளிக்கவும்", Screen.ComplaintSubmission, Icons.Default.ReportProblem),
            DrawerMenuItem("My Complaints", "எனது புகார்கள்", Screen.MyComplaints, Icons.Default.AssignmentLate),
            DrawerMenuItem("Saved Projects", "சேமிக்கப்பட்டவை", Screen.CitizenProfile, Icons.Default.Bookmark),
            DrawerMenuItem("Profile", "சுயவிவரம்", Screen.CitizenProfile, Icons.Default.Person),
            DrawerMenuItem("Official Sign In", "அதிகாரப்பூர்வ உள்நுழைவு", Screen.LoginTypeSelect, Icons.Default.Badge)
        )
    }

    ModalDrawerSheet {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .padding(20.dp)
        ) {
            Column {
                Text(
                    text = "CivicTech TN",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isTa) currentRole.labelTa else currentRole.labelEn,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
                val user = CivicDataRepository.currentUser
                if (user != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = user.name,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (user.officeJurisdiction != null) {
                        Text(
                            text = user.officeJurisdiction,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            items.forEach { item ->
                val selected = currentRoute == item.screen.route
                NavigationDrawerItem(
                    label = { Text(if (isTa) item.titleTa else item.titleEn) },
                    selected = selected,
                    onClick = { onNavigate(item.screen) },
                    icon = { Icon(item.icon, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
                )
            }
        }

        HorizontalDivider()

        NavigationDrawerItem(
            label = { Text(if (isTa) "வெளியேறு" else "Logout") },
            selected = false,
            onClick = onLogout,
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout") },
            modifier = Modifier.padding(12.dp)
        )
    }
}
