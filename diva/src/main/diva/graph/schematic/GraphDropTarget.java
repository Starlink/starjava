/*
 * $Id: GraphDropTarget.java,v 1.4 2001/07/22 22:01:25 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.graph.schematic;

import diva.graph.*;
import diva.graph.basic.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;
import java.awt.Point;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class provides customizable drag-and-drop support for the
 * graph editor widget.  Users can register string keys and
 * object values that get cloned when the keys are dropped onto
 * the graph editor.  When a drop occurs, the graph controller is
 * asked to create a node instance with the value as its semantic
 * object.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class GraphDropTarget extends DropTarget {
    /**
     * The plain-text flavor that we will be using for our
     * basic drag-and-drop protocol.
     */
    static final DataFlavor TEXT_FLAVOR = DataFlavor.plainTextFlavor;
    static final DataFlavor STRING_FLAVOR = DataFlavor.stringFlavor;

    /**
     * A hastable to store the key->figure
     * mapping.
     */
    public HashMap _map = new HashMap();
    
    /**
     * Construct a new graph target to operate
     * on the given JGraph.
     */
    public GraphDropTarget(JGraph g) {
        setComponent(g);
        try {
            addDropTargetListener(new DTListener());
        }
        catch(java.util.TooManyListenersException wow) {
        }
        addDropKey("foo","foo1");
        addDropKey("bar","bar1");
        addDropKey("baz","baz1");
    }

    /**
     * Add a key to the drop target so that
     * when they key is dropped the figure will
     * be cloned and placed as a node in the
     * graph.  Key must be a unique string.
     */
    public void addDropKey(String key, Object val) {
        _map.put(key,val);
    }

    /**
     * Remove a key from the target.
     *
     * @see addDropKey(String,Figure)
     */
    public void removeDropKey(String key) {
        _map.remove(key);
    }
    
    /**
     * A drop target listener that comprehends
     * the different available keys.
     */
    private class DTListener implements DropTargetListener {
        /**
         * Accept the event if the data is a known key.
         */
        public void dragEnter(DropTargetDragEvent dtde) {
            if(dtde.isDataFlavorSupported(TEXT_FLAVOR)) {
                dtde.acceptDrag(DnDConstants.ACTION_COPY_OR_MOVE);
            }
            else {
                dtde.rejectDrag();
            }                
        }

        /**
         * Do nothing.
         */
        public void dragExit(DropTargetEvent dtde) {
        }

        /**
         * Accept the event if the data is a known key.
         */
        public void dragOver(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }

        /**
         * Accept the event if the data is a known key;
         * clone the associated figure and place it in the
         * graph editor.
         */
        public void drop(DropTargetDropEvent dtde) {
            Object val = null;

            if(dtde.isDataFlavorSupported(STRING_FLAVOR)) {
                try {
                    DataFlavor tmp = STRING_FLAVOR;
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    System.out.println("GETTING FLAVOR: " + tmp.getHumanPresentableName() );//DEBUG
                    String key = (String)dtde.getTransferable().getTransferData(tmp);
                    System.out.println("Key is [" + key + "]");//DEBUG
                    val = _map.get(key);
                    System.out.println("Val is [" + val + "]");//DEBUG
                }
                catch(Exception e) {
                    System.out.println(e);//DEBUG
                }
            }
            else if(dtde.isDataFlavorSupported(TEXT_FLAVOR)) {
                try {
                    DataFlavor tmp = null;

                    // NOTE:
                    // A very strange bug and a hacky temp workaround.
                    // The source claims to support TEXT_FLAVOR, but
                    // an UnsupportedDataFlavor exception is thrown
                    // when we try to get data of this type.  So instead
                    // I iterate through the types and get the last
                    // flavor (which happens to be text/plain for
                    // the Editpad application that I'm testing on)
                    // and then try to get the data in that flavor.
                    for(Iterator i = dtde.getCurrentDataFlavorsAsList().iterator(); i.hasNext(); ) {
                        DataFlavor df = (DataFlavor)i.next();
                        System.out.println("SUPPORTED FLAVOR: " + df.getHumanPresentableName());  //DEBUG
                        tmp = df;
                    }
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    System.out.println("GETTING FLAVOR: " + tmp.getHumanPresentableName() );//DEBUG
                    InputStream is = (InputStream)dtde.getTransferable().getTransferData(tmp);
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String key = br.readLine().trim();
                    System.out.println("Key is [" + key + "]");//DEBUG
                    val = _map.get(key);
                    System.out.println("Val is [" + val + "]");//DEBUG
                }
                catch(Exception e) {
                    System.out.println(e);//DEBUG
                }
            }
            else {
                dtde.rejectDrop();
            }
            
            if(val == null) {
                System.out.println("Drop failure"); //DEBUG
                dtde.dropComplete(false); //failure!
            }
            else {
                Point p = dtde.getLocation();
                System.out.println("Dropping [" + val + "] at " + p); //DEBUG
		GraphPane gp = ((JGraph)getComponent()).getGraphPane();
                GraphController gc = gp.getGraphController();
		BasicGraphModel gm = (BasicGraphModel)gc.getGraphModel();
		Object node = gm.createComposite(val);
		
		// Place a new composite node ad (p.x, p.y)
		gc.addNode(node, p.x, p.y);
		// Add the ports, which will be given a position by the
		// controller.
		gc.addNode(gm.createNode(null), node);
		gc.addNode(gm.createNode(null), node);
		dtde.dropComplete(true); //success!
		
            }
        }

        /**
         * Accept the event if the data is a known key.
         */
        public void dropActionChanged(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }
    }
}


