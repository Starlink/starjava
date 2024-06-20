package uk.ac.starlink.util.gui;

import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.swing.SwingUtilities;

/**
 * Utility class to facilitate actions when a string is pasted into a 
 * component.  If you select some text on a windowing system it's possible
 * to paste it into a JTextComponent which is a convenient way 
 * of saving typing.
 * Implementing this for other components is rather fiddly - this 
 * class does the hard work for you.  To use it, implement the abstract
 * {@link #pasted} method and add it to the component you want to act on
 * using {@link java.awt.Component#addMouseListener}.
 *
 * @author   Mark Taylor (Starlink), Sun Microsystems
 * @since    3 Dec 2004
 */
public abstract class StringPaster extends MouseAdapter {

    /*
     * This code was written by inspection of Sun's J2SE1.4
     * javax.swing.text.DefaultCaret class implementation.
     */

    private static final DataFlavor STRING_FLAVOR = DataFlavor.stringFlavor;

    /**
     * Invokes {@link #pasted} if appropriate.
     */
    public void mouseClicked( MouseEvent evt ) {
        if ( isPasteEvent( evt ) ) {
            doPaste();
        }
    }

    /**
     * Determines whether a mouse event counts as a paste.  
     * The default implementation returns true for a single-click using the
     * middle mouse button.
     *
     * @param  evt  mouse event
     * @return  true iff <code>evt</code> counts as a paste gesture
     */
    protected boolean isPasteEvent( MouseEvent evt ) {
        return SwingUtilities.isMiddleMouseButton( evt )
            && evt.getClickCount() == 1;
    }

    /**
     * Returns the Toolkit holding the selection.
     * The default implementation returns AWT's default toolkit.
     *
     * @return  toolkit for selection
     */
    protected Toolkit getToolkit() {
        return Toolkit.getDefaultToolkit();
    }

    /**
     * Invoked when a paste event occurs.
     *
     * @param  str  a string that has been pasted from the 
     *              system-wide selection
     */
    protected abstract void pasted( String str );

    /**
     * Attempts to retrieve the system selection and pass it as a string
     * to {@link #pasted}.
     */
    private void doPaste() {
        Clipboard selection = getToolkit().getSystemSelection();
        if ( selection != null ) {
            Transferable selTrans = selection.getContents( null );
            if ( selTrans != null && 
                 selTrans.isDataFlavorSupported( STRING_FLAVOR ) ) {
                try {
                    String stringData = 
                        (String) selTrans.getTransferData( STRING_FLAVOR );
                    if ( stringData != null ) {
                        pasted( stringData );
                    }
                }
                catch ( UnsupportedFlavorException e ) {
                    // shouldn't happen
                }
                catch ( IOException e ) {
                    // shouldn't happen
                }
            }
        }
    }
}
