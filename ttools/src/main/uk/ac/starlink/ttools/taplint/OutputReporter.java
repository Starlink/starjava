package uk.ac.starlink.ttools.taplint;

/**
 * Interface for application-level logging of validation messages.
 * This extends the Reporter interface to add support for multiple stages.
 * The design (a single-level hierarchy of reporting stages) is not
 * particularly elegant or general, and may be revised at some point,
 * but it serves the current purposes of the TapLint tool.
 *
 * @author   Mark Taylor
 * @since    24 May 2016
 */
public interface OutputReporter extends Reporter {

    /**
     * Signals beginning of reporting.
     *
     * @param  announcements  header information about validator operation;
     *                        plain text, one line per element
     */
    void start( String[] announcements );

    /**
     * Signals end of reporting.
     */
    void end();

    /**
     * Begins a reporting section.
     *
     * @param   scode   short fixed-length (3-char?) identifier for the
     *                  section about to start
     * @param   message  terse (one-line) free-text description of the stage
     */
    void startSection( String scode, String message );

    /**
     * Returns the section code for the most recently-started section.
     *
     * @return  current section code
     */
    String getSectionCode();

    /**
     * Writes to the output stream a summary of messages which were
     * suppressed in a given stage because the maximum repeat count
     * was exceeded.
     *
     * @param   scode  section code to summarise;
     *                 if null, no stage filtering is done
     */
    void summariseUnreportedMessages( String scode );

    /**
     * Ends the current section.
     */
    void endSection();
}
