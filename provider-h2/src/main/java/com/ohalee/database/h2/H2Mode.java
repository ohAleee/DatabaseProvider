package com.ohalee.database.h2;

/**
 * The storage mode for an H2 database.
 */
public enum H2Mode {

    /** In-memory database (data is lost on disconnect). */
    MEMORY,

    /** Embedded file-based database (data persisted to disk). */
    FILE,

    /** Server mode database (connects to a running H2 server). */
    SERVER
}
