package uk.ac.starlink.table.view;

import java.awt.Component;
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
    private int minHeight = 50;
    private int maxHeight = 800;
    private int defHeight = 400;
    private int sbw;
    private int sbh;

    /**
     * Constructs an empty scroll pane.
     */
    public SizingScrollPane() {
        this( null );
    }

    /**
     * Constructs a scroll pane holding a supplied component.
     *
     * @param  view  the component viewed by the new scrollpane
     */
    public SizingScrollPane( Component view ) {
        super( view );
        sbw = getVerticalScrollBar().getPreferredSize().width;
        sbh = getHorizontalScrollBar().getPreferredSize().height;
    }

    public Dimension getPreferredSize() {
        if ( getViewport() != null && getViewport().getView() != null ) {

            // I can't get this right - I want it to resize to exactly
            // the shape of the view if that is within a reasonable size,
            // or have a scrollbar in each appropriate direction if it's
            // too big.  But I always get gaps or scrollbars when they
            // shouldn't be there.
            Dimension vdim = getViewport().getView().getPreferredSize();
            int vw = vdim.width + 6;
            int vh = vdim.height + 6;
            int w = Math.max( Math.min( vw, maxWidth ), minWidth );
            int h = Math.max( Math.min( vh, maxHeight ), minHeight );
            return new Dimension( w + sbw, h + sbh );
        }
        else {
            return new Dimension( defWidth, defHeight );
        }
    }
}
