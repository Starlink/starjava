/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *     28-NOV-2000 (Peter W. Draper):
 *        Original version.
 *     14-APR-2004 (Peter W. Draper):
 *        Changed to differentiate left/right and top/bottom space.
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
     * Fraction of plot to keep for left labels.
     */
    protected double xLeft;

    /**
     * Fraction of plot to keep for right labels.
     */
    protected double xRight;

    /**
     * Fraction of plot to keep for the top labels.
     */
    protected double yTop;

    /**
     * Fraction of plot to keep for the bottom labels.
     */
    protected double yBottom;

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
        clip = false;
        xLeft = 0.05;
        xRight = 0.00;
        yTop = 0.05;
        yBottom = 0.05;
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
     * Get the fraction of display reserved for X labelling on the left.
     */
    public double getXLeft()
    {
        return xLeft;
    }

    /**
     * Get the fraction of display reserved for X labelling on the right.
     */
    public double getXRight()
    {
        return xRight;
    }

    /**
     * Set the fraction of display reserved for X labelling on the left.
     */
    public void setXLeft( double xLeft )
    {
        this.xLeft = xLeft;
        fireChanged();
    }

    /**
     * Set the fraction of display reserved for X labelling on the right.
     */
    public void setXRight( double xRight )
    {
        this.xRight = xRight;
        fireChanged();
    }

    /**
     * Get the fraction of display reserved for Y labelling at the top.
     */
    public double getYTop()
    {
        return yTop;
    }

    /**
     * Get the fraction of display reserved for Y labelling at the bottom.
     */
    public double getYBottom()
    {
        return yBottom;
    }

    /**
     * Set the fraction of display reserved for Y labelling at the top.
     */
    public void setYTop( double yTop )
    {
        this.yTop = yTop;
        fireChanged();
    }

    /**
     * Set the fraction of display reserved for Y labelling at the bottom.
     */
    public void setYBottom( double yBottom )
    {
        this.yBottom = yBottom;
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
        addChildElement( rootElement, "xLeft", xLeft );
        addChildElement( rootElement, "xRight", xRight );
        addChildElement( rootElement, "yTop", yTop );
        addChildElement( rootElement, "yBottom", yBottom );
        addChildElement( rootElement, "clip", clip );
    }

    /**
     * Set the value of a member variable by matching its name to a
     * known local property string.
     */
    public void setFromString( String name, String value )
    {
        if ( name.equals( "xLeft" ) ) {
            setXLeft( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "xRight" ) ) {
            setXRight( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "yTop" ) ) {
            setYTop( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "yBottom" ) ) {
            setYBottom( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "clip" ) ) {
            setClipped( booleanFromString( value ) );
            return;
        }

        //  Backwards compatibility support. Left/right and top/bottom were a
        //  single value.
        if ( name.equals( "xFrac" ) ) {
            setXLeft( doubleFromString( value ) );
            setXRight( doubleFromString( value ) );
            return;
        }
        if ( name.equals( "yFrac" ) ) {
            setYTop( doubleFromString( value ) );
            setYBottom( doubleFromString( value ) );
            return;
        }
    }    
}
