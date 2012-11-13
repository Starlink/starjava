package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.table.StarTable;

/**
 * Straightforward implementation of ConeResultRowSequence based on a
 * ConeQueryRowSequence.
 *
 * @author   Mark Taylor
 * @since    16 Jan 2008
 */
public class SequentialResultRowSequence implements ConeResultRowSequence {

    private final ConeQueryRowSequence querySeq_;
    private final ConeSearcher coneSearcher_;
    private final Footprint footprint_;
    private final boolean bestOnly_;
    private final boolean distFilter_;
    private final String distanceCol_;
    private int nQuery_;
    private int nSkip_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.cone" );

    /**
     * Constructor.
     *
     * @param  querySeq  sequence providing cone search query parameters
     * @param  coneSearcher  cone search implementation
     * @param  footprint   coverage footprint for results, or null
     * @param  bestOnly  whether all results or just best are required
     * @param  distFilter  true to perform post-query filtering on results
     *                     based on the distance between the query position
     *                     and the result row position
     * @param  distanceCol  name of column to hold distance information 
     *                      in output table, or null
     */
    public SequentialResultRowSequence( ConeQueryRowSequence querySeq,
                                        ConeSearcher coneSearcher,
                                        Footprint footprint, boolean bestOnly,
                                        boolean distFilter,
                                        String distanceCol ) {
        querySeq_ = querySeq;
        coneSearcher_ = coneSearcher;
        footprint_ = footprint;
        bestOnly_ = bestOnly;
        distFilter_ = distFilter;
        distanceCol_ = distanceCol;
    }

    public StarTable getConeResult() throws IOException {
        double ra = querySeq_.getRa();
        double dec = querySeq_.getDec();
        double radius = querySeq_.getRadius();

        /* Ensure that at least one query is performed even if all points
         * are outside the footprint.  This way the metadata for an empty
         * table is returned, so at least you have the columns. */
        boolean excluded = nQuery_ + nSkip_ > 0
                        && footprint_ != null
                        && ! footprint_.discOverlaps( ra, dec, radius );
        if ( excluded ) {
            Level level = Level.CONFIG;
            if ( logger_.isLoggable( level ) ) {
                logger_.log( level,
                             "Skipping cone query for point outside footprint "
                           + "(" + (float) ra + "," + (float) dec + ")+"
                           + (float) radius );
            }
            nSkip_++;
            return null;
        }
        else {
            nQuery_++;
            return ConeMatcher
                  .getConeResult( coneSearcher_, bestOnly_, distFilter_,
                                  distanceCol_, ra, dec, radius );
        }
    }

    public boolean next() throws IOException {
        return querySeq_.next();
    }

    public Object getCell( int icol ) throws IOException {
        return querySeq_.getCell( icol );
    }

    public Object[] getRow() throws IOException {
        return querySeq_.getRow();
    }

    public void close() throws IOException {
        querySeq_.close();
        if ( footprint_ != null ) {
            logger_.info( "Submitted " + nQuery_ + ", " + "skipped " + nSkip_
                        + " queries to service" );
        }
    }

    public double getDec() throws IOException {
        return querySeq_.getDec();
    }

    public double getRa() throws IOException {
        return querySeq_.getRa();
    }

    public double getRadius() throws IOException {
        return querySeq_.getRadius();
    }
}
