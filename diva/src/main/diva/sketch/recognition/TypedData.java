/*
 * $Id: TypedData.java,v 1.5 2001/08/28 06:34:12 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */

package diva.sketch.recognition;
import java.util.Set;

/**
 * Typed data refers to a piece of semantic data that results from a
 * recognition process and has an associated type.  For example, a
 * SquareRecognizer might return some SquareData that describes a
 * square that it recognized, and that SquareData might has an associated
 * Type object (available through getType()) that
 * uniquely identifies squares.
 *
 * <p>
 * Most custom recognizers that understand the semantics of the
 * things that they are recognizing will return some custom
 * implementation of the TypedData interface.  For example, a
 * handwriting recognizer might retun a TextData object in its
 * recognition results, which contains the actual text that was
 * recognized and whose getType() method returns a type object
 * that uniquely identifies text.
 *
 * <p>
 * In contrast, there are some generic recognizers that actually
 * know nothing about the semantics of the things they are recognizing.
 * For example, BasicStrokeRecognizer only knows about feature
 * vectors that it extracts from strokes, and is able to map
 * these to string types, such as "square," "circle," or "triangle."
 * For convenience, there is also an implementation of TypedData
 * called SimpleData that actually provides an implementation
 * for such cases when there is no extra semantic information available,
 * and also when the type system has to be dynamic (e.g. if the types
 * are specified in a text file and there is no associated class
 * to go along with them).
 *
 * <p>
 * Additionally, TypedData extends XmlBuilder, so that recognition
 * results can be saved to and from XML files.
 *
 * @see Type
 * @see SimpleData
 * @see Recognition
 * @see SceneElement
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.5 $
 * @rating      Red
 */
public interface TypedData extends diva.util.xml.XmlBuilder {
    /**
     * Return the uniquely identifying type associated
     * with this piece of data.
     */
    public Type getType();
}




