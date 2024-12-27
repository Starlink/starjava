package uk.ac.starlink.util.gui;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.event.MouseInputAdapter;

/**
 * Mouse listener object which watches for a drag gesture, and invokes
 * TransferHandler.exportAsDrag() when it sees one.
 * Note this must be installed as 
 * <em>both</em> a {@link java.awt.event.MouseListener} 
 * <em>and</em>  a {@link java.awt.event.MouseMotionListener}
 * on the component it's watching.
 * 
 * @author   Mark Taylor (Starlink)
 */
public class DragListener extends MouseInputAdapter {

    /** Mouse button with which the drag is done. */
    private static final int BUTTON = MouseEvent.BUTTON1;

    /** Minimum distance in pixels which constitutes a drag gesture. */
    private static final int DRAG_LIMIT = 6;

    /** Transfer action which the drag should signal. */
    private static final int TRANSFER_ACTION = TransferHandler.COPY;

    private Point start;

    public void mousePressed( MouseEvent evt ) {
        start = evt.getPoint();
    }
    public void mouseReleased( MouseEvent evt ) {
        start = null;
    }
    public void mouseEntered( MouseEvent evt ) {
        start = null;
    }
    public void mouseDragged( MouseEvent evt ) {
        if ( start != null && evt.getPoint().distance( start ) > DRAG_LIMIT ) {
            JComponent comp = (JComponent) evt.getSource();
            TransferHandler th = comp.getTransferHandler();
            th.exportAsDrag( comp, evt, TRANSFER_ACTION );
            start = null;
        }
    }
}
