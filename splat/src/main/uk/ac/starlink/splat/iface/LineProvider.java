/*
 * Copyright (C) 2006 Particle Physics and Astronomy Research Council
 *
 *  History:
 *     09-JAN-2006 (Peter W. Draper):
 *       Original version.
 */
package uk.ac.starlink.splat.iface;

/**
 * Defines an interface to be used for interacting with a {@link LineVisitor}
 * control.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public interface LineProvider
{
    /**
     * Move to a view of a line, possibilty restoring some related state
     * information.
     * 
     * @param coords the coordinate of the line.
     * @param state previously returned state information, null for none.
     */
    public void viewLine( double coords, Object state );

    /**
     * Return any state information about the current line.
     * 
     * @return an Object defining the current state
     */
    public Object getLineState();
}
