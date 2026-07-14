package com.olmeteors.meteorevents.event;

/** Final outcome of a manual, ticket or API meteor start request. */
public enum MeteorStartResult {
    STARTED,
    CANCELLED,
    LOCATION_NOT_FOUND,
    WORLD_NOT_FOUND,
    ERROR
}
