/*
 * $Id: RegionLabeler.java,v 1.4 2001/07/22 22:01:45 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.sketch.demo;

import diva.sketch.recognition.*;
import diva.sketch.toolbox.*;
import diva.sketch.*;
import diva.sketch.features.FEUtilities;
import diva.sketch.features.PathLengthFE;
import diva.whiteboard.SketchStoragePolicy;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.Figure;
import diva.canvas.event.LayerEvent;

import diva.gui.AppContext;
import diva.gui.ApplicationContext;
import diva.gui.BasicFrame;
import diva.gui.ExtensionFileFilter;

import java.awt.geom.Rectangle2D;
import java.awt.Component;
import java.awt.GridLayout;
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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * This utility program allows users to sketch drawings,
 * label the strokes in the drawings with prespecified
 * labels, and write the scene out to a file.  The structure
 * of the resulting scene is:
 *
 * <pre>
 *                       root
 *             _________/    \________
 *            /                       \
 *         region1      ...           regionN
 *          /   \                    /   |    \
 *       lbl1   lbl1              lblN  lblN  lblN
 *        |       |                /     |      \
 *     stroke1 stroke2      strokeK-2 strokeK-1 strokeK
 * </pre>
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @version $Revision: 1.4 $
 * @rating Red
 */
public class RegionLabeler { // extends AbstractApplication {
    public static final String DRAW_MODE = "draw";
    public static final String UNKNOWN_REGION = "unknown";
    private HashMap _regionsToSymbols = new HashMap();
    private HashMap _symbolsToRegions = new HashMap();
    LabelingInterpreter _labeler;
    SceneRecognizer _recognizer;
    
    /**
     * Create and run a basic sketch application.
     */
    public static void main(String argv[]) {
        AppContext context = new ApplicationContext();
	new RegionLabeler(context, argv);
    }

    /**
     * Construct a new sketch-based editor.
     */
    public RegionLabeler (AppContext context, String argv[]) {
        if(argv.length == 0) {
            String err = "Usage: regionLabeler lbl1 [... lblN]";
            System.err.println(err);
            System.exit(-1);
        }
        JSketch jsketch = new JSketch();
        context.getContentPane().add("Center", jsketch);
        SketchPane pane = jsketch.getSketchPane();
        SketchController controller = pane.getSketchController();
        _labeler = new LabelingInterpreter(controller);
        controller.setForegroundInterpreter(_labeler);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(1, argv.length+2));
        JButton save = new JButton("save");
        save.addActionListener(new SaveListener());
        panel.add(save);
        
        JButton but = new JButton(DRAW_MODE);
        but.addActionListener(new ButtonListener());
        panel.add(but);
        
        for(int i = 0; i < argv.length; i++) {
            but = new JButton(argv[i]);
            but.addActionListener(new ButtonListener());
            panel.add(but);
        }
        // setStoragePolicy(new SketchStoragePolicy());
        context.getContentPane().add("South", panel);
        
