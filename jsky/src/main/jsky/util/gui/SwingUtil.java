/*
 * Copyright 2002 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: SwingUtil.java,v 1.3 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.Component;
import java.awt.Frame;
import java.beans.PropertyVetoException;

import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;


/**
 * Various Swing related utility methods.
 *
 * @version $Revision: 1.3 $
 * @author Allan Brighton
 */
public class SwingUtil {

    /** Make sure that the given JFrame or JInternalFrame is visible */
    public static void showFrame(Component frame) {
        frame.setVisible(true);

        if (frame instanceof JFrame) {
            ((JFrame) frame).setState(Frame.NORMAL);
        }
        else if (frame instanceof JInternalFrame) {
            JDesktopPane desktop = DialogUtil.getDesktop();
            JInternalFrame f = (JInternalFrame) frame;
            try {
                f.setIcon(false);
            }
            catch (PropertyVetoException e) {
            }
            try {
                f.setClosed(false);
            }
            catch (PropertyVetoException e) {
            }
            desktop.moveToFront(f);
        }
    }

    /** Return the Frame (or JFrame) containing the given window, or null if there is none. */
    public static Frame getFrame(Component window) {
        if (window instanceof Frame || window == null) {
            return (Frame) window;
        }

        return getFrame(window.getParent());
    }
}
