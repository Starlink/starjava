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
 * Extends {@link SpecData} for types of SpecDataImpl that also
 * implement the EditableSpecDataImpl interface, i.e. this provides
 * facilities for modifying the values and coordinates of a
 * SpecData object. 
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class EditableSpecData
    extends SpecData
{
    /**
     * Reference to the EditableSpecDataImpl.
     */
    protected transient EditableSpecDataImpl editableImpl = null;

    /**
     * Create an instance using the data in a given an EditableSpecDataImpl
     * object.
     *
     * @param impl a concrete implementation of an EditableSpecDataImpl
     *             class that is accessing spectral data in of some format.
     * @exception SplatException thrown if there are problems obtaining
     *                           the spectrum.
     */
    public EditableSpecData( EditableSpecDataImpl impl )
        throws SplatException
    {
        super( impl, true );
        this.editableImpl = impl;
    }

    /**
     * Change the complete spectrum data. Copies all data.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     */
    public void setData( double[] data, double[] coords )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        readData();
    }

    /**
     * Change the complete spectrum data. Doesn't copy data, just
     * keeps references.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     */
    public void setDataQuick( double[] data, double[] coords )
        throws SplatException
    {
        if ( data.length != coords.length ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        editableImpl.setDataQuick( data, coords );
        readData();
    }

    /**
     * Change the complete spectrum data. Copies all data.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     * @param errors the errors of the spectrum data values.
     */
    public void setData( double[] data, double[] coords, double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length && errors == null ) ||
             ( data.length != coords.length && 
               data.length != errors.length ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        editableImpl.setData( data, coords, errors );
        readData();
    }

    /**
     * Change the complete spectrum data. Doesn't copy data.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     * @param errors the errors of the spectrum data values.
     */
    public void setDataQuick( double[] data, double[] coords, double[] errors )
        throws SplatException
    {
        if ( ( data.length != coords.length ) ||
             ( ( errors != null ) && ( data.length != errors.length ) ) ) {
            throw new SplatException( "Data and coordinates must have " +
                                      "the same number of values" );
        }
        editableImpl.setDataQuick( data, coords, errors );
        readData();
    }

    /**
     * Change a coordinate value.
     */
    public void setXDataValue( int index, double value )
        throws SplatException
    {
        editableImpl.setXDataValue( index, value );
        readData();
    }

    /**
     * Change a data value.
     */
    public void setYDataValue( int index, double value )
        throws SplatException
    {
        editableImpl.setYDataValue( index, value );
        readData();
    }

    /**
     * Change a data error value.
     */
    public void setYDataErrorValue( int index, double value )
        throws SplatException
    {
        editableImpl.setYDataErrorValue( index, value );
        readData();
    }

    /**
     * Get access to the FrameSet of the original data set (should be
     * 1 dimensional).
     */
    public FrameSet getFrameSet()
    {
        return impl.getAst();
    }

    /**
     * Accept a FrameSet that defines a new set of coordinates.
     */
    public void setFrameSet( FrameSet frameSet )
        throws SplatException
    {
        // Give the new FrameSet to the implementation, then cause a
        // reset of the coordinates.
        editableImpl.setAst( frameSet );
        readData();
    }

}
