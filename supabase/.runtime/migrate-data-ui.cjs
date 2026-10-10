const fs=require('fs'), path=require('path');
const base='app/src/main/java/com/example/civic/ui';
function edit(p,fn){p=path.join(base,p);const s=fs.readFileSync(p,'utf8');const next=fn(s);if(s!==next)fs.writeFileSync(p,next,'utf8');}
edit('navigation/CivicNavHost.kt',s=>s.replaceAll('navController.navigate(Screen.OfficialDashboard.route) {',
  'navController.navigate(if (CivicDataRepository.currentUser?.role == UserRole.CITIZEN) Screen.CitizenDashboard.route else Screen.OfficialDashboard.route) {'));
for(const f of ['ContractorDashboardScreen','ContractorAssignedProjectsScreen','SubmitProgressScreen','DeadlineRevisionScreen'])
  edit(`screens/contractor/${f}.kt`,s=>s.replaceAll('CivicDataRepository.projects','CivicDataRepository.assignedProjects'));
for(const f of ['OfficerMyProjectsScreen','ProgressVerificationScreen'])
  edit(`screens/officer/${f}.kt`,s=>s.replaceAll('CivicDataRepository.projects','CivicDataRepository.managedProjects'));
for(const f of ['DepartmentProjectsScreen','SharedScheduleScreen','CoordinatorReportsScreen'])
  edit(`screens/coordinator/${f}.kt`,s=>s.replaceAll('CivicDataRepository.projects','CivicDataRepository.managedProjects'));
edit('screens/official/OfficialDashboardScreen.kt',s=>s.replace('val projects = CivicDataRepository.projects','val projects = CivicDataRepository.managedProjects'));
edit('screens/officer/OfficerDashboardScreen.kt',s=>s.replace(/val assignedProjects = projects.filter \{[\s\S]*?\n    \}/,'val assignedProjects = CivicDataRepository.assignedProjects'));
edit('screens/official/ActionInboxScreen.kt',s=>s.replace(/remember\(CivicDataRepository.projects.size, CivicDataRepository.complaints.size, CivicDataRepository.conflicts.size, CivicDataRepository.handovers.size\)/,
  'remember(CivicDataRepository.projects.toList(), CivicDataRepository.complaints.toList(), CivicDataRepository.conflicts.toList(), CivicDataRepository.handovers.toList())'));
edit('screens/admin/DepartmentManagementScreen.kt',s=>s.replace(/val depts = listOf\([\s\S]*?\n    \)/,'val depts = CivicDataRepository.departments.map { it.name to it.details }'));
edit('screens/admin/JurisdictionsScreen.kt',s=>s.replace(/val jurisdictions = listOf\([\s\S]*?\n    \)/,'val jurisdictions = CivicDataRepository.jurisdictions.map { it.name to it.details }'));
edit('screens/coordinator/CoordinatorReportsScreen.kt',s=>s.replace(/            ReportStatCard\(title = "Public Works[\s\S]*?ReportStatCard\(title = "Municipal Administration"[^\n]+/,
  `            CivicDataRepository.managedProjects.groupBy { it.department }.forEach { (department, projects) ->
                ReportStatCard(title = department, total = projects.size,
                    ongoing = projects.count { it.status.name == "ONGOING" }, delayed = projects.count { it.status.name == "DELAYED" })
            }`));
edit('screens/citizen/CitizenProfileScreen.kt',s=>s.replace('text = "R. Anbarasan"','text = CivicDataRepository.currentUser?.name ?: "Guest Citizen"')
 .replace('text = "citizen.anbarasan@gmail.com"','text = CivicDataRepository.currentUser?.email.orEmpty()')
 .replace('text = "District: Chennai | Ward 118"','text = CivicDataRepository.currentUser?.officeJurisdiction.orEmpty()'));
// Replace fake file selection with the existing attachment button opening a real system picker.
for(const [p,variable,type,bucket] of [
 ['citizen/ComplaintSubmissionScreen','attachedPhotoName','image/*','complaint-photos'],
 ['contractor/SubmitProgressScreen','attachedPhotoName','image/*','progress-evidence'],
 ['contractor/DeadlineRevisionScreen','attachedDocName','application/pdf','deadline-documents']]){
 edit(`screens/${p}.kt`,s=>{
  s=s.replace('import androidx.compose.runtime.*',`import androidx.compose.runtime.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext`);
  const marker=`var ${variable} by remember { mutableStateOf<String?>(null) }`;
  s=s.replace(marker,marker+`
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> ${variable} = uri?.toString() }
    var savedRecordId by remember { mutableStateOf<String?>(null) }`);
  s=s.replace(new RegExp(`onClick = \\{ ${variable} = "[^"]+" \\}`,'g'),`onClick = { picker.launch("${type}") }`);
  s=s.replace(`${variable} ?:`,`${variable}?.let { "Selected attachment" } ?:`);
  s=s.replace('// Photo Attachment Mock','// Authorized file attachment').replace('// Photo upload mock','// Authorized file attachment').replace('// Document Upload Mock','// Authorized file attachment');
  if(p.includes('ComplaintSubmission')){
    s=s.replace('val created = CivicDataRepository.submitComplaint(', 'val createdId = savedRecordId ?: CivicDataRepository.submitComplaint(');
    s=s.replace('citizenName = "S. Karthik"','citizenName = CivicDataRepository.currentUser?.name.orEmpty()');
    s=s.replace(')\n                        submittedComplaintId = created.id',`).id
                        savedRecordId = createdId
                        CivicDataRepository.uploadAttachment(context, attachedPhotoName, "${bucket}", createdId)
                        submittedComplaintId = createdId`);
  } else if(p.includes('SubmitProgress')) {
    s=s.replace('CivicDataRepository.submitProgressReport(', 'val createdId = savedRecordId ?: CivicDataRepository.submitProgressReport(');
    s=s.replace('milestoneId = "M1"','milestoneId = checkNotNull(selectedProject?.milestones?.find { it.title == selectedMilestone }) { "Select a valid milestone." }.id');
    s=s.replace(')\n                        showConfirmDialog = true',`)
                        savedRecordId = createdId
                        CivicDataRepository.uploadAttachment(context, attachedPhotoName, "${bucket}", createdId)
                        showConfirmDialog = true`);
    s=s.replace('government officer Er. R. Kanimozhi','the authorized government officer');
  } else {
    s=s.replace('showSuccessDialog = true',`CivicDataRepository.launchOperation {
                            val createdId = savedRecordId ?: CivicDataRepository.requestDeadline(
                                checkNotNull(selectedProject) { "Select a project." }.id, proposedDateInput, reasonInput)
                            savedRecordId = createdId
                            CivicDataRepository.uploadAttachment(context, attachedDocName, "${bucket}", createdId)
                            showSuccessDialog = true
                        }`);
    s=s.replace('sent to Er. R. Kanimozhi','sent to the authorized officer');
  }
  return s;
 });
}
console.log('Connected catalog data, scope filters and attachment pickers.');
