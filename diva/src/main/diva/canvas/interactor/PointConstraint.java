/*
 * $Id: PointConstraint.java,v 1.2 2000/05/02 00:43:31 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 *
 */
package diva.canvas.interactor;

import java.awt.geom.Point2D;

/** 
 * An interface implemented by classes that can constrain a point
 * to lie within a certain set of values.
 * 
 * @version $Revision: 1.2 $
 * @author John Reekie
 */
public interface PointConstraint {

    /** Ask the point constraint to constrain this point.
     * The constraint should directly modify the passed
     * point.
     */
    public void constrain(Point2D point);

    /** Ask the point constraint call to constrain()
     * cause a "snap." A snap means that the output value of the point
     * changed from one region to another. If a snap did occur, the
     * caller will probably fire a ConstraintEvent.
     */
    public boolean snapped();
}

