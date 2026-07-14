package com.olmeteors.meteorevents.event;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** Horizontal shape used by a meteor's damage, hazard and participation radius. */
public enum RadiusShape {
    CIRCLE,
    SQUARE,
    TRIANGLE,
    DIAMOND,
    HEXAGON;

    public boolean contains(double deltaX, double deltaZ, double radius) {
        if (radius <= 0) return false;
        final double x = Math.abs(deltaX);
        final double z = Math.abs(deltaZ);
        return switch (this) {
            case CIRCLE -> x * x + z * z <= radius * radius;
            case SQUARE -> x <= radius && z <= radius;
            case DIAMOND -> x + z <= radius;
            case HEXAGON -> z <= radius
                    && x * 0.8660254037844386 + z * 0.5 <= radius;
            case TRIANGLE -> insideTriangle(deltaX, deltaZ, radius);
        };
    }

    /** Distance from the center to this shape's edge at the supplied angle. */
    public double boundaryDistance(double angle, double radius) {
        final double cos = Math.cos(angle);
        final double sin = Math.sin(angle);
        double low = 0;
        double high = radius * Math.sqrt(2.0) + 0.01;
        for (int i = 0; i < 24; i++) {
            final double middle = (low + high) * 0.5;
            if (contains(cos * middle, sin * middle, radius)) low = middle;
            else high = middle;
        }
        return low;
    }

    private boolean insideTriangle(double x, double z, double radius) {
        final double d1 = sign(x, z, 0, -radius, -radius, radius);
        final double d2 = sign(x, z, -radius, radius, radius, radius);
        final double d3 = sign(x, z, radius, radius, 0, -radius);
        final boolean negative = d1 < 0 || d2 < 0 || d3 < 0;
        final boolean positive = d1 > 0 || d2 > 0 || d3 > 0;
        return !(negative && positive);
    }

    private double sign(double px, double pz, double ax, double az, double bx, double bz) {
        return (px - bx) * (az - bz) - (ax - bx) * (pz - bz);
    }

    public static @NotNull RadiusShape parse(String raw) {
        if (raw == null) return CIRCLE;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "square", "kare" -> SQUARE;
            case "triangle", "ucgen", "üçgen" -> TRIANGLE;
            case "diamond", "elmas" -> DIAMOND;
            case "hexagon", "altigen", "altıgen" -> HEXAGON;
            default -> CIRCLE;
        };
    }
}
