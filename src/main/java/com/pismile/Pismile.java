package com.pismile;

import java.time.Duration;
import java.util.Objects;

/**
 * Client pismile côté serveur. Sans dépendance ; sûr entre plusieurs threads (créez-en un seul, par exemple un bean).
 *
 * <pre>{@code
 * Pismile pismile = Pismile.depuisEnvironnement();   // PISMILE_CLIENT_ID, PISMILE_CLIENT_SECRET…
 *
 * SessionPaiement page = pismile.sessions().creer(25000, "cmd-42", "https://site.com/merci", "Commande 42");
 * return "redirect:" + page.url();
 * }</pre>
 */
public final class Pismile {

    private final Sessions sessions;
    private final Paiements paiements;
    private final Webhooks webhooks;

    private Pismile(Builder b) {
        Http http = new Http(b.issuer.replaceAll("/+$", ""), b.clientId, b.clientSecret,
                b.transport != null ? b.transport : Http.jdk(b.delai), b.essais, b.pause);
        this.sessions = new Sessions(http);
        this.paiements = new Paiements(http);
        this.webhooks = new Webhooks(b.webhookSecret);
    }

    /** Avec le client_id et le client_secret de votre application (console pismile). */
    public Pismile(String clientId, String clientSecret) {
        this(builder().clientId(clientId).clientSecret(clientSecret).verifier());
    }

    /**
     * Configuration par variables d'environnement : PISMILE_CLIENT_ID, PISMILE_CLIENT_SECRET, PISMILE_WEBHOOK_SECRET,
     * PISMILE_ISSUER (défaut https://pismile.com). AUTH_PISMILE_ID / AUTH_PISMILE_SECRET sont aussi acceptées.
     */
    public static Pismile depuisEnvironnement() {
        Builder b = builder()
                .clientId(env("PISMILE_CLIENT_ID", "AUTH_PISMILE_ID"))
                .clientSecret(env("PISMILE_CLIENT_SECRET", "AUTH_PISMILE_SECRET"))
                .webhookSecret(env("PISMILE_WEBHOOK_SECRET", null));
        String issuer = env("PISMILE_ISSUER", "AUTH_PISMILE_ISSUER");
        if (issuer != null) {
            b.issuer(issuer);
        }
        return b.build();
    }

    public static Builder builder() {
        return new Builder();
    }

    /** Pages de paiement pismile.com (bouton « Payer avec Alias PI-SPI »). */
    public Sessions sessions() {
        return sessions;
    }

    /** Demandes de paiement aux utilisateurs connectés (scope pismile:payment). */
    public Paiements paiements() {
        return paiements;
    }

    /** Vérification des webhooks avec le secret configuré. */
    public Webhooks webhooks() {
        return webhooks;
    }

    private static String env(String nom, String alternative) {
        String v = System.getenv(nom);
        if ((v == null || v.isBlank()) && alternative != null) {
            v = System.getenv(alternative);
        }
        return v == null || v.isBlank() ? null : v;
    }

    /** Configuration détaillée. */
    public static final class Builder {
        private String clientId;
        private String clientSecret;
        private String webhookSecret;
        private String issuer = "https://pismile.com";
        private Duration delai = Duration.ofSeconds(15);
        private int essais = 2;
        private Duration pause = Duration.ofMillis(400);
        private Transport transport;

        private Builder() {
        }

        public Builder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        /** Ne jamais l'exposer à un navigateur ni à une appli mobile. */
        public Builder clientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
            return this;
        }

        /** Secret des webhooks (whsec_…), affiché une fois dans la console, page Paiements de l'application. */
        public Builder webhookSecret(String webhookSecret) {
            this.webhookSecret = webhookSecret;
            return this;
        }

        /** Adresse de pismile (défaut https://pismile.com). */
        public Builder issuer(String issuer) {
            this.issuer = Objects.requireNonNull(issuer, "issuer");
            return this;
        }

        /** Délai maximal d'un appel (défaut 15 s). */
        public Builder delai(Duration delai) {
            this.delai = Objects.requireNonNull(delai, "delai");
            return this;
        }

        /** Nouveaux essais après une coupure réseau ou une erreur serveur temporaire (défaut 2). */
        public Builder essais(int essais) {
            this.essais = Math.max(0, essais);
            return this;
        }

        /** Pause avant le premier nouvel essai, triplée ensuite (défaut 400 ms). */
        public Builder pause(Duration pause) {
            this.pause = Objects.requireNonNull(pause, "pause");
            return this;
        }

        /** Transport HTTP de remplacement (tests, proxy…). */
        public Builder transport(Transport transport) {
            this.transport = transport;
            return this;
        }

        private Builder verifier() {
            if (clientId == null || clientId.isBlank() || clientSecret == null || clientSecret.isBlank()) {
                throw new PismileException("configuration",
                        "clientId et clientSecret requis (ou PISMILE_CLIENT_ID et PISMILE_CLIENT_SECRET).");
            }
            return this;
        }

        public Pismile build() {
            return new Pismile(verifier());
        }
    }
}
