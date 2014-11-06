package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.TableSink;

/**
 * Defines a service that can perform sky position crossmatches by taking
 * a sequence of cone-like positions for input and submitting them as a
 * block to a remote execution matching service.
 *
 * <p>The details of the methods are specified so that input can be
 * streamed and output can be stored, with a final result table re-using
 * table data from a reference input table that may have columns other
 * than those represented by the cone sequence.  It is also amenable to
 * chunking the queries and responses.
 *
 * @author   Mark Taylor
 * @since    14 May 2014
 */
public interface UploadMatcher {

    /**
     * Scans a sequence of positional queries, uploads it to a remote service,
     * and writes the returned values to a given sink.
     *
     * <p>Both the read and the write should ideally be streamed (read as
     * uploaded and written as received) so that progress can be logged
     * properly.
     *
     * <p>The result is written to the given <code>rawResultSink</code>
     * (which will probably be a {@link uk.ac.starlink.table.RowStore}).
     * To make sense of the table thus written, it is necessary to
     * use the {@link RowMapper} supplied to this method and the
     * {@link ColumnPlan} available from this object.
     * The RowMapper associates result rows with queries from the input
     * row sequence, and the ColumnPlan knows where the special and other
     * columns are in the result table.
     *
     * @param  coneSeq  sequence of cone-like positional queries
     * @param  rawResultSink   destination for result rows obtained from
     *                         the target matcher service
     * @param  rowMapper   used to label rows; queries are labelled with the
     *                     value returned from the <code>getIndex</code>
     *                     method of <code>coneSeq</code>
     * @param  maxrec     user-supplied limit on the maximum number of
     *                    output rows, though the service may truncate
     *                    the result; if &lt;0, no limit is requested
     * @return  true iff the result was truncated due to overflow
     */
    boolean streamRawResult( ConeQueryRowSequence coneSeq,
                             TableSink rawResultSink, RowMapper<?> rowMapper,
                             long maxrec )
            throws IOException;

    /**
     * Returns an object that understands what columns are where in an
     * output table generated from the raw result produced by this matcher.
     *
     * @param  resultCols   columns in the raw result table written by
     *                      this object's <code>streamRawResult</code> method
     * @param  uploadCols   columns from the table that will be joined to
     *                      the raw result to get the output table
     * @return  column plan
     */
    ColumnPlan getColumnPlan( ColumnInfo[] resultCols,
                              ColumnInfo[] uploadCols );
}
