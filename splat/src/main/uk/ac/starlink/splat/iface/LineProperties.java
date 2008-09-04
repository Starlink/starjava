/*
 * Copyright (C) 2001 Central Laboratory of the Research Councils
 * Copyright (C) 2005 Particle Physics and Astronomy Research Council
 * Copyright (C) 2008 Science and Technology Facilities Council
 *
 *  History:
 *     19-JAN-2001 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

import uk.ac.starlink.ast.gui.AstDouble;
import uk.ac.starlink.ast.Frame;
import uk.ac.starlink.splat.plot.DivaPlot;

/**
 * LineProperties is a class for storing and documenting the properties of a
 * spectral line fit.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DivaPlot
 */
public class LineProperties
{
    /**
     * The number of fit types, their enumerations and short descriptions.
     */
    public final static int NTYPES = 4;
    public final static int QUICK = 0;
    public final static int GAUSS = 1;
    public final static int LORENTZ = 2;
    public final static int VOIGT = 3;

    public final static String[] NAMES = {"Quick", "Gaussian",
                                          "Lorentzian", "Voigt"};

    /**
     * The symbolic names of the properties we're storing for each fit type.
     * First index is a fit type.
     */
    public final static String[][] PROP_NAMES = {
        /* QUICK */
        {"ID", "Peak", "Centre", "Width", "Equiv", "Flux", "Asym"},
        /* GAUSS */
        {"ID", "Peak", "PeakErr", "Centre", "CentreErr", "Width", "WidthErr", "FWHM", "FWHMErr", "Flux", "FluxErr", "Rms"},
        /* LORENTZ */
        {"ID", "Peak", "PeakErr", "Centre", "CentreErr", "Width", "WidthErr", "FWHM", "FWHMErr", "Flux", "FluxErr", "Rms"},
        /* VOIGT */
        {"ID", "Peak", "PeakErr", "Centre", "CentreErr", "Gwidth", "GwidthErr", "Lwidth", "LwidthErr", "FWHM", "FWHMErr", "Flux", "FluxErr", "Rms"}
    };

    /**
     * Array for the line properties.
     */
    private double[] values = null;

    /**
     * Methods used for wrapping properties as Number Objects. These can
     * define how the values are represented when displayed. First index is a
     * fit type.
     */
    protected final static int INTEGER = 0;
    protected final static int AST_DOUBLE_X = 1;
    protected final static int AST_DOUBLE_Y = 2;
    protected final static int DOUBLE = 3;

    protected final static int[][] WRAPPERS = {
        /* QUICK */
        {INTEGER, AST_DOUBLE_Y, AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_X,
         DOUBLE, DOUBLE},
        /* GAUSS */
        {INTEGER, AST_DOUBLE_Y, AST_DOUBLE_Y, AST_DOUBLE_X, AST_DOUBLE_X,
         AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_Y,
         AST_DOUBLE_Y, AST_DOUBLE_Y},
        /* LORENTZ */
        {INTEGER, AST_DOUBLE_Y, AST_DOUBLE_Y, AST_DOUBLE_X, AST_DOUBLE_X,
         AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_Y,
         AST_DOUBLE_Y, AST_DOUBLE_Y},
        /* VOIGT */
        {INTEGER, AST_DOUBLE_Y, AST_DOUBLE_Y, AST_DOUBLE_X, AST_DOUBLE_X,
         AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_X,
         AST_DOUBLE_X, AST_DOUBLE_X, AST_DOUBLE_Y, AST_DOUBLE_Y,
         AST_DOUBLE_Y
        }
    };

    /**
     * The type of properties we're storing.
     */
    protected int type = QUICK;

    /**
     * The DivaPlot that we are working with.
     */
    protected DivaPlot plot = null;

    /**
     * Create an instance.
     *
     * @param type the type of line properties we're dealing with.
     * @param plot the plot
     */
    public LineProperties( int type, DivaPlot plot )
    {
        setPlot( plot );
        this.type = type;
        values = new double[count( type )];
    }

    /**
     * Set the plot used.
     *
     * @param plot The new plot value
     */
    public void setPlot( DivaPlot plot )
    {
        this.plot = plot;
    }

    /**
     * Return number of values we would return given the type of measurements
     * that are being stored.
     *
     * @param type the type of line properties we're dealing with.
     * @return the number of properties stored for the given line fit type.
     */
    public static int count( int type )
    {
        return PROP_NAMES[type].length;
    }

    /**
     * Return the name of a property field.
     *
     * @param type the type of line properties we're dealing with.
     * @param index the index of the property
     * @return description of the property expected to be stored at the given
     *      index.
     */
    public static String getName( int type, int index )
    {
        return PROP_NAMES[type][index];
    }

    /**
     * Get the type of properties that we're storing.
     */
    public int type()
    {
        return type;
    }

    /**
     * Get the value of one of our fields.
     */
    public double getField( int index )
    {
        return values[getValidIndex( index )];
    }

    /**
     * Get the value of one of our field wrapped in a suitable Number object.
     */
    public Number getNumberField( int index )
    {
        Number result = null;
        int localIndex = getValidIndex( index );
        double value = values[localIndex];
        switch ( WRAPPERS[type][localIndex] ) {
           case INTEGER:
               result = new Integer( (int) value );
               break;

           case AST_DOUBLE_X:
               //  Double formatted as for X axis of plot.
               result = new AstDouble( value, (Frame) plot.getMapping(), 1 );
               break;

           case AST_DOUBLE_Y:
               //  Double formatted as for Y axis of plot.
               result = new AstDouble( value, (Frame) plot.getMapping(), 2 );
               break;

           case DOUBLE:
               result = new Double( value );
               break;
        }
        return result;
    }

    /**
     * Get the class one of our fields wrapped in a suitable Number object
     * would use for a given type.
     *
     * @param type Description of the Parameter
     * @param index Description of the Parameter
     * @return The numberClass value
     */
    public static Class getNumberClass( int type, int index )
    {
        try {
            switch ( WRAPPERS[type][index] ) {
               case INTEGER:
                   return Integer.class;
               case AST_DOUBLE_X:
               case AST_DOUBLE_Y:
                   return AstDouble.class;
               default:
                   return Double.class;
            }
        }
        catch ( Exception e ) {
            //  Do nothing.
        }
        return Double.class;
    }

    /**
     * Set the value of one of our fields.
     */
    public void setField( int index, double value )
    {
        values[getValidIndex( index )] = value;
    }

    /**
     * Set all the values, if the given array contains enough.
     */
    public void setFields( double[] values )
    {
        if ( values.length >= count( type ) ) {
            this.values = (double[]) values.clone();
        }
    }

    /**
     * Get a valid type index.
     */
    protected int getValidType( int type )
    {
        if ( type < 0 ) {
            type = 0;
        }
        else if ( type > NTYPES - 1 ) {
            type = NTYPES - 1;
        }
        return type;
    }

    /**
     * Get a valid property index for the current type.
     */
    protected int getValidIndex( int index )
    {
        if ( index < 0 ) {
            index = 0;
        }
        else if ( index > count( type ) - 1 ) {
            index = count( type ) - 1;
        }
        return index;
    }

    /**
     * Write a text description of this line. Uses getNumberField so we
     * reasonable formatting.
     */
    public String toString()
    {
        StringBuffer buffer = new StringBuffer();
        for ( int i = 0; i < values.length; i++ ) {
            buffer.append( getNumberField( i ) + " " );
        }
        return buffer.toString();
    }
}
