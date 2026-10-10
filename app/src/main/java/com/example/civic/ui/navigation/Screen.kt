package com.example.civic.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object LoginTypeSelect : Screen("login_type_select")

    object CitizenLogin : Screen("login_citizen")
    object OfficerLogin : Screen("login_officer")
    object CoordinatorLogin : Screen("login_coordinator")
    object ContractorLogin : Screen("login_contractor")
    object AdminLogin : Screen("login_admin")

    // Workspaces Core
    object OfficialDashboard : Screen("official_dashboard")
    object ActionInbox : Screen("action_inbox")

    // Citizen / Public Surface
    object CitizenDashboard : Screen("citizen_dashboard")
    object PublicProjects : Screen("public_projects")
    object ProjectDetails : Screen("project_details/{projectId}") {
        fun createRoute(projectId: String) = "project_details/$projectId"
    }
    object ProjectMap : Screen("project_map")
    object ComplaintSubmission : Screen("complaint_submission?projectId={projectId}") {
        fun createRoute(projectId: String? = null) = if (projectId != null) "complaint_submission?projectId=$projectId" else "complaint_submission"
    }
    object MyComplaints : Screen("my_complaints")
    object ComplaintDetails : Screen("complaint_details/{complaintId}") {
        fun createRoute(complaintId: String) = "complaint_details/$complaintId"
    }
    object CitizenProfile : Screen("citizen_profile")
    object ProjectQRScanner : Screen("project_qr_scanner")
    object ProjectQRGenerator : Screen("project_qr_generator")

    // Officer Workspace
    object OfficerDashboard : Screen("officer_dashboard")
    object OfficerMyProjects : Screen("officer_my_projects")
    object RegisterProject : Screen("register_project")
    object ProgressVerification : Screen("progress_verification")

    // Coordinator Workspace
    object CoordinatorDashboard : Screen("coordinator_dashboard")
    object DepartmentProjects : Screen("department_projects")
    object SharedSchedule : Screen("shared_schedule")
    object ConflictDetection : Screen("conflict_detection")
    object ResolutionHistory : Screen("resolution_history")
    object CoordinatorReports : Screen("coordinator_reports")

    // Contractor Workspace
    object ContractorDashboard : Screen("contractor_dashboard")
    object ContractorAssignedProjects : Screen("contractor_assigned_projects")
    object SubmitProgress : Screen("submit_progress")
    object DeadlineRevision : Screen("deadline_revision")

    // Admin Workspace
    object AdminDashboard : Screen("admin_dashboard")
    object UserManagement : Screen("user_management")
    object DepartmentManagement : Screen("department_management")
    object Jurisdictions : Screen("jurisdictions")
    object AuditLog : Screen("audit_log")
    object AdminSettings : Screen("admin_settings")

    // Handover Continuity Module
    object HandoverDashboard : Screen("handover_dashboard")
    object InitiateHandover : Screen("initiate_handover")
    object IncomingOfficer : Screen("incoming_officer")
}
