/*
 * $Id: SketchTest1.java,v 1.4 2001/07/23 03:59:01 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JFrame;
import java.awt.Frame;

public class SketchTest1 extends Frame {
    private static final int N = 3;
    private int[] _prevX = new int[N];
    private int[] _prevY = new int[N];
    private int _cnt = 0;

    public static void main(String argv[]){
        SketchTest1 st= new SketchTest1();
        st.setSize(600,400);
        st.setVisible(true);
    }
    
    public SketchTest1 () {
        addMouseListener(new LocalMouseListener());
        addMouseMotionListener(new LocalMouseMotionListener());
    }

    public class LocalMouseListener extends MouseAdapter {
        public void mousePressed(MouseEvent e){
            _cnt = 0;
            _prevX[0] = e.getX();
            _prevY[0] = e.getY();
            _cnt++;
        }
    }

    public class LocalMouseMotionListener extends MouseMotionAdapter {
        public void mouseDragged(MouseEvent e){
            _prevX[_cnt] = e.getX();
            _prevY[_cnt] = e.getY();
            _cnt++;
            if(_cnt % N == 0) {
                Graphics2D g2d =
                    (Graphics2D)getGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                for(int i = 0; i < N-1; i++) {
                    g2d.drawLine(_prevX[i], _prevY[i],
                            _prevX[i+1], _prevY[i+1]);
                }
                g2d.dispose();
                _prevX[0] = _prevX[N-1];
                _prevY[0] = _prevY[N-1];
                _cnt = 1;
            }
        }
    }
}


