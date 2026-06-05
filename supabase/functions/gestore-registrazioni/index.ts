// supabase/functions/gestore-registrazioni/index.ts  (AGGIORNATO)
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3"

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

const RUOLI_AUTO_AMMESSI = ['rapper', 'organizzatore_muretto', 'organizzatore_eventi'] as const
type RuoloAuto = typeof RUOLI_AUTO_AMMESSI[number]

async function decrypt(encryptedText: string, secretKey: string) {
  const key = new TextEncoder().encode(secretKey);
  const encryptedData = Uint8Array.from(atob(encryptedText), c => c.charCodeAt(0));
  const cryptoKey = await crypto.subtle.importKey("raw", key, { name: "AES-CBC" }, false, ["decrypt"]);
  const iv = new Uint8Array(16).fill(0);
  const decrypted = await crypto.subtle.decrypt({ name: "AES-CBC", iv: iv }, cryptoKey, encryptedData);
  return new TextDecoder().decode(decrypted);
}

serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response('ok', { headers: corsHeaders })

  try {
    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
    )

    const payload = await req.json()
    const { email, password_temp, nome, cognome, nome_arte, telefono, tipo_account, muretto_id } = payload

    const CHIAVE_SEGRETA = "MurettoPG_Secret";
    let passwordFinale: string;
    try {
      passwordFinale = await decrypt(password_temp, CHIAVE_SEGRETA);
    } catch (e) {
      console.error("Errore decrittazione:", e);
      throw new Error("Errore di sicurezza: Password non valida o chiave errata");
    }

    if (!RUOLI_AUTO_AMMESSI.includes(tipo_account as RuoloAuto)) {
      throw new Error(`Ruolo '${tipo_account}' non autorizzato per l'auto-registrazione.`);
    }

    // 3. Creazione Utente Auth
    const { data: userAuth, error: authError } = await supabaseAdmin.auth.admin.createUser({
      email: email.trim(),
      password: passwordFinale,
      email_confirm: true,
      user_metadata: { nome_arte: nome_arte.trim() }
    })
    if (authError) throw authError
    const nuovoUserId = userAuth.user.id

    // 4. Inserimento Profilo
    //    NOVITÀ: 'email' e 'statoRichiesta' sono NOT NULL.
    //    statoRichiesta = true  -> account APPROVATO/attivo (i rapper entrano subito)
    //    statoRichiesta = false -> in attesa di approvazione admin (gli organizzatori)
    const approvatoSubito = (tipo_account === 'rapper')

    const { error: profileError } = await supabaseAdmin
      .from('profili')
      .insert([{
        id: nuovoUserId,
        tipo_account: tipo_account as RuoloAuto,
        nome: nome.trim(),
        cognome: cognome.trim(),
        nome_arte: nome_arte.trim(),
        telefono: telefono.trim(),
        email: email.trim(),
        muretto_id: muretto_id,
        statoRichiesta: approvatoSubito
      }])

    if (profileError) {
      await supabaseAdmin.auth.admin.deleteUser(nuovoUserId) // Rollback
      throw profileError
    }

    return new Response(
      JSON.stringify({ success: true, message: "Account creato con successo", in_attesa: !approvatoSubito }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    console.error("Errore gestore-registrazioni:", error)
    return new Response(
      JSON.stringify({ error: error.message }),
      { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})