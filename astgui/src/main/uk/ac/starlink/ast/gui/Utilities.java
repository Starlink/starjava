/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     18-FEB-2004 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.ast.gui;

import java.awt.Container;
import java.awt.Insets;
import uk.ac.starlink.util.gui.GridBagLayouter;

/**
 * Static utilities class for astgui package.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public class Utilities
{
    /** Static methods, so no constructor. */
    private Utilities()
    {
        // Do nothing.
    }

    /**
     * Get the usual GridBagLayouter used by this package. 
     * This is a GridBagLayouter.SCHEME3 with our insets.
     */
    public static GridBagLayouter getGridBagLayouter( Container container )
    {
        GridBagLayouter layouter = 
            new GridBagLayouter( container, GridBagLayouter.SCHEME3 );
        layouter.setInsets( Utilities.getStdInsets() );
        return layouter;
    }

    private static Insets insets = new Insets( 5, 2, 2, 5 );

    /**
     * Get standard Insets for this component.
     */
    public static Insets getStdInsets()
    {
        return insets;
    }
}
