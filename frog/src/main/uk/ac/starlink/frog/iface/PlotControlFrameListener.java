package uk.ac.starlink.frog.iface;

import javax.swing.*;
import javax.swing.event.*;
import java.util.EventListener;

import uk.ac.starlink.frog.util.FrogDebug;

/**
 * PlotControlFrameListener is used when listening for the creation, removal 
 * and changes of to a PlotControlFrame (an extension of a JInternalFrame)
 *
 * @since $Date$
 * @since 30-JAN-2003
 * @author Alasdair Allan
 * @version $Id$
 */
public class PlotControlFrameListener implements InternalFrameListener 
{
   /**
     *  Application wide debug manager
     */
    protected FrogDebug debugManager = FrogDebug.getReference();


    /**
     * Create a Listener for a PlotControlFrame
     */
     public PlotControlFrameListener() {
        super();
     }
    
    /**
     * PlotControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameClosing(InternalFrameEvent e) {
       
       //debugManager.print("internalFrameClosing() Event");
       PlotControlFrame frame = (PlotControlFrame)e.getInternalFrame();
       frame.closeFrame();
    }   
    
    /**
     * PlotControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameClosed(InternalFrameEvent e) {
       //debugManager.print("internalFrameClosed() Event");
       // do nothing;
    }

    /**
     * PlotControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameOpened(InternalFrameEvent e) {
       //debugManager.print("internalFrameOpened() Event");
       // do nothing;
    }
    
    /**
     * PlotControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameIconified(InternalFrameEvent e) {
       //debugManager.print("internalFrameIconified() Event");
       // do nothing;
    }

    /**
     * PlotControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameDeiconified(InternalFrameEvent e) {
       //debugManager.print("internalFrameDeiconified() Event");
       // do nothing;
    }

    /**
     * PlotControlFrame is 
     *
     * @param e the InternalFrameEvent
     */

    public void internalFrameActivated(InternalFrameEvent e) {
       //debugManager.print("internalFrameActivated() Event");
       // do nothing;
    }

    /**
     * PlotControlFrame is 
     *
     * @param e the InternalFrameEvent
     */
    public void internalFrameDeactivated(InternalFrameEvent e) {
       //debugManager.print("internalFrameDeactivated() Event");
       // do nothing;
    }

}
