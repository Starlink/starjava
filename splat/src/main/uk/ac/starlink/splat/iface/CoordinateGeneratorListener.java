/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *    03-MAR-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.util.EventListener;

import uk.ac.starlink.ast.FrameSet;

/**
 * Defines an interface to be used when listening for new coordinates
 * are created for a spectrum.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface CoordinateGeneratorListener
    extends EventListener
{
    /**
     * Accept a new column of coordinates and apply to the spectrum.
     */
    public void acceptGeneratedCoords( double[] coords );

    /**
     * The listener should arrange for the spectrum FrameSet to be
     * modified.
     */
    public void changeFrameSet( FrameSet frameSet );
}
