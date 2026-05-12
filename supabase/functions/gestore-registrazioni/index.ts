// supabase/functions/gestore-registrazioni/index.ts
import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3"

const corsHeaders = {
  // In produzione restringi a un dominio specifico, non '*'
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

// LISTA BIANCA dei ruoli che il client può richiedere autonomamente.
// 'admin' NON è in questa lista: si diventa admin solo manualmente dal dashboard.
const RUOLI_AUTO_AMMESSI = ['rapper'] as const
type RuoloAuto = typeof RUOLI_AUTO_AMMESSI[number]

// Validazione email server-side (non fidarsi del client)
function emailValida(s: unknown): s is string {
  return typeof s === 'string' && /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(s)
}

// Validazione lunghezze - rifiuta input troppo grandi che potrebbero
// essere tentativi di DoS o di iniezione di metadati pesanti
function stringaValida(s: unknown, max: number): s is string {
  return typeof s === 'string' && s.length > 0 && s.length <= max
}

serve(async (req) => {
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  // Solo POST: rifiuta GET/PUT/DELETE/PATCH a priori
  if (req.method !== 'POST') {
    return new Response(JSON.stringify({ error: "Metodo non consentito" }), {
      status: 405,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' }
    })
  }

  try {
    // Parse difensivo: se il body non è JSON valido, errore subito
    let dati: any
    try {
      dati = await req.json()
    } catch {
      return new Response(JSON.stringify({ error: "Body non valido" }), {
        status: 400,
        headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    const { email, password, nome, cognome, nome_arte, telefono, tipo_account } = dati

    // VALIDAZIONE INPUT SERVER-SIDE - non fidarsi mai del client
    if (!emailValida(email)) {
      return new Response(JSON.stringify({ error: "Email non valida" }), {
        status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }
    if (!stringaValida(password, 200) || password.length < 8) {
      return new Response(JSON.stringify({ error: "Password troppo corta o non valida" }), {
        status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }
    if (!stringaValida(nome, 80) || !stringaValida(cognome, 80) ||
        !stringaValida(nome_arte, 50) || !stringaValida(telefono, 30)) {
      return new Response(JSON.stringify({ error: "Campi anagrafici non validi" }), {
        status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' }
      })
    }

    // CONTROLLO CRITICO: il ruolo richiesto deve essere nella lista bianca.
    // Se un attaccante tenta tipo_account='admin', qui veniamo rifiutati.
    if (!RUOLI_AUTO_AMMESSI.includes(tipo_account as RuoloAuto)) {
      // Se il ruolo richiesto NON è auto-ammesso (es. organizzatore, admin),
      // il flusso corretto è inserire una RICHIESTA in richieste_account,
      // non creare direttamente l'account. Qui rifiutiamo.
      return new Response(
        JSON.stringify({ error: "Tipo account non disponibile per registrazione diretta. Usa il flusso di richiesta." }),
        { status: 403, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    const supabaseAdmin = createClient(
      Deno.env.get('SUPABASE_URL') ?? '',
      Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
      { auth: { persistSession: false } }  // Edge Function non deve persistere sessioni
    )

    // Crea l'utente in auth.users
    const { data: userAuth, error: authError } = await supabaseAdmin.auth.admin.createUser({
      email: email.trim().toLowerCase(),
      password: password,
      email_confirm: true,
      user_metadata: { nome_arte: nome_arte.trim() }  // metadati visibili all'utente, ma non modificano ruolo
    })

    if (authError) throw authError
    const nuovoUserId = userAuth.user.id

    // Inserisce il profilo FORZANDO il ruolo a quello validato dalla whitelist,
    // NON usando direttamente tipo_account dal body.
    // Anche se l'attaccante manipola il payload, qui scriviamo solo 'rapper'.
    const ruoloDaScrivere: RuoloAuto = tipo_account as RuoloAuto

    const { error: profileError } = await supabaseAdmin
      .from('profili')
      .insert([{
        id: nuovoUserId,
        ruolo: ruoloDaScrivere,
        nome: nome.trim(),
        cognome: cognome.trim(),
        nome_arte: nome_arte.trim(),
        telefono: telefono.trim()
      }])

    if (profileError) {
      // ROLLBACK: se il profilo fallisce, cancella l'utente auth appena creato
      // altrimenti rimane un account "orfano" senza profilo
      await supabaseAdmin.auth.admin.deleteUser(nuovoUserId)
      throw profileError
    }

    return new Response(
      JSON.stringify({ success: true, message: "Account creato con successo" }),
      { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )

  } catch (error) {
    // Non esporre dettagli interni nell'errore (information disclosure)
    console.error("Errore gestore-registrazioni:", error)
    const messaggioPubblico = (error as any)?.message?.includes('already')
      ? "Email già registrata"
      : "Errore durante la registrazione"
    return new Response(
      JSON.stringify({ error: messaggioPubblico }),
      { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
})