const fs=require('fs');
const project=`com.example.civic.data.models.Project(
    id = "TN-PWD-2025-01", title = "Test Project", description = "QR test fixture",
    department = "PWD", district = "Chennai", municipalBody = "Chennai", location = "Test location",
    latitude = 13.04, longitude = 80.23, contractorName = "Test Contractor", contractorId = "CON-TEST",
    responsibleOffice = "Test Office", currentOfficerInCharge = "Test Officer", estimatedBudgetInLakhs = 10.0,
    fundingSource = "Test Fund", startDate = "01 Jan 2025", originalCompletionDate = "01 Dec 2025",
    revisedCompletionDate = "01 Dec 2025", status = com.example.civic.data.models.ProjectStatus.ONGOING,
    progressPercentage = 40)`;
let p='app/src/test/java/com/example/civic/ProjectQRTest.kt',s=fs.readFileSync(p,'utf8');
s=s.replace('CivicDataRepository.projects.first()',project)
 .replace('CivicDataRepository.users.first { it.role == UserRole.OFFICER }','com.example.civic.data.models.User("test-officer", "Test Officer", "officer@example.com", UserRole.OFFICER)')
 .replace('val projects = CivicDataRepository.projects.toList()','val projects = listOf(project)');
fs.writeFileSync(p,s);
p='app/src/androidTest/java/com/example/civic/ProjectQRIntegrationTest.kt';s=fs.readFileSync(p,'utf8');
s=s.replaceAll('CivicDataRepository.projects.first()',project);fs.writeFileSync(p,s);
