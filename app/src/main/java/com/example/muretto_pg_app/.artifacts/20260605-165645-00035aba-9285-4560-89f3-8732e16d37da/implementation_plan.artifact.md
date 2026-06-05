# Refactoring Registrazione per Schema Lineare

Adattamento dell'applicazione a un sistema di registrazione diretto (senza Edge Functions) basato esclusivamente sulla tabella `profili`.

## Proposta di Modifica

### ViewModel e Logica Dati ([Modelli.kt](file:///C:/Users/aless/Documents/GitHub/Muretto_PG_Battle_Roster/app/src/main/java/com/example/muretto_pg_app/Modelli.kt))

- **Rimozione `RichiestaAccount`**: La classe non è più necessaria in quanto i dati vengono scritti direttamente in `ProfiloUtente`.
- **Nuovo Metodo `eseguiRegistrazioneDiretta`**:
    - Esegue `supabase.auth.signUpWith(Email)` per creare l'utente.
    - Se l'Auth ha successo, inserisce immediatamente il record nella tabella `profili` con `statoRichiesta = false` (o `true` per i rapper).
- **Rimozione `eseguiRegistrazioneSicura` e `inviaRichiestaAccount`**: Eliminate tutte le chiamate a Edge Functions.
- **Aggiornamento `rifiutaRichiesta`**: Ora deve eliminare il record da `profili`. (Nota: L'eliminazione dall'Auth di Supabase lato client richiede solitamente privilegi admin o una funzione specifica, se l'admin non può farlo direttamente, informerò l'utente).

### Interfaccia Utente ([SchermataRegistrazione.kt](file:///C:/Users/aless/Documents/GitHub/Muretto_PG_Battle_Roster/app/src/main/java/com/example/muretto_pg_app/SchermataRegistrazione.kt))

- Aggiornamento della chiamata al ViewModel per usare il nuovo flusso diretto.
- Rimozione di riferimenti a password temporanee criptate (Supabase gestisce l'auth internamente).

---

## Piano di Verifica

### Verifiche Manuali
- **Test Registrazione Diretta**: Creare un nuovo account e verificare tramite console Supabase che compaia sia in `Auth.users` che in `public.profili`.
- **Test Approvazione Admin**: Verificare che l'admin veda la richiesta e che cliccando su "Accetta" il campo `statoRichiesta` in `profili` diventi `true`.
- **Test Accesso Negato**: Verificare che un utente con `statoRichiesta = false` non possa accedere alle aree riservate dopo il login.
