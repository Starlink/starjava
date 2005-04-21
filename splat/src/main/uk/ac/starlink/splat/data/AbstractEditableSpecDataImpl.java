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
 * Extends the {@link SpecDataImpl} class to include methods for modifying the
 * spectrum values by implementing the {@link EditableSpecDataImpl} interface.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public abstract class AbstractEditableSpecDataImpl
    extends AbstractSpecDataImpl
    implements EditableSpecDataImpl
{
    /**
     * Constructor - create instance of class.
     *
     * @param name The specification of the spectrum (disk file name
     * etc.).
     */
    public AbstractEditableSpecDataImpl( String name )
        throws SplatException
    {
        super( name );
    }

    /**
     * Constructor, clone from another spectrum.
     *
     * @param name a symbolic name for the spectrum.
     * @param spectrum the spectrum to clone.
     */
    public AbstractEditableSpecDataImpl( String name, SpecData spectrum )
        throws SplatException
    {
        super( name, spectrum );
    }

    abstract public void setSimpleData( double[] coords, String dataUnits,
                                        double[] data )
        throws SplatException;

    abstract public void setSimpleUnitData( FrameSet frameSet, double[] coords,
                                            String dataUnits, double[] data )
        throws SplatException;

    abstract public void setFullData( FrameSet frameSet, String dataUnits,
                                      double[] data )
        throws SplatException;

    abstract public void setSimpleDataQuick( double[] coords, String dataUnits,
                                             double[] data )
        throws SplatException;

    abstract public void setSimpleUnitDataQuick( FrameSet frameSet,
                                                 double[] coords,
                                                 String dataUnits,
                                                 double[] data )
        throws SplatException;

    abstract public void setFullDataQuick( FrameSet frameSet,
                                           String dataUnits, double[] data )
        throws SplatException;

    abstract public void setSimpleData( double[] coords,  String dataUnits,
                                        double[] data, double[] errors )
        throws SplatException;

    abstract public void setSimpleUnitData( FrameSet frameSet, double[] coords,
                                            String dataUnits, double[] data,
                                            double[] errors )
        throws SplatException;

    abstract public void setFullData( FrameSet frameSet, String dataUnits,
                                      double[] data, double[] errors )
        throws SplatException;

    abstract public void setSimpleDataQuick( double[] coords, String dataUnits,
                                             double[] data, double[] errors )
        throws SplatException;

    abstract public void setSimpleUnitDataQuick( FrameSet frameSet,
                                                 double[] coords,
                                                 String dataUnits,
                                                 double[] data,
                                                 double[] errors )
        throws SplatException;

    abstract public void setFullDataQuick( FrameSet frameSet, String dataUnits,
                                           double[] data, double[] errors )
        throws SplatException;

    abstract public void setXDataValue( int index, double value )
        throws SplatException;

    abstract public void setYDataValue( int index, double value )
        throws SplatException;

    abstract public void setYDataErrorValue( int index, double value )
        throws SplatException;

    abstract public void setAst( FrameSet frameSet )
        throws SplatException;
}
