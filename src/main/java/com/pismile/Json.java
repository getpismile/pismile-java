package com.pismile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON minimal (lecture et écriture), pour que le SDK n'impose aucune dépendance. Les nombres entiers deviennent des
 * {@code Long}, les autres des {@code Double} ; les objets des {@code Map} (ordre conservé), les tableaux des {@code List}.
 */
final class Json {

    private Json() {
    }

    // ------------------------------------------------------------------ écriture

    static String ecrire(Object valeur) {
        StringBuilder b = new StringBuilder();
        ecrire(b, valeur);
        return b.toString();
    }

    private static void ecrire(StringBuilder b, Object v) {
        if (v == null) {
            b.append("null");
        } else if (v instanceof String s) {
            chaine(b, s);
        } else if (v instanceof Number || v instanceof Boolean) {
            b.append(v);
        } else if (v instanceof Map<?, ?> m) {
            b.append('{');
            boolean premier = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (e.getValue() == null) {
                    continue; // champs facultatifs absents
                }
                if (!premier) {
                    b.append(',');
                }
                premier = false;
                chaine(b, String.valueOf(e.getKey()));
                b.append(':');
                ecrire(b, e.getValue());
            }
            b.append('}');
        } else if (v instanceof List<?> l) {
            b.append('[');
            for (int i = 0; i < l.size(); i++) {
                if (i > 0) {
                    b.append(',');
                }
                ecrire(b, l.get(i));
            }
            b.append(']');
        } else {
            chaine(b, v.toString());
        }
    }

    private static void chaine(StringBuilder b, String s) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                case '\b' -> b.append("\\b");
                case '\f' -> b.append("\\f");
                default -> {
                    if (c < 0x20) {
                        b.append(String.format("\\u%04x", (int) c));
                    } else {
                        b.append(c);
                    }
                }
            }
        }
        b.append('"');
    }

    // ------------------------------------------------------------------ lecture

    static Object lire(String texte) {
        Lecteur l = new Lecteur(texte);
        l.blancs();
        Object v = l.valeur();
        l.blancs();
        if (!l.fini()) {
            throw l.erreur("contenu après la valeur");
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> objet(String texte) {
        Object v = lire(texte);
        if (!(v instanceof Map)) {
            throw new IllegalArgumentException("JSON : objet attendu");
        }
        return (Map<String, Object>) v;
    }

    private static final class Lecteur {
        private final String s;
        private int i;

        Lecteur(String s) {
            this.s = s == null ? "" : s;
        }

        boolean fini() {
            return i >= s.length();
        }

        IllegalArgumentException erreur(String quoi) {
            return new IllegalArgumentException("JSON illisible (" + quoi + ", position " + i + ")");
        }

        void blancs() {
            while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
                i++;
            }
        }

        Object valeur() {
            if (fini()) {
                throw erreur("fin inattendue");
            }
            char c = s.charAt(i);
            return switch (c) {
                case '{' -> objet();
                case '[' -> tableau();
                case '"' -> chaine();
                case 't' -> mot("true", Boolean.TRUE);
                case 'f' -> mot("false", Boolean.FALSE);
                case 'n' -> mot("null", null);
                default -> nombre();
            };
        }

        private Object mot(String mot, Object valeur) {
            if (!s.startsWith(mot, i)) {
                throw erreur("mot inconnu");
            }
            i += mot.length();
            return valeur;
        }

        private Map<String, Object> objet() {
            Map<String, Object> m = new LinkedHashMap<>();
            i++;
            blancs();
            if (i < s.length() && s.charAt(i) == '}') {
                i++;
                return m;
            }
            while (true) {
                blancs();
                if (fini() || s.charAt(i) != '"') {
                    throw erreur("clé attendue");
                }
                String cle = chaine();
                blancs();
                if (fini() || s.charAt(i) != ':') {
                    throw erreur("« : » attendu");
                }
                i++;
                blancs();
                m.put(cle, valeur());
                blancs();
                if (fini()) {
                    throw erreur("« } » attendu");
                }
                char c = s.charAt(i++);
                if (c == '}') {
                    return m;
                }
                if (c != ',') {
                    throw erreur("« , » ou « } » attendu");
                }
            }
        }

        private List<Object> tableau() {
            List<Object> l = new ArrayList<>();
            i++;
            blancs();
            if (i < s.length() && s.charAt(i) == ']') {
                i++;
                return l;
            }
            while (true) {
                blancs();
                l.add(valeur());
                blancs();
                if (fini()) {
                    throw erreur("« ] » attendu");
                }
                char c = s.charAt(i++);
                if (c == ']') {
                    return l;
                }
                if (c != ',') {
                    throw erreur("« , » ou « ] » attendu");
                }
            }
        }

        private String chaine() {
            StringBuilder b = new StringBuilder();
            i++;
            while (true) {
                if (fini()) {
                    throw erreur("chaîne non terminée");
                }
                char c = s.charAt(i++);
                if (c == '"') {
                    return b.toString();
                }
                if (c != '\\') {
                    b.append(c);
                    continue;
                }
                if (fini()) {
                    throw erreur("échappement incomplet");
                }
                char e = s.charAt(i++);
                switch (e) {
                    case '"', '\\', '/' -> b.append(e);
                    case 'n' -> b.append('\n');
                    case 'r' -> b.append('\r');
                    case 't' -> b.append('\t');
                    case 'b' -> b.append('\b');
                    case 'f' -> b.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) {
                            throw erreur("\\u incomplet");
                        }
                        b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                        i += 4;
                    }
                    default -> throw erreur("échappement inconnu");
                }
            }
        }

        private Object nombre() {
            int debut = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) {
                i++;
            }
            String n = s.substring(debut, i);
            if (n.isEmpty()) {
                throw erreur("valeur attendue");
            }
            try {
                if (n.contains(".") || n.contains("e") || n.contains("E")) {
                    return Double.parseDouble(n);
                }
                return Long.parseLong(n);
            } catch (NumberFormatException e) {
                throw erreur("nombre invalide");
            }
        }
    }

    // ------------------------------------------------------------------ accès typés

    static String texte(Map<String, Object> m, String cle) {
        Object v = m.get(cle);
        return v == null ? null : v.toString();
    }

    static long entier(Map<String, Object> m, String cle) {
        Object v = m.get(cle);
        return v instanceof Number n ? n.longValue() : 0L;
    }

    static boolean booleen(Map<String, Object> m, String cle) {
        return Boolean.TRUE.equals(m.get(cle));
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> sousObjet(Map<String, Object> m, String cle) {
        Object v = m.get(cle);
        return v instanceof Map ? (Map<String, Object>) v : Map.of();
    }
}
