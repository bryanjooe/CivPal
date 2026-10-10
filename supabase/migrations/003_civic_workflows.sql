-- All client writes go through these authorized, atomic functions.
begin;
create or replace function civic_private.require_actor(allowed public.civic_role[])
returns public.profiles language plpgsql security definer set search_path='' as $$
declare actor public.profiles;
begin
  select * into actor from public.profiles where id=auth.uid();
  if actor.id is null or not coalesce(civic_private.active_role()=any(allowed),false) then
    raise exception 'Active authorized account required' using errcode='42501';
  end if;
  return actor;
end $$;
create or replace function civic_private.log_action(action text,kind text,target text,details text)
returns void language sql security definer set search_path='' as $$
  insert into public.audit_logs(event_at,action,performed_by,performed_by_name,target_type,target_id,details)
  select now(),action,p.id,p.name,kind,target,details from public.profiles p where p.id=auth.uid();
$$;
create or replace function civic_private.require_text(value text,label text,max_length integer default 10000)
returns text language plpgsql immutable set search_path='' as $$
begin
  if value is null or length(btrim(value)) not between 1 and max_length then
    raise exception '% must contain 1 to % characters',label,max_length using errcode='22023';
  end if;
  return btrim(value);
end $$;
create or replace function civic_private.next_project_id(dept_code text)
returns text language plpgsql security definer set search_path='' as $$
declare y integer := extract(year from current_date); n bigint; candidate text; initial bigint;
begin
  -- Imported IDs are considered before the first counter is initialized.
  select coalesce(max(substring(id from '[0-9]+$')::bigint),0) into initial from public.projects
    where id ~ ('^TN-'||dept_code||'-'||y||'-[0-9]+$');
  loop
    insert into civic_private.id_counters(prefix,department_code,id_year,last_issued)
      values('TN',dept_code,y,initial+1)
    on conflict(prefix,department_code,id_year) do update
      set last_issued=greatest(civic_private.id_counters.last_issued,initial)+1
    returning last_issued into n;
    candidate := 'TN-'||dept_code||'-'||y||'-'||lpad(n::text,greatest(2,length(n::text)),'0');
    if not exists(select 1 from public.projects where id=candidate) then return candidate; end if;
  end loop;
end $$;

create or replace function public.civic_update_my_profile(p_name text,p_language text,p_notifications boolean)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles;
begin
  a := civic_private.require_actor(array['CITIZEN','OFFICER','COORDINATOR','CONTRACTOR','ADMINISTRATOR']::public.civic_role[]);
  update public.profiles set name=civic_private.require_text(p_name,'Name',200),
    preferred_language=p_language,notifications_enabled=p_notifications,updated_at=now() where id=a.id;
end $$;
create or replace function public.civic_set_follow(p_project text,p_follow boolean)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles;
begin
  a := civic_private.require_actor(array['CITIZEN']::public.civic_role[]);
  if not civic_private.public_project(p_project) then raise exception 'Published project required' using errcode='42501'; end if;
  if p_follow then
    insert into public.project_follows(user_id,project_id) values(a.id,p_project) on conflict do nothing;
  else delete from public.project_follows where user_id=a.id and project_id=p_project; end if;
