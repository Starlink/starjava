/*
 * $Id: WhiteboardView.java,v 1.33 2001/07/22 22:02:27 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;

import diva.canvas.CanvasUtilities;
import diva.canvas.Figure;
import diva.canvas.event.LayerEvent;
import diva.canvas.interactor.AbstractInteractor;
import diva.canvas.interactor.SelectionModel;
import diva.gui.AbstractView;
import diva.gui.Document;
import diva.gui.Page;
import diva.gui.BasicPage;
import diva.gui.MultipageModel;
import diva.gui.MultipageDocument;
import diva.gui.DesktopContext;
import diva.gui.toolbox.ListDataModel;
import diva.util.java2d.ShapeUtilities;
import Acme.JPM.Encoders.GifEncoder;

import diva.sketch.*;

import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.print.PageFormat;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Iterator;
import java.util.ArrayList;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.undo.UndoableEditSupport;

import java.awt.*;

/**
 * WhiteboardView is responsible for view-specific operations
 * on the document (cut, paste, print, etc.)  It also keeps
 * track of the multi-page aspect of the whiteboard documents.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.33 $
 * @rating Red
 */
public class WhiteboardView extends AbstractView implements Printable, ClipboardOwner {
    /**
     * When picking a random offset for pasting, use
     * this as a maximum value on each axis.
     */
    private static int MAX_PASTE_DIST = 100;
    
    protected JSketch _jsketch = null;
    
    /**
     * A mapping from document pages to sketch panes.
     */
    protected HashMap _sketchPanes = null;

    private WhiteboardState _whiteboardState = null;

    public WhiteboardView(MultipageDocument d, WhiteboardState state) {
        super(d);
        _sketchPanes = new HashMap();
        d.getMultipageModel().addPageListener(new LocalPageListener());
        _whiteboardState = state;
        _whiteboardState.addStateListener(new WBVStateListener());
    }

    public void close(){
        System.out.println("CLOSE");
    }

    public Iterator panes(){
        return _sketchPanes.values().iterator();
    }


    /** Return a thumbnail component for the thumbnail
     * view.
     */
    public JButton createThumbnail(Page page) {
        SketchModel model = (SketchModel)page.getModel();
        SketchPane pane = new SketchPane();
        (pane.getSketchController()).setSketchModel(model);
        JSketch jsketch = new JSketch(pane);
        jsketch.setSize(540, 720);	
        SketchThumbnail thumb = new SketchThumbnail(model, jsketch, 80, 100);
	return thumb;
    }

    /** Invert whatever was done in createThumbnail().
     */
    public void destroyThumbnail(JButton but) {
	SketchThumbnail thumb = (SketchThumbnail)but;
	thumb.cleanup();
    }

    /**
     * Invoked when a new document has been created or opened.
     */
    public JComponent getComponent() {
        if(_jsketch == null) {
            // Invoked by new and open actions.
            MultipageModel mpm = ((MultipageDocument)getDocument()).getMultipageModel();
            _jsketch = new JSketch();
            for(Iterator iter = mpm.pages(); iter.hasNext();){
                Page page = (Page)iter.next();
                SketchPane pane = createPane(page, _jsketch);
            }
            SketchPane pane = (SketchPane)_sketchPanes.get(mpm.getPage(0));
            _jsketch.setSketchPane(pane);
            if(!_whiteboardState.getMode().equals(WhiteboardState.SKETCH_MODE)){
                _whiteboardState.setMode(WhiteboardState.SKETCH_MODE);
            }
        }
        return _jsketch;
    }

    /**
     * Return a sketch model of the sketched gestures, for debugging.
     */
    public SketchModel getGestureModel() {
        JSketch jsketch = (JSketch)getComponent();
        SketchPane sketchPane = jsketch.getSketchPane();
        SketchController controller = sketchPane.getSketchController();
        CommandInterpreter cmd = (CommandInterpreter)controller.getBackgroundInterpreter();
        return cmd.getGestureModel();
    }

