/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * 
 * Copyright 2010-2016 The Apache Software Foundation.
 * Copyright 2016 Peter W. Draper.
 *
 * History:
 *    18-APR-2016 (Peter W. Draper)
 *      Version based on WorkbenchScale.java from the Apache opencmis package.
 */
package uk.ac.starlink.util.gui;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel;

/**
 * A scaled version of the Nimbus look and feel. The default scale is
 * 1.5, but this can be changed using the property "uk.ac.starlink.uiscale".
 *
 * Realize an instance using new ScaledLookAndFeel.ScaledNimbusLookAndFeel().
 * 
 * When using this class it is necessary to realize it before any UI
 * components, otherwise some components will not resize correctly
 * (JTable for instance).
 */
public class ScaledLookAndFeel {
    public static final String UI_SCALE = "uk.ac.starlink.uiscale";

    private static double scaleFactor = 1.5;

    static {
        String scaleStr = System.getProperty(UI_SCALE);
        if (scaleStr != null) {
            try {
                scaleFactor = Double.parseDouble(scaleStr.trim());
            } catch (Exception e) {
                // ignore
                scaleFactor = 1.5;
            }
        }
    }

    public static double getScaleFactor() {
        return scaleFactor;
    }

    // Resize away from zero so that we grow in all directions.
    public static int scaleInt(int x) {
        return (int) Math.round(x * getScaleFactor());
    }

    public static Font scaleFont(Font font) {
        Font sfont = font.deriveFont(font.getSize() * (float)getScaleFactor());

        // Make sure this value is honoured by JTable.
        UIManager.put("Table.rowHeight", sfont.getSize());
        return sfont;
    }

    public static Insets scaleInsets(Insets insets) {
        return new Insets(scaleInt(insets.top), scaleInt(insets.left),
                          scaleInt(insets.bottom), scaleInt(insets.right));
    }

    public static Dimension scaleDimension(Dimension dim) {
        return new Dimension(scaleInt(dim.width), scaleInt(dim.height));
    }

    public static Border scaleBorder(Border border) {
        if (border instanceof EmptyBorder) {
            Insets borderInsets =
                scaleInsets(((EmptyBorder) border).getBorderInsets());
            return BorderFactory.createEmptyBorder(borderInsets.top,
                                                   borderInsets.left,
                                                   borderInsets.bottom,
                                                   borderInsets.right);
        } else if (border instanceof LineBorder) {
            return BorderFactory.createLineBorder
                ( ((LineBorder) border).getLineColor(),
                  scaleInt(((LineBorder) border).getThickness()));
        } else if (border instanceof MatteBorder) {
            Insets borderInsets = scaleInsets
                (((MatteBorder) border).getBorderInsets());
            return BorderFactory.createMatteBorder(borderInsets.top,
                                                   borderInsets.left,
                                                   borderInsets.bottom,
                                                   borderInsets.right,
                                  ((MatteBorder) border).getMatteColor());
        }
        return border;
    }

    public static ImageIcon scaleIcon(ImageIcon icon) {
        int newWidth = (int) 
            Math.round(icon.getIconWidth() * getScaleFactor());
        int newHeight = (int) 
            Math.round(icon.getIconHeight() * getScaleFactor());

        BufferedImage img = new BufferedImage(newWidth, newHeight,
                                              BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                           RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(icon.getImage(), 0, 0, newWidth, newHeight, 0, 0,
                    icon.getIconWidth(), icon.getIconHeight(),
                    null);
        g.dispose();

        return new ImageIcon(img);
    }

    public static class ScaledNimbusLookAndFeel extends NimbusLookAndFeel {
        private static final long serialVersionUID = 1L;

        private UIDefaults defs;
        private boolean isScaled;

        public ScaledNimbusLookAndFeel() {
            isScaled = false;
        }

        @Override
        public synchronized UIDefaults getDefaults() {
            if (isScaled) {
                return defs;
            }

            defs = super.getDefaults();

            Map<String, Object> newDefs = new HashMap<String, Object>();

            Enumeration<Object> enumeration = defs.keys();
            while (enumeration.hasMoreElements()) {
                String key = enumeration.nextElement().toString();

                Font font = defs.getFont(key);
                if (font != null) {
                    newDefs.put(key, scaleFont(font));
                }

                Dimension dim = defs.getDimension(key);
                if (dim != null) {
                    newDefs.put(key, scaleDimension(dim));
                }

                Insets insets = defs.getInsets(key);
                if (insets != null) {
                    newDefs.put(key, scaleInsets(insets));
                }

                Border border = defs.getBorder(key);
                if (border != null) {
                    newDefs.put(key, scaleBorder(border));
                }
            }

            for (Map.Entry<String, Object> entry : newDefs.entrySet()) {
                defs.put(entry.getKey(), entry.getValue());
            }

            isScaled = true;

            return defs;
        }
    }
}
