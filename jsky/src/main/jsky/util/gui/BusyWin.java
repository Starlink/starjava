/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: BusyWin.java,v 1.4 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.lang.Runnable;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.SwingUtilities;


/**
 * Utility class used to disable GUI input while work is in progress.
 *
 * @author Allan Brighton
 */
public class BusyWin {

    /**
     * Temporarily disable (or enable) all frames except the given one.
     *
     * @param busy   if true, display the busy cursor and disable all frames except
     *               the given one.
     *
     * @param parent if not null, this frame (JFrame or JInternalFrame)
     *               is ignored (not enabled/disabled)
     */
    public static void setBusy(boolean busy, Component parent) {

        // XXX Note: Glass panes seem to behave differently with internal frames,
        // XXX so the code commented out below did not work correctly.
        // XXX If you clicked on an internal frame, the glass pane was hidden,
        // XXX and when it was supposed to be hidden, it reappeared when the
        // XXX frame lost the focus (at least under Linux, where I tested it).

        //JDesktopPane desktop = DialogUtil.getDesktop();
        //if (desktop == null) {

        // using JFrames
        Frame[] frames = Frame.getFrames();
        for (int i = 0; i < frames.length; i++) {
            Component c = frames[i];
            if (c == parent || !(c instanceof JFrame) || !c.isVisible())
                continue;
            JFrame frame = (JFrame) c;
            Component glassPane = frame.getGlassPane();
            if (!(glassPane instanceof GlassPane)) {
                glassPane = new GlassPane();
                frame.setGlassPane(glassPane);
            }
            glassPane.setVisible(busy);

            if (busy) {
                // force immediate update
                Graphics g = frame.getGraphics();
                if (g != null) {
                    glassPane.paint(g);
                }
            }
        }

        //}
        //else {
        // using JInternalFrames
        //JInternalFrame[] frames = desktop.getAllFrames();
        //for(int i = 0; i < frames.length; i++) {
        //Component c = frames[i];
        //if (c == parent || ! (c instanceof JInternalFrame) || !c.isVisible())
        //    continue;
        //JInternalFrame frame = (JInternalFrame)c;
        //Component glassPane = frame.getGlassPane();
        //if (! (glassPane instanceof GlassPane)) {
        //    glassPane = new GlassPane();
        //    frame.setGlassPane(glassPane);
        //}
        //glassPane.setVisible(busy);
        //}
    }


    /**
     * Temporarily disable (or enable) all of the application's frames.
     *
     * @param busy if true, display the busy cursor and disable all frames except
     *             the given one.
     */
    public static void setBusy(final boolean busy) {
        setBusy(busy, null);
    }

    /**
     * Temporarily show the busy cursor for all frames except the given one.
     *
     * @param busy   if true, display the busy cursor and disable all frames except
     *               the given one.
     *
     * @param parent if not null, this frame (JFrame or JInternalFrame)
     *               is ignored (not enabled/disabled)
     */
    public static void showBusy(final Component parent) {
        setBusy(true, parent);
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                setBusy(false, parent);
            }
        });
    }


    /**
     * Temporarily show the busy cursor for all application frames.
     *
     */
    public static void showBusy() {
        showBusy(null);
    }

}

/**
 * This local class is used to block input events while in busy mode.
 */
class GlassPane extends JComponent implements MouseListener, MouseMotionListener {

    public GlassPane() {
        addMouseListener(this);
        addMouseMotionListener(this);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    public void mouseMoved(MouseEvent e) {
        e.consume();
    }

    public void mouseDragged(MouseEvent e) {
        e.consume();
    }

    public void mouseClicked(MouseEvent e) {
        e.consume();
    }

    public void mouseEntered(MouseEvent e) {
        e.consume();
    }

    public void mouseExited(MouseEvent e) {
        e.consume();
    }

    public void mousePressed(MouseEvent e) {
        e.consume();
    }

    public void mouseReleased(MouseEvent e) {
        e.consume();
    }
}
