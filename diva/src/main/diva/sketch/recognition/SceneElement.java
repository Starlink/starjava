/*
 * $Id: SceneElement.java,v 1.6 2000/08/04 01:24:02 michaels Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import java.awt.geom.Rectangle2D;
import java.util.Set;

/**
 * A scene element is an interface that encompasses various elements
 * that can be found in the scene: stroke elements, composite
 * elements, and choice elements.  Stroke elements represent
 * individual strokes in the scene and lack interpretation.  Composite
 * elements represent interpretations of single strokes, multiple
 * strokes, or other composite elements.  Choice elements are an
 * optimization for the parsing algorithm, and represent a choice
 * between different composite elements that have the same type but
 * different data.
 *
 * @see Scene, StrokeElement, CompositeElement, ChoiceElement
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.6 $
 * @rating Red
 */
public interface SceneElement {
    /**
     * Return the bounds of this element if it is a stroke or the
     * union of the bounds of its children if it is a composite
     * or choice.
     */
    public Rectangle2D getBounds();

    /**
     * Return a set of the parents of this element in the ambiguous
     * parse tree.  The returned set is immutable, so all operations
     * that attempt to modify it will fail with an unsupported
     * operation exception.
     */
    public Set parents();
}
