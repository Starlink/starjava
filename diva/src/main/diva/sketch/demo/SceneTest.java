/*
 * $Id: SceneTest.java,v 1.3 2001/07/22 22:01:46 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.demo;
import diva.sketch.recognition.*;
import diva.sketch.toolbox.*;
import diva.sketch.JSketch;
import diva.sketch.SketchModel;
import diva.sketch.SketchPane;
import diva.sketch.StrokeSymbol;
import diva.sketch.SketchParser;
import diva.sketch.SketchWriter;
import diva.gui.AppContext;
import diva.gui.ApplicationContext;
import diva.gui.BasicFrame;
import diva.gui.ExtensionFileFilter;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import java.util.Iterator;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * This application reads in a ground truth scene file,
 * recognizes it with a specified scene recognizer, and
 * compares the results with the original scene.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.3 $
 * @rating Red
 */
public class SceneTest {
    /**
     * Create and run a basic sketch application.
     */
    public static void main(String argv[]) {
        if(argv.length < 1) {
            String err = "Usage: java SceneTest file1.scn [...]";
            System.err.println(err);
            System.exit(-1);
        }
        try {
            for(int j = 0; j < argv.length; j++) {
                SceneRecognizer rec = new LLRSceneRecognizer(new StrokeSceneRecognizer(new BasicStrokeRecognizer(new FileReader("shapes.tc"))));
                SceneParser parser = new SceneParser();
                Scene db1 = (Scene)parser.parse(new FileReader(argv[j]));
                Scene db2 = new BasicScene();
                for(Iterator i = db1.strokes().iterator(); i.hasNext(); ) {
                    TimedStroke s = ((StrokeElement)i.next()).getStroke();
                    StrokeElement se = db2.addStroke(s);
                    rec.strokeCompleted(se, db2);
                }
                LLRSceneMetric metric = new LLRSceneMetric();
                System.out.println(argv[j] + ": " + metric.apply(db1, db2));
                SceneWriter sw = new SceneWriter();
                sw.write(db2, (SceneElement)(db2.roots().get(0)),
                        new FileWriter(argv[j] + "b"));
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}

