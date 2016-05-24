package uk.ac.starlink.ttools.taplint;

/**
 * Basic interface for logging validation messages.
 *
 * <p>Note that in earlier versions Reporter was a concrete class
 * with more methods, used by the TapLinter harness class.
 * That concrete class, which implements this interface,
 * has now been renamed {@link OutputReporter}.
 *
 * @author   Mark Taylor
 * @since    24 May 2016
 */
public interface Reporter {

    /**
     * Reports a message.
     *
     * <p>This convenience method is equivalent to calling
     * {@link #report(ReportCode,java.lang.String,java.lang.Throwable)
     *         report(code,message,null)}
     *
     * @param    code   report code; messages with the same code should
     *                  identify essentially the same condition
     * @param    message  free-text message; it may be multi-line and/or
     *                    longish, but may in practice be truncated on output
     */
    void report( ReportCode code, String message );

    /**
     * Reports a message with an associated throwable.
     *
     * @param    code   report code; messages with the same code should
     *                  identify essentially the same condition
     * @param    message  free-text message; it may be multi-line and/or
     *                    longish, but may in practice be truncated on output
     * @param    err    throwable
     */
    void report( ReportCode code, String message, Throwable err );
}
