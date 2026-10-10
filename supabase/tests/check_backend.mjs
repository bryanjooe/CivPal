// Isolated PostgreSQL permission/workflow suite. Auth, Storage and Cron services
// themselves are not emulated: this tests database SQL only.
import { PGlite } from './.runtime/node_modules/@electric-sql/pglite/dist/index.js';
import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const db = new PGlite();
const ids={admin:'10000000-0000-0000-0000-000000000001',officer:'10000000-0000-0000-0000-000000000002',
  coordinator:'10000000-0000-0000-0000-000000000003',contractor:'10000000-0000-0000-0000-000000000004',
  citizen:'10000000-0000-0000-0000-000000000005',other:'10000000-0000-0000-0000-000000000006',guest:'10000000-0000-0000-0000-000000000007'};
const emails={admin:'admin@test.com',officer:'officer@test.com',coordinator:'coordinator@test.com',contractor:'contractor@test.com',citizen:'test@gmail.com',other:'other@example.invalid',guest:null};
let passed=0;
async function check(name,fn){await fn();passed++;process.stdout.write(`PASS ${name}\n`);}
async function file(path){return readFile(new URL('../'+path,import.meta.url),'utf8');}
async function as(user,fn){
  await db.exec(`set role ${user==='anon'?'anon':'authenticated'};`);
  await db.query(`select set_config('request.jwt.claim.sub',$1,false)`,[ids[user]??'']);
  try{return await fn();}finally{await db.exec('reset role');await db.exec(`select set_config('request.jwt.claim.sub','',false)`);}
}
async function deny(sql,code='42501'){await assert.rejects(db.exec(sql),e=>e.code===code);}
let complaint,report,request,conflict;
try{
  await db.exec(`create role anon;create role authenticated;create role service_role bypassrls;
    create schema auth;
    create table auth.users(id uuid primary key,email text,raw_user_meta_data jsonb default '{}',email_confirmed_at timestamptz,is_anonymous boolean default false);
    create function auth.uid() returns uuid language sql stable as $$select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid$$;
    grant usage on schema auth to anon,authenticated;grant execute on function auth.uid() to anon,authenticated;
    create schema storage;
    create table storage.buckets(id text primary key,name text,public boolean default false,file_size_limit bigint,allowed_mime_types text[]);
    create table storage.objects(id uuid primary key default gen_random_uuid(),bucket_id text references storage.buckets(id),name text,owner_id text,unique(bucket_id,name));
    alter table storage.objects enable row level security;
    grant usage on schema storage to anon,authenticated;
    grant select,insert,update,delete on storage.objects to anon,authenticated;
    create policy legacy_broad_policy on storage.objects for all to anon,authenticated using(true) with check(true);`);
  await db.exec(await file('migrations/001_civic_schema.sql'));
  for(const [key,id] of Object.entries(ids))await db.query(`insert into auth.users(id,email,email_confirmed_at,is_anonymous)
    values($1,$2,case when $3 then null else now() end,$3)`,[id,emails[key],key==='guest']);
  for(const name of ['admin/001_bootstrap_admin.sql','migrations/002_civic_access.sql','migrations/003_civic_workflows.sql',
    'migrations/004_civic_storage.sql','seeds/001_android_demo.sql','admin/002_assign_test_roles.sql','migrations/005_civic_scheduler.sql']){
    await check('executes '+name,async()=>db.exec(await file(name)));
  }
}catch(e){process.stderr.write('SETUP FAILED: '+e.message+'\n');throw e;}
try {
  await check('demo rows and unresolved source references preserved',async()=>{
    for(const [table,total] of [['projects',2],['project_milestones',4],['complaints',6],['complaint_events',13],['complaint_responses',2],['officer_handovers',1],['project_conflicts',0]])
      assert.equal((await db.query(`select count(*)::int as n from public.${table}`)).rows[0].n,total,table);
    assert.equal((await db.query(`select count(*)::int as n from civic_private.import_rejections`)).rows[0].n,8);
  });
  await check('public views hide raw source snapshots and Auth identities',()=>as('anon',async()=>{
    assert.equal((await db.query('select * from public.civic_projects')).rows.length,2);
    await deny('select source_snapshot from public.projects');
    await deny('select * from public.profiles');
    await deny(`update public.projects set progress_percentage=100`);
    await deny(`select public.civic_set_follow('TN-PWD-2025-01',true)`);
  }));
  await check('citizens see only own profile and cannot self-promote',()=>as('citizen',async()=>{
    assert.equal((await db.query('select * from public.civic_profiles')).rows.length,1);
    await deny(`update public.profiles set role='ADMINISTRATOR'`);
    await deny(`select public.civic_admin_set_user('${ids.citizen}','ADMINISTRATOR',true)`);
    assert.equal((await db.query('select * from public.civic_audit_logs')).rows.length,0);
  }));
  await check('follows are per-user and idempotent',async()=>{
    await as('citizen',()=>db.exec(`select public.civic_set_follow('TN-PWD-2025-01',true);select public.civic_set_follow('TN-PWD-2025-01',true)`));
    assert.equal((await as('citizen',()=>db.query('select * from public.civic_project_follows'))).rows.length,1);
    assert.equal((await as('other',()=>db.query('select * from public.civic_project_follows'))).rows.length,0);
  });
  await check('complaint binds caller, clock and project routing in one transaction',()=>as('citizen',async()=>{
    complaint=(await db.query(`select public.civic_submit_complaint('POTHOLE','Test location','Test complaint','TN-PWD-2025-01') as id`)).rows[0].id;
    const row=(await db.query(`select citizen_id,assigned_department_id,extract(epoch from deadline_at-submitted_at)::int as seconds from public.civic_complaints where id=$1`,[complaint])).rows[0];
    assert.equal(row.citizen_id,ids.citizen);assert.equal(row.assigned_department_id,'PWD');assert.equal(row.seconds,604800);
    assert.equal((await db.query(`select id from public.civic_complaint_events where complaint_id=$1`,[complaint])).rows.length,1);
  }));
  await check('other citizens cannot read or alter this complaint',()=>as('other',async()=>{
    assert.equal((await db.query(`select id from public.civic_complaints where id=$1`,[complaint])).rows.length,0);
    assert.equal((await db.query(`select id from public.civic_complaint_events where complaint_id=$1`,[complaint])).rows.length,0);
    await deny(`select public.civic_reply_complaint('${complaint}','Forged reply')`);
  }));
  await check('anonymous Auth guest can submit an individually owned complaint',()=>as('guest',async()=>{
    const id=(await db.query(`select public.civic_submit_complaint('OTHER','Guest location','Guest complaint',null,'GCC') as id`)).rows[0].id;
    assert.equal((await db.query(`select citizen_id from public.civic_complaints where id=$1`,[id])).rows[0].citizen_id,ids.guest);
  }));
  await check('officer can respond within scope but cannot touch TWAD',()=>as('officer',async()=>{
    await db.exec(`select public.civic_respond_complaint('${complaint}','AWAITING_CITIZEN_INFO','Please provide details')`);
    await deny(`select public.civic_respond_complaint('CMP-2026-101','RESOLVED','Outside scope')`);
    await deny(`select public.civic_record_delay('TN-TWAD-2025-02','2027-01-01','Reason','Explanation')`);
  }));
  await check('citizen reply preserves original deadline',()=>as('citizen',async()=>{
    const before=(await db.query(`select deadline_at from public.civic_complaints where id=$1`,[complaint])).rows[0].deadline_at;
    await db.exec(`select public.civic_reply_complaint('${complaint}','Requested details')`);
    const after=(await db.query(`select deadline_at,status from public.civic_complaints where id=$1`,[complaint])).rows[0];
    assert.equal(after.deadline_at.toISOString(),before.toISOString());assert.equal(after.status,'UNDER_REVIEW');
  }));
  await check('contractor may submit only assigned projects',()=>as('contractor',async()=>{
    await deny(`select public.civic_submit_progress('TN-TWAD-2025-02','M1',90::smallint,'Done','Remaining')`);
    report=(await db.query(`select public.civic_submit_progress('TN-PWD-2025-01','M3',80::smallint,'Culvert work','Surface work') as id`)).rows[0].id;
    await deny(`select public.civic_review_progress('${report}','Approved','Self approval',true)`);
  }));
  const photo=`${ids.contractor}/${report}/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa.jpg`;
  await check('pending report and evidence are not public',()=>as('anon',async()=>{
    assert.equal((await db.query(`select id from public.civic_progress_reports where id=$1`,[report])).rows.length,0);
    await deny(`insert into storage.objects(bucket_id,name,owner_id) values('progress-evidence','${photo}','${ids.contractor}')`);
  }));
  await check('Storage rejects a different uploader despite broad old policy',()=>as('other',()=>deny(`insert into storage.objects(bucket_id,name,owner_id)
    values('progress-evidence','${photo}','${ids.other}')`)));
  await check('contractor can upload and link owned pending evidence',()=>as('contractor',async()=>{
    await db.query(`insert into storage.objects(bucket_id,name,owner_id) values('progress-evidence',$1,$2)`,[photo,ids.contractor]);
    await db.query(`select public.civic_attach_file('progress-evidence',$1,$2)`,[report,photo]);
    assert.equal((await db.query(`select name from storage.objects where name=$1`,[photo])).rows.length,1);
  }));
  await check('linked evidence cannot be deleted or overwritten',()=>as('contractor',async()=>{
    await db.query(`delete from storage.objects where name=$1`,[photo]);
    await db.query(`update storage.objects set name='tampered.jpg' where name=$1`,[photo]);
    assert.equal((await db.query(`select name from storage.objects where name=$1`,[photo])).rows.length,1);
  }));
  await check('officer approval advances progress and publishes selected evidence',()=>as('officer',async()=>{
    await db.exec(`select public.civic_review_progress('${report}','Approved','Internal review note',true)`);
    assert.equal((await db.query(`select progress_percentage from public.civic_projects where id='TN-PWD-2025-01'`)).rows[0].progress_percentage,80);
  }));
  await check('public reads approved evidence but cannot see private officer notes',()=>as('anon',async()=>{
    assert.equal((await db.query(`select id from public.civic_progress_reports where id=$1`,[report])).rows.length,1);
    assert.equal((await db.query(`select name from storage.objects where name=$1`,[photo])).rows.length,1);
    await deny('select * from public.civic_progress_report_reviews');
    await deny('select submitted_by from public.progress_reports');
  }));
  await check('contractor cannot read internal review notes',()=>as('contractor',async()=>{
    assert.equal((await db.query(`select officer_notes from public.civic_progress_report_reviews where report_id=$1`,[report])).rows.length,0);
  }));
  await check('citizen cannot read review notes',()=>as('citizen',async()=>{
    assert.equal((await db.query(`select officer_notes from public.civic_progress_report_reviews where report_id=$1`,[report])).rows.length,0);
  }));
  await check('deadline request does not change approved project schedule',()=>as('contractor',async()=>{
    const before=(await db.query(`select revised_completion_date from public.civic_projects where id='TN-PWD-2025-01'`)).rows[0].revised_completion_date;
    request=(await db.query(`select public.civic_request_deadline('TN-PWD-2025-01','2027-01-01','Material delay') as id`)).rows[0].id;
    assert.equal((await db.query(`select revised_completion_date from public.civic_projects where id='TN-PWD-2025-01'`)).rows[0].revised_completion_date.toISOString(),before.toISOString());
  }));
  await check('deadline approval changes schedule and creates delay history atomically',()=>as('officer',async()=>{
    await db.exec(`select public.civic_review_deadline('${request}',true,'Approved reason for publication')`);
    assert.equal((await db.query(`select status from public.civic_deadline_revision_requests where id=$1`,[request])).rows[0].status,'APPROVED');
    assert.equal((await db.query(`select id from public.civic_delay_records where project_id='TN-PWD-2025-01'`)).rows.length,1);
  }));
  await check('coordinator records/resolves real project pairs',()=>as('coordinator',async()=>{
    conflict=(await db.query(`select public.civic_record_conflict('TN-PWD-2025-01','TN-TWAD-2025-02','RESOURCE_CONTENTION','Test location','Test dates') as id`)).rows[0].id;
    await db.query(`select public.civic_resolve_conflict($1,'Coordinated schedule')`,[conflict]);
    assert.equal((await db.query(`select status from public.civic_project_conflicts where id=$1`,[conflict])).rows[0].status,'RESOLVED');
    await deny(`select public.civic_record_delay('TN-PWD-2025-01','2027-02-01','Reason','Explanation')`);
  }));
  const pdata={title:'New test project',description:'Test description',department_id:'PWD',contractor_id:'CON-LT-9081',
    municipal_body_id:'GCC',responsible_office_id:'PWD-CHENNAI-OFFICE',district:'Chennai',location:'Test location',latitude:13,longitude:80,
    estimated_budget_in_lakhs:1,funding_source:'Test fund',start_date:'2026-01-01',original_completion_date:'2027-01-01'};
  let newProject;
  await check('new immutable project IDs use incrementing database counter',()=>as('officer',async()=>{
    newProject=(await db.query(`select public.civic_register_project($1::jsonb,'[{"milestone_id":"M1","title":"Test milestone","target_date":"2026-12-01"}]') as id`,[JSON.stringify(pdata)])).rows[0].id;
    const second=(await db.query(`select public.civic_register_project($1::jsonb) as id`,[JSON.stringify(pdata)])).rows[0].id;
    assert.notEqual(newProject,second);assert.match(newProject,/^TN-PWD-\d{4}-\d+$/);
  }));
  await check('unpublished project does not leak through public view',()=>as('anon',async()=>{
    assert.equal((await db.query(`select id from public.civic_projects where id=$1`,[newProject])).rows.length,0);
  }));
  await check('unauthorized registration leaves no partially inserted project',()=>as('officer',async()=>{
    await assert.rejects(db.query(`select public.civic_register_project($1::jsonb)`,[JSON.stringify({...pdata,department_id:'TWAD',responsible_office_id:'TWAD-COIMBATORE-OFFICE'})]),e=>e.code==='42501');
  }));
  await check('admin cannot change own status; can explicitly grant another Officer scope',()=>as('admin',async()=>{
    await assert.rejects(db.exec(`select public.civic_admin_set_user('${ids.admin}','CITIZEN',false)`),/cannot change own/);
    await db.exec(`select public.civic_admin_set_user('${ids.other}','OFFICER',true,'PWD','PWD-CHENNAI-OFFICE');
      select public.civic_admin_set_scope('${ids.other}','PWD','PWD-CHENNAI-OFFICE',true)`);
  }));
  let handover;
  await check('handover freezes actual project set and blocks overlapping initiations',()=>as('officer',async()=>{
    handover=(await db.query(`select public.civic_initiate_handover($1,array[$2],current_date,'Test transfer','Test notes') as id`,[ids.other,newProject])).rows[0].id;
    await assert.rejects(db.query(`select public.civic_initiate_handover($1,array[$2],current_date,'Transfer','Notes')`,[ids.other,newProject]),/already has an open handover/);
  }));
  await check('only incoming Officer acknowledges and receives frozen assignment',async()=>{
    await as('officer',()=>deny(`select public.civic_answer_handover('${handover}',true)`));
    await as('other',()=>db.exec(`select public.civic_answer_handover('${handover}',true)`));
    assert.equal((await db.query(`select officer_id from public.project_assignments where project_id=$1`,[newProject])).rows[0].officer_id,ids.other);
  });
  await check('overdue engine escalates once without any client call',async()=>{
    await db.query(`update public.complaints set submitted_at=now()-interval '200 hours' where id=$1`,[complaint]);
    await db.exec('select civic_private.escalate_overdue();select civic_private.escalate_overdue()');
    assert.equal((await db.query(`select count(*)::int as n from public.complaint_events where complaint_id=$1 and event_type='Escalated — Overdue'`,[complaint])).rows[0].n,1);
    await as('citizen',()=>deny('select civic_private.escalate_overdue()'));
  });
  await check('late citizen information retains one overdue event without resetting the deadline',async()=>{
    await db.query(`update public.complaints set status='AWAITING_CITIZEN_INFO' where id=$1`,[complaint]);
    await as('citizen',()=>db.exec(`select public.civic_reply_complaint('${complaint}','Late details')`));
    assert.equal((await db.query(`select count(*)::int as n from public.complaint_events where complaint_id=$1 and event_type='Escalated — Overdue'`,[complaint])).rows[0].n,1);
    assert.equal((await db.query(`select status from public.complaints where id=$1`,[complaint])).rows[0].status,'ESCALATED_OVERDUE');
  });
  await check('inactive account loses protected access immediately',async()=>{
    await as('admin',()=>db.exec(`select public.civic_admin_set_user('${ids.other}','OFFICER',false)`));
    await as('other',async()=>{
      await deny(`select public.civic_record_delay('TN-PWD-2025-01','2028-01-01','Reason','Explanation')`);
      assert.equal((await db.query(`select id from public.civic_officer_handovers where id=$1`,[handover])).rows.length,0);
    });
  });
  await check('anonymous identity cannot receive an elevated role',()=>as('admin',async()=>{
    await assert.rejects(db.exec(`select public.civic_admin_set_user('${ids.guest}','OFFICER',true)`),/confirmed permanent Auth identity/);
  }));
  await check('jurisdiction hierarchy rejects cycles',()=>as('admin',async()=>{
    await db.exec(`select public.civic_admin_jurisdiction('CYCLE-A','Cycle A','ZONE',null,'Test',null,true);
      select public.civic_admin_jurisdiction('CYCLE-B','Cycle B','ZONE','CYCLE-A','Test',null,true)`);
    await assert.rejects(db.exec(`select public.civic_admin_jurisdiction('CYCLE-A','Cycle A','ZONE','CYCLE-B','Test',null,true)`),/cannot contain cycles/);
  }));
  await check('rerunning scripts preserves changed roles, dates and new project rows',async()=>{
    const before=(await db.query(`select count(*)::int as n from public.projects`)).rows[0].n;
    for(const name of ['admin/001_bootstrap_admin.sql','migrations/002_civic_access.sql','migrations/003_civic_workflows.sql','migrations/004_civic_storage.sql','seeds/001_android_demo.sql','admin/002_assign_test_roles.sql'])await db.exec(await file(name));
    assert.equal((await db.query(`select count(*)::int as n from public.projects`)).rows[0].n,before);
    assert.equal((await db.query(`select is_active from public.profiles where id=$1`,[ids.other])).rows[0].is_active,false);
    assert.equal((await db.query(`select status from public.deadline_revision_requests where id=$1`,[request])).rows[0].status,'APPROVED');
  });
  await check('cloud verification SQL executes; only unavailable Cron is false locally',async()=>{
    const result=await db.exec(await file('verification/003_check_backend.sql'));
    assert.equal(result[0].rows.length,15);
    for(const row of result[0].rows)assert.equal(row.passed,!row.check_name.startsWith('15 '),row.check_name);
  });
  await check('official staging import rejects duplicates without changing source records',async()=>{
    await db.exec(await file('import/001_prepare_official_project_staging.sql'));
    await db.exec(`insert into civic_private.official_project_staging(source_id,record_data)
      select id,to_jsonb(p) from public.projects p where id='TN-PWD-2025-01'`);
    await db.exec(await file('import/002_import_official_projects.sql'));
    assert.equal((await db.query(`select status from civic_private.official_project_staging where source_id='TN-PWD-2025-01'`)).rows[0].status,'REJECTED');
    assert.equal((await db.query(`select is_demo from public.projects where id='TN-PWD-2025-01'`)).rows[0].is_demo,true);
  });
  await check('optional official importer accepts validated rows once and leaves them unpublished',async()=>{
    await db.exec(`insert into civic_private.official_project_staging(source_id,record_data)
      select 'OFFICIAL-LOCAL-TEST',to_jsonb(p)||'{"id":"OFFICIAL-LOCAL-TEST"}'::jsonb from public.projects p where id='TN-PWD-2025-01'`);
    await db.exec(await file('import/002_import_official_projects.sql'));
    await db.exec(await file('import/002_import_official_projects.sql'));
    const row=(await db.query(`select is_published,is_demo from public.projects where id='OFFICIAL-LOCAL-TEST'`)).rows[0];
    assert.equal(row.is_published,false);assert.equal(row.is_demo,false);
    assert.equal((await db.query(`select count(*)::int as n from public.projects where id='OFFICIAL-LOCAL-TEST'`)).rows[0].n,1);
  });
  process.stdout.write(`\n${passed} PostgreSQL checks passed. Real Auth/Storage/cron service tests still required.\n`);
} finally {await db.close();}
