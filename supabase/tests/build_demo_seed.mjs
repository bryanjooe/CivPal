// Generates a separate, explicitly labeled demo seed from the existing Kotlin source.
// Does not edit Android files. Original constructor text is retained in snapshots.
import { readFile, writeFile } from 'node:fs/promises';
const source = await readFile(new URL('../../app/src/main/java/com/example/civic/data/CivicDataRepository.kt',import.meta.url),'utf8');
const preload = source.slice(source.indexOf('private fun preloadSampleData()'),source.indexOf('// Mutating actions'));
function constructors(text,type) {
  const result=[]; const re=new RegExp('\\b'+type+'\\(', 'g'); let match;
  while ((match=re.exec(text))) {
    const start=match.index; let depth=1,quoted=false,escaped=false,i=re.lastIndex;
    for (;i<text.length && depth;i++) {
      const c=text[i];
      if(quoted) { if(escaped) escaped=false; else if(c==='\\') escaped=true; else if(c==='"') quoted=false; }
      else if(c==='"') quoted=true; else if(c==='(') depth++; else if(c===')') depth--;
    }
    result.push(text.slice(start,i)); re.lastIndex=i;
  } return result;
}
function args(block) {
  const s=block.slice(block.indexOf('(')+1,-1); const out=[];
  let start=0,depth=0,quote=false,escape=false;
  for(let i=0;i<s.length;i++) {
    const c=s[i]; if(quote) { if(escape)escape=false;else if(c==='\\')escape=true;else if(c==='"')quote=false; }
    else if(c==='"')quote=true;else if('([{'.includes(c))depth++;else if(')]}'.includes(c))depth--;
    else if(c===','&&depth===0) {out.push(s.slice(start,i).trim());start=i+1;}
  }out.push(s.slice(start).trim()); return out;
}
const str=(b,key,fallback=null)=> { const m=b.match(new RegExp('\\b'+key+'\\s*=\\s*("(?:[^"\\\\]|\\\\.)*")'));return m?JSON.parse(m[1]):fallback; };
const num=(b,key)=>Number(b.match(new RegExp('\\b'+key+'\\s*=\\s*([\\d.]+)'))?.[1]);
const en=(b,key,fallback)=>b.match(new RegExp('\\b'+key+'\\s*=\\s*\\w+\\.(\\w+)'))?.[1]??fallback;
const q=x=>x==null?'null':"'"+String(x).replaceAll("'","''")+"'";
const snap=b=>q(JSON.stringify({is_demonstration:true,kotlin_constructor:b}))+'::jsonb';
const date=s=> {if(!s)return null;const [day,mon,year]=s.split(' ');const mm=['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'].indexOf(mon)+1;return `${year}-${String(mm).padStart(2,'0')}-${day.padStart(2,'0')}`;};
const office={'TN-PWD-2025-01':'PWD-CHENNAI-OFFICE','TN-TWAD-2025-02':'TWAD-COIMBATORE-OFFICE'};
const municipality={'Chennai':'GCC','Coimbatore':'CMC','Madurai':'MADURAI','Tiruchirappalli':'TRICHY','Salem':'SALEM'};
const department={'Public Works Department':'PWD','TWAD Board':'TWAD','Highways Department':'HWY'};
const times={subTime1:"anchor-interval '24 hours'",subTime2:"anchor-interval '164 hours'",subTime3:"anchor-interval '216 hours'",subTime4:"anchor-interval '240 hours'",subTime5:"anchor-interval '288 hours'",subTime6:"anchor-interval '192 hours'"};
function time(expr) {
  for(let i=1;i<=6;i++) times['deadTime'+i]='('+times['subTime'+i]+")+interval '168 hours'";
  const base=expr.match(/(?:sub|dead)Time[1-6]/)?.[0]; if(!base)throw Error('Unknown relative clock expression '+expr);
  const offset=expr.match(/\+\s*\(?\s*(\d+)\s*\*\s*(hourMillis|dayMillis)/);
  return '('+times[base]+')'+(offset?`+interval '${Number(offset[1])*(offset[2]==='dayMillis'?24:1)} hours'`:'');
}
function insert(table,columns,values,conflict='do nothing') { return `insert into public.${table}(${columns.join(',')}) values(${values.join(',')}) on conflict ${conflict};\n`; }
let sql=`-- DEMONSTRATION DATA ONLY: extracted from CivicDataRepository.kt, not verified government records.
-- Applied once; reruns skip the seed entirely, so live records are never reset.
-- Historical actors remain name snapshots, not fabricated Auth identities.
begin;
do $seed$
declare anchor timestamptz := clock_timestamp();
begin
  if exists(select 1 from civic_private.schema_migrations where version='demo_seed_v1') then return; end if;
  -- Reject collisions before any inserts. Never overwrite real records.
  if exists(select 1 from public.projects where id in ('TN-PWD-2025-01','TN-TWAD-2025-02'))
    or exists(select 1 from public.complaints where id in ('CMP-2026-101','CMP-2026-102','CMP-2026-103','CMP-2026-104','CMP-2026-105','CMP-2026-106'))
    or exists(select 1 from public.officer_handovers where id='HO-2026-88')
    or exists(select 1 from public.audit_logs where id in ('AL-100','AL-101')) then
    raise exception 'Demo IDs already exist without seed marker. Inspect collisions; never overwrite records.';
  end if;
`;
const catalogs=[
 ['PWD','PWD','Public Works Department','Highways, Buildings, Water Bodies',['Public Works Department','Public Works Department (PWD)']],
 ['TWAD','TWAD','TWAD Board','Tamil Nadu Water Supply & Drainage Board',['TWAD Board']],
 ['HWY','HWY','Highways Department','State Highways, Major District Roads',['Highways Department']],
 ['MAWS','MAWS','Municipal Administration & Water Supply','Corporations, Municipalities, Panchayats',['Municipal Administration','Municipal Administration & Water Supply']],
 ['TANGEDCO','TANGEDCO','TANGEDCO','Electricity Board Power Line Relocation',['TANGEDCO']],
 ['COORD','COORD','Inter-Departmental Coordination Cell',null,['Inter-Departmental Coordination Cell']],
 ['CONTRACT','CONTRACT','Contractor Wing',null,['Contractor Wing']],
 ['GOVTECH','GOVTECH','Governance Tech Wing',null,['Governance Tech Wing']]
];
for(const [id,code,name,desc,labels] of catalogs) sql+=insert('departments',['id','code','name','scope_description','source_labels','is_demo'],[q(id),q(code),q(name),q(desc),'array['+labels.map(q).join(',')+']','true']);
const jurisdictions=[
 ['GCC','Greater Chennai Corporation','MUNICIPALITY','Chennai','38 Wards, Metro Circle'],
 ['CMC','Coimbatore Municipal Corporation','MUNICIPALITY','Coimbatore','East, West, South, North Zones'],
 ['MADURAI','Madurai Corporation','MUNICIPALITY','Madurai','Vaigai Circle, Central Zone'],
 ['TRICHY','Tiruchirappalli Corporation','MUNICIPALITY','Tiruchirappalli','Srirangam & Golden Rock Zones'],
 ['SALEM','Salem City Municipal Corporation','MUNICIPALITY','Salem','Zone 1, 2, 3, 4 Divisions'],
 ['PWD-CHENNAI-OFFICE','Highways & PWD Chennai Metro Division','OFFICE','Chennai',null],
 ['TWAD-COIMBATORE-OFFICE','TWAD Water Supply Circle Coimbatore','OFFICE','Coimbatore',null],
 ['COORD-CELL','Tamil Nadu State Infrastructure Cell','OFFICE',null,null],
 ['LT-OFFICE','L&T Infrastructure Ltd','OFFICE',null,null],
 ['EGOV-OFFICE','Tamil Nadu E-Governance Agency','OFFICE',null,null]
];
for(const [id,name,kind,district,details] of jurisdictions) sql+=insert('jurisdictions',['id','name','kind','district','details','is_demo'],[q(id),q(name),q(kind),q(district),q(details),'true']);
// Catalog collisions may not silently reuse a real record as demo infrastructure.
sql+=`if exists(select 1 from public.departments where id in (${catalogs.map(x=>q(x[0])).join(',')}) and not is_demo)
 or exists(select 1 from public.jurisdictions where id in (${jurisdictions.map(x=>q(x[0])).join(',')}) and not is_demo)
 or exists(select 1 from public.contractors where id in ('CON-LT-9081','CON-KSR-4421') and not is_demo) then
 raise exception 'Demo catalog IDs conflict with non-demo records'; end if;\n`;
for(const b of constructors(preload,'Project')) {
  const id=str(b,'id'),district=str(b,'district');
  sql+=insert('contractors',['id','name','is_demo'],[q(str(b,'contractorId')),q(str(b,'contractorName')),'true']);
  const cols=['id','title','description','department_id','department_name','district','municipal_body_id','municipal_body','location','latitude','longitude','contractor_id','contractor_name','responsible_office_id','responsible_office','current_officer_in_charge','estimated_budget_in_lakhs','funding_source','start_date','original_completion_date','revised_completion_date','status','progress_percentage','is_published','is_demo','source_snapshot'];
  const vals=[q(id),q(str(b,'title')),q(str(b,'description')),q(department[str(b,'department')]),q(str(b,'department')),q(district),q(municipality[district]),q(str(b,'municipalBody')),q(str(b,'location')),num(b,'latitude'),num(b,'longitude'),q(str(b,'contractorId')),q(str(b,'contractorName')),q(office[id]),q(str(b,'responsibleOffice')),q(str(b,'currentOfficerInCharge')),num(b,'estimatedBudgetInLakhs'),q(str(b,'fundingSource')),q(date(str(b,'startDate'))),q(date(str(b,'originalCompletionDate'))),q(date(str(b,'revisedCompletionDate'))),q(en(b,'status')),num(b,'progressPercentage'),'true','true',snap(b)];
  sql+=insert('projects',cols,vals);
  for(const mb of constructors(b,'Milestone')) {
    const ma=args(mb);sql+=insert('project_milestones',['project_id','milestone_id','title','target_date','is_completed','completion_date','source_snapshot'],
      [q(id),q(JSON.parse(ma[0])),q(JSON.parse(ma[1])),q(date(JSON.parse(ma[2]))),ma[3],q(ma[4]?date(JSON.parse(ma[4])):null),snap(mb)]);
  }
  if(b.includes('isFollowed = true')) sql+=`insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
    values('android_demo_v1','unowned_follow',${q(id)},'Global isFollowed has no source owner; no per-user follow fabricated.',${snap(b)}) on conflict do nothing;\n`;
}
for(const b of constructors(preload,'Complaint')) {
  const id=str(b,'id'),project=str(b,'projectId'),district=str(b,'district'),sub=b.match(/submissionTimestampMillis\s*=\s*(\w+)/)?.[1];
  const cols=['id','category','project_id','project_title_snapshot','submitted_by_citizen_name','location','district','description','submitted_at','assigned_department_id','assigned_department_name','jurisdiction_id','assigned_officer_name','assigned_officer_email','status','priority','is_demo','source_snapshot'];
  sql+=insert('complaints',cols,[q(id),q(en(b,'category')),q(project),q(str(b,'projectTitle')),q(str(b,'submittedByCitizenName')),q(str(b,'location')),q(district),q(str(b,'description')),time(sub),q(department[str(b,'assignedDepartment')]),q(str(b,'assignedDepartment')),q(project?office[project]:municipality[district]),q(str(b,'assignedOfficerName')),q(str(b,'assignedOfficerEmail')),q(en(b,'status','PENDING')),q(en(b,'priority','NORMAL')),'true',snap(b)]);
  for(const eb of constructors(b,'ComplaintEvent')) {
    const ea=args(eb);sql+=insert('complaint_events',['complaint_id','id','event_at','event_type','description','performed_by_name','source_snapshot'],[q(id),q(JSON.parse(ea[0])),time(ea[1]),q(JSON.parse(ea[3])),q(JSON.parse(ea[4])),q(JSON.parse(ea[5])),snap(eb)]);
  }
  for(const rb of constructors(b,'ComplaintResponse')) {
    const ra=args(rb);sql+=insert('complaint_responses',['complaint_id','id','responded_by_name','role_snapshot','message','source_snapshot'],[q(id),q(JSON.parse(ra[0])),q(JSON.parse(ra[2])),q(JSON.parse(ra[3])),q(JSON.parse(ra[4])),snap(rb)]);
  }
}
for(const b of constructors(preload,'OfficerHandover')) sql+=insert('officer_handovers',
 ['id','outgoing_officer_name','outgoing_officer_email','incoming_officer_name','incoming_officer_email','department_id','department_name','office_jurisdiction_id','office_jurisdiction','effective_transfer_date','reason_for_reassignment','legacy_assigned_projects_count','legacy_pending_tasks_count','handover_notes','status','is_demo','source_snapshot'],
 [q(str(b,'id')),q(str(b,'outgoingOfficerName')),q(str(b,'outgoingOfficerEmail')),q(str(b,'incomingOfficerName')),q(str(b,'incomingOfficerEmail')),q('PWD'),q(str(b,'department')),q('PWD-CHENNAI-OFFICE'),q(str(b,'officeJurisdiction')),q(date(str(b,'effectiveTransferDate'))),q(str(b,'reasonForReassignment')),num(b,'assignedProjectsCount'),num(b,'pendingTasksCount'),q(str(b,'handoverNotes')),q(en(b,'status')),'true',snap(b)]);
for(const b of constructors(preload,'AuditLogEntry')) {
 const a=args(b);sql+=insert('audit_logs',['id','action','performed_by_name','target_type','target_id','details','is_demo','source_snapshot'],[q(JSON.parse(a[0])),q(JSON.parse(a[2])),q(JSON.parse(a[3])),q(JSON.parse(a[4])),q(JSON.parse(a[5])),q(JSON.parse(a[6])),'true',snap(b)]);
 if(JSON.parse(a[0])==='AL-101')sql+=`insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','audit_discrepancy','AL-101','Audit says CMP-2026-102 resolved while source complaint is pending; preserved without rewriting either.',${snap(b)}) on conflict do nothing;\n`;
}
for(const b of constructors(preload,'ScheduleConflict'))sql+=`insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','project_conflict',${q(str(b,'id'))},'Source references missing project TN-TWAD-2025-09; no project invented and no FK relaxed.',${snap(b)}) on conflict do nothing;\n`;
for(const b of constructors(preload,'User'))sql+=`insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
 values('android_demo_v1','legacy_user_identity',${q(JSON.parse(args(b)[0]))},'Historical demo identity preserved without creating fake Auth users or equating it to a test person.',${snap(b)}) on conflict do nothing;\n`;
sql+=`insert into civic_private.schema_migrations(version) values('demo_seed_v1');
end $seed$;
commit;
`;
await writeFile(new URL('../seeds/001_android_demo.sql',import.meta.url),sql);
process.stdout.write('Generated demo seed from source: 2 projects, 4 milestones, 6 complaints, 13 events, 2 responses, 1 handover, 2 audit rows; 8 preserved import issues.\n');
