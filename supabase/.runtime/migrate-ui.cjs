const fs = require('fs');
const path = require('path');
const root = 'app/src/main/java/com/example/civic/ui';
const read = p => fs.readFileSync(p, 'utf8');
const write = (p,s) => fs.writeFileSync(p,s,'utf8');
const file = p => path.join(root,p);
// Strings in the affected UI callbacks contain balanced interpolation braces.
function closing(s,open) { let d=1; for(let i=open+1;i<s.length;i++){if(s[i]==='{')d++;if(s[i]==='}'&&--d===0)return i;} throw Error('Unbalanced callback'); }
let p=file('screens/auth/RoleLoginScreens.kt'), s=read(p);
const roles=[['Citizen','mobileOrEmail','test@gmail.com'],['Officer','username','officer@test.com'],['Coordinator','username','coordinator@test.com'],['Contractor','contractorId','contractor@test.com'],['Admin','username','admin@test.com']];
for (let i=roles.length-1;i>=0;i--) {
  const [role,field,email]=roles[i];
  const begin=s.indexOf(`fun ${role}LoginScreen(`);
  const next=s.indexOf('\n@',begin);
  const stop=next<0?s.length:next;
  let section=s.slice(begin,stop);
  let matches=[...section.matchAll(/onClick = \{/g)].filter(m=> {
    const e=closing(section,m.index+m[0].length-1);
    return section.slice(m.index,e).includes('CivicDataRepository.currentUser =');
  });
  for(let j=matches.length-1;j>=0;j--){
    const start=matches[j].index, open=start+matches[j][0].length-1, end=closing(section,open);
    const body=j===0 ? `\n                    CivicDataRepository.launchOperation {\n                        CivicDataRepository.login(${field}, password)\n                        password = ""\n                        onLoginSuccess()\n                    }\n                ` : `\n                    ${field} = "${email}"\n                `;
    section=section.slice(0,open+1)+body+section.slice(end);
  }
  section=section.replaceAll('Demo Login as ', 'Use Test Email for ').replaceAll('Demo Login)', 'Test Email)').replace('Mobile Number or Email','Email Address')
    .replaceAll('Contractor ID / Email','Email Address').replaceAll('Contractor ID','Email Address').replaceAll('Admin Username','Email Address').replaceAll('Officer Username / Email','Email Address');
  s=s.slice(0,begin)+section+s.slice(stop);
}
s=s.replace('"Password reset instructions have been sent to your registered mobile number."','"Enter your email and use the reset link sent by Supabase."');
s=s.replace('onClick = { showDialogText = "Enter your email and use the reset link sent by Supabase." }',
  'onClick = { CivicDataRepository.launchOperation { CivicDataRepository.resetPassword(mobileOrEmail); showDialogText = "If this email is registered, reset instructions have been sent." } }');
s=s.replace('onClick = { showDialogText = "Citizen registration account form opened. Demo accounts can log in directly." }',
  'onClick = { CivicDataRepository.launchOperation { CivicDataRepository.signUp(mobileOrEmail, password); password = ""; showDialogText = "Check your email to confirm your citizen account, then sign in." } }');
s=s.replace('"$roleName Demo Account - Tap \'Demo Login\' below for instant access."', '"$roleName: use your account email and password. Your role is verified by the server."');
s=s.replace('"$roleName மாதிரி கணக்கு - கீழே உள்ள \'மாதிரி உள்நுழைவு\' பொத்தானைப் பயன்படுத்தவும்."','"$roleName: மின்னஞ்சல் மற்றும் கடவுச்சொல்லுடன் உள்நுழைக. பங்கு சேவையகத்தில் சரிபார்க்கப்படும்."');
write(p,s);
const mutations='submitComplaint|respondToComplaint|requestCitizenInformation|toggleProjectFollow|registerProject|recordDelay|submitProgressReport|verifyProgressReport|initiateHandover|acknowledgeHandover|resolveConflict|addUser';
function walk(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(dir,e.name)):[path.join(dir,e.name)]);}
for(p of walk(file('screens')).filter(p=>p.endsWith('.kt')&&!p.endsWith('RoleLoginScreens.kt'))){
  s=read(p); const points=new Set();
  for(const m of s.matchAll(new RegExp(`CivicDataRepository\\.(${mutations})\\(`,'g'))){
    const start=s.lastIndexOf('onClick = {',m.index);
    if(start<0)throw Error('No callback '+p);
    const open=start+'onClick = {'.length-1,end=closing(s,open);
    if(end<m.index) continue; // Follow callbacks are wired separately below.
    if(s.slice(open,end).includes('CivicDataRepository.launchOperation'))continue;
    points.add(open);
  }
  for(const open of [...points].sort((a,b)=>b-a)){
    const end=closing(s,open); const body=s.slice(open+1,end);
    s=s.slice(0,open+1)+'\n                    CivicDataRepository.launchOperation {'+body+'\n                    }\n                '+s.slice(end);
  }
  s=s.replace(/onToggleFollow = \{ CivicDataRepository.toggleProjectFollow\(([^)]+)\) \}/g,
    'onToggleFollow = { CivicDataRepository.launchOperation { CivicDataRepository.toggleProjectFollow($1) } }');
  write(p,s);
}
// No unrelated layout or navigation route changes.
console.log('Migrated login callbacks and persistent write callbacks.');
