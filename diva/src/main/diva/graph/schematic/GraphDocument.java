/*
 * $Id: GraphDocument.java,v 1.5 2001/07/22 22:01:24 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;


import diva.graph.GraphModel;
import diva.graph.MutableGraphModel;
import diva.graph.basic.BasicGraphModel;

import diva.gui.AbstractDocument;
import diva.gui.Application;
import diva.gui.Document;
import diva.gui.DocumentFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;

import diva.util.LoggableOp;
import diva.util.xml.XmlDocument;
import diva.util.xml.XmlElement;
import diva.util.xml.XmlReader;
import diva.util.xml.XmlWriter;
import diva.util.xml.XmlBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.net.URL;

/**
 * A class representing graph-structured documents.  This class can
 * read files to construct an instance of GraphModel, and write a
 * GraphModel out to a file.
 *
 * <P> This is one of two classes that must be provided to produce
 * a complete application using the diva.gui framework. (The other
 * is diva.gui.Application -- see the CanvasDemo class.) Generally,
 * the easiest way to implement diva.gui.Document is to extend
 * diva.gui.AbstractDocument.
 *
 * <p> This class implements those methods that are left abstract in
 * diva.gui.AbstractDocument. This is a set of methods that deal with
 * loading the document from a file or URL and vice versa, such
 * as close(), open(), save(), and saveAs(). These methods simply
 * implement the "raw" functionality implied by the method names;
 * they do not perform functions such as querying the user
 * for whether to save a file before closing. (That function is
 * performed by an instance of diva.gui.StoragePolicy.)
 * 
 * <P> To read and write canvas data, it uses the diva.util.xml
 * utility package. In general, there is no need for applications to
 * use the XML utilities; however, it does make it very simple and
 * fast to construct a working application with complete file I/O. See
 * the various method comments for further imformation.
 *
 * @author John Reekie (johnr@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 */
public class GraphDocument extends AbstractDocument {
    
    /** The XML document class the represents this graph document.
     */
    private XmlDocument _xmlDocument;

    /** The graph data of this document
     */
    private BasicGraphModel _model = new SchematicGraphModel();

    /** Construct a graph document that is owned by the given
     *  application. It contains an empty BasicGraph as its data.
     */
    public GraphDocument(Application a) {
        super(a);
    }

    /** Close the document. This method doesn't do anything, as
     * graph data doesn't change.
     */
    public void close () throws Exception {
        // Do nothing
    }

    /** Get the graph data model.
     */
    public BasicGraphModel getGraphModel () {
        return _model;
    }

    /** Get the XML document that contains the raw XML data.
     */
    public XmlDocument getXmlDocument() {
        return _xmlDocument;
    }

    /** Open the document from its current file.  If successful, create
     * and remember a Graph model.
     *
     * <P> The implementation of this method uses the diva.xml
     * package:
     * <ul>
     * <li> Create an instance of the XmlDocument class
     * <li> Create an instance of the XmlReader class and
     * pass the document to it to parse the XML file
     * <li> Traverse the XmlDocument to construct an instance of Graph
     * </ul>
     *
     * @throws Exception  If there is no file, or if the I/O operation failed.
     */
    public void open () throws Exception {
        // Parse the XML
        _xmlDocument = new XmlDocument(getFile());
        XmlReader reader = new XmlReader();
        reader.setVerbose(true);
        reader.parse(_xmlDocument);
        
        // Convert the XML into a (basic) graph
        BasicGraphBuilder builder = new BasicGraphBuilder();
        _model = (BasicGraphModel) builder.build(_xmlDocument.getRoot());
    }

    /** Save the document to the current file. See the saveAs() method
     * for more information.
     * 
     * @throws Exception  If there is no file, or if the I/O operation failed.
     */
    public void save () throws Exception {
        saveAs(getFile());
    }

