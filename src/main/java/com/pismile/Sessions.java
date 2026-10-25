package com.pismile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pages de paiement pismile.com : le bouton « Payer avec Alias PI-SPI ». Le payeur n'a besoin d'aucun compte ;
 * il peut répartir le montant sur plusieurs comptes. Redirigez-le vers {@link SessionPaiement#url()}.
 */
public final class Sessions {

    private static final String CHEMIN = "/api/v1/sessions-paiement";

    private final Http http;

    Sessions(Http http) {
        this.http = http;
    }

    /** Crée la page de paiement (ou renvoie la même pour la même référence). */
    public SessionPaiement creer(NouvelleSession s) {
        if (s.montant() <= 0) {
            throw new PismileException("montant_invalide", "montant : entier positif en F CFA.");
        }
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("montant", s.montant());
        corps.put("motif", s.motif());
        corps.put("reference", s.reference());
        corps.put("retour", s.retour());
        corps.put("annulation", s.annulation());
        return SessionPaiement.depuis(http.appeler("POST", CHEMIN, corps));
    }

    /** Raccourci : {@code creer(NouvelleSession.de(montant, reference, retour).motif(motif))}. */
    public SessionPaiement creer(long montant, String reference, String retour, String motif) {
        return creer(NouvelleSession.de(montant, reference, retour).motif(motif));
    }

    public SessionPaiement lire(String id) {
        return SessionPaiement.depuis(http.appeler("GET", CHEMIN + "/" + encoder(id), null));
    }

    public SessionPaiement lireParReference(String reference) {
        return SessionPaiement.depuis(http.appeler("GET", CHEMIN + "?reference=" + encoder(reference), null));
    }

    static String encoder(String valeur) {
        return URLEncoder.encode(valeur == null ? "" : valeur, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
