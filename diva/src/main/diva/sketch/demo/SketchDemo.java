/*
 * $Id: SketchDemo.java,v 1.16 2001/07/22 22:01:46 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.demo;
import diva.sketch.SketchController;
import diva.sketch.JSketch;
import diva.sketch.SketchModel;
import diva.sketch.SketchPane;
import diva.sketch.SketchParser;
import diva.sketch.SketchWriter;
import diva.gui.AppContext;
import diva.gui.ApplicationContext;
import diva.gui.BasicFrame;
import diva.gui.ExtensionFileFilter;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * This application illustrates how to use the diva.sketch framework
 * to build a sketch-based application.  <p>
 *
 * The demo launches an editor for sketching and provides file saving
 * and loading capabilities.  The main purpose of this demo is to
 * illustrate how to set up a sketch-based UI using the diva.sketch
 * package.  It does not show how to add gesture recognition to
 * interprete the sketch.  Please see SimpleDemo for more information.
 * <p>
 *
 * The figures sketched can be selected by control-clicking on the
 * stroke itself.  When selected, a set of blue handles will appear
 * around the figure and a red drag handle will appear in the center
 * of the figure.  To resize, drag one of the blue handles; to move,
 * drag the red, square handle.
 * <p>
 *
 * The following three lines are all you need to create a sketch-based
 * user interface using the package:
 * 
 * <pre>
 * JFrame f = new JFrame();
 * JSketch sketch = new JSketch();
 * f.getContentPane().add("Center", sketch);
 * </pre>
 *
 * The rest of the code in the demo is mainly to add more functionality.
 *
 * @author Heloise Hse (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.16 $
 * @rating Red
 */
public class SketchDemo {
    private final static String SML = "sml";

    private SketchController _controller;
    private SketchParser _parser;
    private SketchWriter _writer;
    private FileFilter _filter;

    /**
     * Create and run a basic sketch application.
     */
    public static void main(String argv[]) {
        AppContext context = new BasicFrame("Sketch Demo");
	new SketchDemo(context);
    }

    /**
     * Construct a new sketch-based editor.
     */
    public SketchDemo (AppContext context) {
        //instantiate a JSketch and add it to the content pane
        JSketch jsketch = new JSketch();
        context.getContentPane().add("Center", jsketch);

        //keep a reference to the sketch controller
        _controller = jsketch.getSketchPane().getSketchController();

        //save and load utilities
        _parser = new SketchParser();
        _writer = new SketchWriter();
        _filter = new ExtensionFileFilter(SML, "Sketch Markup Language");

        if(context instanceof ApplicationContext){
            ((ApplicationContext)context).addWindowListener(new LocalWindowListener());
            JMenuBar mb = new JMenuBar();
            initializeMenuBar(mb);
            context.setJMenuBar(mb);
        }
        context.setSize(500,500);
        context.setVisible(true);
    }

    private void initializeMenuBar(JMenuBar mb){
        JMenuItem item;

        JMenu menuFile = new JMenu("File");
        menuFile.setMnemonic('F');
        mb.add(menuFile);

        item = menuFile.add(new AbstractAction("Load"){
            public void actionPerformed(ActionEvent e){
                JFileChooser chooser =
                new JFileChooser(System.getProperty("user.dir"));
                chooser.addChoosableFileFilter(_filter);
                chooser.setFileFilter(_filter);
                int returnVal =
                chooser.showOpenDialog((Component)e.getSource());
                if(returnVal == JFileChooser.APPROVE_OPTION){
                    try{
                        File file = chooser.getSelectedFile();
                        SketchModel model = (SketchModel)_parser.parse(new FileReader(file));
                        _controller.setSketchModel(model);
                        System.out.println("Opened " + file.getName());
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        item = menuFile.add(new AbstractAction("Save"){
            public void actionPerformed(ActionEvent e){
                JFileChooser chooser =
                new JFileChooser(System.getProperty("user.dir"));
                chooser.addChoosableFileFilter(_filter);
                chooser.setFileFilter(_filter);
                int returnVal =
                chooser.showSaveDialog((Component)e.getSource());
                if(returnVal == JFileChooser.APPROVE_OPTION){
                    try{
                        File file = chooser.getSelectedFile();
                        //make sure the file extension is .sk
                        if(!_filter.accept(file)){
                            file = new File(file.getName()+"."+SML);
                        }
                        FileWriter fwriter = new FileWriter(file.getName());
                        _writer.writeModel(_controller.getSketchModel(),
                                fwriter);
                        fwriter.close();
                        System.out.println("Saved to " + file.getName());
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        item = menuFile.add(new AbstractAction("Exit"){
             public void actionPerformed(ActionEvent e){
                 System.exit(-1);
             }
        });
    }
    
    private class LocalWindowListener extends WindowAdapter {
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
        public void windowIconified (WindowEvent e) {
            System.out.println(e);
        }
        public void windowDeiconified (WindowEvent e) {
            System.out.println(e);
        }
    }
}