end $$;
create or replace function public.civic_register_project(p_data jsonb,p_milestones jsonb default '[]')
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; d public.departments; c public.contractors; m public.jurisdictions;
  o public.jurisdictions; result text; item jsonb;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into d from public.departments where id=p_data->>'department_id' and is_active;
  select * into c from public.contractors where id=p_data->>'contractor_id' and is_active;
  select * into m from public.jurisdictions where id=p_data->>'municipal_body_id' and kind='MUNICIPALITY' and is_active;
  select * into o from public.jurisdictions where id=p_data->>'responsible_office_id' and kind='OFFICE' and is_active;
  if d.id is null or c.id is null or m.id is null or o.id is null then raise exception 'Valid active department, contractor, municipality and office required'; end if;
  if not coalesce(civic_private.has_scope(d.id,o.id),false) then raise exception 'Project outside authorized scope' using errcode='42501'; end if;
  if jsonb_typeof(p_milestones)<>'array' or jsonb_array_length(p_milestones)>100 then raise exception 'Milestones must be an array of at most 100 items'; end if;
  result := civic_private.next_project_id(d.code);
  insert into public.projects(id,title,description,department_id,department_name,district,municipal_body_id,
    municipal_body,location,latitude,longitude,contractor_id,contractor_name,responsible_office_id,
    responsible_office,current_officer_in_charge,estimated_budget_in_lakhs,funding_source,
    start_date,original_completion_date,revised_completion_date)
  values(result,civic_private.require_text(p_data->>'title','Title',500),
    civic_private.require_text(p_data->>'description','Description'),d.id,d.name,
    civic_private.require_text(p_data->>'district','District',200),m.id,m.name,
    civic_private.require_text(p_data->>'location','Location',1000),
    (p_data->>'latitude')::double precision,(p_data->>'longitude')::double precision,c.id,c.name,o.id,o.name,a.name,
    (p_data->>'estimated_budget_in_lakhs')::numeric,civic_private.require_text(p_data->>'funding_source','Funding source',500),
    (p_data->>'start_date')::date,(p_data->>'original_completion_date')::date,(p_data->>'original_completion_date')::date);
  if a.role='OFFICER' then insert into public.project_assignments(project_id,officer_id) values(result,a.id); end if;
  for item in select value from jsonb_array_elements(p_milestones) loop
    insert into public.project_milestones(project_id,milestone_id,title,target_date)
    values(result,civic_private.require_text(item->>'milestone_id','Milestone ID',100),
      civic_private.require_text(item->>'title','Milestone title',500),(item->>'target_date')::date);
  end loop;
  perform civic_private.log_action('PROJECT_REGISTERED','PROJECT',result,'Project registered with immutable database-generated ID; publication pending.');
  return result;
