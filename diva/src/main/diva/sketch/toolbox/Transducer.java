/*
 * $Id: Transducer.java,v 1.2 2001/07/22 22:01:59 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.ClipboardOwner;
import diva.sketch.SketchModel;

/**
 * An interface for cut/copy operations that perform recognition on
 * the cut/copied strokes and returns a different DataFlavor than
 * "application/sketch/strokes".  Example transducers include
 * "text/plain", "text/html", "image/bitmap", etc.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.2 $
 * @rating Red
 */
public interface Transducer extends Transferable, ClipboardOwner {
    /** Return a new instance of this transducer that
     * can be applied to the given sketch model.
     */
    public Transducer newInstance(SketchModel in);
}

