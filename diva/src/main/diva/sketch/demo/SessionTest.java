/*
 * $Id: SessionTest.java,v 1.5 2001/07/22 22:01:46 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.demo;
import diva.sketch.recognition.*;
import diva.sketch.toolbox.*;
import diva.sketch.SketchController;
import diva.sketch.JSketch;
import diva.sketch.SketchModel;
import diva.sketch.SketchPane;
import diva.sketch.StrokeSymbol;
import diva.gui.AppContext;
import diva.gui.BasicFrame;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileReader;
import java.util.Iterator;
import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * This application allows the user to sketch a collection
 * of strokes and then recognize them as a session.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.5 $
 * @rating Red
 */
public class SessionTest {
    SketchController _controller;
    SceneRecognizer _recognizer;
    
    /**
     * Create and run a basic sketch application.
     */
    public static void main(String argv[]) {
        AppContext context = new BasicFrame("Sketch Demo");
	new SessionTest(context);
    }

    /**
     * Construct a new sketch-based editor.
     */
    public SessionTest (AppContext context) {
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        context.getContentPane().add(panel);
        
        JSketch jsketch = new JSketch();
        panel.add("Center", jsketch);
        SketchPane pane = jsketch.getSketchPane();
        _controller = pane.getSketchController();

        try {
            _recognizer = new DashedPathRecognizer(new StrokeSceneRecognizer(new BasicStrokeRecognizer(new FileReader("shapes.tc"))));
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
            
        JButton but = new JButton("Recognize");
        but.addActionListener(new ButtonListener());
        panel.add("South", but);
        
        //context.addWindowListener(new LocalWindowListener());
        context.setSize(500,500);
        context.setVisible(true);
    }

    private class ButtonListener implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            SketchModel m = _controller.getSketchModel();
            Scene db = new BasicScene();
            StrokeElement[] session = new StrokeElement[m.getSymbolCount()];
            int j = 0;
            StrokeSymbol[] symbols = new StrokeSymbol[m.getSymbolCount()];
            for(Iterator i = m.symbols(); i.hasNext(); ) {
                symbols[j] = (StrokeSymbol)i.next();
                TimedStroke s = symbols[j].getStroke();
                session[j++] = db.addStroke(s);
            }
            System.out.println("ButtonListener, sessionCompleted, "+ session.length + " symbols");
            _recognizer.sessionCompleted(session, db);
            for(j = 0; j < symbols.length; j++) {
                m.removeSymbol(symbols[j]);
            }
        }
    }
}

