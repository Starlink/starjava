package uk.ac.starlink.ttools.plot2;

import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Extracts range information from plot data.
 *
 * @since   4 Feb 2013
 * @author  Mark Taylor
 */
public interface AuxReader {

    /**
     * May use the supplied data specification to update (usually extend)
     * the given range.
     * If available an array of plan objects may be supplied as well.
     * The implementation may be able to make use of these to improve
     * efficiency of the ranging (perhaps to avoid a data scan).
     *
     * @param  surface  plot data destination surface
     * @param  dataSpec    specification for data
     * @param  dataStore   data storage object
     * @param  knownPlans  array of available plan objects; may be empty
     * @param  ranger   object to be updated with range information
     */
    void adjustAuxRange( Surface surface, DataSpec dataSpec,
                         DataStore dataStore, Object[] knownPlans,
                         Ranger ranger );

    /**
     * Returns a scaling that will be used on the result of the aux
     * ranging done by this reader, if any.  If no scaler will be
     * generated from the resulting Span (no special requirements on
     * Span behaviour), then null may be returned.
     *
     * @return   aux scaling type, or null
     */
    Scaling getScaling();

    /**
     * Returns the DataSpec index for the coordinate whose value is used
     * by this reader.  This is provided on a best-efforts basis; if no
     * single coordinate fits this description, then -1 may be returned.
     *
     * @return   DataSpec coord index for auxiliary coordinate being ranged,
     *           or -1
     */
    int getCoordIndex();

    /**
     * Attempts to provide information suitable for labelling the axis
     * corresponding to the values ranged by this reader.
     *
     * @param  dataSpec  data specification
     * @return  info corresponding to this reader's scale,
     *          or null if none known
     */
    ValueInfo getAxisInfo( DataSpec dataSpec );
}
