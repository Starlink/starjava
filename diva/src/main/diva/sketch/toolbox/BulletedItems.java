/*
 * $Id: BulletedItems.java,v 1.4 2001/07/23 04:11:24 johnr Exp $
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
 * @author Niraj Shah  (niraj@eecs.berkeley.edu)
 * $version $Revision: 1.4 $
 * @rating Red
 */
public class BulletedItems extends TextAnnotations {
    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(BulletedItems.class);
    public BulletedItems() {
        super();
    }
    public BulletedItems(double charHeight, double charWidth) {
        super(charHeight,charWidth);
    }
    public Type getType() {
        return BulletedItems.type;
    }
}

