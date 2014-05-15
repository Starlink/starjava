package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;
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
     * <p>The result is written in an opaque format to the given
     * <code>rawResultSink<code>
     * (which will probably be a {@link uk.ac.starlink.table.RowStore}),
     * using the supplied RowMapper to associate output rows with queries
     * from the input query sequence.
     * The table stored thus can then be turned into a result using
     * {@link #createOutputTable createOutputTable}.
     *
     * @param  coneSeq  sequence of cone-like positional queries
     * @param  rawResultSink   destination for result rows obtained from
     *                         the target matcher service
     * @param  rowMapper   used to label rows; the first query is labelled
     *                    with rowIndex=0 etc, the second with rowIndex=1 etc
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
     * Takes a table obtained by use of <code>streamRawResult</code>
     * and joins it up with columns from an associated input table to
     * produce a useful joined result.
     *
     * @param  rawResult  table formed of rows from an earlier call to
     *                    <code>streamRawResult</code>
     * @param  uploadTable  a table from which the input positional queries
     *                      were derived; it must have its rows in an order
     *                      corresponding to the the sequence of cone queries
     * @param  rowMapper    used to identify which uploadTable rows correspond
     *                     to which rawResult rows
     * @return    some kind of joined table composed of the rows of the
     *            rawResult and uploadTable, and the columns of both
     */
    StarTable createOutputTable( StarTable rawResult, StarTable uploadTable,
                                 RowMapper<?> rowMapper );
}
