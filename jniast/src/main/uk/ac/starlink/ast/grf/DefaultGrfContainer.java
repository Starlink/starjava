/*
 * Copyright (C) 2002 Central Laboratory of the Research Councils
 *
 *  History:
 *    31-MAY-2002 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.grf;

/**
 * Java class to store a GRF "context". This means the GRF type, arrays of
 * double precision X and Y coordinates, a GrfState object and an optional
 * integer related value.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DefaultGrfContainer
{
    /**
     * The internal state of container object.
     */
    protected int grfType;
    protected double[] xPositions;
    protected double[] yPositions;
    protected double[] bbox;
    protected double xPos;
    protected double yPos;
    protected double angle;
    protected int value;
    protected DefaultGrfState state = null;
    protected String text = null;

    /**
     * Types of GRF states.
     */
    public final static int LINE = 1;
    public final static int TEXT = 2;
    public final static int MARK = 3;


    /**
     * Create and initialise a default container.
     */
    public DefaultGrfContainer()
    {
        // Do nothing.
    }


    /**
     * Create and initialise a container.
     */
    public DefaultGrfContainer( int type, double[] x, double[] y,
                                DefaultGrfState gstate ) 
    {
        grfType = type;
        xPositions = x;
        yPositions = y;
        state = (DefaultGrfState) gstate.clone();
    }


    /**
     * Create and initialise a container.
     */
    public DefaultGrfContainer( int type, double[] x, double[] y, 
                                int assoc, DefaultGrfState gstate ) 
    {
        grfType = type;
        xPositions = x;
        yPositions = y;
        value = assoc;
        state = (DefaultGrfState) gstate.clone();
    }


    /**
     * Create and initialise a container.
     */
    public DefaultGrfContainer( int type, String ltext, double x, double y,
                                double langle, double[] lbbox, 
                                DefaultGrfState gstate )
    {
        grfType = type;
        xPos = x;
        yPos = y;
        text = new String( ltext );
        angle = langle;
        bbox = lbbox;
        state = (DefaultGrfState) gstate.clone();
    }


    /**
     * Return the internal state.
     */
    public int getType()
    {
        return grfType;
    }


    /**
     * Sets the type attribute of the DefaultGrfContainer object
     */
    public void setType( int type )
    {
        grfType = type;
    }


    /**
     * Gets the int attribute of the DefaultGrfContainer object
     */
    public int getInt()
    {
        return value;
    }


    /**
     * Sets the int attribute of the DefaultGrfContainer object
     */
    public void setInt( int value )
    {
        this.value = value;
    }


    /**
     * Gets the xPositions attribute of the DefaultGrfContainer object
     */
    public double[] getXPositions()
    {
        return xPositions;
    }


    /**
     * Sets the xPositions attribute of the DefaultGrfContainer object
     */
    public void setXPositions( double[] xPositions )
    {
        this.xPositions = xPositions;
    }


    /**
     * Gets the yPositions attribute of the DefaultGrfContainer object
     */
    public double[] getYPositions()
    {
        return yPositions;
    }


    /**
     * Sets the yPositions attribute of the DefaultGrfContainer object
     */
    public void setYPositions( double[] yPositions )
    {
        this.yPositions = yPositions;
    }


    /**
     * Gets the x attribute of the DefaultGrfContainer object
     */
    public double getX()
    {
        return xPos;
    }


    /**
     * Sets the x attribute of the DefaultGrfContainer object
     */
    public void setX( double xPos )
    {
        this.xPos = xPos;
    }


    /**
     * Gets the y attribute of the DefaultGrfContainer object
     */
    public double getY()
    {
        return yPos;
    }


    /**
     * Sets the y attribute of the DefaultGrfContainer object
     */
    public void setY( double yPos )
    {
        this.yPos = yPos;
    }


    /**
     * Gets the angle attribute of the DefaultGrfContainer object
     */
    public double getAngle()
    {
        return angle;
    }


    /**
     * Sets the angle attribute of the DefaultGrfContainer object
     */
    public void setAngle( double angle )
    {
        this.angle = angle;
    }


    /**
     * Gets the bBox attribute of the DefaultGrfContainer object
     */
    public double[] getBBox()
    {
        return bbox;
    }


    /**
     * Sets the bBox attribute of the DefaultGrfContainer object
     */
    public void setBBox( double[] bbox )
    {
        this.bbox = bbox;
    }


    /**
     * Gets the text attribute of the DefaultGrfContainer object
     */
    public String getText()
    {
        return text;
    }


    /**
     * Sets the text attribute of the DefaultGrfContainer object
     */
    public void setText( String text )
    {
        this.text = text;
    }


    /**
     * Gets the grfState attribute of the DefaultGrfContainer object
     */
    public DefaultGrfState getGrfState()
    {
        return state;
    }


    /**
     * Sets the grfState attribute of the DefaultGrfContainer object
     */
    public void setGrfState( DefaultGrfState state )
    {
        this.state = state;
    }
}
