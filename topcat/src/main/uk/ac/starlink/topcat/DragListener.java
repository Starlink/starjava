package uk.ac.starlink.topcat;

import java.awt.Point;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.event.MouseInputAdapter;

/**
 * Mouse listener object which watches for a drag gesture, and invokes
 * TransferHandler.exportAsDrag() when it sees one.
 * Note this must be installed as <em>both</em> a MouseListener <em>and</em>
 * a MouseMotionListener on the component it's watching.
 * 
 * @author   Mark Taylor (Starlink)
 */
class DragListener extends MouseInputAdapter {

    private static final int BUTTON = MouseEvent.BUTTON1;
    private static final int DRAG_LIMIT = 6;
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
