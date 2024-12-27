package uk.ac.starlink.util.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Toolkit;
import javax.swing.JComponent;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

/**
 * Adds some functionality to the JScrollPane class;
 * a SizingScrollPane will attempt to match the shape of its 
 * view component if it is a sensible shape.  Otherwise it will assume
 * some sensible shape.
 *
 * @author   Mark Taylor
 */
public class SizingScrollPane extends JScrollPane {

    private SizeConfig config_;

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
    }

    public Dimension getPreferredSize() {
        SizeConfig config = getConfig();
        if ( getViewport() != null && getViewport().getView() != null ) {
            Dimension vdim = getViewport().getView().getPreferredSize();
            int vw = vdim.width;
            int vh = vdim.height;

            Insets insets = getInsets();
            vw += insets.left + insets.right;
            vh += insets.top + insets.bottom;

            Component rHead = getRowHeader();
            if ( rHead != null ) {
                vw += rHead.getPreferredSize().width;
            }
            Component cHead = getColumnHeader();
            if ( cHead != null ) {
                vh += cHead.getPreferredSize().height;
            }

            if ( vw > config.maxWidth_ ) {
                vh += getVerticalScrollBar().getPreferredSize().width;
            }
            if ( vh > config.maxHeight_ ) {
                vw += getHorizontalScrollBar().getPreferredSize().height;
            }
            int w = limit( vw, config.minWidth_, config.maxWidth_ );
            int h = limit( vh, config.minHeight_, config.maxHeight_ );
            return new Dimension( w, h );
        }
        else {
            return new Dimension( config.defWidth_, config.defHeight_ );
        }
    }

    /**
     * Gets a shape configuration object for this panel.
     *
     * @return  lazily constructed sizeconfig
     */
    private SizeConfig getConfig() {
        if ( config_ == null ) {
            config_ = new SizeConfig( Toolkit.getDefaultToolkit()
                                             .getScreenSize() );
        }
        return config_;
    }

    /**
     * Returns a value limited by supplied minimum and maximum values.
     *
     * @param   pref  preferred value
     * @param   min   lower bound
     * @param   max   upper bound
     * @return  max(min(max,pref),min)
     */
    private static int limit( int pref, int min, int max ) {
        if ( min >= max ) {
            throw new IllegalArgumentException();
        }
        return Math.max( Math.min( pref, max ), min );
    }

    /**
     * Container for max/min/default size settings.
     */
    private static class SizeConfig {
        final int minWidth_;
        final int maxWidth_;
        final int defWidth_;
        final int minHeight_;
        final int maxHeight_;
        final int defHeight_;

        /**
         * Constructor.
         *
         * @param   screen   dimensions of the physical display device
         */
        SizeConfig( Dimension screen ) {
            minWidth_ = 100;
            maxWidth_ = limit( (int) ( 0.6 * screen.width ), 300, 700 );
            defWidth_ = limit( (int) ( 0.3 * screen.width ), 300, 500 );
            minHeight_ = 100;
            maxHeight_ = limit( (int) ( 0.3 * screen.height ), 200, 500 );
            defHeight_ = limit( (int) ( 0.2 * screen.height ), 200, 300 );
        }
    }
}
