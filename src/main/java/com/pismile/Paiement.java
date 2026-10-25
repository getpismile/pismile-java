package com.pismile;

import java.time.Instant;
import java.util.Map;

/**
 * Une demande de paiement envoyée à un utilisateur « Se connecter avec pismile » (scope pismile:payment).
 *
 * @param statut EN_ATTENTE, PAYE, REFUSE, ANNULE, EXPIRE ou ECHEC
 * @param compte compte du payeur, masqué (« Wave · +228 •••• 12 »)
 */
public record Paiement(String id, String reference, String sub, long montant, String devise, String motif,
                       String statut, String compte, Instant creeLe, Instant expireLe, Instant payeLe, String raison,
                       boolean test) {

    public boolean estPaye() {
        return "PAYE".equals(statut);
    }

    static Paiement depuis(Map<String, Object> m) {
        return new Paiement(Json.texte(m, "id"), Json.texte(m, "reference"), Json.texte(m, "sub"),
                Json.entier(m, "montant"), Json.texte(m, "devise"), Json.texte(m, "motif"), Json.texte(m, "statut"),
                Json.texte(m, "compte"), Dates.lire(m, "creeLe"), Dates.lire(m, "expireLe"), Dates.lire(m, "payeLe"),
                Json.texte(m, "raison"), Json.booleen(m, "test"));
    }
}
