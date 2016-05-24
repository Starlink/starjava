package uk.ac.starlink.ttools.taplint;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Reporter implementation that records reports, and can dump them
 * on request to another Reporter instance.
 *
 * <p>Not thread-safe.
 *
 * @author   Mark Taylor
 * @since    24 May 2016
 */
public class HoldReporter implements Reporter {

    private final List<Report> list_;

    /**
     * Constructor.
     */
    public HoldReporter() {
        list_ = new LinkedList<Report>();
    }

    public void report( ReportCode code, String msg ) {
        report( code, msg, null );
    }

    public void report( ReportCode code, String msg, Throwable err ) {
        list_.add( new Report( code, msg, err ) );
    }

    /**
     * Dumps all reports received to date to a supplied reporter.
     * Reports are passed on in order of receipt.
     * The list of held reports is emptied by calling this method.
     *
     * @param  reporter  destination reporter
     */
    public void dumpReports( Reporter reporter ) {
        for ( Iterator<Report> it = list_.iterator(); it.hasNext(); ) {
            Report report = it.next();
            it.remove();
            reporter.report( report.code_, report.msg_, report.err_ );
        }
    }

    /**
     * Stores report content.
     */
    private static class Report {
        final ReportCode code_;
        final String msg_;
        final Throwable err_;

        /**
         * Constructor.
         *
         * @param    code   report code
         * @param    msg    message
         * @param    err    throwable, may be null
         */
        Report( ReportCode code, String msg, Throwable err ) {
            code_ = code;
            msg_ = msg;
            err_ = err;
        }
    }
}
