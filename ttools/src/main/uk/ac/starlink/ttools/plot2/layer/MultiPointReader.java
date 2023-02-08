package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.AuxReader;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Equality;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.data.Tuple;

/**
 * Defines data reading behaviour for use by a MultiPointForm.
 *
 * @author   Mark Taylor
 * @since    8 Feb 2023
 */
@Equality
public interface MultiPointReader {

    /**
     * Returns the coord set defining the non-central coordinates
     * used with this reader.
     *
     * @return  extra positions coordSet
     */
    MultiPointCoordSet getExtraCoordSet();

    /**
     * Indicates whether autoscaling should be applied.
     * If true, before plotting is carried out a scan of all the
     * data values is performed to determine the range of values,
     * and the supplied offsets are scaled accordingly,
     * so that the largest ones are a reasonable size on the screen.
     *
     * @return   true for autoscaling, false to use raw values
     */
    boolean isAutoscale();

    /**
     * Returns a reader for the non-central parts of the MultiPointCoordSet.
     *
     * <p>If {@link #isAutoscale} returns true, the <code>sizeSpan</code>
     * argument will contain a characterisation of the sizes of the items
     * to be plotted, as calculated by an earlier call to the
     * {@link #createSizeReader} method.
     *
     * @param  geom  data geometry used to interpret coordinate values
     * @param  sizeSpan  characteristic size in case of autoscaling,
     *                   otherwise null
     * @return   coordinate reader
     */
    ExtrasReader createExtrasReader( DataGeom geom, Span sizeSpan );

    /**
     * Returns an AuxReader that can obtain the characteristic size
     * of the shapes to be plotted with the data from this reader.
     * The returned AuxReader will be used iff {@link #isAutoscale}
     * returns true.
     *
     * @param  geom  data geometry used to interpret coordinate values
     * @return   multipoint shape size reader
     */
    AuxReader createSizeReader( DataGeom geom );

    /**     
     * Returns the base size scaling value.
     * Manual adjustment may be applied on top of this value.
     *      
     * @param   surface  plot surface
     * @param   sizeSpan   size range calculated from data by request,
     *                     null if autoscale is not in effect
     */         
    double getBaseScale( Surface surface, Span sizeSpan );

    /**
     * Reads the non-central data positions for a MultiPointCoordSet.
     */
    public interface ExtrasReader {

        /**
         * Reads the non-central points from a supplied tuple.
         * The central data position must be supplied as input.
         *
         * @param  tuple  tuple
         * @param  dpos0  nDataDim-element array giving central data position
         * @param  dposExtras   [nPointCount][nDataDim]-shaped array into which
         *                      the non-central data positions will be written
         * @return  true iff the conversion was successful
         */
        boolean readPoints( Tuple tuple, double[] dpos0,
                            double[][] dposExtras );
    }
}
