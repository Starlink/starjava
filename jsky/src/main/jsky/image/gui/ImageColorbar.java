/*
 * ESO Archive
 *
 * $Id: ImageColorbar.java,v 1.4 2002/07/09 13:30:37 brighton Exp $
 *
 * who             when        what
 * --------------  ----------  ----------------------------------------
 * Allan Brighton  1999/11/17  Created
 *
 * Frank Tanner    2001/12/20  Added Constructor to use only the
 * 		   most minimal of information.
 */

package jsky.image.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

import javax.media.jai.LookupTableJAI;
import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MouseInputListener;

import jsky.image.BasicImageReadableProcessor;
import jsky.image.ImageChangeEvent;
import jsky.image.ImageProcessor;


/**
 * This widget displays a color bar with the colors used in the image
 * and allows you to manipulate the color lookup table by dragging
 * the mouse over the bar (based on the JAITutor demo by Dennis Sigel).
 *
 * @version $Revision: 1.4 $
 * @author Allan Brighton
 */
public class ImageColorbar extends JComponent implements MouseInputListener {

    /** Main image display */
    protected BasicImageReadableProcessor imageDisplay;

    /** Image processor object belonging to the display */
    protected ImageProcessor imageProcessor;

    /** Width of the colorbar */
    protected int componentWidth;

    /** Height of the colorbar */
    protected int componentHeight;

    /** Set to SwingConstants.HORIZONTAL or SwingConstants.VERTICAL for orientation of bar */
    protected int orient = SwingConstants.HORIZONTAL;

    /** Color lookup table used by the image */
    protected byte[][] lut;

    // starting position of mouse drag
    protected int mark;

    /**
     * Default constructor - displays a gray ramp.
     */
    public ImageColorbar() {
        defaultColormap();
    }

    /**
     * Creates an ImageColorbar with minimal image information
     * available.
     *
     * @param imageDisplay The interface through which the
     * 		ImageColorbar can access a processor and other
     * 		image information.
     */
    public ImageColorbar(BasicImageReadableProcessor imageDisplay) {
        this.orient = SwingConstants.HORIZONTAL;
        setImageDisplay(imageDisplay);
    }

    /**
     * Show the colormap for the given image display.
     */
    public ImageColorbar(BasicImageReadableProcessor imageDisplay, int orient) {
        this.orient = orient;
        setImageDisplay(imageDisplay);

    }


    public void setImageDisplay(BasicImageReadableProcessor imageDisplay) {
        this.imageDisplay = imageDisplay;
        imageProcessor = imageDisplay.getImageProcessor();

        // register to receive mouse events
        addMouseListener(this);
        addMouseMotionListener(this);

        // register to receive notification when the colormap changes
        imageProcessor.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent ce) {
                ImageChangeEvent e = (ImageChangeEvent) ce;
                if (e.isNewColormap())
                    newColormap();
            }
        });

        newColormap();
    }


    /** Return the orientation of colorbar (SwingConstants.HORIZONTAL or SwingConstants.VERTICAL) */
    public int getOrient() {
        return orient;
    }

    /** Set the orientation of the colorbar (to SwingConstants.HORIZONTAL or SwingConstants.VERTICAL) */
    public void setOrient(int orient) {
        this.orient = orient;
    }


    /**
     * Called when the image colormap is changed
     */
    public void newColormap() {
        LookupTableJAI lutJAI = imageProcessor.getColorLookupTable();
        if (lutJAI != null) {
            lut = lutJAI.getByteData();
        }
        else {
            defaultColormap();
        }
        repaint();
    }


    /** Make a default grayscale colormap */
    protected void defaultColormap() {
        lut = new byte[3][256];
        for (int i = 0; i < 256; i++) {
            lut[0][i] = (byte) i;
            lut[1][i] = (byte) i;
            lut[2][i] = (byte) i;
        }
    }


    /** Records a new size.  Called by the AWT. */
    public void setBounds(int x, int y, int width, int height) {
        componentWidth = width;
        componentHeight = height;
        super.setBounds(x, y, width, height);
    }


    /**
     * Paint the colorbar onto a Graphics object.
     */
    public synchronized void paintComponent(Graphics g) {
        Graphics2D g2D = (Graphics2D) g;
        g2D.setColor(getBackground());
        g2D.fillRect(0, 0, componentWidth, componentHeight);

        if (orient == SwingConstants.HORIZONTAL) {
            float slope = (float) componentWidth / 256.0F;

            for (int n = 0; n < lut[0].length; n++) {
                int w = componentWidth - (int) ((float) n * slope);
                int v = lut[0].length - n - 1;
                int red = lut[0][v] & 0xFF;
                int green = lut[1][v] & 0xFF;
                int blue = lut[2][v] & 0xFF;
                g.setColor(new Color(red, green, blue));
                g.fillRect(0, 0, w, componentHeight);
            }
        }
        else if (orient == SwingConstants.VERTICAL) {
            float slope = (float) componentHeight / 256.0F;

            for (int n = 0; n < lut[0].length; n++) {
                int h = componentHeight - (int) ((float) n * slope);
                int red = lut[0][n] & 0xFF;
                int green = lut[1][n] & 0xFF;
                int blue = lut[2][n] & 0xFF;
                g.setColor(new Color(red, green, blue));
                g.fillRect(0, 0, componentWidth, h);
            }
        }
    }


    /**
     * Invoked when a mouse button is pressed on the image
     */
    public void mousePressed(MouseEvent e) {
        mark = e.getX();
        if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) == 0) {
            // not button 1
            imageProcessor.resetColormap();
            imageProcessor.update();
        }
    }


    /**
     * Invoked when a mouse button is pressed on the component and then dragged.
     */
    public void mouseDragged(MouseEvent e) {
        int amount = e.getX() - mark;
        if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
            // button 1 dragged
            if (e.isShiftDown()) {
                // shift-B1 dragged
                imageProcessor.rotateColormap(amount);
                mark = e.getX();
                imageProcessor.update();
            }
            else {
                // B1 dragged
                imageProcessor.shiftColormap(amount);
                imageProcessor.update();
            }
        }
        else {
            // other button dragged
            imageProcessor.scaleColormap(amount);
            imageProcessor.update();
        }
    }

    /**
     * Invoked when a mouse button is released.
     */
    public void mouseReleased(MouseEvent e) {
        // save current state for next operation
        imageProcessor.saveColormap();
    }


    /** These are not currently used */
    public void mouseMoved(MouseEvent e) {
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }
}
