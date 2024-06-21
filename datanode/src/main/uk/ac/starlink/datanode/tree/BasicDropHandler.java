package uk.ac.starlink.datanode.tree;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import javax.swing.JComponent;
import javax.swing.TransferHandler;

/**
 * Custom DropTarget subclass.  This provides basic
 * DropTarget/DropTargetListener functionality for use with components 
 * which want to define their own drop behaviour.  An instance of this
 * class can be slotted into a JComponent using its 
 * {@link javax.swing.JComponent#setDropTarget} method to override the 
 * one which is installed with it (which probably comes from the basic UI).
 */
public class BasicDropHandler extends DropTarget {

    private boolean canImport;
    private JComponent comp;

    /**
     * Constructs a new drop handler for use with a given component.
     *
     * @param  comp  the component which this drop handler will control
     */
    public BasicDropHandler( JComponent comp ) {
        this.comp = comp;
        setComponent( comp );
    }

    /*
     * DropTargetListener methods (DropTarget is its own listener)
     */

    public void dragEnter( DropTargetDragEvent evt ) {
        int dropAction = evt.getDropAction();
        canImport = getTransferHandler()
                   .canImport( comp, evt.getCurrentDataFlavors() );
        if ( canImport && actionSupported( dropAction ) &&
             isDropLocation( evt.getLocation() ) ) {
            evt.acceptDrag( dropAction );
        }
        else {
            evt.rejectDrag();
        }
    }

    public void dragOver( DropTargetDragEvent evt ) {
        int dropAction = evt.getDropAction();
        if ( canImport && actionSupported( dropAction ) && 
             isDropLocation( evt.getLocation() ) ) {
            evt.acceptDrag( dropAction );
        }
        else {
            evt.rejectDrag();
        }
    }

    public void dragExit( DropTargetEvent evt ) {
    }

    public void drop( DropTargetDropEvent evt ) {
        int dropAction = evt.getDropAction();
        TransferHandler importer = getTransferHandler();
        if ( canImport && actionSupported( dropAction ) ) {
            evt.acceptDrop( dropAction );
            try {
                Transferable trans = evt.getTransferable();
                evt.dropComplete( importer.importData( comp, trans ) );
            }
            catch ( RuntimeException e ) {
                e.printStackTrace();
                evt.dropComplete( false );
            }
        }
        else {
            evt.rejectDrop();
        }
    }

    private boolean actionSupported( int action ) {
        return ( action & DnDConstants.ACTION_COPY_OR_MOVE ) != 0
            && getTransferHandler() != null;
    }

    /**
     * Indicates whether a given location is permissible for a drop event.
     *
     * @param   loc  the point at which a drop might take place
     * @return  <code>true</code> iff it's OK to drop at <code>loc</code>
     */
    protected boolean isDropLocation( Point loc ) {
        return true;
    }

    private TransferHandler getTransferHandler() {
        return comp.getTransferHandler();
    }
}

