/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     27-JAN-2004 (Peter W. Draper):
 *       Original version.
 */

package uk.ac.starlink.ast.gui;

import uk.ac.starlink.ast.Plot; // for javadocs

/**
 * Interface defining access to a {@link Plot} held by some other
 * object. Typically {@link Plot}'s are re-created frequently so
 * holding a single reference is generally a bad idea and inquiries
 * for updates should made. This interface provides that sort of
 * access. 
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public interface AstPlotSource
{
    /** 
     * Access the {@link Plot} held by the implementor of this
     * interface.
     * 
     * @return the {@link Plot}
     */
    public Plot getPlot();
}
