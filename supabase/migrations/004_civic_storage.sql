-- Private buckets; object paths are user UUID / business record ID / random UUID.ext.
begin;
insert into storage.buckets(id,name,public,file_size_limit,allowed_mime_types) values
 ('complaint-photos','complaint-photos',false,10485760,array['image/jpeg','image/png','image/webp']),
 ('progress-evidence','progress-evidence',false,10485760,array['image/jpeg','image/png','image/webp']),
 ('deadline-documents','deadline-documents',false,10485760,array['application/pdf'])
on conflict(id) do nothing;
do $$ begin
  if exists(select 1 from storage.buckets where id in ('complaint-photos','progress-evidence','deadline-documents')
    and (public or file_size_limit is distinct from 10485760::bigint or
      allowed_mime_types is distinct from case when id='deadline-documents' then array['application/pdf']
        else array['image/jpeg','image/png','image/webp'] end)) then
    raise exception 'Existing CivicTech bucket configuration conflicts; inspect it instead of overwriting';
  end if;
end $$;
create or replace function civic_private.storage_upload_allowed(bucket text,path text)
returns boolean language sql stable security definer set search_path='' as $$
  select civic_private.active_role() is not null
    and split_part(path,'/',1)=auth.uid()::text
    and array_length(string_to_array(path,'/'),1)=3
    and split_part(path,'/',3) ~ case when bucket='deadline-documents'
      then '^[0-9a-f-]{36}\.pdf$' else '^[0-9a-f-]{36}\.(jpg|jpeg|png|webp)$' end
    and case bucket
      when 'complaint-photos' then exists(select 1 from public.complaints c
        where c.id=split_part(path,'/',2) and c.citizen_id=auth.uid()
          and civic_private.active_role()='CITIZEN' and c.status not in ('RESOLVED','REJECTED'))
      when 'progress-evidence' then exists(select 1 from public.progress_reports r
        where r.id=split_part(path,'/',2) and r.submitted_by=auth.uid()
          and r.status='Pending Verification' and civic_private.contractor_project(r.project_id))
      when 'deadline-documents' then exists(select 1 from public.deadline_revision_requests r
        where r.id=split_part(path,'/',2) and r.submitted_by=auth.uid()
          and r.status='PENDING' and civic_private.contractor_project(r.project_id))
      else false end;
$$;
create or replace function civic_private.storage_is_linked(bucket text,path text)
returns boolean language sql stable security definer set search_path='' as $$
  select case bucket
    when 'complaint-photos' then exists(select 1 from public.complaints where photo_path=path)
    when 'progress-evidence' then exists(select 1 from public.progress_reports where photo_path=path)
    when 'deadline-documents' then exists(select 1 from public.deadline_revision_requests where document_path=path)
    else false end;
$$;
create or replace function civic_private.storage_read_allowed(bucket text,path text)
returns boolean language sql stable security definer set search_path='' as $$
  select civic_private.storage_upload_allowed(bucket,path) or case bucket
    when 'complaint-photos' then exists(select 1 from public.complaints c where c.photo_path=path and civic_private.can_read_complaint(c.id))
    when 'progress-evidence' then exists(select 1 from public.progress_reports r where r.photo_path=path
      and ((r.is_public and r.status='Approved' and civic_private.public_project(r.project_id)) or civic_private.internal_project(r.project_id)))
    when 'deadline-documents' then exists(select 1 from public.deadline_revision_requests r where r.document_path=path
      and (civic_private.can_manage_project(r.project_id) or (r.submitted_by=auth.uid() and civic_private.contractor_project(r.project_id))))
    else false end;