end $$;
create or replace function public.civic_publish_project(p_project text,p_publish boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  perform 1 from public.projects where id=p_project for update;
  if not civic_private.can_manage_project(p_project) then raise exception 'Project outside authorized scope' using errcode='42501'; end if;
  update public.projects set is_published=p_publish,updated_at=now() where id=p_project;
  perform civic_private.log_action('PROJECT_PUBLICATION_CHANGED','PROJECT',p_project,'Published: '||p_publish);
end $$;
create or replace function public.civic_submit_complaint(p_category public.civic_complaint_category,
  p_location text,p_description text,p_project text default null,p_jurisdiction text default null)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; p public.projects; j public.jurisdictions; d public.departments; result text;
begin
  a := civic_private.require_actor(array['CITIZEN']::public.civic_role[]);
  if p_project is not null then
    select * into p from public.projects where id=p_project and is_published;
    if p.id is null then raise exception 'Published project required' using errcode='42501'; end if;
    select * into d from public.departments where id=p.department_id and is_active;
    select * into j from public.jurisdictions where id=p.responsible_office_id and is_active;
  else
    select * into j from public.jurisdictions where id=p_jurisdiction and kind='MUNICIPALITY' and is_active;
    select * into d from public.departments where is_active and code=case p_category
      when 'POTHOLE' then 'HWY' when 'WATER_LEAK' then 'TWAD' when 'DRAINAGE_OVERFLOW' then 'PWD' else 'MAWS' end;
  end if;
  if j.id is null or d.id is null then raise exception 'Active routing department and jurisdiction required'; end if;
  insert into public.complaints(category,project_id,project_title_snapshot,citizen_id,submitted_by_citizen_name,
    location,district,description,assigned_department_id,assigned_department_name,jurisdiction_id)
  values(p_category,p.id,p.title,a.id,a.name,civic_private.require_text(p_location,'Location',1000),
    coalesce(j.district,p.district,j.name),civic_private.require_text(p_description,'Description'),d.id,d.name,j.id)
  returning id into result;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(result,'Submitted','Complaint registered. Official response deadline: 168 hours.',a.id,a.name);
  perform civic_private.log_action('COMPLAINT_SUBMITTED','COMPLAINT',result,'Category: '||p_category||'; department: '||d.code);
  return result;
end $$;
create or replace function public.civic_respond_complaint(p_complaint text,p_status public.civic_complaint_status,p_message text)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; c public.complaints;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into c from public.complaints where id=p_complaint for update;
  if not civic_private.can_manage_complaint(p_complaint) then raise exception 'Complaint outside authorized scope' using errcode='42501'; end if;
  if c.status in ('RESOLVED','REJECTED') then raise exception 'Complaint is already closed'; end if;
  if p_status not in ('UNDER_REVIEW','ACTION_PROPOSED','RESOLVED','REJECTED','AWAITING_CITIZEN_INFO') then raise exception 'Invalid officer decision'; end if;
  insert into public.complaint_responses(complaint_id,responded_by,responded_by_name,role_snapshot,message,responded_at)
  values(c.id,a.id,a.name,a.role::text,civic_private.require_text(p_message,'Response'),now());
  update public.complaints set status=p_status,updated_at=now() where id=c.id;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(c.id,case when now()>c.deadline_at then 'Late Response Recorded' else 'Response Recorded' end,
    'Status: '||p_status||'. '||btrim(p_message),a.id,a.name);
  perform civic_private.log_action('COMPLAINT_RESPONSE','COMPLAINT',c.id,'Status: '||p_status);
end $$;
create or replace function public.civic_reply_complaint(p_complaint text,p_message text)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; c public.complaints;
begin
  a := civic_private.require_actor(array['CITIZEN']::public.civic_role[]);
  select * into c from public.complaints where id=p_complaint for update;
  if c.citizen_id is distinct from a.id then raise exception 'Own complaint required' using errcode='42501'; end if;
  if c.status<>'AWAITING_CITIZEN_INFO' then raise exception 'Complaint is not awaiting citizen information'; end if;
  insert into public.complaint_responses(complaint_id,responded_by,responded_by_name,role_snapshot,message,responded_at)
  values(c.id,a.id,a.name,'CITIZEN',civic_private.require_text(p_message,'Additional information'),now());
  update public.complaints set status=case when now()>deadline_at then 'ESCALATED_OVERDUE'::public.civic_complaint_status
    else 'UNDER_REVIEW'::public.civic_complaint_status end,priority=case when now()>deadline_at then 'HIGH'::public.civic_complaint_priority else priority end,updated_at=now() where id=c.id;
  if now()>c.deadline_at then
    insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name)
    values(c.id,'EVT-ESC-'||c.id,c.deadline_at,'Escalated — Overdue',
      '168-hour official response deadline exceeded; citizen information does not reset it.','System Escalation Engine') on conflict do nothing;
    if found then
      insert into public.audit_logs(event_at,action,performed_by_name,target_type,target_id,details)
      values(now(),'COMPLAINT_AUTO_ESCALATED','System Escalation Engine','COMPLAINT',c.id,'Original deadline exceeded while awaiting citizen information.');
    end if;
  end if;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(c.id,'Citizen Information Provided',btrim(p_message),a.id,a.name);
  perform civic_private.log_action('CITIZEN_INFORMATION','COMPLAINT',c.id,'Citizen supplied requested information; original deadline retained.');
end $$;
create or replace function public.civic_assign_complaint(p_complaint text,p_officer uuid)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; c public.complaints; o public.profiles;
begin
  a := civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  select * into c from public.complaints where id=p_complaint for update;
  if c.id is null then raise exception 'Complaint not found'; end if;
  if p_officer is not null then
    select * into o from public.profiles where id=p_officer and is_active and role='OFFICER';
    if o.id is null or not exists(select 1 from public.profile_scopes s where s.profile_id=o.id
      and s.department_id=c.assigned_department_id and s.jurisdiction_id=c.jurisdiction_id) then
      raise exception 'Officer requires an explicit matching complaint scope';
    end if;
  end if;
  update public.complaints set assigned_officer_id=o.id,assigned_officer_name=o.name,assigned_officer_email=o.email,updated_at=now() where id=c.id;
  insert into public.complaint_events(complaint_id,event_type,description,performed_by,performed_by_name)
  values(c.id,'Officer Assigned',coalesce(o.name,'Unassigned'),a.id,a.name);
  perform civic_private.log_action('COMPLAINT_ASSIGNMENT','COMPLAINT',c.id,coalesce(o.id::text,'Unassigned'));
end $$;
create or replace function public.civic_submit_progress(p_project text,p_milestone text,p_percentage smallint,
  p_completed text,p_remaining text,p_obstacles text default null)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; result text;
