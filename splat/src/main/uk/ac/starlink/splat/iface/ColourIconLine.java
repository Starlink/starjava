// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    21-NOV-2000 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;
import uk.ac.starlink.ast.grf.DefaultGrf;
import uk.ac.starlink.ast.grf.DefaultGrfState;
import uk.ac.starlink.ast.grf.DefaultGrfContainer;

/**
 * Creates an Icon that shows a line that can have its colour, thickness and
 * style set to those that match a spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 * @see DefaultGrf
 */
public class ColourIconLine 
    implements Icon
{
    /**
     * Grf State object for storing line properties.
     */
    protected DefaultGrfState state = new DefaultGrfState();

    /**
     * Grf container for all graphics properties.
     */
    protected DefaultGrfContainer container = new DefaultGrfContainer();

    /**
     * Total width of the icon.
     */
    protected int totalWidth = 12;

    /**
     * Total height of the Icon.
     */
    protected int totalHeight = 12;

    /**
     * Width of the border region.
     */
    protected int borderWidth = 2;


    /**
     * Create an icon with the given line properties.
     *
     * @param colour the colour of the line.
     * @param thickness the thickness of the line.
     * @param style the style of the line.
     * @param totalWidth the total width of the Icon.
     * @param totalheight Description of the Parameter
     */
    public ColourIconLine( Color colour, int thickness, int style,
                           int totalWidth, int totalheight )
    {
        container.setGrfState( state );
        setLineColour( colour );
        setLineThickness( thickness );
        setLineStyle( style );
        setIconWidth( totalWidth );
        setIconHeight( totalHeight );
    }


    /**
     * Create an icon with the given colour and defaults.
     *
     * @param colour the colour.
     */
    public ColourIconLine( Color colour )
    {
        container.setGrfState( state );
        setLineColour( colour );
    }


    /**
     * Create an icon with all defaults.
     */
    public ColourIconLine()
    {
        container.setGrfState( state );
    }


    /**
     * Set the line colour.
     *
     * @param colour the colour.
     */
    public void setLineColour( Color colour )
    {
        state.setColour( (double) colour.getRGB() );
    }


    /**
     * Get the line colour.
     *
     * @return the line colour.
     */
    public Color getLineColour()
    {
        return new Color( (int) state.getColour() );
    }


    /**
     * Set the line colour.
     *
     * @param colour the colour as RGB int value.
     */
    public void setLineColour( int colour )
    {
        state.setColour( colour );
    }


    /**
     * Set the line thickness.
     *
     * @param thickness the thickness
     */
    public void setLineThickness( int thickness )
    {
        state.setWidth( (double) thickness );
    }


    /**
     * Get the line thickness
     *
     * @return the line thickness.
     */
    public int getLineThickness()
    {
        return (int) state.getWidth();
    }


    /**
     * Set the line style.
     *
     * @param style the line style.
     */
    public void setLineStyle( int style )
    {
        state.setStyle( (double) style );
    }


    /**
     * Get the line style.
     *
     * @return the line style.
     */
    public int getLineStyle()
    {
        return (int) state.getStyle();
    }


    /**
     * Set the icon width.
     *
     * @param width the total width of the icon.
     */
    public void setIconWidth( int width )
    {
        this.totalWidth = width;
    }


    /**
     * Get the icon width.
     *
     * @return the total width of the icon.
     */
    public int getIconWidth()
    {
        return totalWidth;
    }


    /**
     * Set the icon height.
     *
     * @param height the total width of the icon.
     */
    public void setIconHeight( int height )
    {
        this.totalHeight = height;
    }


    /**
     * Get the icon height.
     *
     * @return the total height of the icon.
     */
    public int getIconHeight()
    {
        return totalHeight;
    }


    /**
     * Set the border width.
     *
     * @param width the width of the border region.
     */
    public void setBorderWidth( int width )
    {
        this.borderWidth = width;
    }


    /**
     * Get the border width.
     *
     * @return the width of the border region.
     */
    public int getBorderWidth()
    {
        return borderWidth;
    }


    /**
     * Re-draw the Icon.
     */
    public void paintIcon( Component c, Graphics g, int x, int y )
    {
        double[] xEnds = new double[2];
        xEnds[0] = x + getBorderWidth();
        xEnds[1] = x + getIconWidth() - 2 * getBorderWidth() - 1;
        container.setXPositions( xEnds );

        double[] yEnds = new double[2];
        yEnds[0] = yEnds[1] = y + getIconHeight() * 0.5;
        container.setYPositions( yEnds );

        DefaultGrf.drawLine( (Graphics2D) g, container );
    }
}
