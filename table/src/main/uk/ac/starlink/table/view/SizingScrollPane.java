package uk.ac.starlink.table.view;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

/**
 * Adds some functionality to the JScrollPane class;
 * a ResizingScrollPane will attempt to match the shape of its 
 * view component if it is a sensible shape.  Otherwise it will assume
 * some sensible shape.
 */
class SizingScrollPane extends JScrollPane {

    private int minWidth = 100;
    private int maxWidth = 800;
    private int defWidth = 600;
    private int minHeight = 100;
    private int maxHeight = 800;
    private int defHeight = 400;

    public Dimension getPreferredSize() {
        if ( getViewport() != null && getViewport().getView() != null ) {
            Dimension vdim = getViewport().getView().getPreferredSize();
            int vw = vdim.width
                   + getVerticalScrollBar().getPreferredSize().width
                   + 4;
            int vh = vdim.height
                   + getHorizontalScrollBar().getPreferredSize().height
                   + 4;
            return new Dimension(
                Math.max( Math.min( vw, maxWidth ), minWidth ),
                Math.max( Math.min( vh, maxHeight ), minHeight ) );
        }
        else {
            return new Dimension( defWidth, defHeight );
        }
    }
}
