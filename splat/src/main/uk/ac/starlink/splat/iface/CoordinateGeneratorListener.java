/*
 * Copyright (C) 2003 Central Laboratory of the Research Councils
 *
 *  History:
 *    03-MAR-2003 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.splat.iface;

import java.util.EventListener;

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
     * Update anything that is required for displaying the new
     * coordinates.
     */
    public void generatedCoordinates();
}
