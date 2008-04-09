package uk.ac.starlink.topcat.plot;

/**
 * Extends the {@link Points} interface to provide a facility for 
 * storing points as well as retrieving them.
 * To populate this object with point values, the {@link #storePoint}
 * method must be called once for each point in sequence.
 * Points cannot in general be re-written.
 * Implementation classes may have rules about what sequence methods
 * must be called in, for instance whether all the writes must be
 * performed before any of the read methods can be used.
 *
 * @author   Mark Taylor
 * @since    29 Mar 2007
 */
public interface PointStore extends Points {
    
    /**
     * Stores the next point in sequence to this object.
     * The lengths of the arrays supplied here are not necessarily the
     * same as those returned by the {@link #getNdim} and {@link #getNerror}
     * methods, since there may be some translation between the arrays.
     *
     * <p>The {@link PointSelection} submits rows here as retrieved
     * directly from the AxesSelector {@link AxesSelector#getData} and
     * {@link AxesSelector#getErrorData} tables.
     *
     * @param  coordRow  array of objects representing coordinate values
     * @param  errorRow  array of objects representing error values
     * @param  label     string labelling the point
     */
    void storePoint( Object[] coordRow, Object[] errorRow, String label );
}
