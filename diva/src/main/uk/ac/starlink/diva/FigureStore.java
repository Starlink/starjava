/*
 * Copyright (C) 2004 Central Laboratory of the Research Councils
 *
 *  History:
 *     22-JAN-2004 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.diva;

/**
 * Interface providing access to a facility for storing and restoring
 * XML descriptions of {@link DrawFigures} that are displayed on a
 * {@link DrawGraphicsPane} by a {@link DrawActions} instance.
 * <p>
 * When used in conjuction with {@link DrawGraphicMenu} this interface
 * will have the {@link #activate()} method invoked, after an update
 * of the current instance of {@link DrawActions} is made.
 *
 * @author Peter W. Draper
 * @version $Id$
 */      
public interface FigureStore
{
    /**
     * Set the {@link DrawActions} instance to use when storing and
     * restoring previous Figures.
     */
    public void setDrawActions( DrawActions drawActions );

    /**
     * Activate the interface so that the user can either choose to
     * save the currently displayed figures, or restore a previously
     * saved set of figures. This may also offer the ability to either
     * clear or append to the existing display.
     */
    public void activate();
}
