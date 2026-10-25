package com.pismile;

import java.io.IOException;
import java.util.Map;

/**
 * Envoi HTTP. Par défaut, le {@code java.net.http.HttpClient} du JDK ; remplaçable (tests, proxy d'entreprise…).
 */
@FunctionalInterface
public interface Transport {

    /** Réponse brute : statut HTTP et corps (texte, éventuellement vide). */
    record Reponse(int statut, String corps) {
    }

    /**
     * @throws IOException coupure réseau (le SDK réessaie)
     * @throws java.net.http.HttpTimeoutException délai dépassé (le SDK réessaie)
     */
    Reponse envoyer(String methode, String url, Map<String, String> entetes, String corps)
            throws IOException, InterruptedException;
}
