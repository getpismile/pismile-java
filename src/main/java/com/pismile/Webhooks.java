package com.pismile;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Vérification des webhooks : en-tête {@code Pismile-Signature: t=<horodatage>,v1=<hex>} avec
 * {@code v1 = HMAC-SHA256(secret, t + "." + corps brut)}.
 */
public final class Webhooks {

    /** Nom de l'en-tête de signature. */
    public static final String ENTETE = "Pismile-Signature";
    /** Écart accepté entre l'horodatage signé et maintenant (contre le rejeu). */
    public static final Duration TOLERANCE = Duration.ofMinutes(5);

    private static final Pattern HORODATAGE = Pattern.compile("\\d{1,12}");
    private static final Pattern HEX = Pattern.compile("[0-9a-f]{64}");

    private final String secret;

    Webhooks(String secret) {
        this.secret = secret;
    }

    /**
     * Vérifie un webhook avec le secret configuré et renvoie son contenu.
     *
     * @param corpsBrut le corps tel que reçu (String brut, pas un objet déjà converti)
     * @param entete    la valeur de l'en-tête Pismile-Signature
     * @throws PismileException signature_invalide ou signature_expiree : répondez 400 et ignorez le message
     */
    public Evenement verifier(String corpsBrut, String entete) {
        return verifier(corpsBrut, entete, secret, Instant.now());
    }

    /** Variante sans client : utile si le secret vient d'ailleurs. */
    public static Evenement verifier(String corpsBrut, String entete, String secret, Instant maintenant) {
        if (secret == null || secret.isBlank()) {
            throw new PismileException("configuration", "Secret du webhook manquant (PISMILE_WEBHOOK_SECRET).");
        }
        if (corpsBrut == null || entete == null || entete.isBlank()) {
            throw new PismileException("signature_invalide", "En-tête Pismile-Signature absent.");
        }
        String t = null;
        String v1 = null;
        for (String morceau : entete.split(",")) {
            int egal = morceau.indexOf('=');
            if (egal <= 0) {
                continue;
            }
            String cle = morceau.substring(0, egal).trim();
            String valeur = morceau.substring(egal + 1).trim();
            if (cle.equals("t")) {
                t = valeur;
            } else if (cle.equals("v1")) {
                v1 = valeur;
            }
        }
        if (t == null || v1 == null || !HORODATAGE.matcher(t).matches() || !HEX.matcher(v1).matches()) {
            throw new PismileException("signature_invalide", "En-tête Pismile-Signature illisible.");
        }
        byte[] attendu = hmac(secret, t + "." + corpsBrut);
        if (!MessageDigest.isEqual(attendu, HexFormat.of().parseHex(v1))) {
            throw new PismileException("signature_invalide", "Signature incorrecte : mauvais secret ou corps modifié.");
        }
        if (Math.abs(maintenant.getEpochSecond() - Long.parseLong(t)) > TOLERANCE.getSeconds()) {
            throw new PismileException("signature_expiree", "Webhook trop ancien (rejeu ?).");
        }
        try {
            return Evenement.depuis(Json.objet(corpsBrut));
        } catch (IllegalArgumentException e) {
            throw new PismileException("signature_invalide", "Corps JSON illisible.");
        }
    }

    /** Signe un corps comme pismile (pour vos tests). */
    public static String signer(String corps, String secret, long horodatage) {
        return "t=" + horodatage + ",v1=" + HexFormat.of().formatHex(hmac(secret, horodatage + "." + corps));
    }

    private static byte[] hmac(String secret, String contenu) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(contenu.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC-SHA256 indisponible", e);
        }
    }
}
