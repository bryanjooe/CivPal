package com.example.civic.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.civic.data.CivicDataRepository
import com.example.civic.data.models.User
import com.example.civic.data.models.UserRole
import com.example.civic.ui.components.CivicBottomNavigation
import com.example.civic.ui.components.CivicDrawerContent
import com.example.civic.ui.screens.admin.*
import com.example.civic.ui.screens.auth.*
import com.example.civic.ui.screens.citizen.*
import com.example.civic.ui.screens.contractor.*
import com.example.civic.ui.screens.coordinator.*
import com.example.civic.ui.screens.handover.*
import com.example.civic.ui.screens.official.*
import com.example.civic.ui.screens.officer.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle

@Composable
fun CivicNavHost(
    navController: NavHostController = rememberNavController()
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentUser = CivicDataRepository.currentUser
    val currentRole = currentUser?.role ?: UserRole.CITIZEN
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(30_000)
                if (CivicDataRepository.ready && !CivicDataRepository.loading && !CivicDataRepository.working)
                    CivicDataRepository.launchOperation { CivicDataRepository.refresh() }
            }
        }
    }
    LaunchedEffect(currentUser?.id, currentRoute) {
        if (currentUser != null && !CivicDataRepository.isGuest && currentRoute in listOf(
                Screen.Welcome.route, Screen.LoginTypeSelect.route,
                Screen.CitizenLogin.route, Screen.OfficerLogin.route,
                Screen.CoordinatorLogin.route, Screen.ContractorLogin.route, Screen.AdminLogin.route)) {
            navController.navigate(if (currentRole == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    LaunchedEffect(currentRoute) {
        if (drawerState.isOpen) {
            runCatching { drawerState.close() }
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.PublicProjects.route,
        Screen.MyComplaints.route,
        Screen.ActionInbox.route,
        Screen.ProjectMap.route,
        Screen.CitizenProfile.route,
        Screen.CitizenDashboard.route,
        Screen.OfficerDashboard.route,
        Screen.CoordinatorDashboard.route,
        Screen.ContractorDashboard.route,
        Screen.AdminDashboard.route,
        Screen.OfficialDashboard.route
    )

    val showDrawer = currentRoute !in listOf(
        Screen.Splash.route,
        Screen.Welcome.route,
        Screen.LoginTypeSelect.route,
        Screen.CitizenLogin.route,
        Screen.OfficerLogin.route,
        Screen.CoordinatorLogin.route,
        Screen.ContractorLogin.route,
        Screen.AdminLogin.route
    )

    fun navigateTo(route: String) {
        coroutineScope.launch { drawerState.close() }
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun handleLogout() {
        coroutineScope.launch {
            runCatching { drawerState.close() }
        }
        CivicDataRepository.launchOperation {
            try {
                CivicDataRepository.logout()
            } finally {
                navController.navigate(Screen.Welcome.route) {
                    popUpTo(0) { inclusive = true }
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = showDrawer && currentRoute != Screen.ProjectMap.route,
        drawerContent = {
            if (showDrawer) {
                CivicDrawerContent(
                    currentRole = currentRole,
                    currentRoute = currentRoute,
                    onNavigate = { screen -> navigateTo(screen.route) },
                    onLogout = { handleLogout() }
                )
            }
        }
    ) {
        Scaffold(
            topBar = { BackendStatus() },
            contentWindowInsets = WindowInsets(0.dp),
            bottomBar = {
                if (showBottomBar) {
                    CivicBottomNavigation(
                        currentRoute = currentRoute,
                        onNavigate = { screen -> navigateTo(screen.route) }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Splash.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                // Auth Destinations
                composable(Screen.Splash.route) {
                    SplashScreen(onNavigateToWelcome = {
                        val user = CivicDataRepository.currentUser
                        val isGuest = CivicDataRepository.isGuest
                        val target = if (user == null || isGuest) Screen.Welcome.route
                            else if (user.role == UserRole.CITIZEN) Screen.CitizenDashboard.route
                            else Screen.OfficialDashboard.route
                        navController.navigate(target) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    })
                }

                composable(Screen.Welcome.route) {
                    WelcomeScreen(
                        onSelectRole = { role ->
                            when (role) {
                                UserRole.CITIZEN -> {}
                                UserRole.OFFICER -> navController.navigate(Screen.OfficerLogin.route)
                                UserRole.COORDINATOR -> navController.navigate(Screen.CoordinatorLogin.route)
                                UserRole.CONTRACTOR -> navController.navigate(Screen.ContractorLogin.route)
                                UserRole.ADMINISTRATOR -> navController.navigate(Screen.AdminLogin.route)
                            }
                        },
                        onScanQrCode = {
                            navController.navigate(Screen.ProjectQRScanner.route)
                        },
                        onCitizenLoginSuccess = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN)
                                Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.LoginTypeSelect.route) {
                    LoginTypeSelectScreen(
                        initialTab = 1,
                        onNavigateBack = { navController.popBackStack() },
                        onSelectRole = { role ->
                            when (role) {
                                UserRole.CITIZEN -> navController.navigate(Screen.CitizenLogin.route)
                                UserRole.OFFICER -> navController.navigate(Screen.OfficerLogin.route)
                                UserRole.COORDINATOR -> navController.navigate(Screen.CoordinatorLogin.route)
                                UserRole.CONTRACTOR -> navController.navigate(Screen.ContractorLogin.route)
                                UserRole.ADMINISTRATOR -> navController.navigate(Screen.AdminLogin.route)
                            }
                        },
                        onCitizenLoginSuccess = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN)
                                Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.CitizenLogin.route) {
                    LoginTypeSelectScreen(
                        initialTab = 0,
                        onNavigateBack = { navController.popBackStack() },
                        onSelectRole = { role ->
                            when (role) {
                                UserRole.CITIZEN -> {}
                                UserRole.OFFICER -> navController.navigate(Screen.OfficerLogin.route)
                                UserRole.COORDINATOR -> navController.navigate(Screen.CoordinatorLogin.route)
                                UserRole.CONTRACTOR -> navController.navigate(Screen.ContractorLogin.route)
                                UserRole.ADMINISTRATOR -> navController.navigate(Screen.AdminLogin.route)
                            }
                        },
                        onCitizenLoginSuccess = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN)
                                Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.OfficerLogin.route) {
                    OfficerLoginScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLoginSuccess = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.CoordinatorLogin.route) {
                    CoordinatorLoginScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLoginSuccess = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.ContractorLogin.route) {
                    ContractorLoginScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLoginSuccess = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(Screen.AdminLogin.route) {
                    AdminLoginScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onLoginSuccess = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.Welcome.route) { inclusive = true }
                            }
                        }
                    )
                }

                // Official Workspace Destinations
                composable(Screen.OfficialDashboard.route) {
                    OfficialDashboardScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.ActionInbox.route) {
                    ActionInboxScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                // Citizen / Public Surface Destinations
                composable(Screen.ProjectQRGenerator.route) {
                    ProjectQRGeneratorScreen(onNavigateBack = { navController.popBackStack() })
                }

                composable(Screen.ProjectQRScanner.route) {
                    ProjectQRScannerScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onProjectScanned = { projectId ->
                            navController.navigate(Screen.ProjectDetails.createRoute(android.net.Uri.encode(projectId))) {
                                popUpTo(Screen.ProjectQRScanner.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable(Screen.CitizenDashboard.route) {
                    CitizenDashboardScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.PublicProjects.route) {
                    PublicProjectsScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(
                    route = Screen.ProjectDetails.route,
                    arguments = listOf(navArgument("projectId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val projectId = backStackEntry.arguments?.getString("projectId") ?: ""
                    ProjectDetailsScreen(
                        projectId = projectId,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToComplaint = { projId ->
                            navigateTo(Screen.ComplaintSubmission.createRoute(projId))
                        }
                    )
                }

                composable(Screen.ProjectMap.route) {
                    ProjectMapScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(
                    route = Screen.ComplaintSubmission.route,
                    arguments = listOf(navArgument("projectId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val prefilledProjectId = backStackEntry.arguments?.getString("projectId")
                    ComplaintSubmissionScreen(
                        prefilledProjectId = prefilledProjectId,
                        onNavigateBack = { navController.popBackStack() },
                        onComplaintSubmitted = {
                            navController.navigate(Screen.MyComplaints.route) {
                                popUpTo(Screen.CitizenDashboard.route)
                            }
                        }
                    )
                }

                composable(Screen.MyComplaints.route) {
                    MyComplaintsScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(
                    route = Screen.ComplaintDetails.route,
                    arguments = listOf(navArgument("complaintId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
                    ComplaintDetailsScreen(
                        complaintId = complaintId,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.CitizenProfile.route) {
                    CitizenProfileScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onLogout = { handleLogout() },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                // Officer Destinations
                composable(Screen.OfficerDashboard.route) {
                    OfficerDashboardScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.OfficerMyProjects.route) {
                    OfficerMyProjectsScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.RegisterProject.route) {
                    RegisterProjectScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onProjectRegistered = {
                            navController.navigate(Screen.OfficerMyProjects.route) {
                                popUpTo(Screen.OfficialDashboard.route)
                            }
                        }
                    )
                }

                composable(Screen.ProgressVerification.route) {
                    ProgressVerificationScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                // Coordinator Destinations
                composable(Screen.CoordinatorDashboard.route) {
                    CoordinatorDashboardScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.DepartmentProjects.route) {
                    DepartmentProjectsScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.SharedSchedule.route) {
                    SharedScheduleScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.ConflictDetection.route) {
                    ConflictDetectionScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.ResolutionHistory.route) {
                    ResolutionHistoryScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.CoordinatorReports.route) {
                    CoordinatorReportsScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                // Contractor Destinations
                composable(Screen.ContractorDashboard.route) {
                    ContractorDashboardScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.ContractorAssignedProjects.route) {
                    ContractorAssignedProjectsScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.SubmitProgress.route) {
                    SubmitProgressScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onProgressSubmitted = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.OfficialDashboard.route)
                            }
                        }
                    )
                }

                composable(Screen.DeadlineRevision.route) {
                    DeadlineRevisionScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onRequestSubmitted = {
                            navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {
                                popUpTo(Screen.OfficialDashboard.route)
                            }
                        }
                    )
                }

                // Admin Destinations
                composable(Screen.AdminDashboard.route) {
                    AdminDashboardScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.UserManagement.route) {
                    UserManagementScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.DepartmentManagement.route) {
                    DepartmentManagementScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.Jurisdictions.route) {
                    JurisdictionsScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.AuditLog.route) {
                    AuditLogScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.AdminSettings.route) {
                    AdminSettingsScreen(
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } },
                        onLogout = { handleLogout() }
                    )
                }

                // Handover Module Destinations
                composable(Screen.HandoverDashboard.route) {
                    HandoverDashboardScreen(
                        onNavigate = { route -> navigateTo(route) },
                        onOpenDrawer = { coroutineScope.launch { drawerState.open() } }
                    )
                }

                composable(Screen.InitiateHandover.route) {
                    InitiateHandoverScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onHandoverSubmitted = {
                            navController.navigate(Screen.HandoverDashboard.route) {
                                popUpTo(Screen.HandoverDashboard.route)
                            }
                        }
                    )
                }

                composable(Screen.IncomingOfficer.route) {
                    IncomingOfficerScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToProjects = {
                            navController.navigate(
                                if (currentRole == UserRole.OFFICER) Screen.OfficerMyProjects.route else Screen.PublicProjects.route
                            )
                        }
                    )
                }
            }
        }
    }
}
