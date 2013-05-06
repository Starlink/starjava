package uk.ac.starlink.ttools.plot2;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

/**
 * Mouse and keyboard listener that handles zoom events.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
public abstract class ZoomListener {

    /**
     * Performs the actual zooming action, as caused by mouse events
     * gathered by this listener.
     *
     * @param   nZoom  number of zoom steps;
     *          positive for zoom in, negative for zoom out
     * @param   point  graphics position about which zoom has been requested
     */
    public abstract void zoom( int nZoom, Point point );

    /**
     * Installs this listener to listen on a GUI component.
     *
     * @param  comp  target component
     */
    public void install( JComponent comp ) {

        /* Install listener on mouse wheel.
         * Multitouch devices sometimes trigger this event with
         * double-finger slide or similar. */
        comp.addMouseWheelListener( new MouseWheelListener() {
            public void mouseWheelMoved( MouseWheelEvent evt ) {
                zoom( - evt.getWheelRotation(), evt.getPoint() );
            }
        } );

        /* Install listener for some keys.  Doesn't seem to work all that well
         * since the focus often doesn't seem to be where you want it. */
        InputMap inputMap =
            comp.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW );
        ActionMap actionMap = comp.getActionMap();
        Object zoomIn = "zoom+";
        Object zoomOut = "zoom-";
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_UP, 0 ),
                      zoomIn );
        inputMap.put( KeyStroke.getKeyStroke( KeyEvent.VK_PAGE_DOWN, 0 ),
                      zoomOut );
        actionMap.put( zoomIn, new ZoomAction( comp, +2 ) );
        actionMap.put( zoomOut, new ZoomAction( comp, -2 ) );
    }

    /**
     * Action that causes a zoom of a fixed number of steps on a component.
     */
    private class ZoomAction extends AbstractAction {
        private final JComponent comp_;
        private final int nZoom_;

        /**
         * Constructor.
         *
         * @param   comp  target component
         * @param   nZoom  number of zoom steps
         */
        ZoomAction( JComponent comp, int nZoom ) {
            comp_ = comp;
            nZoom_ = nZoom;
        }

        public void actionPerformed( ActionEvent evt ) {
            zoom( nZoom_, comp_.getMousePosition() );
        }
    }
}
