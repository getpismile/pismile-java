package com.pismile;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PismileTest {

    private static final String SESSION = """
            {"id":"cs_abc","url":"https://pismile.test/payer/cs_abc","reference":"cmd-42","montant":25000,
             "devise":"XOF","motif":"Boutique - Commande 42","statut":"OUVERTE","montantPaye":0,
             "creeLe":"2026-10-25T10:00:00.123456Z","expireLe":"2026-10-25T10:30:00Z","test":true}""";

    private static Pismile client(FauxTransport t, int essais) {
        return Pismile.builder().clientId("psm_test_abc").clientSecret("pss:secret").issuer("https://pismile.test/")
                .transport(t).essais(essais).pause(Duration.ofMillis(1)).build();
    }

    @Test
    void creeUnePageDePaiementAuthentifiee() {
        FauxTransport t = new FauxTransport().repondre(201, SESSION);
        SessionPaiement s = client(t, 2).sessions().creer(25000, "cmd-42", "https://site.com/merci", "Commande 42");

        assertEquals("https://pismile.test/payer/cs_abc", s.url());
        assertEquals(25000, s.montant());
        assertFalse(s.estPayee());
        FauxTransport.Requete r = t.requetes.get(0);
        assertEquals("POST", r.methode());
        assertEquals("https://pismile.test/api/v1/sessions-paiement", r.url());
        assertEquals("Basic " + Base64.getEncoder().encodeToString("psm_test_abc:pss:secret".getBytes(StandardCharsets.UTF_8)),
                r.entetes().get("Authorization"));
        assertEquals("{\"montant\":25000,\"motif\":\"Commande 42\",\"reference\":\"cmd-42\",\"retour\":\"https://site.com/merci\"}",
                r.corps());
    }

    @Test
    void transmetLeCodeDErreurSansReessayer() {
        FauxTransport t = new FauxTransport()
                .repondre(400, "{\"erreur\":\"retour_domaine_non_verifie\",\"message\":\"Domaine non vérifié.\"}");
        PismileException e = assertThrows(PismileException.class,
                () -> client(t, 2).sessions().creer(100, "r1", "https://autre.com", null));

        assertEquals("retour_domaine_non_verifie", e.getCode());
        assertEquals(400, e.getStatut());
        assertEquals(1, t.requetes.size());
    }

    @Test
    void reessaieApresUneErreurTemporaire() {
        FauxTransport t = new FauxTransport().repondre(503, "").repondre(200, SESSION);
        SessionPaiement s = client(t, 1).sessions().lire("cs_abc");

        assertEquals("cs_abc", s.id());
        assertEquals(2, t.requetes.size());
    }

    @Test
    void signaleUneCoupureReseauApresLesEssais() {
        FauxTransport t = new FauxTransport().couper().couper();
        PismileException e = assertThrows(PismileException.class, () -> client(t, 1).sessions().lire("cs_abc"));

        assertEquals("reseau", e.getCode());
        assertEquals(2, t.requetes.size());
    }

    @Test
    void lireParReferenceEncodeLaReference() {
        FauxTransport t = new FauxTransport().repondre(200, SESSION);
        client(t, 0).sessions().lireParReference("cmd:42 a");

        assertEquals("https://pismile.test/api/v1/sessions-paiement?reference=cmd%3A42%20a", t.requetes.get(0).url());
    }

    @Test
    void demandeUnPaiementAUnUtilisateurConnecte() {
        FauxTransport t = new FauxTransport().repondre(201, """
                {"id":"p1","reference":"cmd-7","sub":"kX9","montant":5000,"devise":"XOF","statut":"EN_ATTENTE",
                 "compte":"Wave · +228 •••• 12","creeLe":"2026-10-25T10:00:00Z","expireLe":"2026-10-26T10:00:00Z","test":false}""");
        Paiement p = client(t, 0).paiements().creer("kX9", 5000, "cmd-7", null);

        assertEquals("EN_ATTENTE", p.statut());
        assertEquals("Wave · +228 •••• 12", p.compte());
        assertEquals("{\"sub\":\"kX9\",\"montant\":5000,\"reference\":\"cmd-7\"}", t.requetes.get(0).corps());
    }

    @Test
    void refuseUnMontantInvalideAvantToutAppel() {
        FauxTransport t = new FauxTransport();
        PismileException e = assertThrows(PismileException.class,
                () -> client(t, 0).sessions().creer(0, "r", "https://site.com", null));
        assertEquals("montant_invalide", e.getCode());
        assertTrue(t.requetes.isEmpty());
    }

    @Test
    void exigeLesIdentifiants() {
        PismileException e = assertThrows(PismileException.class, () -> new Pismile("", "x"));
        assertEquals("configuration", e.getCode());
    }

    @Test
    void reponseIllisible() {
        FauxTransport t = new FauxTransport().repondre(502, "<html>Bad gateway</html>");
        PismileException e = assertThrows(PismileException.class, () -> client(t, 0).sessions().lire("x"));
        assertEquals("reponse_invalide", e.getCode());
        assertNull(Map.of().get("x"));
    }
}
