// Local isolated SQL checks; does not connect to the user's Supabase project.
import { PGlite } from './.runtime/node_modules/@electric-sql/pglite/dist/index.js';
import { readFile } from 'node:fs/promises';
import assert from 'node:assert/strict';
const db = new PGlite();
const script = await readFile(new URL('../admin/001_bootstrap_admin.sql', import.meta.url), 'utf8');
let passed = 0;
async function check(name, action) {
  await action();
  process.stdout.write(`PASS ${name}\n`);
  passed++;
}
async function rejectScript(pattern) {
  await assert.rejects(db.exec(script), pattern);
  await db.exec('rollback');
}
try {
  await db.exec(`create role anon; create role authenticated; create role service_role bypassrls;
    create schema auth;
    create table auth.users(id uuid primary key, email text, raw_user_meta_data jsonb default '{}',
      email_confirmed_at timestamptz, is_anonymous boolean default false);`);
  await db.exec(await readFile(new URL('../migrations/001_civic_schema.sql', import.meta.url), 'utf8'));
  await check('missing account fails without creating an Administrator', () => rejectScript(/exactly one Auth account/));
  await db.exec(`insert into auth.users(id,email) values
    ('11111111-1111-1111-1111-111111111111','admin@test.com'),
    ('22222222-2222-2222-2222-222222222222','officer@test.com');`);
  await check('unconfirmed account is rejected', () => rejectScript(/confirmed, non-anonymous/));
  await db.exec(`update auth.users set email_confirmed_at=now(),is_anonymous=true where email='admin@test.com'`);
  await check('anonymous identity is rejected', () => rejectScript(/confirmed, non-anonymous/));
  await db.exec(`update auth.users set is_anonymous=false where email='admin@test.com';
    update public.profiles set is_active=false where email='admin@test.com'`);
  await check('inactive account is rejected', () => rejectScript(/active profile is required/));
  await db.exec(`update public.profiles set is_active=true where email='admin@test.com';
    update public.profiles set role='ADMINISTRATOR' where email='officer@test.com'`);
  await check('cannot bootstrap a second Administrator', () => rejectScript(/Administrator already exists/));
  await db.exec(`update public.profiles set role='CITIZEN' where email='officer@test.com'`);
  await check('only intended identity is elevated, with one audit record', async () => {
    await db.exec(script);
    assert.equal((await db.query(`select role from public.profiles where email='admin@test.com'`)).rows[0].role,'ADMINISTRATOR');
    assert.equal((await db.query(`select role from public.profiles where email='officer@test.com'`)).rows[0].role,'CITIZEN');
    assert.equal((await db.query(`select count(*)::int as n from public.audit_logs`)).rows[0].n,1);
  });
  await check('repeat succeeds without duplicate audit records', async () => {
    await db.exec(script);
    assert.equal((await db.query(`select count(*)::int as n from public.audit_logs`)).rows[0].n,1);
  });
  await check('authenticated client cannot run the bootstrap', async () => {
    await db.exec('set role authenticated');
    try { await rejectScript(/only in SQL Editor as postgres/); }
    finally { await db.exec('reset role'); }
  });
  process.stdout.write(`\n${passed} local checks passed. Cloud execution remains pending.\n`);
} finally { await db.close(); }
