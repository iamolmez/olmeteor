package com.olmeteors.meteorevents.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RadiusShapeTest {

    @Test
    void parsesEnglishAndTurkishNames() {
        assertEquals(RadiusShape.CIRCLE, RadiusShape.parse("daire"));
        assertEquals(RadiusShape.SQUARE, RadiusShape.parse("kare"));
        assertEquals(RadiusShape.TRIANGLE, RadiusShape.parse("üçgen"));
        assertEquals(RadiusShape.DIAMOND, RadiusShape.parse("elmas"));
        assertEquals(RadiusShape.HEXAGON, RadiusShape.parse("altıgen"));
    }

    @Test
    void shapesHaveDifferentCorners() {
        assertTrue(RadiusShape.SQUARE.contains(9, 9, 10));
        assertFalse(RadiusShape.CIRCLE.contains(9, 9, 10));
        assertFalse(RadiusShape.DIAMOND.contains(6, 6, 10));
        assertTrue(RadiusShape.TRIANGLE.contains(0, 0, 10));
        assertFalse(RadiusShape.TRIANGLE.contains(9, -9, 10));
    }

    @Test
    void previewBoundaryLandsOnEveryShape() {
        for (RadiusShape shape : RadiusShape.values()) {
            for (int degrees = 0; degrees < 360; degrees += 15) {
                final double angle = Math.toRadians(degrees);
                final double distance = shape.boundaryDistance(angle, 20);
                assertTrue(shape.contains(Math.cos(angle) * distance,
                        Math.sin(angle) * distance, 20));
                assertFalse(shape.contains(Math.cos(angle) * (distance + 0.1),
                        Math.sin(angle) * (distance + 0.1), 20));
            }
        }
    }
}