$$;
create or replace function public.civic_attach_file(p_bucket text,p_record text,p_path text)
returns void language plpgsql security definer set search_path='' as $$
declare existing text;
begin
  perform civic_private.require_actor(array['CITIZEN','CONTRACTOR']::public.civic_role[]);
  -- Lock business row first, then check pending status to serialize with review.
  case p_bucket
    when 'complaint-photos' then select photo_path into existing from public.complaints where id=p_record for update;
    when 'progress-evidence' then select photo_path into existing from public.progress_reports where id=p_record for update;
    when 'deadline-documents' then select document_path into existing from public.deadline_revision_requests where id=p_record for update;
    else raise exception 'Unsupported bucket';
  end case;
  if split_part(p_path,'/',2)<>p_record or not coalesce(civic_private.storage_upload_allowed(p_bucket,p_path),false) then
    raise exception 'Authorized record path required' using errcode='42501';
  end if;
  if existing is not null and existing<>p_path then raise exception 'Linked evidence is immutable; upload evidence for a new submission'; end if;
  if not exists(select 1 from storage.objects where bucket_id=p_bucket and name=p_path and owner_id=auth.uid()::text) then
    raise exception 'Upload the owned object through Supabase Storage first';
  end if;
  case p_bucket
    when 'complaint-photos' then update public.complaints set photo_path=p_path,updated_at=now() where id=p_record;
    when 'progress-evidence' then update public.progress_reports set photo_path=p_path where id=p_record;
    when 'deadline-documents' then update public.deadline_revision_requests set document_path=p_path where id=p_record;
  end case;
  perform civic_private.log_action('ATTACHMENT_LINKED','ATTACHMENT',p_record,'Bucket: '||p_bucket);
end $$;
-- Restrictive guards prevent broader pre-existing permissive policies from
-- accidentally opening these buckets. Other buckets retain existing behavior.
drop policy if exists civic_storage_read_guard on storage.objects;
create policy civic_storage_read_guard on storage.objects as restrictive for select to anon,authenticated
using(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents') or civic_private.storage_read_allowed(bucket_id,name));
drop policy if exists civic_storage_insert_guard on storage.objects;
create policy civic_storage_insert_guard on storage.objects as restrictive for insert to anon,authenticated
with check(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents') or
  (owner_id=(select auth.uid())::text and civic_private.storage_upload_allowed(bucket_id,name)));
drop policy if exists civic_storage_update_guard on storage.objects;
create policy civic_storage_update_guard on storage.objects as restrictive for update to anon,authenticated
using(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents'))
with check(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents'));
drop policy if exists civic_storage_delete_guard on storage.objects;
create policy civic_storage_delete_guard on storage.objects as restrictive for delete to anon,authenticated
using(bucket_id not in ('complaint-photos','progress-evidence','deadline-documents'));
drop policy if exists civic_storage_read on storage.objects;
create policy civic_storage_read on storage.objects for select to anon,authenticated
using(bucket_id in ('complaint-photos','progress-evidence','deadline-documents') and civic_private.storage_read_allowed(bucket_id,name));
drop policy if exists civic_storage_insert on storage.objects;
create policy civic_storage_insert on storage.objects for insert to authenticated
with check(bucket_id in ('complaint-photos','progress-evidence','deadline-documents')
  and owner_id=(select auth.uid())::text and civic_private.storage_upload_allowed(bucket_id,name));
drop policy if exists civic_storage_delete on storage.objects;
-- No client DELETE or UPDATE policy. Trusted maintenance may clean orphaned
-- uploads through Storage API after checking that no business record links them.
revoke all on function civic_private.storage_upload_allowed(text,text),
  civic_private.storage_is_linked(text,text),civic_private.storage_read_allowed(text,text) from public,anon,authenticated;
grant execute on function civic_private.storage_upload_allowed(text,text),
  civic_private.storage_is_linked(text,text),civic_private.storage_read_allowed(text,text) to anon,authenticated;
revoke all on function public.civic_attach_file(text,text,text) from public,anon,authenticated;
grant execute on function public.civic_attach_file(text,text,text) to authenticated;
insert into civic_private.schema_migrations(version) values('004_civic_storage') on conflict do nothing;
commit;
