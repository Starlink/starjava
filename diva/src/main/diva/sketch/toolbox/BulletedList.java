/*
 * $Id: BulletedList.java,v 1.3 2001/07/22 22:01:57 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.Type;
import diva.sketch.recognition.TypedData;

/**
 * Native class that defines a collection of TextLine's. It also defines 
 * the avg x-height and character width. 
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @author Niraj Shah  (niraj@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class BulletedList extends TextAnnotations {
    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(BulletedList.class);
    private double _charHeight;
    private double _charWidth;
    public BulletedList(double ch, double cw) {
        _charHeight = ch;
        _charWidth = cw;
    }
    public Type getType() {
        return BulletedList.type;
    }
    public double getCharHeight() {
        return _charHeight;
    }
    public double getCharWidth() {
        return _charWidth;
    }
    public String toString() {
        return "BulletedList[ charHeight = " + _charHeight + 
		  ", charWidth = " + _charWidth + "]";
    }
}