begin
  a := civic_private.require_actor(array['CONTRACTOR']::public.civic_role[]);
  perform 1 from public.projects where id=p_project for share;
  if not civic_private.contractor_project(p_project) then raise exception 'Assigned contractor project required' using errcode='42501'; end if;
  insert into public.progress_reports(project_id,milestone_id,submitted_by,submitted_by_name,submitted_at,
    completion_percentage,work_completed,remaining_work,obstacles)
  values(p_project,p_milestone,a.id,a.name,now(),p_percentage,civic_private.require_text(p_completed,'Completed work'),
    civic_private.require_text(p_remaining,'Remaining work'),left(p_obstacles,10000)) returning id into result;
  perform civic_private.log_action('PROGRESS_SUBMITTED','PROGRESS_REPORT',result,'Project: '||p_project||'; milestone: '||p_milestone);
  return result;
end $$;
create or replace function public.civic_review_progress(p_report text,p_decision public.civic_progress_status,p_notes text,p_publish boolean default false)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; r public.progress_reports;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into r from public.progress_reports where id=p_report for update;
  if not civic_private.can_manage_project(r.project_id) or r.submitted_by=a.id then raise exception 'Authorized independent reviewer required' using errcode='42501'; end if;
  if r.status<>'Pending Verification' then raise exception 'Report already reviewed'; end if;
  if p_decision='Pending Verification' or (p_publish and p_decision<>'Approved') then raise exception 'Invalid review decision'; end if;
  insert into public.progress_report_reviews(report_id,reviewed_by,reviewer_name,officer_notes,decision,reviewed_at)
  values(r.id,a.id,a.name,civic_private.require_text(p_notes,'Review notes'),p_decision,now());
  update public.progress_reports set status=p_decision,is_public=p_publish where id=r.id;
  if p_decision='Approved' then
    update public.projects set progress_percentage=greatest(progress_percentage,r.completion_percentage),
      status=case when greatest(progress_percentage,r.completion_percentage)=100 then 'COMPLETED'::public.civic_project_status
        when status='PLANNED' then 'ONGOING'::public.civic_project_status else status end,updated_at=now() where id=r.project_id;
    if r.completion_percentage=100 then update public.project_milestones set is_completed=true,completion_date=current_date
      where project_id=r.project_id and milestone_id=r.milestone_id and not is_completed; end if;
  end if;
  perform civic_private.log_action('PROGRESS_REVIEWED','PROGRESS_REPORT',r.id,'Decision: '||p_decision);
end $$;
create or replace function public.civic_record_delay(p_project text,p_date date,p_reason text,p_explanation text)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; p public.projects; result text;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into p from public.projects where id=p_project for update;
  if not civic_private.can_manage_project(p_project) then raise exception 'Project outside authorized scope' using errcode='42501'; end if;
  if p.status='COMPLETED' or p_date<=p.revised_completion_date then raise exception 'A later deadline for an unfinished project is required'; end if;
  insert into public.delay_records(project_id,recorded_by,recorded_by_name,recorded_at,reason,
    original_completion_date,revised_completion_date,published_explanation,is_published)
  values(p.id,a.id,a.name,now(),civic_private.require_text(p_reason,'Reason'),p.revised_completion_date,p_date,
    civic_private.require_text(p_explanation,'Public explanation'),true) returning id into result;
  update public.projects set revised_completion_date=p_date,status='DELAYED',updated_at=now() where id=p.id;
  perform civic_private.log_action('DELAY_RECORDED','PROJECT',p.id,'New deadline: '||p_date);
  return result;
end $$;
create or replace function public.civic_request_deadline(p_project text,p_date date,p_reason text)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; p public.projects; result text;
begin
  a := civic_private.require_actor(array['CONTRACTOR']::public.civic_role[]);
  select * into p from public.projects where id=p_project for share;
  if not civic_private.contractor_project(p_project) then raise exception 'Assigned project required' using errcode='42501'; end if;
  if p.status='COMPLETED' then raise exception 'Project already completed'; end if;
  insert into public.deadline_revision_requests(project_id,submitted_by,current_deadline_snapshot,proposed_completion_date,reason)
  values(p.id,a.id,p.revised_completion_date,p_date,civic_private.require_text(p_reason,'Reason')) returning id into result;
  perform civic_private.log_action('DEADLINE_REQUESTED','DEADLINE_REQUEST',result,'Project: '||p.id);
  return result;
