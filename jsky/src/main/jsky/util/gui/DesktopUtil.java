/*
 * Copyright 2000 Association for Universities for Research in Astronomy, Inc.,
 * Observatory Control System, Gemini Telescopes Project.
 *
 * $Id: DesktopUtil.java,v 1.2 2002/07/09 13:30:38 brighton Exp $
 */

package jsky.util.gui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;

import jsky.util.Resources;


/**
 * Draws tiles of a given image on the background of a given JDesktopPane.
 *
 * @version $Revision: 1.2 $
 * @author Allan Brighton
 */
public class DesktopUtil {

    JDesktopPane desktop;
    private Image bg;
    private JComponent component;

    /**
     * Make an icon image, tile it on a JComponent and put the component
     * in a background layer of the desktop.
     */
    public DesktopUtil(JDesktopPane desk, String iconName) {
        desktop = desk;
        ImageIcon icon = (ImageIcon) Resources.getIcon(iconName);
        bg = icon.getImage();

        component = new JComponent() {

            protected void paintComponent(Graphics g) {
                int w = 0, h = 0, width = getWidth(), height = getHeight();
                while (w < width) {
                    g.drawImage(bg, w, h, this);
                    while ((h + bg.getHeight(this)) < height) {
                        h += bg.getHeight(this);
                        g.drawImage(bg, w, h, this);
                    }
                    h = 0;
                    w += bg.getWidth(this);
                }
            }
        };
        desktop.add(component, new Integer(Integer.MIN_VALUE));

        // monitor resize events
        desktop.addComponentListener(new ComponentAdapter() {

            public void componentResized(ComponentEvent e) {
                component.setSize(desktop.getSize());
            }
        });
    }
}

