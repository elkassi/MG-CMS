package com.lear.MGCMS.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CncImageControllerTest {

    @Test
    void extractsCharacters11To13() {
        // L003206673CXNAF -> positions 11,12,13 = C,X,N
        assertEquals("CXN", CncImageController.extractCode("L003206673CXNAF"));
    }

    @Test
    void returnsNullWhenShorterThan13() {
        assertNull(CncImageController.extractCode("SHORT"));
        assertNull(CncImageController.extractCode("123456789012")); // 12 chars
        assertNull(CncImageController.extractCode(null));
    }
}
