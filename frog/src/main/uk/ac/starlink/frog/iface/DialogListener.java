package uk.ac.starlink.frog.iface;

import javax.swing.*;
import javax.swing.event.*;
import java.util.EventListener;

import uk.ac.starlink.frog.util.FrogDebug;

/**
 * DialogListener is used when listening for the creation, removal 
 * and changes of to various dialogs, all of which are extensions of 
 * a JInternalFrame)
 *
 * @since $Date$
 * @since 17-FEB-2003
 * @author Alasdair Allan
 * @version $Id$
 */
public class DialogListener implements InternalFrameListener 
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


    /**
     * Create a Listener for a PlotControlFrame
     */
     public DialogListener() {
        super();
     }
    
    /**
     * Dialog is closing
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameClosing(InternalFrameEvent e) {
       // do nothing;
    }   
    
    /**
     * Dialog is closed
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameClosed(InternalFrameEvent e) {
       // do nothing;
    }

    /**
     * Dialog is opening
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameOpened(InternalFrameEvent e) {
       // do nothing;
    }
    
    /**
     * Dialog is iconified
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameIconified(InternalFrameEvent e) {
       // do nothing;
    }

    /**
     * Dialog is deiconified
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameDeiconified(InternalFrameEvent e) {
       // do nothing;
    }

    /**
     * Dialog is activated
     *
     * @param e the InternalFrameEvent
     */

    public void internalFrameActivated(InternalFrameEvent e) {
       // do nothing;
    }

    /**
     * Dialog is deactivated
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameDeactivated(InternalFrameEvent e) {
       // do nothing;
    }

}
