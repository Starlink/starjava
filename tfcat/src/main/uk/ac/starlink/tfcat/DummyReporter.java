package uk.ac.starlink.tfcat;

/**
 * Reporter that discards all messages.
 *
 * <p>Since there is no internal state, in general
 * the {@link #INSTANCE} can be used,
 * but a protected constructor is available in case you want to subclass it.
 *
 * @author   Mark Taylor
 * @since    9 Feb 2022
 */
public class DummyReporter implements Reporter {

    /** Instance suitable for general use. */
    public static final DummyReporter INSTANCE = new DummyReporter();

    /**
     * Constructor.
     */
    protected DummyReporter() {
    }

    public void checkUcd( String ucd ) {
    }

    public void checkUnit( String unit ) {
    }

    public Reporter createReporter( int subContext ) {
        return this;
    }

    public Reporter createReporter( String subContext ) {
        return this;
    }

    public void report( String msg ) {
    }
}
