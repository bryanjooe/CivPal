// Runs ONLY on Supabase Edge Functions. No privileged credential is sent to Android.
const url = Deno.env.get("SUPABASE_URL")!;
const publicKey = Deno.env.get("SUPABASE_ANON_KEY")!;
const serverKey = Deno.env.get("CIVIC_SUPABASE_SECRET_KEY") || Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const serverHeaders = { apikey: serverKey, "Content-Type": "application/json",
  ...(serverKey.startsWith("sb_secret_") ? {} : { Authorization: `Bearer ${serverKey}` }) };
const reply = (status: number, body: unknown) => new Response(JSON.stringify(body), {
  status, headers: { "Content-Type": "application/json" },
});

Deno.serve(async (request: Request) => {
  if (request.method !== "POST") return reply(405, { error: "POST required" });
  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return reply(401, { error: "Sign in required" });
  try {
    const userResponse = await fetch(`${url}/auth/v1/user`, {
      headers: { apikey: publicKey, Authorization: authorization },
    });
    if (!userResponse.ok) return reply(401, { error: "Invalid session" });
    const actor = await userResponse.json();
    if (actor.is_anonymous || !actor.email_confirmed_at) return reply(403, { error: "Confirmed account required" });
    const profileResponse = await fetch(`${url}/rest/v1/civic_profiles?id=eq.${encodeURIComponent(actor.id)}&select=id,name,role,is_active`, {
      headers: { apikey: publicKey, Authorization: authorization },
    });
    if (!profileResponse.ok) return reply(403, { error: "Profile unavailable" });
    const profiles = await profileResponse.json();
    if (profiles.length !== 1 || profiles[0].role !== "ADMINISTRATOR" || !profiles[0].is_active)
      return reply(403, { error: "Administrator required" });
    const length = Number(request.headers.get("Content-Length") || 0);
    if (length > 4096) return reply(413, { error: "Request too large" });
    const raw = await request.text();
    if (raw.length > 4096) return reply(413, { error: "Request too large" });
    const input = JSON.parse(raw);
    const name = typeof input.name === "string" ? input.name.trim() : "";
    const email = typeof input.email === "string" ? input.email.trim() : "";
    if (!name || name.length > 200 || email.length > 320 || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      return reply(400, { error: "Valid name and email required" });
    // The database trigger creates CITIZEN by default. Elevation is a separate audited RPC
    // after the invited person confirms their identity; request metadata cannot elevate roles.
    const invite = await fetch(`${url}/auth/v1/invite?redirect_to=${encodeURIComponent("civic://auth-callback")}`, {
      method: "POST",
      headers: serverHeaders,
      body: JSON.stringify({ email, data: { name } }),
    });
    if (!invite.ok) return reply(400, { error: "Invitation failed. Check whether the user exists and email delivery is configured." });
    const created = await invite.json();
    const audit = await fetch(`${url}/rest/v1/audit_logs`, {
      method: "POST",
      headers: serverHeaders,
      body: JSON.stringify({ event_at: new Date().toISOString(), action: "USER_INVITED", performed_by: actor.id,
        performed_by_name: profiles[0].name, target_type: "PROFILE", target_id: created.id, details: "Administrator invited a citizen account." }),
    });
    if (!audit.ok) return reply(500, { error: "Invitation sent, but audit recording failed. Inspect the account before retrying." });
    return reply(200, { id: created.id, invited: true, role: "CITIZEN" });
  } catch {
    return reply(500, { error: "Unable to invite the account" });
  }
});
