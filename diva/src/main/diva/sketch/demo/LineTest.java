/*
 * $Id: LineTest.java,v 1.4 2001/08/28 06:37:11 hwawen Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.demo;

import diva.sketch.JSketch;
import diva.sketch.SketchController;
import diva.sketch.BasicInterpreter;
import diva.sketch.recognition.Recognition;
import diva.sketch.recognition.RecognitionSet;
import diva.sketch.toolbox.LineRecognizer;
import diva.canvas.event.LayerEvent;

import diva.gui.AppContext;
import diva.gui.BasicFrame;
import java.awt.BorderLayout;
import javax.swing.JPanel;

/**
 * This application allows the user to sketch strokes one by one, and
 * it tries to recognize whether the stroke is a line.  Prints out
 * type and confidence value on standard out.
 *
 * @author Heloise Hse  (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class LineTest {

    public static void main (String argv[]){
        AppContext context = new BasicFrame("Line Test");
	new LineTest(context);
    }
    
    public LineTest(AppContext context){
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        context.getContentPane().add(panel);
        
        JSketch jsketch = new JSketch();
        SketchController controller = jsketch.getSketchPane().getSketchController();
        controller.setForegroundInterpreter(new LineInterpreter(controller));

        panel.add("Center", jsketch);
         context.setSize(500,500);
        context.setVisible(true);
    }

    private class LineInterpreter extends BasicInterpreter {
        LineRecognizer _recognizer;
        
        public LineInterpreter(SketchController controller){
            super(controller);
            _recognizer = new LineRecognizer();
        }
        
        public void mouseReleased(LayerEvent e){
            super.mouseReleased(e);
            RecognitionSet set = _recognizer.strokeCompleted(getCurrentStroke());
            Recognition best = set.getBestRecognition();
            if(best != null){
                System.out.println(best.getType().getID() +": " + best.getConfidence());
            }
        }
    }
}

