package com.pismile;

import java.time.Instant;
import java.util.Map;

/**
 * Un webhook pismile, signature vérifiée.
 * <p>
 * Types : {@code session.payee}, {@code session.expiree} (pages de paiement) ; {@code paiement.paye},
 * {@code paiement.refuse}, {@code paiement.annule}, {@code paiement.expire} (demandes aux utilisateurs connectés) ;
 * {@code test} (bouton de la console). Un même événement peut arriver deux fois : dédoublonnez avec {@link #id()}.
 */
public record Evenement(String id, String type, Instant creeLe, boolean test, Map<String, Object> donnees) {

    public boolean estSessionPayee() {
        return "session.payee".equals(type);
    }

    public boolean estPaiementPaye() {
        return "paiement.paye".equals(type);
    }

    /** Payé, quelle que soit l'origine (page de paiement ou demande à un utilisateur). */
    public boolean estPaye() {
        return estSessionPayee() || estPaiementPaye();
    }

    /** Votre référence (« cmd-42 ») ; null pour un événement de test. */
    public String reference() {
        return Json.texte(donnees, "reference");
    }

    /** Les données en page de paiement (événements session.*) ; null sinon. */
    public SessionPaiement session() {
        return type != null && type.startsWith("session.") ? SessionPaiement.depuis(donnees) : null;
    }

    /** Les données en paiement (événements paiement.*) ; null sinon. */
    public Paiement paiement() {
        return type != null && type.startsWith("paiement.") ? Paiement.depuis(donnees) : null;
    }

    static Evenement depuis(Map<String, Object> m) {
        return new Evenement(Json.texte(m, "id"), Json.texte(m, "type"), Dates.lire(m, "creeLe"),
                Json.booleen(m, "test"), Json.sousObjet(m, "donnees"));
    }
}
