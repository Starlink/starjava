package uk.ac.starlink.frog.iface;

import javax.swing.*;
import javax.swing.event.*;
import java.util.EventListener;

import uk.ac.starlink.frog.util.FrogDebug;

/**
 * GramControlFrameListener is used when listening for the creation, removal 
 * and changes of to a GramControlFrame (an extension of a JInternalFrame)
 *
 * @since $Date$
 * @since 30-JAN-2003
 * @author Alasdair Allan
 * @version $Id$
 */
public class GramControlFrameListener implements InternalFrameListener 
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


    /**
     * Create a Listener for a GramControlFrame
     */
     public GramControlFrameListener() {
        super();
     }
    
    /**
     * GramControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameClosing(InternalFrameEvent e) {
       
       //debugManager.print("internalFrameClosing() Event");
       GramControlFrame frame = (GramControlFrame)e.getInternalFrame();
       frame.closeFrame();
    }   
    
    /**
     * GramControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameClosed(InternalFrameEvent e) {
       //debugManager.print("internalFrameClosed() Event");
       // do nothing;
    }

    /**
     * GramControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameOpened(InternalFrameEvent e) {
       //debugManager.print("internalFrameOpened() Event");
       // do nothing;
    }
    
    /**
     * GramControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameIconified(InternalFrameEvent e) {
       //debugManager.print("internalFrameIconified() Event");
       // do nothing;
    }

    /**
     * GramControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameDeiconified(InternalFrameEvent e) {
       //debugManager.print("internalFrameDeiconified() Event");
       // do nothing;
    }

    /**
     * GramControlFrame is 
     *
     * @param e the InternalFrameEvent
     */

    public void internalFrameActivated(InternalFrameEvent e) {
       //debugManager.print("internalFrameActivated() Event");
       // do nothing;
    }

    /**
     * GramControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameDeactivated(InternalFrameEvent e) {
       //debugManager.print("internalFrameDeactivated() Event");
       // do nothing;
    }

}