end $$;
create or replace function public.civic_review_deadline(p_request text,p_approve boolean,p_notes text)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; r public.deadline_revision_requests; p public.projects;
begin
  a := civic_private.require_actor(array['OFFICER','ADMINISTRATOR']::public.civic_role[]);
  select * into r from public.deadline_revision_requests where id=p_request for update;
  select * into p from public.projects where id=r.project_id for update;
  if not civic_private.can_manage_project(r.project_id) or r.submitted_by=a.id then raise exception 'Independent authorized reviewer required' using errcode='42501'; end if;
  if r.status<>'PENDING' then raise exception 'Request already reviewed'; end if;
  if p_approve and (p.revised_completion_date<>r.current_deadline_snapshot or p.status='COMPLETED') then raise exception 'Project deadline/status changed; request must be resubmitted'; end if;
  update public.deadline_revision_requests set status=case when p_approve then 'APPROVED' else 'REJECTED' end,
    reviewed_by=a.id,reviewer_notes=civic_private.require_text(p_notes,'Review notes'),reviewed_at=now() where id=r.id;
  if p_approve then perform public.civic_record_delay(p.id,r.proposed_completion_date,r.reason,p_notes); end if;
  perform civic_private.log_action('DEADLINE_REVIEWED','DEADLINE_REQUEST',r.id,'Approved: '||p_approve);
end $$;
create or replace function public.civic_record_conflict(p_a text,p_b text,p_type public.civic_conflict_type,p_location text,p_dates text,p_resolution text default null)
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; pa public.projects; pb public.projects; result text;
begin
  a := civic_private.require_actor(array['COORDINATOR','ADMINISTRATOR']::public.civic_role[]);
  select * into pa from public.projects where id=p_a;
  select * into pb from public.projects where id=p_b;
  if not coalesce(civic_private.has_scope(pa.department_id,pa.responsible_office_id) and
    civic_private.has_scope(pb.department_id,pb.responsible_office_id),false) then raise exception 'Both projects must be in coordination scope' using errcode='42501'; end if;
  insert into public.project_conflicts(project_a_id,project_b_id,project_a_title_snapshot,project_b_title_snapshot,
    department_a_snapshot,department_b_snapshot,location,conflicting_dates,conflict_type,proposed_resolution)
  values(pa.id,pb.id,pa.title,pb.title,pa.department_name,pb.department_name,civic_private.require_text(p_location,'Location',1000),
    civic_private.require_text(p_dates,'Conflicting dates',500),p_type,left(p_resolution,10000)) returning id into result;
  perform civic_private.log_action('CONFLICT_RECORDED','CONFLICT',result,'Projects: '||pa.id||', '||pb.id);
  return result;
end $$;
create or replace function public.civic_resolve_conflict(p_conflict text,p_resolution text)
returns void language plpgsql security definer set search_path='' as $$
declare c public.project_conflicts;
begin
  perform civic_private.require_actor(array['COORDINATOR','ADMINISTRATOR']::public.civic_role[]);
  select * into c from public.project_conflicts where id=p_conflict for update;
  if not coalesce(civic_private.internal_project(c.project_a_id) and civic_private.internal_project(c.project_b_id),false) then raise exception 'Both projects must be in coordination scope' using errcode='42501'; end if;
  if c.status='RESOLVED' then raise exception 'Conflict already resolved'; end if;
  update public.project_conflicts set proposed_resolution=civic_private.require_text(p_resolution,'Resolution'),
    status='RESOLVED',resolved_at=now(),resolved_by=auth.uid() where id=c.id;
  perform civic_private.log_action('CONFLICT_RESOLVED','CONFLICT',c.id,'Resolution recorded.');
end $$;

