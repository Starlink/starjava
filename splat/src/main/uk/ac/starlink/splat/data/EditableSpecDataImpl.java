/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     03-FEB-2003 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.data;

import uk.ac.starlink.splat.util.SplatException;

/**
 * Extends the SpecDataImpl class to include methods for modifying the
 * spectrum values.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public abstract class EditableSpecDataImpl
    extends SpecDataImpl
{
    /**
     * Constructor - create instance of class.
     *
     * @param name The specification of the spectrum (disk file name
     * etc.).
     */
    public EditableSpecDataImpl( String name ) 
    {
        super( name );
    }
    
    /**
     * Constructor, clone from another spectrum.
     *
     * @param name a symbolic name for the spectrum.
     * @param spectrum the spectrum to clone.
     */
    public EditableSpecDataImpl( String name, SpecData spectrum ) 
    {
        super( name, spectrum );
    }

    /**
     * Change the complete spectrum data. Takes a copy of all data.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     */
    abstract public void setData( double[] data, double[] coords )
        throws SplatException;

    /**
     * Change the complete spectrum data. Original data is not copied.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     */
    abstract public void setDataQuick( double[] data, double[] coords )
        throws SplatException;

    /**
     * Change the complete spectrum data. Takes a copy of all data.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     * @param errors the errors of the spectrum data values.
     */
    abstract public void setData( double[] data, double[] coords, 
                                  double[] errors )
        throws SplatException;

    /**
     * Change the complete spectrum data. Original data is not copied.
     *
     * @param data the spectrum data values.
     * @param coords the spectrum coordinates, one per data value.
     * @param errors the errors of the spectrum data values.
     */
    abstract public void setDataQuick( double[] data, double[] coords, 
                                       double[] errors )
        throws SplatException;

    /**
     * Change a coordinate value.
     */
    abstract public void setXDataValue( int index, double value )
        throws SplatException;

    /**
     * Change a data value.
     */
    abstract public void setYDataValue( int index, double value )
        throws SplatException;

    /**
     * Change a data error value.
     */
    abstract public void setYDataErrorValue( int index, double value )
        throws SplatException;
}

