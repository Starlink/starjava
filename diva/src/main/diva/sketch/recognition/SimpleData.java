/*
 * $Id: SimpleData.java,v 1.8 2001/07/22 22:01:54 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;
import diva.util.xml.XmlElement;
import diva.util.xml.AbstractXmlBuilder;

/**
 * An instance of typed data that represents dynamic, user-defined
 * types.  If you are writing a low-level recognizer and that
 * recognizes strokes based on a feature vector and knows nothing
 * about the semantics of the recognition, other than a string
 * representation of the type, then this is the class for you
 * (e.g. new SimpleData("scribble")).  However, if you have semantic
 * knowledge of the data that is being represented, e.g. the number of
 * sides on the polygon that you just recognized, then you should
 * probably be a statically-typed form of TypedData
 * (e.g. PolygonData).
 *
 * @see Type
 * @see TypedData
 * @author 	Michael Shilman (michaels@eecs.berkeley.edu)
 * @version	$Revision: 1.8 $
 * @rating      Red
 */
public final class SimpleData extends AbstractXmlBuilder implements TypedData {
    /**
     * The dynamic type identifier associated with
     * this empty data.
     */
    private String _id;

    /** Used in the builder interface--do not call
     * this constructor from code!
     */
    public SimpleData() {
    }

    /**
     * Construct a simple data with the given dynamic
     * identifier.  Throw an illegal argument exception
     * if the given identifier is already registered as
     * static in the type system.
     *
     * @see Type.isStaticType(String)
     */
    public SimpleData(String typeId) {
        setTypeID(typeId);
    }
	
    /**
     * Construct a simple data with the given dynamic
     * identifier.
     */
    public Type getType() {
        return Type.makeType(_id);
    }

    /**
     * Return the dynamic type identifier of this data item.
     */
    public String getTypeID() {
        return _id;
    }

    /**
     * Equality test: are the types equal?
     */
    public boolean equals(Object o) {
        if(o instanceof SimpleData) {
            String id = ((SimpleData)o).getTypeID();
            return _id.equals(id);
        }
        return false;
    }

    /** 
     * Set the type id of this data.
     * @throws IllegalArgumentException if the type ID is already
     *   registered as static with the Type class.
     */
    private void setTypeID(String typeId) {
        if(Type.isStaticType(typeId)) {
            String err = "Type ID " + typeId +
                " is already registered as static!";
            throw new IllegalArgumentException(err);
        }
        _id = typeId;
    }

    /**
     * Print the dynamic type of this data in brackets as
     * short hand to denote that it is a dynamic type.
     */
    public String toString() {
        return "<" + _id + ">";
    }

    /** Generate an XML element from this instance
     */
    public XmlElement generate(Object in) {
        XmlElement out = new XmlElement("SimpleData");
        out.setAttribute("type", ((SimpleData)in).getTypeID());
        return out;
    }

    
    /** Build a SimpleData from the given XmlElement
     */
    public Object build(XmlElement in, String type) {
        setTypeID(in.getAttribute("type"));
        return this;
    }
}

