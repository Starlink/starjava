/*
 * $Id: SceneWriter.java,v 1.8 2000/08/04 01:24:02 michaels Exp $
 *
 * Copyright (c) 1998 The Regents of the University of California.
 * All rights reserved.  See the file COPYRIGHT for details.
 */
package diva.sketch.recognition;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;

/**
 * SceneWriter writes a single interpretation of a scene to an output
 * stream.  This interpretation can then be read in by a SceneParser
 * for testing purposes.  It currently has the limitation that it
 * can only write interpretations that have "SimpleData" interpretations
 * of the scene, because it doesn't know how to handle complex data.
 *
 * @see SceneParser
 * @author Michael Shilman      (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.8 $
 * @rating Red
 */
public class SceneWriter {
    /**
     * Write the single interpretation of the scene given rooted by
     * the given root to the character-output stream.  The caller is
     * responsible for closing the stream.  Throw a runtime exception
     * if it encounters typed data that is not of the type
     * SimpleData.
     */
    public void write(Scene db, SceneElement root, Writer writer)
            throws IOException {
        writeHeader(writer);
        writer.write("<" + SceneParser.SCENE_TAG + ">\n");
        writeElement(db, root, "root", "  ", writer);
        writer.write("</"+ SceneParser.SCENE_TAG + ">\n");
        writer.flush();
    }

    /**
     * Write the stroke information (x, y, timestamp) and its
     * label (indicating either positive or negative example) to the
     * character-output stream.
     */
    private void writeElement(Scene db, SceneElement root, String name,
            String prefix, Writer writer) throws IOException {
        writer.write(prefix);
        if(root instanceof StrokeElement) {
            StrokeElement strokeElt = (StrokeElement)root;
            writeStroke(strokeElt.getStroke(), writer);
        }
        else {
            CompositeElement compElt = (CompositeElement)root;
            writer.write("<" + SceneParser.SCENE_ELEMENT_TAG + " " 
                    + SceneParser.NAME_TAG + "=\"" + name + "\" "
                    + SceneParser.TYPE_TAG + "=\"");
            TypedData data = compElt.getData();
            if(data instanceof SimpleData) {
                writer.write(((SimpleData)data).getTypeID());
            }
            else {
                String err = "Only support simple types";
                throw new UnsupportedOperationException(err);
            }
            writer.write("\" " + SceneParser.CONFIDENCE_TAG + "=\"");
            writer.write(String.valueOf(compElt.getConfidence()));
            writer.write("\">\n");
            //write type information
            Iterator j = compElt.childNames().iterator();
            for(Iterator i = compElt.children().iterator(); i.hasNext(); ) {
                SceneElement elt = (SceneElement)i.next();
                String cname = (String)j.next();
                writeElement(db, elt, cname, prefix+"  ", writer);
            }
            writer.write(prefix);
            writer.write("</" + SceneParser.SCENE_ELEMENT_TAG + ">\n");
        }
    }
    

    /**
     * Write the stroke information (x, y, timestamp) and its
     * label (indicating either positive or negative example) to the
     * character-output stream.
     */
    private void writeStroke(TimedStroke stroke, Writer writer)
            throws IOException {
        writer.write("<" + SceneParser.STROKE_ELEMENT_TAG + " " + SceneParser.POINTS_TAG + "=\"");
        if(stroke == null){
            System.out.println("NULL stroke!");
        }
        int len = stroke.getVertexCount();
        for(int k=0; k<len; k++){
            double x = stroke.getX(k);
            String xs = String.valueOf(x);
            writer.write(xs);
            writer.write(" ");
            double y = stroke.getY(k);
            String ys = String.valueOf(y);
            writer.write(ys);
            writer.write(" ");
            long t = stroke.getTimestamp(k);
            String ts = String.valueOf(t);
            writer.write(ts);
            if(k != (len-1)){
                writer.write(" ");
            }
        }
        writer.write("\"/>\n");
    }
    
    /**
     * Write header information to the character-output stream.
     */
    private void writeHeader(Writer writer) throws IOException {
        writer.write("<?xml version=\"1.0\" standalone=\"no\"?>\n");
        writer.write("<!DOCTYPE " + SceneParser.SCENE_TAG + " PUBLIC \""
                + SceneParser.PUBLIC_ID + "\"\n\t\""
                + SceneParser.DTD_URL + "\">\n\n");
    }
}
