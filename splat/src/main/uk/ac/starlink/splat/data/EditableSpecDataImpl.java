/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     03-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.splat.util.SplatException;
import uk.ac.starlink.ast.FrameSet;

/**
 * Extends the SpecDataImpl interface to include methods for modifying the
 * spectrum values.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface EditableSpecDataImpl
    extends SpecDataImpl
{
    /**
     * Change the complete spectrum data.
     * Takes a copy of all data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     */
    public void setSimpleData( double[] coords, String dataUnits, 
                               double[] data )
        throws SplatException;

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet.
     * Takes a copy of all data.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     */
    public void setSimpleUnitData( FrameSet frameSet, double[] coords,
                                   String dataUnits, double[] data )
        throws SplatException;

    /**
     * Change the spectrum data and WCS. Takes a copy of all data.
     *
     * @param frameSet the FrameSet to be used for generating
     *                 coordinates.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     */
    public void setFullData( FrameSet frameSet, String dataUnits, 
                             double[] data )
        throws SplatException;

    /**
     * Change the complete spectrum data. Original data is not copied.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     */
    public void setSimpleDataQuick( double[] coords, String dataUnits, 
                                    double[] data )
        throws SplatException;

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet.
     * Original data is not copied.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     */
    public void setSimpleUnitDataQuick( FrameSet frameSet, double[] coords,
                                        String dataUnits, double[] data )
        throws SplatException;

    /**
     * Change the spectrum data and WCS.
     * Original data is not copied.
     *
     * @param frameSet the FrameSet to be used for generating
     *                 coordinates.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     */
    public void setFullDataQuick( FrameSet frameSet, String dataUnits, 
                                  double[] data )
        throws SplatException;

    /**
     * Change the complete spectrum data.
     * Takes a copy of all data.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleData( double[] coords,  String dataUnits, 
                               double[] data, double[] errors )
        throws SplatException;

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet.
     * Takes a copy of all data.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleUnitData( FrameSet frameSet, double[] coords,
                                   String dataUnits, double[] data, 
                                   double[] errors )
        throws SplatException;

    /**
     * Change the spectrum data and WCS. Takes a copy of all data.
     *
     * @param frameSet the FrameSet to be used for generating
     *                 coordinates.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setFullData( FrameSet frameSet, String dataUnits, 
                             double[] data, double[] errors )
        throws SplatException;

    /**
     * Change the complete spectrum data.
     * Original data is not copied.
     *
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleDataQuick( double[] coords, String dataUnits, 
                                    double[] data, double[] errors )
        throws SplatException;

    /**
     * Change the complete spectrum data, but preserving the properties of an
     * existing FrameSet as part of a new FrameSet.
     * Original data is not copied.
     *
     * @param frameSet the 1D FrameSet to be used for properties. The current
     *                 Frame defines the spectrum coordinate system.
     * @param coords the spectrum coordinates, one per data value.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setSimpleUnitDataQuick( FrameSet frameSet, double[] coords,
                                        String dataUnits, double[] data, 
                                        double[] errors )
        throws SplatException;

    /**
     * Change the complete spectrum data.
     * Original data is not copied.
     *
     * @param frameSet the FrameSet to be used for generating
     *                 coordinates.
     * @param dataUnits the data units, if known.
     * @param data the spectrum data values.
     * @param errors the errors of the spectrum data values.
     */
    public void setFullDataQuick( FrameSet frameSet, String dataUnits, 
                                  double[] data, double[] errors )
        throws SplatException;

    /**
     * Change a coordinate value.
     */
    public void setXDataValue( int index, double value )
        throws SplatException;

    /**
     * Change a data value.
     */
    public void setYDataValue( int index, double value )
        throws SplatException;

    /**
     * Change a data error value.
     */
    public void setYDataErrorValue( int index, double value )
        throws SplatException;

    /**
     * Set the FrameSet used for the coordinate system. This should
     * be one-dimensional (at the base and current frames at least)
     * and map the base coordinates to some wavelength-related
     * coordinate.
     */
    public void setAst( FrameSet frameSet )
        throws SplatException;
}
