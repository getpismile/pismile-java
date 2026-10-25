package com.pismile;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Map;

final class Dates {

    private Dates() {
    }

    static Instant lire(Map<String, Object> m, String cle) {
        Object v = m.get(cle);
        if (v == null) {
            return null;
        }
        try {
            return Instant.parse(v.toString());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
