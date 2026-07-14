package com.olmeteors.meteorevents.event;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public enum MeteorFallMode {
    INSTANT,
    NORMAL,
    SLOW;

    public static @NotNull MeteorFallMode parse(String raw) {
        if (raw == null) return NORMAL;
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "instant", "aninda", "anında", "fast" -> INSTANT;
            case "slow", "cinematic", "yavas", "yavaş" -> SLOW;
            default -> NORMAL;
        };
    }
}
