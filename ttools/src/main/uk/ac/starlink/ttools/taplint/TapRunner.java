package uk.ac.starlink.ttools.taplint;

import java.io.IOException;
import java.text.DecimalFormat;
import org.xml.sax.SAXException;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.vo.TapQuery;

/**
 * Executes TAP queries for the validator.
 *
 * @author   Mark Taylor
 * @since    9 Jun 2011
 */
public abstract class TapRunner {

    private final String description_;
    private int nQuery_;
    private int nResult_;
    private long queryTime_;

    /**
     * Constructor.
     *
     * @param  description   short description of this object's type
     */
    protected TapRunner( String description ) {
        description_ = description;
    }
  
    /**
     * Returns a short description.
     *
     * @return  descriptive label
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Executes a TAP query and returns the result table, or null if the
     * query failed for some reason.  Errors are reported through the reporter
     * as appropriate.
     *
     * @param  reporter  validation message destination
     * @param  tq  TAP query specification
     * @return  result table, or null if there was an error
     */
    public StarTable getResultTable( Reporter reporter, TapQuery tq ) {
        try {
            return attemptGetResultTable( reporter, tq );
        }
        catch ( IOException e ) {
            reporter.report( ReportType.ERROR, "QERR", "TAP query failed", e );
            return null;
        }
        catch ( SAXException e ) {
            reporter.report( ReportType.ERROR, "QERX",
                             "TAP query result parse failed",
                             e );
            return null;
        }
    }

    /**
     * Attempts to execute a TAP query and returns the result table,
     * or throws an exception if the query failed for some reason.
     *
     * @param  reporter  validation message destination
     * @param  tq  TAP query specification
     * @return  result table, not null
     */
    public StarTable attemptGetResultTable( Reporter reporter, TapQuery tq )
            throws IOException, SAXException {
        reporter.report( ReportType.INFO, "QSUB",
                         "Submitting query: " + tq.getAdql() );
        nQuery_++;
        long start = System.currentTimeMillis();
        StarTable table = executeQuery( reporter, tq );
        queryTime_ += System.currentTimeMillis() - start;
        nResult_++;
        return table;
    }

    /**
     * Executes a TAP query, performing reporting as appropriate.
     * The result may be null, but will normally be either a table or
     * an IOException will result.
     *
     * @param  reporter  validation message destination
     * @param  query  query to execute
     * @return  result table
     */
    protected abstract StarTable executeQuery( Reporter reporter,
                                               TapQuery query )
        throws IOException, SAXException;

    /**
     * Reports a summary of the queries executed by this object.
     *
     * @param  reporter  validation message destination
     */
    public void reportSummary( Reporter reporter ) {
        String cmsg = new StringBuilder()
            .append( "Successful/submitted TAP queries: " )
            .append( nResult_ )
            .append( "/" )
            .append( nQuery_ )
            .toString();
        reporter.report( ReportType.SUMMARY, "QNUM", cmsg );
        if ( nResult_ > 0 ) {
            double qtime = 0.001 * queryTime_ / nResult_;
            String fmt = "0.0";
            if ( qtime < 0.2 ) {
                fmt = "0.00";
            }
            if ( qtime < 0.02 ) {
                fmt = "0.000";
            }
            String tmsg = new StringBuilder()
                .append( "Average successful query time: " )
                .append( new DecimalFormat( fmt ).format( qtime ) )
                .append( "s" )
                .toString();
            reporter.report( ReportType.SUMMARY, "QTIM", tmsg );
        }
    }
}