create or replace function public.civic_initiate_handover(p_incoming uuid,p_projects text[],p_effective date,p_reason text,p_notes text,p_tasks jsonb default '[]')
returns text language plpgsql security definer set search_path='' as $$
declare a public.profiles; incoming public.profiles; first_project public.projects; p public.projects; result text; item jsonb;
begin
  a := civic_private.require_actor(array['OFFICER']::public.civic_role[]);
  select * into incoming from public.profiles where id=p_incoming and is_active and role='OFFICER';
  if incoming.id is null or incoming.id=a.id then raise exception 'A different active incoming Officer is required'; end if;
  if coalesce(cardinality(p_projects),0) not between 1 and 100 or cardinality(p_projects)<>(select count(distinct x) from unnest(p_projects) x) then raise exception 'Select 1 to 100 distinct projects'; end if;
  if p_effective is null or p_effective<current_date then raise exception 'Transfer date must be today or later'; end if;
  if jsonb_typeof(p_tasks)<>'array' or jsonb_array_length(p_tasks)>100 then raise exception 'Tasks must be an array of at most 100 items'; end if;
  -- Serialize overlapping initiations and approvals for the same outgoing officer.
  perform pg_advisory_xact_lock(hashtextextended('civic:handover:'||a.id,0));
  select * into first_project from public.projects where id=p_projects[1];
  for p in select * from public.projects where id=any(p_projects) order by id for update loop
    if p.department_id<>first_project.department_id or p.responsible_office_id<>first_project.responsible_office_id
      or not civic_private.can_manage_project(p.id) or not exists(select 1 from public.project_assignments where project_id=p.id and officer_id=a.id)
      or not exists(select 1 from public.profile_scopes where profile_id=incoming.id and department_id=p.department_id and jurisdiction_id=p.responsible_office_id) then
      raise exception 'Every project must be assigned to you in one office, with approved incoming scope' using errcode='42501';
    end if;
    if exists(select 1 from public.handover_projects hp join public.officer_handovers h on h.id=hp.handover_id
      where hp.project_id=p.id and h.status in ('INITIATED','CLARIFICATION_REQUESTED')) then raise exception 'Project already has an open handover'; end if;
  end loop;
  if (select count(*) from public.projects where id=any(p_projects))<>cardinality(p_projects) then raise exception 'Unknown project'; end if;
  insert into public.officer_handovers(outgoing_officer_id,outgoing_officer_name,outgoing_officer_email,
    incoming_officer_id,incoming_officer_name,incoming_officer_email,department_id,department_name,
    office_jurisdiction_id,office_jurisdiction,effective_transfer_date,reason_for_reassignment,handover_notes)
  values(a.id,a.name,a.email,incoming.id,incoming.name,incoming.email,first_project.department_id,first_project.department_name,
    first_project.responsible_office_id,first_project.responsible_office,p_effective,
    civic_private.require_text(p_reason,'Transfer reason'),civic_private.require_text(p_notes,'Handover notes')) returning id into result;
  insert into public.handover_projects(handover_id,project_id) select result,unnest(p_projects);
  for item in select value from jsonb_array_elements(p_tasks) loop
    insert into public.handover_pending_tasks(handover_id,id,related_project_id,title,priority,due_date)
    values(result,'TASK-'||gen_random_uuid(),item->>'project_id',civic_private.require_text(item->>'title','Task title',500),item->>'priority',(item->>'due_date')::date);
  end loop;
  perform civic_private.log_action('HANDOVER_INITIATED','HANDOVER',result,'Projects: '||cardinality(p_projects));
  return result;
