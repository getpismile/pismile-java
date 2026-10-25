package com.pismile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/** Transport de test : réponses préparées, requêtes enregistrées. */
final class FauxTransport implements Transport {

    record Requete(String methode, String url, Map<String, String> entetes, String corps) {
    }

    final List<Requete> requetes = new ArrayList<>();
    private final Deque<Object> reponses = new ArrayDeque<>();

    FauxTransport repondre(int statut, String corps) {
        reponses.add(new Reponse(statut, corps));
        return this;
    }

    FauxTransport couper() {
        reponses.add(new IOException("connexion refusée"));
        return this;
    }

    @Override
    public Reponse envoyer(String methode, String url, Map<String, String> entetes, String corps) throws IOException {
        requetes.add(new Requete(methode, url, entetes, corps));
        Object r = reponses.poll();
        if (r instanceof IOException e) {
            throw e;
        }
        return (Reponse) r;
    }
}
