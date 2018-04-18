package uk.ac.starlink.topcat.activate;

/**
 * Enum defining suitability levels for the use of a particular
 * ActivationType with a given TopcatModel.
 *
 * @author   Mark Taylor
 * @since    23 Mar 2018
 */
public enum Suitability {

    /** Activation action listed and active. */
    ACTIVE,

    /** Activation action listed prominently and inactive. */
    SUGGESTED,

    /** Activation action listed less prominently and inactive. */
    PRESENT,

    /** Activation action available from a menu. */
    AVAILABLE,

    /** Activation action visible in a menu but not available. */
    DISABLED,

    /** Activation action completely hidden. */
    NONE;
}