end $$;
create or replace function public.civic_answer_handover(p_handover text,p_acknowledge boolean,p_clarification text default null)
returns void language plpgsql security definer set search_path='' as $$
declare a public.profiles; h public.officer_handovers; p public.projects;
begin
  a := civic_private.require_actor(array['OFFICER']::public.civic_role[]);
  select * into h from public.officer_handovers where id=p_handover for update;
  if h.incoming_officer_id is distinct from a.id or not civic_private.can_read_handover(h.id) then raise exception 'Authorized incoming Officer required' using errcode='42501'; end if;
  if h.status='ACKNOWLEDGED' then raise exception 'Handover already acknowledged'; end if;
  if p_acknowledge then
    if current_date<h.effective_transfer_date then raise exception 'Transfer date has not arrived'; end if;
    perform pg_advisory_xact_lock(hashtextextended('civic:handover:'||h.outgoing_officer_id,0));
    for p in select pr.* from public.projects pr join public.handover_projects hp on hp.project_id=pr.id
      where hp.handover_id=h.id order by pr.id for update of pr loop
      if not civic_private.can_manage_project(p.id) or not exists(select 1 from public.project_assignments where project_id=p.id and officer_id=h.outgoing_officer_id) then
        raise exception 'Scope or outgoing assignment has changed' using errcode='42501';
      end if;
      update public.project_assignments set officer_id=a.id,updated_at=now() where project_id=p.id;
      update public.projects set current_officer_in_charge=a.name,updated_at=now() where id=p.id;
    end loop;
    if not exists(select 1 from public.handover_projects where handover_id=h.id) then raise exception 'Handover has no transferable projects'; end if;
    update public.officer_handovers set status='ACKNOWLEDGED',acknowledged_at=now() where id=h.id;
  else
    update public.officer_handovers set status='CLARIFICATION_REQUESTED',
      clarification_notes=civic_private.require_text(p_clarification,'Clarification') where id=h.id;
  end if;
  perform civic_private.log_action('HANDOVER_ANSWERED','HANDOVER',h.id,'Acknowledged: '||p_acknowledge);
end $$;

create or replace function public.civic_admin_set_user(p_user uuid,p_role public.civic_role,p_active boolean,
  p_department text default null,p_office text default null,p_contractor text default null)
returns void language plpgsql security definer set search_path='' as $$
declare target public.profiles;
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  perform pg_advisory_xact_lock(hashtextextended('civic:admin-user-management',0));
  select * into target from public.profiles where id=p_user for update;
  if target.id is null then raise exception 'Existing Auth profile required'; end if;
  if p_user=auth.uid() then raise exception 'Administrator cannot change own role or account status'; end if;
  if p_role<>'CITIZEN' and not exists(select 1 from auth.users where id=p_user and not coalesce(is_anonymous,false) and email_confirmed_at is not null) then raise exception 'Elevated roles require a confirmed permanent Auth identity'; end if;
  if p_role='CONTRACTOR' and (p_contractor is null or not exists(select 1 from public.contractors where id=p_contractor and is_active)) then raise exception 'Active contractor association required'; end if;
  if p_role<>'CONTRACTOR' and p_contractor is not null then raise exception 'Contractor association applies only to contractors'; end if;
  update public.profiles set role=p_role,is_active=p_active,department_id=p_department,
    office_jurisdiction_id=p_office,contractor_id=p_contractor,updated_at=now() where id=p_user;
  -- Reauthorization is explicit after role/scope identity changes; stale scopes are removed.
  delete from public.profile_scopes where profile_id=p_user;
  perform civic_private.log_action('USER_AUTHORIZATION_CHANGED','PROFILE',p_user::text,'Role: '||p_role||'; active: '||p_active);
