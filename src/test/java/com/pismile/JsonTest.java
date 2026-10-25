package com.pismile;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonTest {

    @Test
    void litLesTypesDeBase() {
        Map<String, Object> m = Json.objet("{\"a\":1,\"b\":-2.5,\"c\":\"é\\\"\\u00e9\\n\",\"d\":true,\"e\":null,\"f\":[1,{\"g\":2}]}");
        assertEquals(1L, m.get("a"));
        assertEquals(-2.5, m.get("b"));
        assertEquals("é\"é\n", m.get("c"));
        assertEquals(true, m.get("d"));
        assertEquals(null, m.get("e"));
        assertEquals(2L, ((Map<?, ?>) ((List<?>) m.get("f")).get(1)).get("g"));
    }

    @Test
    void ecritEnIgnorantLesChampsNuls() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("texte", "a\"b\\c\u0001");
        m.put("nombre", 25000L);
        m.put("absent", null);
        m.put("liste", List.of(1, "x"));
        assertEquals("{\"texte\":\"a\\\"b\\\\c\\u0001\",\"nombre\":25000,\"liste\":[1,\"x\"]}", Json.ecrire(m));
    }

    @Test
    void refuseUnJsonIllisible() {
        assertThrows(IllegalArgumentException.class, () -> Json.objet("{\"a\":}"));
        assertThrows(IllegalArgumentException.class, () -> Json.objet("[1,2]"));
        assertThrows(IllegalArgumentException.class, () -> Json.objet("{\"a\":1} x"));
    }
}