    /** Save the document to the given file. Note that this method
     * does not change the file attribute to the new File object.
     *
     * <p> The implementation of this class uses the diva.util.xml
     * package:
     * <ul>
     * <li> If there is not already an instance of XmlDocument (eg
     * created when parsing), create one now.
     * <li> Clear the contents of the XML document.
     * <li> Use an instance of XMLBuilder to construct an
     * XML tree.
     * <li> Use an instance of XmlWriter to write the document to disk.
     * </ul>
     * 
     * @throws Exception  If the I/O operation failed.
     */
    public void saveAs (File file) throws Exception {
        // Create the XML document
        if (_xmlDocument == null) {
            _xmlDocument = new XmlDocument(file);
        } else {
            _xmlDocument.setURL(null);
            _xmlDocument.setFile(file);
        }

        // Populate the XML document and write it out
        BasicGraphBuilder builder = new BasicGraphBuilder();
        XmlWriter writer = new XmlWriter();

        XmlElement root = builder.generate(_model);
        _xmlDocument.setRoot(root);
        writer.write(_xmlDocument);
    }

    /** Save the document to the given file. This method uses
     * diva.util.xml to do the work -- see the saveAs(File) method for
     * more description. It will probably fail if the URL is not a
     * file-based url.
     *
     * @throws Exception Always
     */
    public void saveAs (URL url) throws Exception {
        // Create the XML document
        if (_xmlDocument == null) {
            _xmlDocument = new XmlDocument(url);
        } else {
            _xmlDocument.setURL(url);
            _xmlDocument.setFile(null);
        }

        // Populate the XML document and write it out
        BasicGraphBuilder builder = new BasicGraphBuilder();
        XmlWriter writer = new XmlWriter();

        XmlElement root = builder.generate(_model);
        _xmlDocument.setRoot(root);
        writer.write(_xmlDocument);
    }

    /** Set the graph data model.
     */
    public void setGraphModel (BasicGraphModel m) {
        _model = m;
    }

    /** Print information about the graph document
     */
    public String toString () {
        return
            getClass().getName() + "["
            + "title = " + getTitle()
            + ", file = " + getFile()
            + ", url = " + getURL()
            + "]";
    }

    /** GraphDocument.Factory is a factory for graph documents.  We
     * put this in an inner class of GraphDocument because this
     * factory can only produce one kind of document.
     */
    public static class Factory implements DocumentFactory {
        /** Create an empty graph document
         */
        public Document createDocument (Application app) {
            GraphDocument d = new GraphDocument(app);
            return d;
        }

        /** Create a new graph that contains data at the given URL
         */
        public Document createDocument (Application app, URL url) {
            GraphDocument d = new GraphDocument(app);
            d.setURL(url);
            return d;
        }

        /** Create a new graph that contains the given file path
         */
        public Document createDocument (Application app, File file) {
            GraphDocument d = new GraphDocument(app);
            d.setFile(file);
            return d;
        }
    }

    /**
     * A class for converting XmlElement trees to a GraphModel and vice versa.
     * Most graph applications will want to implement their own file
     * format; this is just included as an example.
     *
     * @author John Reekie (johnr@eecs.berkeley.edu)
     * @version $Revision: 1.5 $
     */
    private class BasicGraphBuilder extends LoggableOp implements XmlBuilder {
        /** Given an object, which should be a basic graph model,
         * create and return a new tree of XmlElements.
         */
        public XmlElement generate (Object obj) {
            return generate((BasicGraphModel) obj);
        }

        /**
         * Given a BasicGraphModel, create and return a new tree
         * of XmlElements
         */
        public XmlElement generate (BasicGraphModel model) {
            XmlElement root = new XmlElement("graph");
            generate(model, model.getRoot(), root);
            return root;
        }

        /** Given a Graph and a parent element, populate the XmlElement
         * with the contents of the graph.
         */
        public void generate (BasicGraphModel model, Object node, 
                XmlElement parent) {
            HashMap nodes = new HashMap();
            int unique = 0;
            generateNodes(model, node, parent, 0, nodes);
            generateEdges(model, node, parent, 0, nodes);
        }

