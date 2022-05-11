package uk.ac.starlink.table.join;

import java.io.IOException;
import java.util.function.Predicate;
import java.util.function.Supplier;
import uk.ac.starlink.table.StarTable;

/**
 * Defines some computationally intensive operations required for
 * row matching.
 *
 * @author   Mark Taylor
 * @since    25 Aug 2021
 */
interface MatchComputer {

    /**
     * Create a map from match bin to list of all the row indices
     * associated with that bin, for a given table.
     *
     * @param   kitFact  match criteria
     * @param   rowSelector   filter for rows to be included;
     *                        row values that fail this test are ignored
     * @param   tableR  table to bin, random access is available
     * @param   indicator  progress indicator to be messaged with progress
     * @param   stageTxt  message describing this stage of the matching
     * @return   binning results
     */
    BinnedRows binRowIndices( Supplier<MatchKit> kitFact,
                              Predicate<Object[]> rowSelector, StarTable tableR,
                              ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException;

    /**
     * Adds entries for the rows of a table to a given ObjectBinner.
     *
     * <p>The <code>newBins</code> parameter determines whether new bins
     * will be started in the <code>bins</code> object.  If true, then
     * every relevant row in the table will be binned.  If false, then
     * only rows with entries in bins which are already present in
     * the supplied binner will be added and others will be ignored.
     *
     * @param   kitFact   match criteria
     * @param   rowSelector   filter for rows to be included;
     *                        row values that fail this test are ignored
     * @param   table  table to bin
     * @param   tIndex  index of table for use in row references
     * @param   binner   object binner to update with new entries
     * @param   newBins  whether new bins may be added to binner
     * @param   indicator  progress indicator to be messaged with progress
     * @param   stageTxt  message describing this stage of the matching
     * @return   number of rows actually considered (not excluded)
     */
    long binRowRefs( Supplier<MatchKit> kitFact,
                     Predicate<Object[]> rowSelector,
                     StarTable table, int tIndex,
                     ObjectBinner<Object,RowRef> binner, boolean newBins,
                     ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException;

    /**
     * Scans a table S sequentially with reference to a supplied LongBinner
     * containing bin assignments for a random-access table R,
     * to identify matched pairs between rows in the two tables.
     *
     * @param  kitFact   match criteria
     * @param  rowSelector   filter for rows to be included;
     *                       row values in table S that fail this test
     *                       are ignored
     * @param  tableR  table R which will be accessed randomly
     * @param  indexR  index of table R for use in row references
     * @param  tableS  table S which will be access sequentially
     * @param  indexS  index of table S for use in row references
     * @param  bestOnly  true iff only the best S-R match is required;
     *                   if false multiple matches in R may be returned
     *                   for each row in S
     * @param  binnerR   map from bin value to list of row indices in R
     *                   to which that bin relates
     * @param  linksetCreator  LinkSet factory
     * @param  indicator  progress indicator to be messaged with progress
     * @param  stageTxt  message describing this stage of the matching
     * @return  links representing pair matches
     */
    LinkSet scanBinsForPairs( Supplier<MatchKit> kitFact,
                              Predicate<Object[]> rowSelector,
                              StarTable tableR, int indexR,
                              StarTable tableS, int indexS,
                              boolean bestOnly, LongBinner binnerR,
                              Supplier<LinkSet> linksetCreator,
                              ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException;

    /**
     * Determines the NdRange for selected columns from a given table
     * by scanning through all its rows.
     *
     * @param   table   table whose data is to be scanned
     * @param   colFlags  array of same length as table column count
     *                    indicating which columns are to be scanned
     *                    for range; output NdRange will be blank in
     *                    dimensions for which these flags are false
     * @param   indicator  progress indicator to be messaged with progress
     * @param   stageTxt  message describing this stage of the matching
     * @return   N-dimensional range for selected columns;
     *           note this may contain null or infinite values even
     *           in the selected columns, so may require post-processing
     */
    NdRange rangeColumns( StarTable table, boolean[] colFlags,
                          ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException;

    /**
     * Counts the rows in a table that are included by a given filter.
     *
     * @param   table   table whose data is to be scanned
     * @param   rowSelector   filter for rows to be included;
     * @param   indicator  progress indicator to be messaged with progress
     * @param   stageTxt  message describing this stage of the matching
     * @return  row count
     */
    long countRows( StarTable table, Predicate<Object[]> rowSelector,
                    ProgressIndicator indicator, String stageTxt )
            throws IOException, InterruptedException;

    /**
     * Returns a short user-readable description of the kind of processing
     * performed by this computer.
     *
     * @return  description text
     */
    String getDescription();

    /**
     * Aggregates results of a row binning operation.
     */
    interface BinnedRows {

        /**
         * Returns a binner with keys that are match bins and values
         * that are lists of row indices associated with those bins.
         *
         * @return  binned results
         */
        LongBinner getLongBinner();

        /**
         * Returns the number of row references represented by the binner.
         *
         * @return  row ref count
         */
        long getNref();

        /**
         * Returns the number of rows ignored during binning.
         *
         * @return  row exclusion count
         */
        long getNexclude();
    }
}
