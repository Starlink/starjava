/*
 * $Id: SceneMetric.java,v 1.3 2001/08/28 06:37:13 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.StrokeRecognizer;
import diva.sketch.recognition.TimedStroke;
import diva.sketch.recognition.Scene;

/**
 * A metric interface for comparing the similarity of
 * two different interpretations of a scene.  Implementations
 * of this interface can be used to evaluate the performance
 * of a scene recognizer.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public interface SceneMetric {
    /** Return the name of this metric so that the score can be
     * associated with a meaningful label.
     */
    public String getName();

    /** Return a similarity metric for the given tree (root1) relative
     * to a correct reference scene interpretation (root2).  The metric
     * should be zero if the two scenes are the same, and one if the
     * two scenes are completely different.  Although all scene
     * metrics are normalized between zero and one, the metrics'
     * meanings are different and thus two different metrics should
     * not be compared to one another.
     *
     * <p>
     * Note that SceneMetrics assume that the given trees are built
     * from the same strokes in the same order.  They also only operate
     * on roots that cover all of the leaves.
     */
    public double apply(Scene db1, Scene db2);
    
//      public double apply(Scene db1, CompositeElement root1,
//              Scene db2, CompositeElement root2);
}




