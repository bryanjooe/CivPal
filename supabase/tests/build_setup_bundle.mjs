// Rebuilds the one-paste setup file from reviewed migration/seed/admin sources.
import { readFile,writeFile } from 'node:fs/promises';
const ordered=[
 'admin/001_bootstrap_admin.sql',
 'migrations/002_civic_access.sql',
 'migrations/003_civic_workflows.sql',
 'migrations/004_civic_storage.sql',
 'seeds/001_android_demo.sql',
 'admin/002_assign_test_roles.sql',
 'migrations/005_civic_scheduler.sql'
];
let sql=`-- CIVICTECH: ALL REMAINING SQL TOGETHER
-- Run the entire file as postgres in SQL Editor AFTER 001_civic_schema.sql.
-- Includes clearly labeled Android DEMO data and the five confirmed test emails.
-- No passwords, API keys, Android edits or public self-promotion are included.
-- This includes the admin bootstrap whether or not you already ran it.
-- Everything below is one transaction: any error rolls the entire bundle back.
begin;
do $$ begin
 if current_user<>'postgres' then raise exception 'Use SQL Editor execution role postgres'; end if;
 if not exists(select 1 from civic_private.schema_migrations where version='001_civic_schema') then
   raise exception 'Initial schema 001 must already be installed'; end if;
 perform pg_advisory_xact_lock(hashtextextended('civic:remaining-setup',0));
end $$;
`;
for(const name of ordered){
  const content=await readFile(new URL('../'+name,import.meta.url),'utf8');
  sql+=`\n-- ===== ${name} =====\n`+content.replace(/^begin;\r?\n/gm,'').replace(/^commit;\r?\n/gm,'');
}
sql+=`\ninsert into civic_private.schema_migrations(version) values('remaining_setup_v1') on conflict do nothing;
notify pgrst,'reload schema';
commit;
\n-- ===== Read-only cloud verification =====\n`;
const checks=await readFile(new URL('../verification/003_check_backend.sql',import.meta.url),'utf8');
// Put the 15 configuration booleans last for the dashboard's final result grid.
const split=checks.indexOf('\nselect email,role');
sql+=checks.slice(split)+'\n'+checks.slice(0,split);
await writeFile(new URL('../ALL_REMAINING_SETUP.sql',import.meta.url),sql);
process.stdout.write(`Built ALL_REMAINING_SETUP.sql (${sql.length} characters), with seven ordered components and verification.\n`);
