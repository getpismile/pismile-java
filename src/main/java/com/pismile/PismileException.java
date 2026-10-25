package com.pismile;

/**
 * Erreur pismile : {@link #getCode()} est stable (à tester dans votre code), le message est lisible.
 * <p>
 * Codes de l'API : {@code authentification}, {@code autorisation_absente}, {@code compte_absent},
 * {@code montant_invalide}, {@code reference_invalide}, {@code reference_deja_utilisee}, {@code motif_invalide},
 * {@code retour_invalide}, {@code retour_domaine_non_verifie}, {@code module_encaissement_requis},
 * {@code acces_participant_non_verifie}, {@code organisation_non_verifiee}, {@code quota_atteint},
 * {@code trop_de_demandes_en_attente}, {@code limite_journaliere}, {@code participant_refus}, {@code session_introuvable},
 * {@code paiement_introuvable}…
 * Codes du SDK : {@code reseau}, {@code delai_depasse}, {@code reponse_invalide}, {@code configuration},
 * {@code requete_invalide}, {@code signature_invalide}, {@code signature_expiree}.
 */
public class PismileException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;
    private final int statut;

    public PismileException(String code, String message, int statut) {
        super(message);
        this.code = code;
        this.statut = statut;
    }

    PismileException(String code, String message) {
        this(code, message, 0);
    }

    /** Code stable, par exemple « autorisation_absente ». */
    public String getCode() {
        return code;
    }

    /** Statut HTTP de la réponse de pismile (0 si aucune réponse). */
    public int getStatut() {
        return statut;
    }
}
