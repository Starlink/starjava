package uk.ac.starlink.frog.plot;

import java.awt.Color;

/**
 * FigureProps is a simple container class for passing configuration
 * properties of Diva Figures around.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see Figure
 * @see Plot
 */

public class FigureProps
{
    /**
     * X coordinate of some position in figure.
     */ 
    private double xCoordinate;

    /**
     * Y coordinate of some position in figure.
     */ 
    private double yCoordinate;

    /**
     * Some X dimension.
     */ 
    private double xLength;

    /**
     * Some Y dimension.
     */ 
    private double yLength;

    /**
     * X coordinate of a second position in the figure.
     */
    private double xSecondary;

    /**
     * Y coordinate of a second position in the figure.
     */
    private double ySecondary;

    /**
     * A main colour.
     */ 
    private Color mainColour;

    /**
     * A secondary colour.
     */
    private Color secondaryColour;

    /**
     *  Default constructor. All items keep their default values.
     */
    public FigureProps()
    {
        reset();
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle. 
     */
    public FigureProps( double x, double y, double width, 
                        double height )
    {
        reset();
        setXCoordinate( x );
        setYCoordinate( y );
        setXLength( width );
        setYLength( height );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle. 
     */
    public FigureProps( double x, double y, double width, 
                        double height, Color mainColour )
    {
        reset();
        setXCoordinate( x );
        setYCoordinate( y );
        setXLength( width );
        setYLength( height );
        setMainColour( mainColour );
    }

    /**
     *  Constructor that provides enough information to describe a
     *  rectangle, with other colour.
     */
    public FigureProps( double xCoordinate, double yCoordinate, 
                        double width, double height, 
                        double xSecondary, double ySecondary,
                        Color mainColour, Color secondaryColor )
    {
        reset();
        setXCoordinate( xCoordinate );
        setYCoordinate( yCoordinate );
        setXSecondary( xSecondary );
        setYSecondary( ySecondary );
        setXLength( width );
        setYLength( height );
        setMainColour( mainColour );
        setSecondaryColour( secondaryColour );
    }

    /**
     *  Reset all items to their defaults.
     */
    public void reset()
    {
        setXCoordinate( 0.0 );
        setYCoordinate( 0.0 );
        setXSecondary( 0.0 );
        setYSecondary( 0.0 );
        setXLength( 1.0 );
        setYLength( 1.0 );
        setMainColour( Color.blue );
        setSecondaryColour( Color.black );
    }

    /**
     * Get the value of xCoordinate.
     *
     * @return value of xCoordinate.
     */
    public double getXCoordinate() 
    {
        return xCoordinate;
    }
    
    /**
     * Set the value of xCoordinate.
     *
     * @param v  Value to assign to xCoordinate.
     */
    public void setXCoordinate(double  v)  
    {
        this.xCoordinate = v;
    }
    
    /**
     * Get the value of yCoordinate.
     *
     * @return value of yCoordinate.
     */
    public double getYCoordinate() 
    {
        return yCoordinate;
    }
    
    /**
     * Set the value of yCoordinate.
     *
     * @param v  Value to assign to yCoordinate.
     */
    public void setYCoordinate(double  v) 
    {
        this.yCoordinate = v;
    }

    /**
     * Get the value of xSecondary.
     *
     * @return value of xSecondary.
     */
    public double getXSecondary() 
    {
        return xSecondary;
    }
    
    /**
     * Set the value of xSecondary.
     * @param v  Value to assign to xSecondary.
     */
    public void setXSecondary(double  v) 
    {
        this.xSecondary = v;
    }
    
    /**
     * Get the value of ySecondary.
     * @return value of ySecondary.
     */
    public double getYSecondary() 
    {
        return ySecondary;
    }
    
    /**
     * Set the value of ySecondary.
     * @param v  Value to assign to ySecondary.
     */
    public void setYSecondary(double  v) 
    {
        this.ySecondary = v;
    }

    /**
     * Get the value of xLength.
     * @return value of xLength.
     */
    public double getXLength() 
    {
        return xLength;
    }
    
    /**
     * Set the value of xLength.
     * @param v  Value to assign to xLength.
     */
    public void setXLength(double  v) 
    {
        this.xLength = v;
    }
    
    /**
     * Get the value of yLength.
     * @return value of yLength.
     */
    public double getYLength() 
    {
        return yLength;
    }
    
    /**
     * Set the value of yLength.
     * @param v  Value to assign to yLength.
     */
    public void setYLength(double  v) 
    {
        this.yLength = v;
    }

    /**
     * Get the value of mainColour.
     * @return value of mainColour.
     */
    public Color getMainColour() 
    {
        return mainColour;
    }
    
    /**
     * Set the value of mainColour.
     * @param v  Value to assign to mainColour.
     */
    public void setMainColour(Color  v) 
    {
        this.mainColour = v;
    }

    /**
     * Get the value of secondaryColour.
     * @return value of secondaryColour.
     */
    public Color getSecondaryColour() 
    {
        return secondaryColour;
    }
    
    /**
     * Set the value of secondaryColour.
     * @param v  Value to assign to secondaryColour.
     */
    public void setSecondaryColour(Color  v) 
    {
        this.secondaryColour = v;
    }
}