        //context.addWindowListener(new LocalWindowListener());
        context.setSize(500,500);
        context.setVisible(true);
    }

    private class SaveListener implements ActionListener {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            Scene out = mapToScene(_labeler.getController().getSketchModel());
            SceneElement root = (SceneElement)out.roots().get(0);
            try {
                SceneWriter writer = new SceneWriter();
                writer.write(out, root, new FileWriter("test.scn"));
            }
            catch(Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        }
    }
    
    private class ButtonListener implements ActionListener {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            JButton but = (JButton)evt.getSource();
            String type = but.getText();
            _labeler.setLabel(type);
        }
    }

    private class LabelingInterpreter extends BasicInterpreter {
        private LassoSelectionAction _lassoAction = new LassoSelectionAction(this);
        private String _label;

        public LabelingInterpreter(SketchController c) {
            super(c);
            setLabel(DRAW_MODE);
        }

        public void setLabel(String label) {
            System.err.println("Setting label: " + label);
            _label = label;
        }

        public String getLabel() {
            return _label;
        }

        protected void finishStroke(LayerEvent e) {
            super.finishStroke(e);
            if(!_label.equals(DRAW_MODE)) {
                SelectionModel sm = getController().getSelectionModel();
                sm.clearSelection();
                _lassoAction.actionPerformed(new ActionEvent(this, 0, "selection"));
                System.out.println("# selected: " + sm.getSelectionCount());
                if(sm.getSelectionCount() > 0) {
                    Region region = new Region();
                    region.label = _label;
                    for(Iterator i = sm.getSelection(); i.hasNext(); ) {
                        Figure f = (Figure)i.next();
                        Symbol s = (Symbol)f.getUserObject();
                        s.setOutline(java.awt.Color.gray);
                        getController().getSketchModel().updateSymbol(s);
                        mapSymbol(s, region);
                    }
                }
            }
        }
    }

    private void mapSymbol(Symbol s, Region r) {
        Region curRegion = (Region)_symbolsToRegions.get(s);
        if(curRegion != null) {
            List l = (List)_regionsToSymbols.get(curRegion);
            l.remove(s);
            if(l.size() == 0) {
                _regionsToSymbols.remove(curRegion);
            }
        }
        List l = (List)_regionsToSymbols.get(r);
        if(l == null) {
            l = new ArrayList();
            _regionsToSymbols.put(r,l);
        }
        l.add(s);
        _symbolsToRegions.put(s,r);
    }

    private Scene mapToScene(SketchModel sm) {
        Scene out = new BasicScene();
        ArrayList unknown = new ArrayList();
        HashMap symbolsToStrokeElts = new HashMap();
        for(Iterator i = sm.symbols(); i.hasNext(); ) {
            StrokeSymbol s = (StrokeSymbol)i.next();
            StrokeElement se = out.addStroke(s.getStroke());
            if(_symbolsToRegions.get(s) == null) {
                unknown.add(se);
            }
            symbolsToStrokeElts.put(s, se);
        }
        System.out.println("# unknown: " + unknown.size());
        SceneElement[] strokeArray = new SceneElement[1];
        String[] strokeName = { "stroke" };
        String[] regionNames;
        SceneElement[] regionMembers;
        SimpleData regionData = new SimpleData("region");
        for(Iterator i = _regionsToSymbols.keySet().iterator(); i.hasNext(); ) {
            Region r = (Region)i.next();
            List l = (List)_regionsToSymbols.get(r);
            regionMembers = new SceneElement[l.size()];
            SimpleData dat = new SimpleData(r.label);
            for(int j = 0; j < l.size(); j++) {
                StrokeSymbol s = (StrokeSymbol)l.get(j);
                strokeArray[0] = (StrokeElement)symbolsToStrokeElts.get(s);
                regionMembers[j] = out.addComposite(dat, 100, strokeArray, strokeName);
            }
            regionNames = new String[l.size()];
            for(int j = 0; j < regionNames.length; j++) {
                regionNames[j] = "member";
            }
            out.addComposite(regionData, 100, regionMembers, regionNames);
            System.out.println("region: (" + r.label + ", " + l.size() + ")");
        }

        regionNames = new String[unknown.size()];
        regionMembers = new SceneElement[unknown.size()];
        for(int i = 0; i < regionNames.length; i++) {
            regionNames[i] = "member";
        }
        SimpleData unknownData = new SimpleData("unknown");
        for(int i = 0; i < unknown.size(); i++) {
            strokeArray[0] = (StrokeElement)unknown.get(i);
            regionMembers[i] = out.addComposite(unknownData, 100, strokeArray, strokeName);
        }
        
        List roots = out.roots();
        regionNames = new String[roots.size()];
        for(int i = 0; i < regionNames.length; i++) {
            regionNames[i] = "member";
        }
        SceneElement[] regions = new SceneElement[roots.size()];
        roots.toArray(regions);
        out.addComposite(new SimpleData("root"), 100, regions, regionNames);
        return out;
    }
    
    private class Region {
        String label;
    }
}

