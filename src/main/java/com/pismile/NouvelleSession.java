package com.pismile;

import java.util.Objects;

/**
 * Une page de paiement à créer.
 *
 * <pre>{@code
 * NouvelleSession.de(25000, "cmd-42", "https://site.com/paiement/merci")
 *         .motif("Commande 42")
 *         .annulation("https://site.com/panier");
 * }</pre>
 *
 * @see Sessions#creer(NouvelleSession)
 */
public final class NouvelleSession {

    private final long montant;
    private final String reference;
    private final String retour;
    private String motif;
    private String annulation;

    private NouvelleSession(long montant, String reference, String retour) {
        this.montant = montant;
        this.reference = reference;
        this.retour = retour;
    }

    /**
     * @param montant   montant entier en F CFA
     * @param reference votre référence, unique (1 à 64 caractères : lettres, chiffres, . _ : -) ; la renvoyer
     *                  renvoie la même page, jamais un second paiement
     * @param retour    adresse HTTPS de retour, sur un domaine vérifié de votre organisation (console → Domaines) ;
     *                  reçoit {@code ?pismile_session=cs_…&statut=…}
     */
    public static NouvelleSession de(long montant, String reference, String retour) {
        return new NouvelleSession(montant, Objects.requireNonNull(reference, "reference"),
                Objects.requireNonNull(retour, "retour"));
    }

    /** Facultatif, 100 caractères au plus : affiché sur la page et dans la banque du payeur. */
    public NouvelleSession motif(String motif) {
        this.motif = motif;
        return this;
    }

    /** Facultatif : adresse si le payeur abandonne (défaut : retour). */
    public NouvelleSession annulation(String annulation) {
        this.annulation = annulation;
        return this;
    }

    long montant() {
        return montant;
    }

    String reference() {
        return reference;
    }

    String retour() {
        return retour;
    }

    String motif() {
        return motif;
    }

    String annulation() {
        return annulation;
    }
}
