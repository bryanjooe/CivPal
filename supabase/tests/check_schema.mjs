// Isolated local schema test. Does not connect to Supabase or modify Android.
// npm.cmd install --prefix supabase/tests/.runtime --no-save --package-lock=false --ignore-scripts @electric-sql/pglite
// node supabase/tests/check_schema.mjs
import { PGlite } from './.runtime/node_modules/@electric-sql/pglite/dist/index.js';
import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';

const db = new PGlite();
let passed = 0;
async function check(name, action) {
  await action();
  passed++;
  process.stdout.write(`PASS ${name}\n`);
}
async function rejects(sql, code) {
  await assert.rejects(db.exec(sql), error => error.code === code);
}
try {
  // Minimal Supabase-owned objects for testing application SQL only.
  await db.exec(`
    create role anon; create role authenticated; create role service_role bypassrls;
    create schema auth;
    create table auth.users(id uuid primary key, email text, raw_user_meta_data jsonb default '{}');
  `);
  const migration = await readFile(new URL('../migrations/001_civic_schema.sql', import.meta.url), 'utf8');
  await check('migration executes atomically', () => db.exec(migration));
  await check('all 21 exposed tables have RLS and no anonymous/authenticated table grants', async () => {
    const { rows } = await db.query(`select relname,relrowsecurity,
      has_table_privilege('anon',oid,'SELECT,INSERT,UPDATE,DELETE') as anon_access,
      has_table_privilege('authenticated',oid,'SELECT,INSERT,UPDATE,DELETE') as auth_access
      from pg_class where relnamespace='public'::regnamespace and relkind='r'`);
    assert.equal(rows.length,21);
    for (const row of rows) {
      assert.equal(row.relrowsecurity,true,row.relname);
      assert.equal(row.anon_access,false,row.relname);
      assert.equal(row.auth_access,false,row.relname);
    }
  });
  await check('Auth user metadata cannot self-assign elevated roles', async () => {
    await db.exec(`insert into auth.users values
      ('11111111-1111-1111-1111-111111111111','test@example.invalid',
       '{"name":"Test citizen","role":"ADMINISTRATOR","department_id":"PWD","is_active":true}'),
      ('22222222-2222-2222-2222-222222222222',null,'{}');`);
    const { rows } = await db.query('select role,name,email from public.profiles order by id');
    assert.equal(rows[0].role,'CITIZEN');
    assert.equal(rows[1].role,'CITIZEN');
    assert.equal(rows[1].name,'Guest Citizen');
    assert.equal(rows[1].email,null);
  });
  await check('Auth email changes synchronize the correct profile', async () => {
    await db.exec(`update auth.users set email='changed@example.invalid'
      where id='11111111-1111-1111-1111-111111111111'`);
    assert.equal((await db.query(`select email from public.profiles where id='11111111-1111-1111-1111-111111111111'`)).rows[0].email,
      'changed@example.invalid');
  });
  await db.exec(`
    insert into public.departments(id,code,name) values ('PWD','PWD','Test department');
    insert into public.jurisdictions(id,name,kind) values ('TEST','Test municipality','MUNICIPALITY');
    insert into public.contractors(id,name) values ('CON-TEST','Test contractor');
    insert into public.projects(id,title,description,department_id,department_name,district,
      municipal_body_id,municipal_body,location,latitude,longitude,contractor_id,contractor_name,
      responsible_office_id,responsible_office,current_officer_in_charge,estimated_budget_in_lakhs,
      funding_source,start_date,original_completion_date,revised_completion_date)
    select id,'Test project','Test description','PWD','Test department','Test district','TEST','Test municipality',
      'Test location',13,80,'CON-TEST','Test contractor','TEST','Test office','Test officer',1,
      'Test fund','2025-01-01'::date,'2025-02-01'::date,'2025-02-01'::date
    from (values ('TN-PWD-2025-01'),('TN-PWD-2025-02')) fixture(id);
  `);
  await check('project IDs cannot be changed', () => rejects(
    `update public.projects set id='CHANGED' where id='TN-PWD-2025-01'`, '23514'));
  await check('same milestone ID works in different projects', () => db.exec(`
    insert into public.project_milestones(project_id,milestone_id,title,target_date)
    values ('TN-PWD-2025-01','M1','Test milestone','2025-02-01'),
           ('TN-PWD-2025-02','M1','Test milestone','2025-02-01');`));
  await check('progress report cannot reference another project milestone', () => rejects(`
    insert into public.progress_reports(project_id,milestone_id,submitted_by_name,
      completion_percentage,work_completed,remaining_work)
    values ('TN-PWD-2025-02','M2','Test submitter',50,'Test work','Test remaining');`, '23503'));
  await check('complaint deadline is exactly 168 hours even across DST and rejects tampering', async () => {
    await db.exec(`set timezone='America/New_York';
      insert into public.complaints(id,category,submitted_by_citizen_name,location,district,description,
        submitted_at,assigned_department_id,assigned_department_name,jurisdiction_id)
      values ('TEST-CMP','OTHER','Test citizen','Test location','Test district','Test complaint',
        '2026-03-07 12:00:00-05','PWD','Test department','TEST');
      update public.complaints set deadline_at=submitted_at where id='TEST-CMP';`);
    assert.equal(Number((await db.query(`select extract(epoch from deadline_at-submitted_at) as seconds
      from public.complaints where id='TEST-CMP'`)).rows[0].seconds),604800);
  });
  await check('unapproved progress cannot be published', () => rejects(`
    insert into public.progress_reports(project_id,milestone_id,submitted_by_name,
      completion_percentage,work_completed,remaining_work,is_public)
    values ('TN-PWD-2025-01','M1','Test submitter',50,'Test work','Test remaining',true)`, '23514'));
  await check('anonymous API cannot read private profiles or write projects', async () => {
    await db.exec('set role anon');
    try {
      await rejects('select * from public.profiles','42501');
      await rejects(`update public.projects set progress_percentage=100`,'42501');
    } finally { await db.exec('reset role'); }
  });
  await check('authenticated API cannot self-promote or read private notes', async () => {
    await db.exec('set role authenticated');
    try {
      await rejects(`update public.profiles set role='ADMINISTRATOR'`,'42501');
      await rejects('select * from public.progress_report_reviews','42501');
    } finally { await db.exec('reset role'); }
  });
  await check('verification queries execute', async () => {
    const verification = await readFile(new URL('../verification/001_check_schema.sql',import.meta.url),'utf8');
    const results = await db.exec(verification);
    assert.equal(results[0].rows.length,21);
    assert.ok(results[0].rows.every(row => row.table_exists && row.rls_enabled && row.api_access_closed));
  });
  await check('rerunning installation fails safely rather than overwriting records', async () => {
    await assert.rejects(db.exec(migration), /schema already exists/);
    await db.exec('rollback');
    assert.equal((await db.query('select count(*)::int as total from public.projects')).rows[0].total,2);
  });
  process.stdout.write(`\n${passed} checks passed. Local PostgreSQL only; Supabase cloud is unverified.\n`);
} finally { await db.close(); }
