package com.olmeteors.meteorevents.event;

/**
 * Represents the current phase of a meteor event lifecycle.
 * Each phase has specific behaviors and visual effects associated with it.
 */
public enum EventPhase {

    /**
     * Event is scheduled but not yet active.
     */
    SCHEDULED(false, false),

    /**
     * Pre-impact warning phase - visual effects, sound cues, screen shake.
     */
    PRE_IMPACT(true, false),

    /**
     * The meteor has impacted - schematic pasting, initial damage.
     */
    IMPACT(true, false),

    /**
     * Active event phase - hazards active, boss spawned, vault available.
     */
    ACTIVE(true, true),

    /**
     * Rollback phase - system is cleaning up and restoring the area.
     */
    ROLLBACK(true, false),

    /**
     * Event has completed successfully.
     */
    COMPLETED(false, false),

    /**
     * Event was cancelled or failed.
     */
    CANCELLED(false, false);

    private final boolean active;
    private final boolean hazardsEnabled;

    EventPhase(boolean active, boolean hazardsEnabled) {
        this.active = active;
        this.hazardsEnabled = hazardsEnabled;
    }

    /**
     * Whether the event is still in an active state.
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Whether hazards (radiation, wind charges, etc.) should be enabled.
     */
    public boolean hazardsEnabled() {
        return hazardsEnabled;
    }

    /**
     * Whether players can interact with the vault.
     */
    public boolean vaultAccessible() {
        return this == ACTIVE;
    }

    /**
     * Whether the boss should be spawned.
     */
    public boolean bossActive() {
        return this == ACTIVE;
    }
}
