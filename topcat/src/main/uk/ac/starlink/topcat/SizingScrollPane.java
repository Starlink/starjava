package uk.ac.starlink.topcat;

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
    private int headh;

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

            Dimension vdim = getViewport().getView().getPreferredSize();


            // Don't know why I need these plus fours, but it doesn't seem
            // to be quite big enough without.
            int vw = vdim.width + 4;
            int vh = vdim.height + 4;

            Component rHead = getRowHeader();
            if ( rHead != null ) {
                vw += rHead.getPreferredSize().width;
            }
            Component cHead = getColumnHeader();
            if ( cHead != null ) {
                vh += cHead.getPreferredSize().height;
            }

            int w = Math.max( Math.min( vw, maxWidth ), minWidth );
            int h = Math.max( Math.min( vh, maxHeight ), minHeight );
            return new Dimension( w, h );
        }
        else {
            return new Dimension( defWidth, defHeight );
        }
    }
}
