/*
 * $Id: BulletedData.java,v 1.3 2001/07/22 22:01:56 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.Type;

/**
 * Native class that defines a collection of TextLine's. It also defines 
 * the avg x-height and character width. 
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @author Niraj Shah  (niraj@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class BulletedData extends TextAnnotations {
    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(BulletedData.class);
    public BulletedData() {
    }
    public BulletedData(double charHeight, double charWidth) {
        super(charHeight, charWidth);
    }
    public Type getType() {
        return BulletedData.type;
    }
}

