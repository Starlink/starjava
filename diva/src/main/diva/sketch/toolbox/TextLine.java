/*
 * $Id: TextLine.java,v 1.4 2001/07/22 22:01:59 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.Type;

/**
 * Native class that defines a line of text. It also defines the avg 
 * x-height and character width. 
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @author Niraj Shah  (niraj@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class TextLine extends TextAnnotations {
    /**
     * The static type associated with this typed data.
     */
    public static final Type type = Type.makeType(TextLine.class);
    public TextLine() {
        super();
    }
    public TextLine(double charHeight, double charWidth) {
        super(charHeight, charWidth);
    }
    public Type getType() {
        return TextLine.type;
    }
}

