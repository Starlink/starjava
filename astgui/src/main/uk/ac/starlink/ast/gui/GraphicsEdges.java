/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-NOV-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.gui;

import org.w3c.dom.Element;

/**
 * GraphicsEdges defines options about how the edges of a plot should
 * be drawn (note these are not AST plot related). Currently this
 * covers the amount of space reserved around the plot for data labels
 * and whether any graphics should be clipped to lie within the border.
 *
 * @author Peter W. Draper
 * @version $Id$
 *
 * @see PlotConfigurator
 * @see PlotConfiguration
 */
public class GraphicsEdges 
    extends AbstractPlotControlsModel
{
    /**
     * Whether to clip the plotted data.
     */
    protected boolean clip;

    /**
     * Fraction of plot to keep for the X axis labels.
     */
    protected double xFrac;

    /**
     * Fraction of plot to keep for the Y axis labels.
     */
    protected double yFrac;

    /**
     * The suggested minimum fraction.
     */
    public static final double GAP_MIN = 0.0;
    
    /**
     * The suggested maximum fraction.
     */
    public static final double GAP_MAX = 0.4;

    /**
     * The suggested fraction resolution (i.e. interval between steps).
     */
    public static final double GAP_STEP = 0.005;

    /**
     * Create an instance.
     */
    public GraphicsEdges()
    {
        setDefaults();
    }
    
    /**
     * Set object back to its defaults.
     */
    public void setDefaults()
    {
        clip = true;
        xFrac = 0.05;
        yFrac = 0.0;
        fireChanged();
    }

    /**
     * See if graphics should be clipped to lie within border.
     */
    public boolean isClipped()
    {
        return clip;
    }

    /**
     * Set if graphics should be clipped to lie within border.
     */
    public void setClipped( boolean clip )
    {
        this.clip = clip;
        fireChanged();
    }

    /**
     * Get the fraction of display reserved for X labelling.
     */
    public double getXFrac()
    {
        return xFrac;
    }

    /**
     * Set the fraction of display reserved for X labelling.
     */
    public void setXFrac( double xFrac )
    {
        this.xFrac = xFrac;
        fireChanged();
    }

    /**
     * Get the fraction of display reserved for Y labelling.
     */
    public double getYFrac()
    {
        return yFrac;
    }

    /**
     * Set the fraction of display reserved for Y labelling.
     */
    public void setYFrac( double yFrac )
    {
        this.yFrac = yFrac;
        fireChanged();
    }

//
// Encode and decode this object to/from XML representation.
//
    /**
     * The name of our enclosing tag.
     */
    public String getTagName()
    {
        return "graphicsedges";
    }

    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "xFrac", xFrac );
        addChildElement( rootElement, "yFrac", yFrac );
        addChildElement( rootElement, "clip", clip );
    }

    /**
     * Set the value of a member variable by matching its name to a
     * known local property string.
     */
    public void setFromString( String name, String value )
    {
        if ( name.equals( "xFrac" ) ) {
            setXFrac( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "yFrac" ) ) {
            setYFrac( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "clip" ) ) {
            setClipped( booleanFromString( value ) );
            return;
        }
    }    
}
