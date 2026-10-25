package com.pismile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Demandes de paiement aux utilisateurs connectés avec « Se connecter avec pismile » (scope pismile:payment) :
 * l'utilisateur valide dans sa banque, l'argent arrive sur le compte de votre organisation.
 */
public final class Paiements {

    private static final String CHEMIN = "/api/v1/paiements";

    private final Http http;

    Paiements(Http http) {
        this.http = http;
    }

    /**
     * @param sub       identifiant de l'utilisateur reçu à la connexion
     * @param montant   entier en F CFA
     * @param reference votre référence, unique : la renvoyer ne crée jamais un second paiement
     * @param motif     facultatif (null), 100 caractères au plus
     */
    public Paiement creer(String sub, long montant, String reference, String motif) {
        if (sub == null || sub.isBlank() || reference == null || reference.isBlank()) {
            throw new PismileException("requete_invalide", "sub et reference sont obligatoires.");
        }
        if (montant <= 0) {
            throw new PismileException("montant_invalide", "montant : entier positif en F CFA.");
        }
        Map<String, Object> corps = new LinkedHashMap<>();
        corps.put("sub", sub);
        corps.put("montant", montant);
        corps.put("motif", motif);
        corps.put("reference", reference);
        return Paiement.depuis(http.appeler("POST", CHEMIN, corps));
    }

    public Paiement lire(String id) {
        return Paiement.depuis(http.appeler("GET", CHEMIN + "/" + Sessions.encoder(id), null));
    }

    public Paiement lireParReference(String reference) {
        return Paiement.depuis(http.appeler("GET", CHEMIN + "?reference=" + Sessions.encoder(reference), null));
    }
}
