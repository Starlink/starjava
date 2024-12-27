package uk.ac.starlink.auth;

/**
 * Enumerates different types of authentication available for a connection.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2022
 */
public enum AuthType {

    /** Only anonymous operation is available. */
    NONE,

    /** Both authenticated and anonymous operation are permitted. */
    OPTIONAL,

    /** Authentication is necessary for use. */
    REQUIRED,

    /** Information not available. */
    UNKNOWN;
}
