/*
 * $Id: Symbol.java,v 1.14 2000/11/11 10:49:50 johnr Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch;
import diva.sketch.recognition.TimedStroke;
import java.awt.Color;
import java.awt.Shape;
import java.util.Iterator;

/**
 * A tagging interface for type safety that is implemented
 * by either stroke or composite symbols.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.14 $
 * @rating Red
 */
public interface Symbol {
    public static final Color MIXED_COLOR = new Color(128,128,128);
    public static final float MIXED_LINEWIDTH = -1.0f;
    public float getLineWidth();
    public Color getOutline();
    public Color getFill();
    public void setLineWidth(float lineWidth);
    public void setOutline(Color c);
    public void setFill(Color c);
}

