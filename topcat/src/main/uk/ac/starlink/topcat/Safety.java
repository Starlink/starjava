package uk.ac.starlink.topcat;

/**
 * Characterises a security risk.
 * There are currently two values, SAFE and UNSAFE.
 *
 * <p>A value of SAFE indicates that the action so characterised
 * is known to be harmless.  This applies to actions that can only
 * affect the internal state of the application.
 * Actions that might be the target of an injection attack should be
 * marked UNSAFE.
 *
 * <p>Potentially unsafe capabilities, which should be marked UNSAFE, include:
 * <ul>
 * <li>invocation of activation-type JEL functions
 *     (those from uk.ac.starlink.topcat.func.*,
 *     though not those from uk.ac.starlink.ttools.func.*)</li>
 * <li>unrestricted execution of System (shell) commands</li>
 * <li>acquiring input streams from
 *     {@link uk.ac.starlink.util.DataSource#makeDataSource}
 *     (these may include shell execution; note that table load
 *     typically calls this method)</li>
 * <li>sending a SAMP message of unknown semantics (except one marked
 *     as <em>mostly-harmless</em>)</li>
 * </ul>
 *
 * @author   Mark Taylor
 * @since    13 Jul 2018
 */
public enum Safety {

    /** Poses no security risk. */
    SAFE,

    /** Poses a potential security risk. */
    UNSAFE;
}
