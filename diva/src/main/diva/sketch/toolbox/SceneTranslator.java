/*
 * $Id: SceneTranslator.java,v 1.4 2001/08/27 22:21:37 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.toolbox;
import diva.sketch.recognition.*;
import diva.util.xml.*;
import java.io.*;
import java.util.Iterator;

/**
 * A utility program for translating old scene database
 * files into new scene database files.
 *
 * @author  Michael Shilman (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class SceneTranslator {
    public static void main(String args[]) throws Exception {
        if(args.length < 2) {
            System.err.println("Usage: java SceneTranslator [-r] in.xml out.xml");
            System.exit(-1);
        }
        if(args.length == 3 && args[0].equals("-r")) {
            FileReader in = new FileReader(args[1]);
            FileWriter out = new FileWriter(args[2]);
            SceneParser parse = new SceneParser();
            Scene scene = (Scene)parse.parse(in);
            
            OldSceneWriter write = new OldSceneWriter();
            write.write(scene, (CompositeElement)scene.roots().get(0), out);
        }
        else {
            FileReader in = new FileReader(args[0]);
            FileWriter out = new FileWriter(args[1]);
            OldSceneParser parse = new OldSceneParser();
            Scene scene = (Scene)parse.parse(in);
            
            SceneWriter write = new SceneWriter();
            write.write(scene, (CompositeElement)scene.roots().get(0), out);
        }
    }
    
    /**
     * SceneWriter writes a single interpretation of a scene to an output
     * stream.  This interpretation can then be read in by a OldSceneParser
     * for testing purposes.  It currently has the limitation that it
     * can only write interpretations that have "SimpleData" interpretations
     * of the scene, because it doesn't know how to handle complex data.
     */
    public static class OldSceneWriter {
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
            writer.write("<" + OldSceneParser.SCENE_TAG + ">\n");
            writeElement(db, root, "root", "  ", writer);
            writer.write("</"+ OldSceneParser.SCENE_TAG + ">\n");
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
                writer.write("<" + OldSceneParser.SCENE_ELEMENT_TAG + " " 
                        + OldSceneParser.NAME_TAG + "=\"" + name + "\" "
                        + OldSceneParser.TYPE_TAG + "=\"");
                TypedData data = compElt.getData();
                if(data instanceof SimpleData) {
                    writer.write(((SimpleData)data).getTypeID());
                }
                else {
                    String err = "Only support simple types";
                    throw new UnsupportedOperationException(err);
                }
                writer.write("\" " + OldSceneParser.CONFIDENCE_TAG + "=\"");
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
                writer.write("</" + OldSceneParser.SCENE_ELEMENT_TAG + ">\n");
            }
        }
    

        /**
         * Write the stroke information (x, y, timestamp) and its
         * label (indicating either positive or negative example) to the
         * character-output stream.
         */
        private void writeStroke(TimedStroke stroke, Writer writer)
                throws IOException {
            writer.write("<" + OldSceneParser.STROKE_ELEMENT_TAG + " " + OldSceneParser.POINTS_TAG + "=\"");
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
            writer.write("<!DOCTYPE " + OldSceneParser.SCENE_TAG + " PUBLIC \""
                    + OldSceneParser.PUBLIC_ID + "\"\n\t\""
                    + OldSceneParser.DTD_URL + "\">\n\n");
        }
    }

    /**
     * OldSceneParser parses an XML file representing a single
     * interpretation of a scene into a Scene data structure.  This
     * interpretation can then be used for testing purposes.  It
     * currently has the limitation that interprets all typed data as
     * "SimpleData", because it doesn't know how to handle complex
     * data.
     */
    public static class OldSceneParser implements diva.util.ModelParser {
        /**
         * The public identity of the sketch dtd file.
         */
        public static final String PUBLIC_ID = "-//UC Berkeley//DTD scene 1//EN";

        /**
         * The URL where the DTD is stored.
         */
        public static final String DTD_URL = "http://www.gigascale.org/diva/dtd/scene.dtd";

        /**
         * The DTD for sketch files.
         */
        public static final String DTD_1 =
        "<!ELEMENT scene (sceneElement)> <!ELEMENT sceneElement (sceneElement+|strokeElement)> <!ATTLIST sceneElement confidence CDATA \"1.0\" name CDATA #REQUIRED type CDATA #REQUIRED> <!ELEMENT strokeElement EMPTY> <!ATTLIST strokeElement points CDATA #REQUIRED>";

        /**
         * Indicates the file contains a scene.
         */
        public static final String SCENE_TAG = "scene";
    
        /**
         * Indicates a scene element.
         */
        public static final String SCENE_ELEMENT_TAG = "sceneElement";

        /**
         * Indicates a stroke element.
         */
        public static final String STROKE_ELEMENT_TAG = "strokeElement";
    
        /**
         * Indicates the type of scene element.
         */
        public static final String TYPE_TAG = "type";

        /**
         * Indicates the name of scene element in a composite element.
         */
        public static final String NAME_TAG = "name";

        /**
         * Indicates the confidence of a scene element.
         */
        public static final String CONFIDENCE_TAG = "confidence";

        /**
         * Indicates the stroke path for a stroke element.
         */
        public static final String POINTS_TAG = "points";

        /**
         * Parse the input stream dictated by the given
         * reader intoa scene.
         */
        public Object parse(Reader in) throws java.lang.Exception  {
            XmlDocument doc = new XmlDocument();
            doc.setDTDPublicID(PUBLIC_ID);
            doc.setDTD(DTD_1);
            XmlReader reader = new XmlReader();
            reader.parse(doc, in);
            if(reader.getErrorCount() > 0) {
                throw new Exception("errors encountered during parsing");
            }
            XmlElement scene = doc.getRoot();
            if(!scene.getType().equals(SCENE_TAG)) {
                throw new Exception("no scene");
            }
            XmlElement sceneElt = (XmlElement)scene.elements().next();
            Scene db = new BasicScene();
            buildSceneElement(db, sceneElt);
            return db;
        }

        /**
         * Given a scene element represented by its parsed XML equivalent,
         * first build all of its children in the database, then build
         * it in the database.
         */
        private SceneElement buildSceneElement(Scene db, XmlElement eltXml) {
            if(eltXml.getType().equals(STROKE_ELEMENT_TAG)) {
                TimedStroke stroke = SSTrainingParser.parsePoints(eltXml.getAttribute(POINTS_TAG));
                return db.addStroke(stroke);
            }
            else {
                SceneElement[] children = new SceneElement[eltXml.elementCount()];
                int i = 0;
                for(Iterator cs = eltXml.elements(); cs.hasNext(); ) {
                    children[i++] = buildSceneElement(db, (XmlElement)cs.next());
                }
                String type = eltXml.getAttribute(TYPE_TAG);
                String conf = eltXml.getAttribute(CONFIDENCE_TAG);
                double confidence;
                if(conf == null) {
                    confidence = 1;
                }
                else {
                    Double tmp = Double.valueOf(conf);
                    confidence = tmp.doubleValue();
                }
                String[] names = childNames(eltXml);
                return db.addComposite(new SimpleData(type), confidence,
                        children, names);
            }
        }

        /**
         * Return the array of child names given an xml element
         * that represents a composite scene element.
         */
        private String[] childNames(XmlElement elt) {
            String[] out = new String[elt.elementCount()];
            int j = 0;
            for(Iterator i = elt.elements(); i.hasNext(); ) {
                XmlElement child = (XmlElement)i.next();
                out[j++] = child.getAttribute(NAME_TAG);
            }
            return out;
        }
    }
}

