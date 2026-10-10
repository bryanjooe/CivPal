-- OPTIONAL trusted import from staging. Validate the actual dataset first.
-- Required active departments/jurisdictions/contractors must already be loaded.
-- Each rejected row retains its source payload and exact error; no row is overwritten.
begin;
do $$ declare s record; p public.projects;
begin
  if current_user<>'postgres' then raise exception 'Trusted SQL Editor import only'; end if;
  for s in select * from civic_private.official_project_staging where status='PENDING' order by source_id for update loop
    begin
      p := jsonb_populate_record(null::public.projects,s.record_data);
      if p.id is distinct from s.source_id then raise exception 'Source ID must exactly match permanent Project ID'; end if;
      if exists(select 1 from public.projects where id=p.id) then raise exception 'Project ID already exists; no overwrite permitted'; end if;
      if not exists(select 1 from public.departments where id=p.department_id and is_active)
        or not exists(select 1 from public.contractors where id=p.contractor_id and is_active)
        or not exists(select 1 from public.jurisdictions where id=p.municipal_body_id and is_active)
        or not exists(select 1 from public.jurisdictions where id=p.responsible_office_id and is_active) then
        raise exception 'Active mapped catalogs required before import';
      end if;
      insert into public.projects(id,title,description,department_id,department_name,district,
        municipal_body_id,municipal_body,location,latitude,longitude,contractor_id,contractor_name,
        responsible_office_id,responsible_office,current_officer_in_charge,estimated_budget_in_lakhs,
        funding_source,start_date,original_completion_date,revised_completion_date,status,progress_percentage,
        is_published,is_demo,source_snapshot)
      values(p.id,p.title,p.description,p.department_id,p.department_name,p.district,p.municipal_body_id,
        p.municipal_body,p.location,p.latitude,p.longitude,p.contractor_id,p.contractor_name,
        p.responsible_office_id,p.responsible_office,p.current_officer_in_charge,p.estimated_budget_in_lakhs,
        p.funding_source,p.start_date,p.original_completion_date,p.revised_completion_date,
        coalesce(p.status,'PLANNED'),coalesce(p.progress_percentage,0),false,false,s.record_data);
      update civic_private.official_project_staging set status='IMPORTED',imported_at=now(),error_detail=null where source_id=s.source_id;
      insert into public.audit_logs(event_at,action,performed_by_name,target_type,target_id,details)
      values(now(),'OFFICIAL_DATA_IMPORTED','Supabase SQL Editor (postgres)','PROJECT',p.id,'Reviewed external dataset imported as unpublished; publication requires a separate authorized action.');
    exception when others then
      update civic_private.official_project_staging set status='REJECTED',error_detail=sqlerrm where source_id=s.source_id;
      insert into civic_private.import_rejections(source_dataset,entity_type,source_id,reason,source_record)
      values('official_project_import','project',s.source_id,sqlerrm,s.record_data)
      on conflict(source_dataset,entity_type,source_id) do update set reason=excluded.reason,source_record=excluded.source_record,recorded_at=now();
    end;
  end loop;
end $$;
commit;
select source_id,status,error_detail from civic_private.official_project_staging order by source_id;
