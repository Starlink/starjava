/*
 * Copyright (C) 2000-2002 Central Laboratory of the Research Councils
 *
 *  History:
 *    28-SEP-2000 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.ast.grf;

import java.awt.Font;
import java.util.ArrayList;

/**
 * DefaultGrfFontManager is a singleton class for managing the list of known
 * fonts known to the Grf class. Each font can be associated with its position
 * in the list (hence providing the necessary mapping to an integer), but note
 * that as fonts are removed these values change (so a request to map a Font
 * to an integer should be made immediately) before passing to AST integer. A
 * default font that is generally available is provided with index 0.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class DefaultGrfFontManager
{
    /**
     * Create the single class instance.
     */
    private final static DefaultGrfFontManager instance = 
        new DefaultGrfFontManager(); 


    /**
     * Hide the constructor from use.
     */
    private DefaultGrfFontManager()
    {
        defaultFont = new Font( "Lucida Sans", Font.PLAIN, 14 );
        add( defaultFont );
    }


    /**
     * Return reference to the only allowed instance of this class.
     */
    public static DefaultGrfFontManager getReference()
    {
        return instance;
    }


    /**
     * The default font (Lucida Sans 14).
     */
    protected Font defaultFont;

    /**
     * ArrayList of references to all fonts.
     */
    protected ArrayList fonts = new ArrayList();


    /**
     * Get the number of fonts.
     */
    public int count()
    {
        return fonts.size();
    }


    /**
     * Add a font, returns its index.
     */
    public int add( Font font )
    {
        fonts.add( font );
        return count() - 1;
    }


    /**
     * Remove a font by name, returning its index.
     */
    public int remove( Font font )
    {
        int index = fonts.indexOf( font );
        if ( index > 0 ) {
            remove( index );
            return index;
        }
        return -1;
    }


    /**
     * Remove a font by index.
     */
    public void remove( int index )
    {
        if ( index > 0 ) {
            fonts.remove( index );
        }
    }


    /**
     * Get the index of a known font. If not known return the default font
     * index.
     */
    public int getIndex( Font font )
    {
        int index = fonts.indexOf( font );
        if ( index != -1 ) {
            return index;
        }
        return 0;
    }


    /**
     * Get a font by its index. If not known return the default font.
     */
    public Font getFont( int index )
    {
        Font font = null;
        try {
            font = (Font) fonts.get( index );
        }
        catch ( IndexOutOfBoundsException e ) {
            font = defaultFont;
        }
        return font;
    }
}
