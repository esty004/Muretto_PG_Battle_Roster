import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.3"

// Regole di sicurezza per permettere all'app di parlare con questo script
const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
}

serve(async (req) => {
  // 1. Risponde al "pre-controllo" di sicurezza del telefono
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  try {
        // 2. RECUPERA LA CHIAVE "GOD MODE" IN SEGRETO
        // Lascia scritte queste etichette ESATTAMENTE così:
        const supabaseAdmin = createClient(
          Deno.env.get('SUPABASE_URL') ?? '',
          Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? ''
        )

    // 3. LEGGE I DATI INVIATI DALLA TUA APP
    const datiRichiesta = await req.json()
    const { email, password, nome, cognome, nome_arte, telefono, tipo_account } = datiRichiesta

    // 4. CREA L'ACCOUNT BYPASSANDO I BLOCCHI RLS
    const { data: userAuth, error: authError } = await supabaseAdmin.auth.admin.createUser({
      email: email,
      password: password,
      email_confirm: true // Conferma la mail in automatico!
    })

    if (authError) throw authError
    const nuovoUserId = userAuth.user.id

    // 5. INSERISCE IL NUOVO UTENTE NELLA TABELLA PROFILI
    const { error: profileError } = await supabaseAdmin
      .from('profili')
      .insert([
        {
          id: nuovoUserId,
          ruolo: tipo_account, // Assicurati che il nome della colonna sia 'ruolo' nel tuo DB
          nome: nome,
          cognome: cognome,
          nome_arte: nome_arte,
          telefono: telefono
        }
      ])

    if (profileError) throw profileError

    // 6. TUTTO OK! MANDA IL SEGNALE DI SUCCESSO ALL'APP
    return new Response(
      JSON.stringify({ success: true, message: "Utente creato con successo!" }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 200 }
    )

  } catch (error) {
    // 7. OPS, ERRORE! (Es. mail già usata)
    return new Response(
      JSON.stringify({ error: error.message }),
      { headers: { ...corsHeaders, 'Content-Type': 'application/json' }, status: 400 }
    )
  }
})