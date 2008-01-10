package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.StarTable;

/**
 * Object which can perform repeated sky cone searches.
 *
 * @author   Mark Taylor
 * @since    10 Aug 2007
 */
public interface ConeSearcher {

    /**
     * Returns a table consisting of all the objects within a given search
     * radius of a specified point on the sky.
     * If no matching objects are found, then it is preferable to return
     * an empty table with the correct columns.  However, if this cannot
     * be done, it is permissible to return null.
     * For every table returned, the columns should be the same.
     *
     * @param  ra  right ascension in degrees of search region centre
     * @param  dec  declination in degrees of search region centre
     * @param  sr  search radius in degrees
     * @return   table containing records in the given cone,
     *           or possibly null if no records are found
     */
    StarTable performSearch( double ra, double dec, double sr )
            throws IOException ;

    /**
     * Returns the index of a column giving the right ascension in the 
     * returned table.  This should be the same quantity which is used
     * for the match assessment.
     *
     * @param  result  table returned by <code>performSearch</code>
     * @return   index of RA column, or -1 if it can't be found
     */
    int getRaIndex( StarTable result );

    /**
     * Returns the index of a column giving the declination in the
     * returned table.  This should be the same quantity which is used
     * for the match assessment.
     *
     * @param  result  table returned by <code>performSearch</code>
     * @return  index of dec column, or -1 if it can't be found
     */
    int getDecIndex( StarTable result );

    /**
     * Called when this searcher is no longer required.
     */
    void close();
}
