/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: CanvasFigureGroup.java,v 1.2 2002/07/09 13:30:36 brighton Exp $
 */

package jsky.graphics;

/**
 * This defines an abstract interface for a group of canvas figures that should be
 * displayed and selected together as a unit.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public abstract interface CanvasFigureGroup extends CanvasFigure {

    /**
     * Add a figure to the group.
     */
    public void add(CanvasFigure fig);

    /**
     * Remove a figure from the group.
     */
    public void remove(CanvasFigure fig);
}

