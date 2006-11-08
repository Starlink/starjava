/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *     24-APR-2003 (Peter W. Draper):
 *        Original version.
 */
package uk.ac.starlink.splat.iface;

//
// Not used as couldn't get the realization of the widget to happen
// reliably. See SplashWindow instead.
//

/**
 * Class that creates an instance of the SPLAT splash screen shown
 * during startup. This is just the AboutFrame wrapped to be a plain
 * window and positioned at the centre of the screen.
 *
 * @author Peter W. Draper (Starlink, University of Durham);
 * @version $Id$
 */
public class SplatSplash 
    extends AboutFrame
{
    /**
     * Create an instance. This produces a copy of the AboutFrame
     * which is positioned at the centre of the screen and which has
     * no decorations. Note the window is not realized, do this
     * yourself by calling {@link #setVisible}.
     */
    public SplatSplash() 
    {
        super( null );

        //  Make window undecorated, but prominant.
        setUndecorated( true );
        setBackground( java.awt.Color.white );

        // Position the window at the centre of the screen.
        setLocationRelativeTo( null );
        setVisible( true );
    } 
}
