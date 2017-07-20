package uk.ac.starlink.ttools.task;

/**
 * Defines the credibility of a value.
 * It can be used to indicate how reliable the value is for its
 * intended purpose.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2017
 */
public enum Credibility {

    /** Probably or certainly reliable. */
    YES,

    /** May be reliable. */
    MAYBE,

    /** Probably or certainly unreliable. */
    NO;
}