    /**
     * Return a sketch model of the sketched gestures, for debugging.
     */
    public void playGestureModel(SketchModel model) {
        JSketch jsketch = (JSketch)getComponent();
        SketchPane sketchPane = jsketch.getSketchPane();
        SketchController controller = sketchPane.getSketchController();
        CommandInterpreter cmd = (CommandInterpreter)controller.getBackgroundInterpreter();
        for(Iterator i = model.symbols(); i.hasNext(); ) {
            StrokeSymbol s = (StrokeSymbol)i.next();
            //FIXME            cmd.processCommand(s.getStroke());
            //            cmd.processCommand(s.getStroke(), (MultipageDocument)getDocument(), controller);
        }
    }
    
    /**
     * Called by LocalPageListener::intervalAdded
     */
    protected SketchPane createPane(Page page, JSketch jsketch) {
        //create a pane to view the model
        SketchModel model = (SketchModel)page.getModel();
        SketchPane pane = new SketchPane();
        SketchController bsc = pane.getSketchController();
        bsc.setPenColor(_whiteboardState.getPenColor());
        bsc.setLineWidth(_whiteboardState.getPenWidth());
        MultipageDocument d = (MultipageDocument)getDocument();
        bsc.setForegroundInterpreter(new SketchInterpreter(bsc));
        bsc.setBackgroundInterpreter(new CommandInterpreter(bsc, /*jsketch,*/ _whiteboardState, d));
        bsc.setSketchModel(model);
        _sketchPanes.put(page, pane);
        model.addSketchListener(new LocalSketchListener());
        return pane;
    }

