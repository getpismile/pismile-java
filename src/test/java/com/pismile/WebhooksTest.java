package com.pismile;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhooksTest {

    private static final String SECRET = "whsec_test";
    private static final String CORPS = """
            {"id":"evt_1","type":"session.payee","creeLe":"2026-10-25T10:00:00Z","test":false,
             "donnees":{"id":"cs_abc","url":"https://pismile.com/payer/cs_abc","reference":"cmd-42","montant":25000,
             "devise":"XOF","motif":"Commande 42","statut":"PAYEE","montantPaye":25000,
             "creeLe":"2026-10-25T09:50:00Z","expireLe":"2026-10-25T10:20:00Z","payeLe":"2026-10-25T10:00:00Z","test":false}}""";

    @Test
    void accepteLaSignatureDuServeurPismile() {
        // Produite par le serveur pismile : SignatureWebhook.entete("whsec_test", 1700000000, "{\"id\":\"evt_1\"}")
        String entete = "t=1700000000,v1=c89214b5b5da833daed6f0b8c5bb6bd58cea9022bd80ccc78230f3942d632925";
        Evenement e = Webhooks.verifier("{\"id\":\"evt_1\"}", entete, SECRET, Instant.ofEpochSecond(1700000010));
        assertEquals("evt_1", e.id());
    }

    @Test
    void litUnePagePayee() {
        long t = Instant.now().getEpochSecond();
        Pismile pismile = Pismile.builder().clientId("a").clientSecret("b").webhookSecret(SECRET).build();
        Evenement e = pismile.webhooks().verifier(CORPS, Webhooks.signer(CORPS, SECRET, t));

        assertTrue(e.estSessionPayee());
        assertTrue(e.estPaye());
        assertEquals("cmd-42", e.reference());
        assertEquals(25000, e.session().montantPaye());
        assertTrue(e.session().estPayee());
        assertNull(e.paiement());
    }

    @Test
    void refuseUnMauvaisSecret() {
        long t = Instant.now().getEpochSecond();
        PismileException x = assertThrows(PismileException.class,
                () -> Webhooks.verifier(CORPS, Webhooks.signer(CORPS, "whsec_autre", t), SECRET, Instant.now()));
        assertEquals("signature_invalide", x.getCode());
    }

    @Test
    void refuseUnCorpsModifie() {
        long t = Instant.now().getEpochSecond();
        String entete = Webhooks.signer(CORPS, SECRET, t);
        PismileException x = assertThrows(PismileException.class,
                () -> Webhooks.verifier(CORPS.replace("25000", "250"), entete, SECRET, Instant.now()));
        assertEquals("signature_invalide", x.getCode());
    }

    @Test
    void refuseUnWebhookTropAncien() {
        String entete = Webhooks.signer(CORPS, SECRET, 1000);
        PismileException x = assertThrows(PismileException.class,
                () -> Webhooks.verifier(CORPS, entete, SECRET, Instant.ofEpochSecond(1000 + 301)));
        assertEquals("signature_expiree", x.getCode());
    }

    @Test
    void refuseUnEnteteAbsentOuIllisible() {
        assertEquals("signature_invalide", assertThrows(PismileException.class,
                () -> Webhooks.verifier(CORPS, null, SECRET, Instant.now())).getCode());
        assertEquals("signature_invalide", assertThrows(PismileException.class,
                () -> Webhooks.verifier(CORPS, "n'importe quoi", SECRET, Instant.now())).getCode());
    }

    @Test
    void exigeLeSecret() {
        Pismile pismile = new Pismile("a", "b");
        assertEquals("configuration", assertThrows(PismileException.class,
                () -> pismile.webhooks().verifier(CORPS, "t=1,v1=00")).getCode());
    }
}
