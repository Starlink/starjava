package uk.ac.starlink.splat.iface;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
 *  Creates a rectangular Icon that is painted with a given colour and
 *  has a coloured border of a specified thickness.
 *
 *  @since $Date$
 *  @since 26-SEP-2000
 *  @author Peter W. Draper
 *  @version $Id$
 */
public class ColourIcon implements Icon 
{
    /**
     *  Colour of the icon.
     */
    protected Color mainColour = Color.red;

    /**
     *  Border colour of the icon.
     */
    protected Color borderColour = Color.black;

    /**
     *  Width of the icon.
     */
    protected int totalWidth = 12;

    /**
     *  Height of the Icon.
     */
    protected int totalHeight = 12;

    /**
     *  Width of the coloured border.
     */
    protected int borderWidth = 2;

    /**
     *  Create an icon with the given colours and sizes.
     *
     *  @param mainColour the colour.
     *  @param width the total width.
     *  @param height the total height.
     *  @param borderColour the colour of the border.
     *  @param thickness the thickness of the border.
     */
    public ColourIcon( Color mainColour, int width, int height,
                       Color borderColour, int thickness ) 
    {
        setMainColour( mainColour );
        setIconWidth( width );
        setIconHeight( height );
        setBorderWidth( thickness );
        setBorderColour( borderColour );
    }

    /**
     *  Create an icon with the given colour and defaults.
     *
     *  @param mainColour the colour.
     */
    public ColourIcon( Color colour ) 
    {
        setMainColour( colour );
    }

    /**
     *  Set the icon colour.
     *
     *  @param mainColour the colour.
     */
    public void setMainColour( Color mainColour ) 
    {
        this.mainColour = mainColour;
    }

    /**
     *  Get the icon colour.
     *
     *  @return the icon colour.
     */
    public Color getMainColour() 
    {
        return mainColour;
    }

    /**
     *  Set the border colour.
     *
     *  @param borderColour the colour.
     */
    public void setBorderColour( Color borderColour ) 
    {
        this.borderColour = borderColour;
    }

    /**
     *  Get the border colour.
     *
     *  @return the border colour.
     */
    public Color getBorderColour() 
    {
        return borderColour;
    }

    /**
     *  Set the icon width.
     *
     *  @param width the total width of the icon.
     */
    public void setIconWidth( int width ) 
    {
        this.totalWidth = width;
    }

    /**
     *  Get the icon width.
     *
     *  @return the total width of the icon.
     */
    public int getIconWidth() 
    {
        return totalWidth;
    }

    /**
     *  Set the icon height.
     *
     *  @param height the total width of the icon.
     */
    public void setIconHeight( int height ) 
    {
        this.totalHeight = height;
    }

    /**
     *  Get the icon height.
     *
     *  @return the total height of the icon.
     */
    public int getIconHeight() 
    {
        return totalHeight;
    }

    /**
     *  Set the border width.
     *
     *  @param width the width of the border region.
     */
    public void setBorderWidth( int width ) 
    {
        this.borderWidth = width;
    }

    /**
     *  Get the border width.
     *
     *  @return the width of the border region.
     */
    public int getBorderWidth() 
    {
        return borderWidth;
    }

    /**
     *  Re-draw the Icon.
     */
    public void paintIcon( Component c, Graphics g, int x, int y ) 
    {
        g.setColor( borderColour );
        g.fillRect( x, y, getIconWidth() - 1, getIconHeight() - 1 );
        g.setColor( mainColour );
        g.fillRect( x + getBorderWidth(),
                    y + getBorderWidth(),
                    getIconWidth() - 2 * getBorderWidth() - 1,
                    getIconHeight() - 2 * getBorderWidth() - 1 );
    }
}