        /**
         * Helper function that writes out all of the nodes in
         * a graph recursively with unique id's and stores the
         * nodes ID's in a hash for use in writing out the
         * edges.
         */
        private int generateNodes (BasicGraphModel model, Object node, 
                XmlElement parent, int unique, HashMap nodes) {
        
            // Nodes
            for (Iterator i = model.nodes(node); i.hasNext(); ) {
                Object n = i.next();
                String id = "node_" + unique++;
                nodes.put(n,id);
                XmlElement elt;
                if (model.isComposite(n)) {
                    elt = new XmlElement("compositeNode");
                    unique = generateNodes(model, n, elt, unique, nodes);
                } else {
                    elt = new XmlElement("node");
                }
                elt.setAttribute("id", id);
                parent.addElement(elt);
            }
            return unique;
        }

        /**
         * Helper function that writes out all of the edges in
         * a graph recursively with unique id's.
         */
        private int generateEdges (BasicGraphModel model, Object node, 
                XmlElement parent, int unique, HashMap nodes) {
            // Edges
            for (Iterator i = model.nodes(node); i.hasNext(); ) {
                Object n = (Object) i.next();
                for (Iterator j = model.inEdges(n); j.hasNext(); ) {
                    Object e = (Object) j.next();
                    String id = "edge_" + unique++;

                    String head = (String) nodes.get(n);
                    String tail = (String) nodes.get(model.getTail(e));

                    XmlElement elt = new XmlElement("edge");
                    elt.setAttribute("id", id);
                    elt.setAttribute("head", head);
                    elt.setAttribute("tail", tail);
                    if (model.isDirected(e)) {
                        elt.setAttribute("directed", "true");
                    }
                    parent.addElement(elt);
                }
                if(model.isComposite(n)) {
                    unique = generateEdges(model, n, parent, unique, nodes);
                }
            }
            return unique;
        }

        /** Given an XmlElement, create and return a represention
         * of it as a graph model. The type of the element should be "graph".
         */
        public Object build (XmlElement elt) {
            return build(elt, elt.getType());
        }

        /** Do nothing.
         */
        public void setDelegate(XmlBuilder delegate) {
        }
    
        /** Given an XmlElement, create and return a represention
         * of it as a graph model. The type variable must be "graph".
         */
        public Object build (XmlElement elt, String type) {
            if (!type.equals("graph")) {
                throw new RuntimeException(
                        "Root element must have type graph (not " + 
                        type + ")");
            }
            BasicGraphModel model = new BasicGraphModel();
            build(model, model.getRoot(), elt);
            return model;
        }

        /**
         * Helper function to populate a model's nodes given an
         * XML element root.
         */
        private void buildNodes(BasicGraphModel model, Object parent,
                XmlElement root, HashMap nodes) {
            for (Iterator i = root.elements(); i.hasNext(); ) {
                XmlElement elt = (XmlElement) i.next();
                String type = elt.getType();

                if (type.equals("node")) {
                    Object n = model.createNode(null);
                    model.addNode(this, n, parent);
                    nodes.put(elt.getAttribute("id"), n);

                } else if (type.equals("compositeNode")) {
                    Object n = model.createComposite(null);
                    model.addNode(this, n, parent);
                    nodes.put(elt.getAttribute("id"), n);
                    buildNodes(model, n, elt, nodes);
                }
            }
        }

        /**
         * Helper function to populate a model's edges given an
         * XML element root.
         */
        private void buildEdges(BasicGraphModel model, Object parent,
                XmlElement root, HashMap nodes) {
            for (Iterator i = root.elements(); i.hasNext(); ) {
                XmlElement elt = (XmlElement) i.next();
                String type = elt.getType();
            
                if (type.equals("edge")) {
                    Object head = nodes.get(elt.getAttribute("head"));
                    Object tail = nodes.get(elt.getAttribute("tail"));
                    Object e = model.createEdge(null);
                    model.setEdgeHead(this, e, head);
                    model.setEdgeTail(this, e, tail);
                }
            }
        }

        /** Given a tree of XmlElements rooted by a graph element, populate the
         *  graph model with elements representing the graph.
         */
        public void build (BasicGraphModel model, Object parent,
                XmlElement root) {
            Iterator i;
            Iterator j;
            HashMap nodes = new HashMap();
            buildNodes(model, parent, root, nodes);
            buildEdges(model, parent, root, nodes);
        }
    }
}