end $$;
create or replace function public.civic_admin_set_scope(p_user uuid,p_department text,p_jurisdiction text,p_grant boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  if not exists(select 1 from public.profiles where id=p_user and is_active and role in ('OFFICER','COORDINATOR')) then raise exception 'Active Officer or Coordinator required'; end if;
  if p_grant then
    if not exists(select 1 from public.departments where id=p_department and is_active)
      or not exists(select 1 from public.jurisdictions where id=p_jurisdiction and is_active) then raise exception 'Active department and jurisdiction required'; end if;
    insert into public.profile_scopes(profile_id,department_id,jurisdiction_id,granted_by)
      values(p_user,p_department,p_jurisdiction,auth.uid()) on conflict do nothing;
  else delete from public.profile_scopes where profile_id=p_user and department_id=p_department and jurisdiction_id=p_jurisdiction; end if;
  perform civic_private.log_action('USER_SCOPE_CHANGED','PROFILE',p_user::text,p_department||' / '||p_jurisdiction||'; grant: '||p_grant);
end $$;
create or replace function public.civic_admin_department(p_id text,p_code text,p_name text,p_description text,p_active boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  insert into public.departments(id,code,name,scope_description,is_active) values(p_id,p_code,p_name,p_description,p_active)
  on conflict(id) do update set code=excluded.code,name=excluded.name,scope_description=excluded.scope_description,is_active=excluded.is_active;
  perform civic_private.log_action('DEPARTMENT_CHANGED','DEPARTMENT',p_id,'Catalog details/status updated.');
end $$;
create or replace function public.civic_admin_jurisdiction(p_id text,p_name text,p_kind text,p_parent text,p_district text,p_details text,p_active boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  perform pg_advisory_xact_lock(hashtextextended('civic:jurisdiction-tree',0));
  if p_parent=p_id or exists(with recursive ancestors(id,parent_id) as (
    select id,parent_id from public.jurisdictions where id=p_parent
    union select j.id,j.parent_id from public.jurisdictions j join ancestors a on a.parent_id=j.id)
    select 1 from ancestors where id=p_id) then raise exception 'Jurisdiction hierarchy cannot contain cycles'; end if;
  insert into public.jurisdictions(id,name,kind,parent_id,district,details,is_active)
    values(p_id,p_name,p_kind,p_parent,p_district,p_details,p_active)
  on conflict(id) do update set name=excluded.name,kind=excluded.kind,parent_id=excluded.parent_id,
    district=excluded.district,details=excluded.details,is_active=excluded.is_active;
  perform civic_private.log_action('JURISDICTION_CHANGED','JURISDICTION',p_id,'Catalog details/status updated.');
end $$;
create or replace function public.civic_admin_contractor(p_id text,p_name text,p_active boolean)
returns void language plpgsql security definer set search_path='' as $$
begin
  perform civic_private.require_actor(array['ADMINISTRATOR']::public.civic_role[]);
  insert into public.contractors(id,name,is_active) values(p_id,p_name,p_active)
  on conflict(id) do update set name=excluded.name,is_active=excluded.is_active;
  perform civic_private.log_action('CONTRACTOR_CHANGED','CONTRACTOR',p_id,'Catalog details/status updated.');
end $$;
create or replace function civic_private.escalate_overdue()
returns integer language plpgsql security definer set search_path='' as $$
declare c public.complaints; total integer := 0;
begin
  for c in select * from public.complaints where deadline_at<now()
    and status in ('PENDING','UNDER_REVIEW','AWAITING_CITIZEN_INFO') order by id for update skip locked loop
    update public.complaints set status='ESCALATED_OVERDUE',priority='HIGH',updated_at=now() where id=c.id;
    insert into public.complaint_events(complaint_id,id,event_at,event_type,description,performed_by_name)
    values(c.id,'EVT-ESC-'||c.id,c.deadline_at,'Escalated — Overdue',
      '168-hour response deadline passed while complaint remained pending, under review or awaiting citizen information.', 'System Escalation Engine') on conflict do nothing;
    insert into public.audit_logs(event_at,action,performed_by_name,target_type,target_id,details)
    values(now(),'COMPLAINT_AUTO_ESCALATED','System Escalation Engine','COMPLAINT',c.id,'168-hour deadline exceeded.');
    total := total+1;
  end loop;
  return total;
end $$;

-- Explicitly grant only named civic_* RPCs, never every function in public.
do $$ declare f record;
begin
  for f in select p.oid::regprocedure as signature from pg_proc p join pg_namespace n on n.oid=p.pronamespace
    where n.nspname='public' and p.proname in (
      'civic_update_my_profile','civic_set_follow','civic_register_project','civic_publish_project',
      'civic_submit_complaint','civic_respond_complaint','civic_reply_complaint','civic_assign_complaint',
      'civic_submit_progress','civic_review_progress','civic_record_delay','civic_request_deadline',
      'civic_review_deadline','civic_record_conflict','civic_resolve_conflict','civic_initiate_handover',
      'civic_answer_handover','civic_admin_set_user','civic_admin_set_scope','civic_admin_department',
      'civic_admin_jurisdiction','civic_admin_contractor') loop
    execute format('revoke all on function %s from public,anon,authenticated',f.signature);
    execute format('grant execute on function %s to authenticated',f.signature);
  end loop;
end $$;
revoke all on function civic_private.require_actor(public.civic_role[]),
  civic_private.log_action(text,text,text,text),civic_private.require_text(text,text,integer),
  civic_private.next_project_id(text),civic_private.escalate_overdue() from public,anon,authenticated;
insert into civic_private.schema_migrations(version) values('003_civic_workflows') on conflict do nothing;
commit;
