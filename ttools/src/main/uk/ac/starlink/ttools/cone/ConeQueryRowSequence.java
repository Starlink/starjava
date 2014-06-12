package uk.ac.starlink.ttools.cone;

import java.io.IOException;
import uk.ac.starlink.table.RowSequence;

/**
 * RowSequence sub-interface which additionally defines methods for 
 * retrieving RA, Dec search radius and row index for each row.
 *
 * @author    Mark Taylor
 * @since     16 Oct 2007
 */
public interface ConeQueryRowSequence extends RowSequence {

    /**
     * Get central right ascension for the current row's cone search request
     * in degrees.
     *
     * @return  right ascension
     */
    public double getRa() throws IOException;

    /**
     * Get central declination for the current row's cone search request
     * in degrees.
     *
     * @return  declination
     */
    public double getDec() throws IOException;

    /**
     * Get search radius for the current row's cone search request
     * in degrees.
     *
     * @return   search radius
     */
    public double getRadius() throws IOException;

    /**
     * Get the index in the underlying table to which the current row relates.
     * The identity of this underlying table is not specified by this
     * interface, but must be understood by the creator and user of instances.
     * In particular, the return value does not necessarily increment by
     * one for each call to <code>next</code>.
     *
     * @return  row index
     */
    public long getIndex() throws IOException;
}
