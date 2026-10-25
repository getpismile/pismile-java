package com.pismile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Appels authentifiés à l'API pismile, avec nouveaux essais sur les erreurs temporaires. */
final class Http {

    /** Statuts temporaires : un nouvel essai a des chances de réussir (la référence évite tout double paiement). */
    private static final Set<Integer> TEMPORAIRES = Set.of(500, 503, 504);
    static final String VERSION = "pismile-java/0.1.0";

    private final String base;
    private final String autorisation;
    private final Transport transport;
    private final int essais;
    private final Duration pause;

    Http(String base, String clientId, String clientSecret, Transport transport, int essais, Duration pause) {
        this.base = base;
        this.autorisation = "Basic " + Base64.getEncoder()
                .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        this.transport = transport;
        this.essais = essais;
        this.pause = pause;
    }

    Map<String, Object> appeler(String methode, String chemin, Map<String, Object> corps) {
        PismileException derniere = null;
        for (int essai = 0; essai <= essais; essai++) {
            if (essai > 0) {
                attendre(pause.multipliedBy((long) Math.pow(3, essai - 1)));
            }
            try {
                return une(methode, chemin, corps);
            } catch (PismileException e) {
                derniere = e;
                boolean temporaire = e.getCode().equals("reseau") || e.getCode().equals("delai_depasse")
                        || TEMPORAIRES.contains(e.getStatut());
                if (!temporaire) {
                    throw e;
                }
            }
        }
        throw derniere;
    }

    private Map<String, Object> une(String methode, String chemin, Map<String, Object> corps) {
        Map<String, String> entetes = new LinkedHashMap<>();
        entetes.put("Authorization", autorisation);
        entetes.put("Accept", "application/json");
        entetes.put("User-Agent", VERSION);
        String texte = null;
        if (corps != null) {
            entetes.put("Content-Type", "application/json; charset=utf-8");
            texte = Json.ecrire(corps);
        }
        Transport.Reponse r;
        try {
            r = transport.envoyer(methode, base + chemin, entetes, texte);
        } catch (HttpTimeoutException e) {
            throw new PismileException("delai_depasse", "pismile n'a pas répondu à temps.");
        } catch (IOException e) {
            throw new PismileException("reseau", "pismile injoignable : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PismileException("reseau", "Appel interrompu.");
        }
        Map<String, Object> donnees = Map.of();
        if (r.corps() != null && !r.corps().isBlank()) {
            try {
                donnees = Json.objet(r.corps());
            } catch (IllegalArgumentException e) {
                throw new PismileException("reponse_invalide", "Réponse inattendue (HTTP " + r.statut() + ").",
                        r.statut());
            }
        }
        if (r.statut() < 200 || r.statut() >= 300) {
            String code = Json.texte(donnees, "erreur");
            String message = Json.texte(donnees, "message");
            throw new PismileException(code != null ? code : "http_" + r.statut(),
                    message != null ? message : "Erreur HTTP " + r.statut() + ".", r.statut());
        }
        return donnees;
    }

    private static void attendre(Duration duree) {
        try {
            Thread.sleep(duree.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Transport par défaut : HttpClient du JDK, sans suivre les redirections. */
    static Transport jdk(Duration delai) {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        return (methode, url, entetes, corps) -> {
            HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(url)).timeout(delai);
            entetes.forEach(b::header);
            b.method(methode, corps == null ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(corps, StandardCharsets.UTF_8));
            HttpResponse<String> reponse = client.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return new Transport.Reponse(reponse.statusCode(), reponse.body());
        };
    }
}
