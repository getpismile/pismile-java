package com.pismile;

import java.time.Instant;
import java.util.Map;

/**
 * Une page de paiement pismile.com (bouton « Payer avec Alias PI-SPI »).
 *
 * @param id          identifiant « cs_… »
 * @param url         page où envoyer le payeur : https://pismile.com/payer/cs_…
 * @param statut      OUVERTE, PAYEE, EXPIREE ou ANNULEE
 * @param montantPaye déjà payé : une page expirée peut avoir été payée en partie (plusieurs comptes)
 */
public record SessionPaiement(String id, String url, String reference, long montant, String devise, String motif,
                              String statut, long montantPaye, Instant creeLe, Instant expireLe, Instant payeLe,
                              boolean test) {

    public boolean estPayee() {
        return "PAYEE".equals(statut);
    }

    static SessionPaiement depuis(Map<String, Object> m) {
        return new SessionPaiement(Json.texte(m, "id"), Json.texte(m, "url"), Json.texte(m, "reference"),
                Json.entier(m, "montant"), Json.texte(m, "devise"), Json.texte(m, "motif"), Json.texte(m, "statut"),
                Json.entier(m, "montantPaye"), Dates.lire(m, "creeLe"), Dates.lire(m, "expireLe"),
                Dates.lire(m, "payeLe"), Json.booleen(m, "test"));
    }
}
