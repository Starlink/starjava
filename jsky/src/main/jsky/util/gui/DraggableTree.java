//=== File Prolog =============================================================
//	This code was developed by NASA, Goddard Space Flight Center, Code 588
//	for the Scientist's Expert Assistant (SEA) project.
//
//--- Contents ----------------------------------------------------------------
//	class DraggableTree
//
//--- Description -------------------------------------------------------------
//	Extends JTree by adding the ability to drag tree nodes.  Any node that
//	implements the Transferable interface is potentially draggable.  The
//	dragging happens automatically.  Note that this class only does dragging -
//	it does not do dropping (which is less capable of generalization).
//
//--- Notes -------------------------------------------------------------------
//
//--- Development History -----------------------------------------------------
//
//	05/21/99	J. Jones / 588
//
//		Original implementation.
//
//--- DISCLAIMER---------------------------------------------------------------
//
//	This software is provided "as is" without any warranty of any kind, either
//	express, implied, or statutory, including, but not limited to, any
//	warranty that the software will conform to specification, any implied
//	warranties of merchantability, fitness for a particular purpose, and
//	freedom from infringement, and any warranty that the documentation will
//	conform to the program, or any warranty that the software will be error
//	free.
//
//	In no event shall NASA be liable for any damages, including, but not
//	limited to direct, indirect, special or consequential damages, arising out
//	of, resulting from, or in any way connected with this software, whether or
//	not based upon warranty, contract, tort or otherwise, whether or not
//	injury was sustained by persons or property or otherwise, and whether or
//	not loss was sustained from or arose out of the results of, or use of,
//	their software or services provided hereunder.
//
//=== End File Prolog =========================================================

//package GOV.nasa.gsfc.sea.util.gui;

package jsky.util.gui;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceEvent;
import javax.swing.JTree;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;

import jsky.util.TransferableList;

/**
 * Extends JTree by adding the ability to drag tree nodes.  Any node that
 * implements the Transferable interface is potentially draggable.  The
 * dragging happens automatically.  Note that this class only does dragging -
 * it does not do dropping (which is less capable of generalization).
 *
 * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
 * for the Scientist's Expert Assistant (SEA) project.
 *
 * @version		05/21/99
 * @author		J. Jones / 588
 **/
public class DraggableTree extends JTree implements DragGestureListener {

    /**
     * The tree's DragSource instance that initiates drag gestures.
     **/
    protected DragSource fDragSource = DragSource.getDefaultDragSource();

    /**
     * The required DragSourceListener for the tree.
     **/
    protected final static DragSourceListener sDragSourceListener = new TreeDragSourceListener();

    /**
     * Creates a new JTree that supports dragging tree nodes.
     **/
    public DraggableTree() {
        fDragSource.createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    /**
     * Creates a new JTree that supports dragging tree nodes.  The specified TreeModel
     * is used as the data model.
     *
     * @param	model	the TreeModel to use as the data model
     **/
    public DraggableTree(TreeModel model) {
        super(model);

        fDragSource.createDefaultDragGestureRecognizer(
                this, DnDConstants.ACTION_COPY_OR_MOVE, this);
    }

    /**
     * Subclasses can override this method to return true only when the current
     * tree selection is valid for dragging.  Return false when you don't want the
     * user to be able to drag the current selection.
     *
     * @return	true if the current selection may be dragged
     **/
    public boolean isSelectionDraggable() {
        return true;
    }

    // DragGestureListener method

    /**
     * This method is called when the user initiates a drag operation.
     * It attempts to start the drag.
     *
     * @param	event	the DragGestureEvent describing the gesture that has just occurred
     **/
    public void dragGestureRecognized(DragGestureEvent event) {
        // Give subclasses a chance to stop the drag
        if (!isSelectionDraggable()) {
            return;
        }

        if (getSelectionCount() == 1) {
            TreePath path = getSelectionPath();
            if (path != null) {
                DefaultMutableTreeNode selection = (DefaultMutableTreeNode) path.getLastPathComponent();

                if (selection instanceof Transferable) {
                    // Start the drag - a single Transferable node
                    fDragSource.startDrag(
                            event,
                            (event.getDragAction() == DnDConstants.ACTION_COPY)
                            ? DragSource.DefaultCopyDrop : DragSource.DefaultMoveDrop,
                            (Transferable) selection,
                            sDragSourceListener);
                }
            }
        }
        else if (getSelectionCount() > 1) {
            TreePath[] paths = getSelectionPaths();

            TransferableList selections = new TransferableList();

            for (int i = 0; i < paths.length; ++i) {
                DefaultMutableTreeNode selection = (DefaultMutableTreeNode) paths[i].getLastPathComponent();

                if (selection instanceof Transferable) {
                    selections.add(selection);
                }
            }

            if (selections.size() > 0) {
                // Start the drag - a TransferableList of nodes
                fDragSource.startDrag(
                        event,
                        (event.getDragAction() == DnDConstants.ACTION_COPY)
                        ? DragSource.DefaultCopyDrop : DragSource.DefaultMoveDrop,
                        selections,
                        sDragSourceListener);
            }
        }
    }

    /**
     * A DragSourceListener is required, but DraggableTree does not currently
     * use this feature so an empty implementation is used.
     *
     * <P>This code was developed by NASA, Goddard Space Flight Center, Code 588
     * for the Scientist's Expert Assistant (SEA) project.
     *
     * @version		05/21/99
     * @author		J. Jones / 588
     **/
    protected static class TreeDragSourceListener implements DragSourceListener {

        public void dragDropEnd(DragSourceDropEvent DragSourceDropEvent) {
        }

        public void dragEnter(DragSourceDragEvent DragSourceDragEvent) {
        }

        public void dragExit(DragSourceEvent DragSourceEvent) {
        }

        public void dragOver(DragSourceDragEvent DragSourceDragEvent) {
        }

        public void dropActionChanged(DragSourceDragEvent DragSourceDragEvent) {
        }
    }
}
