package uk.ac.starlink.splat.iface;

import java.awt.Graphics2D;
import java.awt.RenderingHints;

import org.jdom.Element;

import uk.ac.starlink.splat.util.AbstractStorableConfig;

/**
 * GraphicsHints defines the RenderingHints that should be used when
 * drawing of a Plot. Currently there are only two possible values,
 * antialiased text, or antialiased text and lines.
 *
 * @since $Date$
 * @since 16-NOV-2000
 * @author Peter W. Draper
 * @version $Id$
 * @copyright Copyright (C) 2000 Central Laboratory of the Research Councils
 * @see Plot, PlotConfig.
 */
public class GraphicsHints extends AbstractStorableConfig
{
    /**
     * Whether to antialias text.
     */
    protected boolean textAntialiased;

    /**
     * Whether to antialias everything.
     */
    protected boolean allAntialiased;

    /**
     * Create an instance.
     */
    public GraphicsHints()
    {
        setDefaults();
    }

    /**
     * Set object back to its defaults.
     */
    public void setDefaults()
    {
        textAntialiased = true;
        allAntialiased = false;
        fireChanged();
    }

    /**
     * See if the text should be antialiased.
     */
    public boolean isTextAntialiased()
    {
        return textAntialiased;
    }

    /**
     * See if everything should be antialiased.
     */
    public boolean isAllAntialiased()
    {
        return allAntialiased;
    }

    /**
     * Set if the text should be antialiased.
     */
    public void setTextAntialiased( boolean textAntialiased )
    {
        this.textAntialiased = textAntialiased;
        fireChanged();
    }

    /**
     * Set if everything should be antialiased.
     */
    public void setAllAntialiased( boolean allAntialiased )
    {
        this.allAntialiased = allAntialiased;
        fireChanged();
    }

    /**
     * Apply the RenderingHints description for the current
     * configuration to a graphics object.
     */
    public void applyRenderingHints( Graphics2D g2 )
    {
        if ( allAntialiased ) {
            g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON );
        } else if ( textAntialiased ) {
            g2.setRenderingHint( RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON );
        }
    }

//
// Encode and decode this object to/from XML representation.
//
    public void encode( Element rootElement )
    {
        addChildElement( rootElement, "textAntialiased", textAntialiased );
        addChildElement( rootElement, "allAntialiased", allAntialiased );
    }

    /**
     * Set the value of a member variable by matching its name to a
     * known local property string.
     */
    public void setFromString( String name, String value )
    {
        if ( name.equals( "textAntialiased" ) ) {
            setTextAntialiased( booleanFromString( value ) );
            return;
        }
        if ( name.equals( "allAntialiased" ) ) {
            setAllAntialiased( booleanFromString( value ) );
            return;
        }
    }    
}
