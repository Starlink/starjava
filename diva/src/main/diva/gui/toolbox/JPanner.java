/*
 * $Id: JPanner.java,v 1.7 2000/08/16 20:24:33 neuendor Exp $
 *
 * Copyright (c) 1998-2000 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.gui.toolbox;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.event.*;
import diva.canvas.CanvasUtilities;
import diva.util.java2d.ShapeUtilities;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

/**
 * A panner is a window that provides a mechanism to visualize and
 * manipulate a JViewport object without using scrollbars.  Unlike the
 * viewport, which contains a partial, full size rendition of the
 * contained component, this class contains a complete, scaled down
 * rendition of the component.  The bounds of the component are represented
 * by a blue rectangle and the bounds of the viewport on the component 
 * are visible as a red rectangle. Clicking or dragging within the
 * JPanner centers the viewport at that point on the component.
 *
 * @author Michael Shilman (michaels@eecs.berkeley.edu)
 * @author Steve Neuendorffer (neuendor@eecs.berkeley.edu)
 * @version $Revision: 1.7 $
 */
public class JPanner extends JPanel {
    /**
     * The target window that is being wrapped.
     */
    private JViewport _target = null;

    /**
     * The scrolling listener;
     */
    private ScrollListener _listener = new ScrollListener();

    /**
     * Construct a new panner that is initially viewing
     * nothing.  Use setViewport() to assign it to something.
     */
    public JPanner() {
        this(null);
    }
    
    /**
     * Construct a new wrapper that wraps the given
     * target.
     */
    public JPanner(JViewport target) {
        setViewport(target);
	addMouseListener(new PanMouseListener());
        addMouseMotionListener(new PanMouseListener());
    }

    /**
     * Set the target component that is being
     * wrapped.
     */
    public void setViewport(JViewport target) {
        if(_target != null) {
            _target.removeChangeListener(_listener);
        }
        _target = target;
        if(_target != null) {
            _target.addChangeListener(_listener);
        }
	repaint();
    }

    /**
     * Return the target component that is being
     * wrapped.
     */
    public JViewport getViewport() {
        return _target;
    }

    public void paintComponent(Graphics g) {
	if(_target != null) {
            Dimension viewSize =_target.getView().getSize();
	    Rectangle viewRect = 
		new Rectangle(0, 0, viewSize.width, viewSize.height);
	    Rectangle myRect = _getInsetBounds();
		
            AffineTransform forward = 
		CanvasUtilities.computeFitTransform(viewRect, myRect);
	    AffineTransform inverse;
            try {
                inverse = forward.createInverse();
            }
            catch(NoninvertibleTransformException e) {
                throw new RuntimeException(e.toString());
            }

            Graphics2D g2d = (Graphics2D)g;
            g2d.transform(forward);
            _target.getView().paint(g);
            
            g.setColor(Color.red);
            Rectangle r = _target.getViewRect();
            g.drawRect(r.x, r.y, r.width, r.height);

            g.setColor(Color.blue);
            Dimension d = _target.getView().getSize();
            g.drawRect(0, 0, d.width, d.height);

            g2d.transform(inverse);
        } else {
	    Rectangle r = _getInsetBounds();	    
	    g.clearRect(r.x, r.y, r.width, r.height);
	}
    }

    // Return a rectangle that fits inside the border
    private Rectangle _getInsetBounds() {
	Dimension mySize = getSize();
	Insets insets = getInsets();
	Rectangle myRect = 
	    new Rectangle(insets.left, insets.top, 
			  mySize.width - insets.top - insets.bottom,
			  mySize.height - insets.left - insets.right);
	return myRect;
    }
 

    //paint???
    private class ScrollListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            repaint();
        }
    }

    private Point _p;
    
    private class PanMouseListener extends MouseAdapter
        implements MouseMotionListener {
	public void setPosition(int x, int y) {
	    Dimension viewSize =_target.getView().getSize();
	    Rectangle viewRect = 
		new Rectangle(0, 0, viewSize.width, viewSize.height);
	    Rectangle myRect = _getInsetBounds();
		
	    AffineTransform forward = 
		CanvasUtilities.computeFitTransform(viewRect, myRect);
	    AffineTransform inverse;
	    try {
		inverse = forward.createInverse();
	    }
	    catch(NoninvertibleTransformException e) {
		throw new RuntimeException(e.toString());
	    }

	    x = (int)(x * inverse.getScaleX());
	    y = (int)(y * inverse.getScaleY());
	    Dimension extentSize = _target.getExtentSize();
	    _target.setViewPosition(new Point(x - extentSize.width/2, 
					      y - extentSize.height/2));
	}

        public void mousePressed(MouseEvent evt) {
            if(_target != null &&
                    (evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
		setPosition(evt.getX(), evt.getY());
            }
        }
        public void mouseMoved(MouseEvent evt) {
        }
        public void mouseDragged(MouseEvent evt) {
            if(_target != null &&
                    (evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {
		setPosition(evt.getX(), evt.getY());
	    }
        }
    }

    public static void main(String argv[]) {
        JFrame f = new JFrame();
        JList l = new JList();
        String[] data = {"oneeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee",
                         "twoooooooooooooooooooooooooooooooooooooooo",
                         "threeeeeeeeeeeeeeeee",
                         "fourrrrrrrrrrrrrrrrrrrrrrrrr"};
        JList dataList = new JList(data);
        JScrollPane p = new JScrollPane(dataList);
        p.setSize(200, 200);
        JPanner pan = new JPanner(p.getViewport());
        pan.setSize(200, 200);
        f.getContentPane().setLayout(new GridLayout(2, 1));
        f.getContentPane().add(p);
        f.getContentPane().add(pan);
        f.setSize(200, 400);
        f.setVisible(true);
    }
}