    /**
     * Save the current page of the specified document to the file in
     * GIF format.
     */
    protected void saveAsGIF(java.io.File outputFile){
        MultipageDocument document = (MultipageDocument)getDocument();
        Page page = document.getMultipageModel().getCurrentPage();
        SketchPane pane = (SketchPane)_sketchPanes.get(page);
        int w = getDesktopContext().makeComponent().getWidth();
        int h = getDesktopContext().makeComponent().getHeight();
        BufferedImage image =
            (BufferedImage)getDesktopContext().makeComponent().createImage(w, h);
        Graphics2D g = (Graphics2D)image.getGraphics();
        //write to the image
        pane.paint(g);
        // write it out in the format you want
        try{
            FileOutputStream os =
                new FileOutputStream(outputFile);
            
            GifEncoder enc = new GifEncoder(image, os);
            enc.encode();
            os.close();
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
        //dispose of the graphics content
        g.dispose();
    }

    

    /** Print the document to a printer, represented by the specified
     * graphics object.  This method assumes that a view exists of the this
     * document in the application.
     *  @param graphics The context into which the page is drawn.
     *  @param format The size and orientation of the page being drawn.
     *  @param index The zero based index of the page to be drawn.
     *  @returns PAGE_EXISTS if the page is rendered successfully, or
     *   NO_SUCH_PAGE if pageIndex specifies a non-existent page.
     *  @exception PrinterException If the print job is terminated.
     */
    public int print(Graphics graphics, PageFormat format,
            int index) throws PrinterException {
        //FIXME - use index/pageable instead
        JSketch sketch = (JSketch)getComponent();
        sketch.setSize(540, 720);

        if(sketch != null) {
            return sketch.print(graphics, format, index);
        }
        else return NO_SUCH_PAGE;
    }

    private void debug(String s) {
        System.out.println(s);
    }

    /** Get the currently selected objects from this document, if any,
     * and place them on the given clipboard.
     * @param cut  Whether or not to cut the objects from the current
     *             document.
     */
    private void cutCopy (Clipboard c, boolean cut) {
        JSketch jsketch = (JSketch)getComponent();
        SketchPane sketchPane = jsketch.getSketchPane();
        SketchController controller = sketchPane.getSketchController();
        SelectionModel model = controller.getSelectionModel();
        SketchModel sketchModel = controller.getSketchModel();
        SketchModel copy = new SketchModel();
        Object selection[] = model.getSelectionAsArray();
        //        debug("cut/copy");
        for(int i = 0; i < selection.length; i++) {
            //            debug("  SELECTION COUNT = " + i);
            if(selection[i] instanceof Figure) {
                Symbol s = (Symbol)((Figure)selection[i]).getUserObject();
                copy.addSymbol(s);
                if(cut) {
                    sketchModel.removeSymbol(s);
                }
            }
        }
        if(cut && (copy.getSymbolCount()>0)){//create a cut edit
            WhiteboardEdits.CutEdit cutEdit = new WhiteboardEdits.CutEdit();
            MultipageModel mpm =
                ((MultipageDocument)getDocument()).getMultipageModel();
            for(Iterator i = copy.symbols(); i.hasNext();){
                Symbol s = (Symbol)i.next();
                WhiteboardEdits.DeleteStrokeEdit e =
                    new WhiteboardEdits.DeleteStrokeEdit(sketchModel, s, mpm);
                cutEdit.addEdit(e);
            }
            cutEdit.end();
            getDocument().getEditSupport().postEdit(cutEdit);
        }
        StringWriter buffer = new StringWriter();
        SketchWriter writer = new SketchWriter();
        try {
            writer.writeModel(copy, buffer);
            c.setContents(new StringSelection(buffer.toString()), this);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Get the currently selected objects from this document, if any,
     * and place them on the given clipboard. 
     */
    public void cut (Clipboard c) {
        cutCopy(c, true);
    }

    /** Get the currently selected objects from this document, if any,
     * and place them on the given clipboard. 
     */
    public void copy (Clipboard c) {
        cutCopy(c, false);
    }



    /** Do nothing.
     */
    public void lostOwnership(Clipboard clipboard, 
            Transferable transferable) {
    }
     
    /** Clone the objects currently on the clipboard, if any,
     * and place them in the given document.  If the document does not
     * support such an operation, then do nothing.  This method is responsible
     * for copying the data.
     */
    public void paste (Clipboard c) {
        Transferable transferable = c.getContents(this);
        JSketch jsketch = (JSketch)getComponent();
        SketchPane sketchPane = jsketch.getSketchPane();
        SketchController controller = sketchPane.getSketchController();
        SketchModel model = controller.getSketchModel();
        if(transferable == null) {
            return;
        }
        try {
            String string = (String)
                transferable.getTransferData(DataFlavor.stringFlavor);
            StringReader buffer = new StringReader(string);
            SketchParser reader = new SketchParser();
            SketchModel copy = (SketchModel)reader.parse(buffer);
            Random r = new Random(System.currentTimeMillis());
            double dx = MAX_PASTE_DIST*r.nextDouble() - MAX_PASTE_DIST/2.0;
            double dy = MAX_PASTE_DIST*r.nextDouble() - MAX_PASTE_DIST/2.0;
            WhiteboardEdits.PasteEdit pasteEdit =
                new WhiteboardEdits.PasteEdit();
            MultipageModel mpm =
                ((MultipageDocument)getDocument()).getMultipageModel();
            for(Iterator i = copy.symbols(); i.hasNext(); ) {
                StrokeSymbol s = (StrokeSymbol)i.next();
                s.getStroke().translate(dx,dy);
                model.addSymbol(s);
                i.remove();
                WhiteboardEdits.AddStrokeEdit e =
                    new WhiteboardEdits.AddStrokeEdit(model, s, mpm);
                pasteEdit.addEdit(e);
            }
            pasteEdit.end();
            getDocument().getEditSupport().postEdit(pasteEdit);
        } catch (UnsupportedFlavorException ex) {
            System.out.println("Transferable object didn't " + 
                    "support stringFlavor: " +
                    ex.getMessage());
        } catch (IOException ex) {
            System.out.println("IOException when pasting: " + 
                    ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex.getMessage());
        }
    }
     
    protected DesktopContext getDesktopContext() {
        return ((Whiteboard)getDocument().getApplication()).getDesktopContext();
    }
    
    public String getTitle() {
        return "Sketch";
    }
    
    public String getShortTitle() {
        return "Sketch";
    }

    public void groupSelected() {
        MultipageModel mpm = ((MultipageDocument)getDocument()).getMultipageModel();
        Page curPage = mpm.getCurrentPage();
        JSketch jsketch = (JSketch)getDesktopContext().getCurrentContentPane();
        SketchPane pane = (SketchPane)_sketchPanes.get(curPage);
        SketchController controller = pane.getSketchController();
        SelectionModel model = controller.getSelectionModel();
        SketchModel sketchModel = controller.getSketchModel();
        Object selection[] = model.getSelectionAsArray();
        ArrayList group = new ArrayList();
        System.out.println("GROUP ==========================");
        for(int i = 0; i < selection.length; i++) {
            if(selection[i] instanceof Figure) {
                Symbol s = (Symbol)((Figure)selection[i]).getUserObject();
                System.out.println("Selection[i].userObj = " + s);
                group.add(s);
            }
        }
        if(group.size() > 1) {
            for(Iterator i = group.iterator(); i.hasNext(); ) {
                Symbol s = (Symbol)i.next();
                System.out.println("Removing symbol: " + s);
                sketchModel.removeSymbol(s);
            }
            Symbol[] children = new Symbol[group.size()];
            group.toArray(children);
            CompositeSymbol composite = new CompositeSymbol(children);
            sketchModel.addSymbol(composite);
            //FIXME: get the figure and add it to the selection
        }
    }

    public void ungroupSelected() {
        MultipageModel mpm = ((MultipageDocument)getDocument()).getMultipageModel();
        Page curPage = mpm.getCurrentPage();
        JSketch jsketch = (JSketch)getDesktopContext().getCurrentContentPane();
        SketchPane pane = (SketchPane)_sketchPanes.get(curPage);
        SketchController controller = pane.getSketchController();
        SelectionModel model = controller.getSelectionModel();
        SketchModel sketchModel = controller.getSketchModel();
        Object selection[] = model.getSelectionAsArray();
        ArrayList allChildren = new ArrayList();
        System.out.println("UNGROUP ==========================");
        for(int i = 0; i < selection.length; i++) {
            if(selection[i] instanceof Figure) {
                Symbol s = (Symbol)((Figure)selection[i]).getUserObject();
                System.out.println("Selction[i].userObj = " + s);
                if(s instanceof CompositeSymbol) {
                    CompositeSymbol cs = (CompositeSymbol)s;
                    Symbol[] children = cs.getChildren();
                    System.out.println("Removing composite symbol: " + cs);
                    sketchModel.removeSymbol(cs);
                    for(int j = 0; j < children.length; j++) {
                        sketchModel.addSymbol(children[j]);
                    }
                }
            }
        }
    }

    public void nextPage() {
        MultipageModel mpm = ((MultipageDocument)getDocument()).getMultipageModel();
        Page page = mpm.getCurrentPage();
        int indexWanted = mpm.indexOf(page)+1;
        Page page2 = mpm.getPage(indexWanted);
        mpm.setCurrentPage(page2);
    }
    
    public void previousPage() {
        MultipageModel mpm = ((MultipageDocument)getDocument()).getMultipageModel();
        Page page = mpm.getCurrentPage();
        int indexWanted = mpm.indexOf(page)-1;
        Page page2 = mpm.getPage(indexWanted);
        mpm.setCurrentPage(page2);
    }
    
    protected class LocalSketchListener implements SketchListener {
        public void symbolAdded(SketchEvent e){
            MultipageDocument d = (MultipageDocument)getDocument();
            d.setDirty(true);
        }
        public void symbolRemoved(SketchEvent e){
            MultipageDocument d = (MultipageDocument)getDocument();
            d.setDirty(true);
        }
        public void symbolModified(SketchEvent e){
            getDocument().setDirty(true);
            //AR: should I create StrokeFormatEdit here?
        }
    }
    
    protected class LocalPageListener implements ListDataListener {
        /**
         * When a new page has been added, create a new sketch pane
         * to display the model of this page.
         */
        public void intervalAdded(ListDataEvent e) {
            ListDataModel ldm = (ListDataModel)e.getSource();
            BasicPage page = (BasicPage)ldm.getElementAt(e.getIndex0());
            JSketch jsketch =
                (JSketch)getDesktopContext().getCurrentContentPane();
            createPane(page, jsketch);
            if(!_whiteboardState.getMode().equals(WhiteboardState.SKETCH_MODE)){            
                _whiteboardState.setMode(WhiteboardState.SKETCH_MODE);
            }
        }

        public void intervalRemoved(ListDataEvent e) {
        }

        /**
         * Invoked when current page has been switched.
         */
        public void contentsChanged(ListDataEvent e) {
            MultipageModel mpm = ((MultipageDocument)getDocument()).getMultipageModel();
            Page curPage = mpm.getCurrentPage();
            JSketch jsketch = (JSketch)getDesktopContext().getCurrentContentPane();
            SketchPane pane = (SketchPane)_sketchPanes.get(curPage);
            jsketch.setSketchPane(pane);
            jsketch.repaint();
        }
    }

    protected class WBVStateListener implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e){
            String propertyName = e.getPropertyName();
            if(propertyName.equals(WhiteboardState.PEN_COLOR)){
                Color c = (Color)e.getNewValue();
                //set colors in each pane
                for(Iterator panes = panes(); panes.hasNext();){
                    SketchPane p = (SketchPane)panes.next();
                    SketchController controller = p.getSketchController();
                    controller.setPenColor(c);
                    //update currently selected symbols
                    SelectionModel sm = controller.getSelectionModel();
                    for(Iterator i = sm.getSelection(); i.hasNext();){
                        Figure f = (Figure)i.next();
                        Symbol s = (Symbol)f.getUserObject();
                        Color old = s.getOutline();
                        s.setOutline(c);
                        SketchModel skm = controller.getSketchModel();
                        skm.updateSymbol(s);
                        //create an edit
                        WhiteboardEdits.StrokeOutlineColorEdit edit =
                            new WhiteboardEdits.StrokeOutlineColorEdit(skm, s, old, c);
                        getDocument().getEditSupport().postEdit(edit);
                    }
                }
            }
            else if(propertyName.equals(WhiteboardState.FILL_COLOR)){
                Color c = (Color)e.getNewValue();
                //set colors in each pane
                for(Iterator panes = panes(); panes.hasNext();){
                    SketchPane p = (SketchPane)panes.next();
                    SketchController controller = p.getSketchController();
                    controller.setFillColor(c);
                    //controller.updateSelectedSymbols();
                    SelectionModel sm = controller.getSelectionModel();
                    for(Iterator i = sm.getSelection(); i.hasNext();){
                        Figure f = (Figure)i.next();
                        Symbol s = (Symbol)f.getUserObject();
                        Color old = s.getFill();
                        s.setFill(c);
                        SketchModel skm = controller.getSketchModel();
                        skm.updateSymbol(s);
                        //create an edit
                        WhiteboardEdits.StrokeFillColorEdit edit =
                            new WhiteboardEdits.StrokeFillColorEdit(skm, s, old, c);
                        getDocument().getEditSupport().postEdit(edit);
                    }
                }
            }
            else if(propertyName.equals(WhiteboardState.PEN_WIDTH)){
                //button state
                float w = ((Float)e.getNewValue()).floatValue();
                //set widths in each pane                
                for(Iterator panes = panes(); panes.hasNext();){
                    SketchPane p = (SketchPane)panes.next();
                    SketchController controller = p.getSketchController();
                    controller.setLineWidth(w);

                    SelectionModel sm = controller.getSelectionModel();
                    for(Iterator i = sm.getSelection(); i.hasNext();){
                        Figure f = (Figure)i.next();
                        Symbol s = (Symbol)f.getUserObject();
                        float old = s.getLineWidth();
                        s.setLineWidth(w);
                        SketchModel skm = controller.getSketchModel();
                        skm.updateSymbol(s);
                        //create an edit
                        WhiteboardEdits.StrokeWidthEdit edit =
                            new WhiteboardEdits.StrokeWidthEdit(skm, s, old, w);
                        getDocument().getEditSupport().postEdit(edit);
                    }
                }
            }
            else if(propertyName.equals(WhiteboardState.MODE)){
                System.out.println("MODE change");
                String mode = (String)e.getNewValue();
                for(Iterator panes = panes(); panes.hasNext();){
                    SketchPane p = (SketchPane)panes.next();
                    SketchController controller = p.getSketchController();
                    if(mode.equals(WhiteboardState.COMMAND_MODE)){
                        System.out.println("change to COMMAND");
                        _jsketch.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));                        
                        controller.getForegroundInterpreter().setEnabled(false);
                        controller.getBackgroundInterpreter().setEnabled(true);
                    }
                    else if(mode.equals(WhiteboardState.HIGHLIGHT_MODE)){
                        System.out.println("change to HIGHLIGHT");
                        _jsketch.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                        controller.setForegroundInterpreter(new HighlightInterpreter(controller));
                        controller.getBackgroundInterpreter().setEnabled(false);
                        controller.getSelectionInteractor().getSelectionModel().clearSelection();
                    }
                    else {
                        System.out.println("change to SKETCH");
                        _jsketch.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                        controller.setForegroundInterpreter(new SketchInterpreter(controller));
                        controller.getBackgroundInterpreter().setEnabled(false);
                        controller.getSelectionInteractor().getSelectionModel().clearSelection();
                    }
                }
            }
        }
    }

    private class SketchThumbnail extends JButton implements SketchListener {
        private JViewport _target;
        private JSketch _sketch;
        private int _minWidth;
        private int _minHeight;
	private SketchModel _model;
        public SketchThumbnail(SketchModel model, JSketch sketch, int minWidth, int minHeight) {
	    JScrollPane sp = new JScrollPane(sketch);
	    _model = model;
	    _model.addSketchListener(this);
	    _sketch = sketch;
            _target = sp.getViewport();
	    //            _target.addChangeListener(this);
            _minWidth = minWidth;
            _minHeight = minHeight;
        }
        public Dimension getMinimumSize() {
            return new Dimension(_minWidth, _minHeight);
        }
        public Dimension getMaximumSize() {
            return getMinimumSize();
        }
        public Dimension getPreferredSize() {
            return getMinimumSize();
        }
	public void setBackground(Color c) {
	    if(_sketch != null) {
		_sketch.setBackground(c);
	    }
	    super.setBackground(c);
	}
	private void cleanup() {
	    _model.removeSketchListener(this);
	}
        public void symbolAdded(SketchEvent e) {
            //            System.out.println("SKETCH EVENT");
            repaint();
            //            invalidate();
            //            validate();
        }
        public void symbolRemoved(SketchEvent e) {
            //            System.out.println("SKETCH EVENT");
            repaint();
            //            invalidate();
            //            validate();
        }            
        public void symbolModified(SketchEvent e) {
            //            System.out.println("SKETCH EVENT");
            repaint();
            //            invalidate();
            //            validate();
        }
        public void paintComponent(Graphics g) {
            if(_target != null) {
                Dimension viewSize =_target.getView().getSize();
                Rectangle viewRect = 
                    new Rectangle(0, 0, viewSize.width, viewSize.height);
                Dimension mySize = getSize();
                Rectangle myRect = new Rectangle(0, 0, mySize.width, mySize.height);
		
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
                g2d.transform(inverse);
            } else {
                Rectangle r = getBounds();	    
                g.clearRect(r.x, r.y, r.width, r.height);
            }
        }
        public void stateChanged(ChangeEvent e) {
            repaint();
        }
    }

    public class SketchInterpreter extends MultiStateInterpreter {
        boolean _singleCommand = false;
    
        public SketchInterpreter(SketchController c){
            super(c);
            addClickListener(new ModeChangeListener());
        }

        public void mousePressed(LayerEvent evt){
            if(_singleCommand){
                return;
            }
            else{
                super.mousePressed(evt);
            }
        }

        public void mouseDragged(LayerEvent evt){
            if(_singleCommand){
                return;
            }
            else{
                super.mouseDragged(evt);
            }
        }
        
        public void mouseReleased (LayerEvent evt) {
            if(_singleCommand){
                System.out.println("process single command");
                _singleCommand = false;
                //don't consume event, pass it down to background event layer
            }
            else{
                super.mouseReleased(evt);
                //create and post an edit if the stroke is not a double click
                SketchModel m = getController().getSketchModel();
                MultipageModel mpm = ((MultipageDocument)getDocument()).getMultipageModel();
                WhiteboardEdits.AddStrokeEdit edit =
                    new WhiteboardEdits.AddStrokeEdit(m, getCurrentSymbol(), mpm);
                getDocument().getEditSupport().postEdit(edit);
                if(getController().getBackgroundInterpreter().isEnabled()){
                    _jsketch.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
                    getController().getBackgroundInterpreter().setEnabled(false);
                }
                evt.consume();
            }
        }
        

        private class ModeChangeListener extends AbstractInteractor {
            public void mouseClicked(LayerEvent evt){
                System.out.println("mouseClicked!");
                if(getClickCount() == 2){
                    System.out.println("click count = 2!");
                    _singleCommand = true;
                    _jsketch.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));                        
                    //controller.getForegroundInterpreter().setEnabled(false);
                    getController().getBackgroundInterpreter().setEnabled(true);
                    evt.consume();
                }
            }
        }
    }
}

