/*
 * $Id: ThumbnailView.java,v 1.15 2001/07/22 22:02:26 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;

import diva.gui.AbstractView;
import diva.gui.Document;
import diva.gui.Page;
import diva.gui.BasicPage;
import diva.gui.MultipageModel;
import diva.gui.MultipageDocument;
import diva.gui.DesktopContext;

import diva.sketch.JSketch;
import diva.sketch.SketchPane;
import diva.sketch.SketchListener;
import diva.sketch.SketchModel;
import diva.sketch.SketchEvent;
import diva.sketch.SketchParser;
import diva.sketch.SketchWriter;
import diva.sketch.Symbol;

import diva.gui.Application;
import diva.gui.ViewAdapter;
import diva.gui.ViewEvent;
import diva.gui.toolbox.ListDataModel;
import diva.canvas.interactor.SelectionModel;
import diva.canvas.Figure;
import diva.util.java2d.PaintedPath;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.print.Printable;
import java.awt.print.Pageable;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.print.PageFormat;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.Iterator;
import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListDataEvent;
import javax.swing.BoxLayout;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.BorderFactory;
import javax.swing.border.*;
import java.awt.dnd.*;
import java.awt.datatransfer.*;

/**
 * A thumbnail view displays a list of thumbnails of the
 * pages of the current document.  It does this by listening
 * to the application to see what the current document is,
 * and by listening to the current document to see when pages
 * are added, removed, or modified, causing its widget to
 * be re-rendered.
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.15 $
 * @rating Red
 */
public class ThumbnailView {
    private WhiteboardView _currentView = null;
    private JScrollPane _scroll;
    private JPanel _thumbnail = new JPanel();
    private JCursorPanel _palette = new JCursorPanel();
    //private ThumbnailPalette _palette = new ThumbnailPalette();
    private ViewListener _viewListener = new ViewListener();
    private PageListener _pageListener = new PageListener();
    private PageTurner _pageTurner = new PageTurner();
    private Application _app = null;
    private boolean _showing = true;
    private int _curSelect = -1;
    private GridBagConstraints _constraints = new GridBagConstraints();
    private Border _border;

    /**
     * Construct a thumbnail view that operates in the given
     * MDI context.
     */
    public ThumbnailView(Application app) {
        Border inner = BorderFactory.createEtchedBorder();
        //        Border outer = BorderFactory.createEmptyBorder(5,5,5,5);
        Color c = null;
        Border outer = BorderFactory.createMatteBorder(3,6,3,6,c);
        _border = BorderFactory.createCompoundBorder(outer, inner);
        
        _scroll = new JScrollPane(_palette);//, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        _palette.setLayout(new BoxLayout(_palette, BoxLayout.Y_AXIS));
        try {
            DropTarget dt = new DropTarget();
            dt.setComponent(_palette);
            dt.addDropTargetListener(new PaletteDTListener());
        }
        catch(java.util.TooManyListenersException wow) {
        }

        //        _palette.setLayout(new GridBagLayout());
	_app = app;
	_app.addViewListener(_viewListener);
        _currentView = (WhiteboardView)app.getCurrentView();
	MultipageModel mpm = getMultipageModel();
        if(mpm != null) {
            mpm.addPageListener(_pageListener);
        }
        _thumbnail.setLayout(new BorderLayout());
        _thumbnail.add("Center", _scroll);
        JLabel trash = new JLabel("trash");
        try {
            DropTarget dt = new DropTarget();
            dt.setComponent(trash);
            dt.addDropTargetListener(new TrashDTListener());
        }
        catch(java.util.TooManyListenersException wow) {
        }
        _thumbnail.add("South", trash);
        
        _constraints.weightx = 1;
        _constraints.weighty = 1;
        _constraints.anchor = GridBagConstraints.NORTH;
        _constraints.gridwidth = GridBagConstraints.REMAINDER;
        _constraints.insets = new java.awt.Insets(4,4,4,4);
        redraw();
    }

    /** Return the component that this view manages.
     */
    public JComponent getComponent() {
        return _thumbnail;
        //return _palette;
    }

    /** Debugging output.
     */
    private void debug(String s) {
        System.out.println(s);
    }

    /**
     * Create the thumbnail icon.
     */
    private JComponent createThumbnail(final Page page) {
        JButton but = _currentView.createThumbnail(page);
        but.addActionListener(_pageTurner);
        final DragSourceListener dsl = new DragSourceListener() {
            public void dragDropEnd(DragSourceDropEvent dsde) {}
            public void dragEnter(DragSourceDragEvent dsde) {
                DragSourceContext context = dsde.getDragSourceContext();
                //intersection of the users selected action, and the
                //source and target actions
                int myaction = dsde.getDropAction();
                if( (myaction & DnDConstants.ACTION_MOVE) != 0) { 
                    context.setCursor(DragSource.DefaultMoveDrop); 
                } else {
                    context.setCursor(DragSource.DefaultMoveNoDrop); 
                }
            }
            public void dragExit(DragSourceEvent dse) {}
            public void dragOver(DragSourceDragEvent dsde) {}
            public void dropActionChanged(DragSourceDragEvent dsde) {}
        };
        final DragGestureListener dgl = new DragGestureListener() {
            public void dragGestureRecognized(DragGestureEvent e) {
                try {
                    // check to see if action is OK ...
                    Transferable transferable = new StringSelection(Integer.toString(getMultipageModel().indexOf(page)));
                    //initial cursor, transferable, dsource listener 
                    e.startDrag(DragSource.DefaultMoveNoDrop, transferable, dsl);
                }
                catch(InvalidDnDOperationException idoe) {
                    System.err.println( idoe );
                }
            }
        };
        DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
                but, DnDConstants.ACTION_MOVE, dgl);

        try {
            DropTarget dt = new DropTarget();
            dt.setComponent(but);
            dt.addDropTargetListener(new ButtonDTListener());
        }
        catch(java.util.TooManyListenersException wow) {
        }

        /*
        JPanel p = new JPanel();
        p.setPreferredSize(new Dimension(0,0));
        p.setBorder(_border);
        p.add("Center",but);
        */
        return but;
    }

    /**
     * Render the page and add it to the palette.
     */
    private void addPage(Page p) {
	JComponent comp = createThumbnail(p);
	MultipageModel mpm = getMultipageModel();
        Page currentPage = mpm.getCurrentPage();
        _curSelect = mpm.indexOf(currentPage);
	_palette.add(comp, _constraints, mpm.indexOf(p));
        _palette.invalidate();
        _palette.validate();
    }

    /**
     * Remove all of the thumbnails.
     */
    private void removeAll() {
	Component[] paletteComps = _palette.getComponents();
	_palette.removeAll();
        _palette.validate();
        _palette.repaint();
	if(_currentView != null) {
	    for(int i = 0; i < paletteComps.length; i++) {
		JButton but = (JButton)paletteComps[i];
		_currentView.destroyThumbnail(but);
	    }
	}
    }

    /**
     * Remove the page's rendering from the palette.
     *
    private void removePage(Page p) {
	MultipageModel mpm = getMultipageModel();
	JButton but = (JButton)_palette.getComponent(mpm.indexOf(p));
	_palette.remove(mpm.indexOf(p));
        _palette.validate();
	_currentView.destroyThumbnail(but);
    }
    */
    private void removePage(int pageIndex){
	JButton but = (JButton)_palette.getComponent(pageIndex);
	_palette.remove(pageIndex);
        _palette.validate();
        _palette.repaint();
	_currentView.destroyThumbnail(but);
        System.err.println("removing: " + pageIndex);
	MultipageModel mpm = getMultipageModel();
        Page currentPage = mpm.getCurrentPage();
        _curSelect = mpm.indexOf(currentPage);
    }

    /**
     * Private utility method, because we do this all over the place.
     */
    private MultipageModel getMultipageModel() {
        if(_currentView == null) {
            return null;
        }
        MultipageDocument doc =
            (MultipageDocument)_currentView.getDocument();
        if(doc != null) {
            return doc.getMultipageModel();
        }
        else {
            return null;
        }
    }

    private static final Color HIGHLIGHT_COLOR = new Color(255,220,220);
    private static final Color NORMAL_COLOR = Color.white;
    
    /**
     * Highlight the page's rendering in the palette.
     */
    private void setCurrentPage(Page p) {
        MultipageModel mpm = getMultipageModel();
        int index = mpm.indexOf(p);
        //        debug("CURRENT PAGE CHANGE");
        if(index != _curSelect) {
            //            debug("DESELECTING " + _curSelect);
            if(_curSelect >= 0) {
                ((Container)_palette.getComponent(_curSelect)).setBackground(NORMAL_COLOR);
            }
            if(index >= 0) {
                ((Container)_palette.getComponent(index)).setBackground(HIGHLIGHT_COLOR);
            }
            _curSelect = index;
        }
    }
    
    /**
     * Remove all of the pages in the palette, and then
     * redraw all of them.
     */
    private void redraw() {
	removeAll();
        if(_currentView == null) {
            return;
        }
	MultipageModel mpm = getMultipageModel();
        if(mpm != null) {
            Page currentPage = mpm.getCurrentPage();
            _curSelect = mpm.indexOf(currentPage);
            for(Iterator i = mpm.pages(); i.hasNext(); ) {
	    Page p = (Page)i.next();
	    JComponent comp = createThumbnail(p);
	    if(p == currentPage) {
		((Container)comp).setBackground(HIGHLIGHT_COLOR);
	    }
	    _palette.add(comp, _constraints);
            }
        }
        _palette.validate();
        _palette.invalidate();
    }

    /**
     * Listen for selections of different views, so that we can update
     * the document that is being displayed in the palette.
     */
    private class ViewListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            	    debug("ThumbnailView: view added");
        }
        public void intervalRemoved(ListDataEvent e) {
            	    debug("ThumbnailView: view removed");
        }	
        public void contentsChanged(ListDataEvent e) {
            debug("ThumbnailView: view changed");
            MultipageModel mpm = getMultipageModel();
            if(mpm != null) {
                mpm.removePageListener(_pageListener);
            }
	    _currentView = (WhiteboardView)_app.getCurrentView();
	    if(_currentView != null) {
                mpm = getMultipageModel(); //must reassign since view changed
                if(mpm != null) {
                    mpm.addPageListener(_pageListener);
                }
	    }
            redraw();
        }
    }
    
    /**
     * Listen for modifications of the current document, and
     * update the view accordingly by adding or removing
     * pages.
     */
    private class PageListener implements ListDataListener {
        public void intervalAdded(ListDataEvent e) {
            debug("ThumbnailView: page added, " + e.getIndex0());
            MultipageModel mpm = getMultipageModel();
            addPage(mpm.getPage(e.getIndex0()));
        }
        public void intervalRemoved(ListDataEvent e) {
            debug("ThumbnailView: page removed, " + e.getIndex0());
            //page has already been removed from the model, so
            //mpm.getPage is going to cause an ArrayOutOfBound exception
            //MultipageModel mpm = getMultipageModel();
            //removePage(mpm.getPage(e.getIndex0()));
            int pageIndex = e.getIndex0();
            removePage(pageIndex);
        }	
        public void contentsChanged(ListDataEvent e) {
            debug("ThumbnailView: current page " + e.getIndex0() +
                    ", " + e.getIndex1());
            MultipageModel mpm = getMultipageModel();
            setCurrentPage(mpm.getCurrentPage());
        }
    }

    /** Turn the page when the button is pressed.
     */
    private class PageTurner implements ActionListener {
        public void actionPerformed(ActionEvent evt) {
            MultipageModel mpm = getMultipageModel();
            Component source = (Component)evt.getSource();
            Component[] paletteComps = _palette.getComponents();
            for(int i = 0; i < paletteComps.length; i++) {
                Component comp = paletteComps[i];
                if(comp == source && i != _curSelect) {
                    mpm.setCurrentPage(mpm.getPage(i));
                    break;
                }
            }
        }
    }

    private class TrashDTListener implements DropTargetListener {
        /**
         * Accept the event if the data is a known key.
         */
        public void dragEnter(DropTargetDragEvent dtde) {
            if(dtde.isDataFlavorSupported(TEXT_FLAVOR)) {
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                _palette.clearCursor();
            }
            else {
                dtde.rejectDrag();
            }
        }

        /**
         * Do nothing.
         */
        public void dragExit(DropTargetEvent dtde) {
        }

        /**
         * Accept the event if the data is a known key.
         */
        public void dragOver(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }

        /**
         * Accept the event if the data is a known key;
         * clone the associated figure and place it in the
         * graph editor.
         */
        public void drop(DropTargetDropEvent dtde) {
            try {
                String data = (String)dtde.getTransferable().getTransferData(STRING_FLAVOR);
                System.err.println("TRASH: " + data);
                int index = Integer.parseInt(data);
                if(index < 0 ||
                        index >= getMultipageModel().getPageCount() ||
                        getMultipageModel().getPageCount() == 1) {
                    return;
                }

                MultipageModel mpm = getMultipageModel();
                Page curPage = mpm.getCurrentPage();
                Page page = mpm.getPage(index);
                mpm.removePage(page);
                WhiteboardEdits.DeletePageEdit edit =
                    new WhiteboardEdits.DeletePageEdit(mpm, page, curPage, index);
                _currentView.getDocument().getEditSupport().postEdit(edit);               
                dtde.dropComplete(true); //success!
                _palette.clearCursor();
            }
            catch(Exception ex) {
                System.err.println(ex.toString());
            }
        }
            
        /**
         * Accept the event if the data is a known key.
         */
        public void dropActionChanged(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }
    }
    

    private class ButtonDTListener implements DropTargetListener {
        /**
         * Accept the event if the data is a known key.
         */
        public void dragEnter(DropTargetDragEvent dtde) {
            if(dtde.isDataFlavorSupported(TEXT_FLAVOR)) {
                int which = -1;
                Component[] paletteComps = _palette.getComponents();
                Component source = dtde.getDropTargetContext().getComponent();
                for(int i = 0; i < paletteComps.length; i++) {
                    Component comp = paletteComps[i];
                    if(comp == source) {
                        which = i;
                        break;
                    }
                }
                if(which >= 0) {
                    _palette.setCursor(which);
                    dtde.acceptDrag(DnDConstants.ACTION_MOVE);
                    return;
                }
            }
            dtde.rejectDrag();
        }

        /**
         * Do nothing.
         */
        public void dragExit(DropTargetEvent dtde) {
        }

        /**
         * Accept the event if the data is a known key.
         */
        public void dragOver(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }

        /**
         * Accept the event if the data is a known key;
         * clone the associated figure and place it in the
         * graph editor.
         */
        public void drop(DropTargetDropEvent dtde) {
            try {
                String data = (String)dtde.getTransferable().getTransferData(STRING_FLAVOR);
                int index = Integer.parseInt(data);
                if(index < 0 ||
                        index >= getMultipageModel().getPageCount() ||
                        getMultipageModel().getPageCount() == 1) {
                    return;
                }
                MultipageModel mpm = getMultipageModel();
                Component[] paletteComps = _palette.getComponents();
                int which = -1;
                Component source = dtde.getDropTargetContext().getComponent();
                for(int i = 0; i < paletteComps.length; i++) {
                    Component comp = paletteComps[i];
                    if(comp == source) {
                        which = i;
                        break;
                    }
                }
                if(which != index) {
                    Page page = mpm.getPage(index);
                    Page curPage = mpm.getCurrentPage();
                    mpm.removePage(page);
                    // we have removed a page so we need to adjust
                    if(which > index) {
                        which = which-1; 
                    }
                    mpm.insertPage(page, which);


                    WhiteboardEdits.ReorderPageEdit edit =
                        new WhiteboardEdits.ReorderPageEdit(mpm, page, curPage, index, which);
                    _currentView.getDocument().getEditSupport().postEdit(edit);               
                    
                    _palette.clearCursor();
                    dtde.dropComplete(true); //success!
                }
            }
            catch(Exception ex) {
                System.err.println(ex.toString());
            }
        }

        /**
         * Accept the event if the data is a known key.
         */
        public void dropActionChanged(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }
    }
        
    /**
     * A drop target listener that comprehends
     * the different available keys.
     */
    private class PaletteDTListener implements DropTargetListener {
        /**
         * Accept the event if the data is a known key.
         */
        public void dragEnter(DropTargetDragEvent dtde) {
            if(dtde.isDataFlavorSupported(TEXT_FLAVOR)) {
                _palette.setCursor(_palette.getComponentCount());
                dtde.acceptDrag(DnDConstants.ACTION_MOVE);
            }
            else {
                dtde.rejectDrag();
            }
        }

        /**
         * Do nothing.
         */
        public void dragExit(DropTargetEvent dtde) {
        }

        /**
         * Accept the event if the data is a known key.
         */
        public void dragOver(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }

        /**
         * Accept the event if the data is a known key;
         * clone the associated figure and place it in the
         * graph editor.
         */
        public void drop(DropTargetDropEvent dtde) {
            try {
                String data = (String)dtde.getTransferable().getTransferData(STRING_FLAVOR);
                System.err.println("MOVE1: " + data);
                int index = Integer.parseInt(data);
                if(index < 0 ||
                        index >= getMultipageModel().getPageCount() ||
                        getMultipageModel().getPageCount() == 1) {
                    return;
                }
                
                MultipageModel mpm = getMultipageModel();
                Page p = mpm.getPage(index);
                mpm.removePage(p);
                mpm.addPage(p);
                _palette.clearCursor();
                dtde.dropComplete(true); //success!
            }
            catch(Exception ex) {
                System.err.println(ex.toString());
            }
            
            /*
            Iterator iterator = null;
            if(dtde.isDataFlavorSupported(PtolemyTransferable.namedObjFlavor)) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    iterator = (Iterator)dtde.getTransferable().
                        getTransferData(PtolemyTransferable.namedObjFlavor);
                } catch(Exception e) {
                    ExceptionHandler.show(
                        "Couldn't find a supported data flavor in " + dtde, e);
                }
            } else {
                dtde.rejectDrop();
            }

            if(iterator == null) {
                // Nothing to drop!
                return;
            }

            final Point point = dtde.getLocation();
            final GraphController controller =
                ((JGraph)getComponent()).getGraphPane().getGraphController();
            GraphModel model = controller.getGraphModel();
            final CompositeEntity container = (CompositeEntity)model.getRoot();
            while(iterator.hasNext()) {
                NamedObj data = (NamedObj) iterator.next();
                // FIXME: Might consider giving a simpler name and then
                // displaying the classname in the icon.
                final String name = container.uniqueName(data.getName());
                String moml = data.exportMoML(name);
               
                ChangeRequest request = new MoMLChangeRequest(this,
                        container, moml) {
                    protected void _execute() throws Exception {
                        super._execute();
                        // Set the location of the icon.
                        // Note that this really needs to be done after
                        // the change request has succeeded, which is why
                        // it is done here.  When the graph controller
                        // gets around to handling this, it will draw 
                        // the icon at this location.

                        // FIXME: Have to know whether this is an entity,
                        // port, etc. For now, assuming it is an entity.
                        NamedObj newObject = container.getEntity(name);
                        Icon icon = (Icon) newObject.getAttribute("_icon");
                        // If there is no icon, then manufacture one.
                        if(icon == null) {
                            icon = new EditorIcon(newObject, "_icon");
                        }
                      
                        double location[] = new double[2];
                        location[0] = ((int)point.x);
                        location[1] = ((int)point.y);
                        icon.setLocation(location);
                    }
                };

                container.requestChange(request);
            }
            */
        }

        /**
         * Accept the event if the data is a known key.
         */
        public void dropActionChanged(DropTargetDragEvent dtde) {
            dragEnter(dtde); //for now
        }
    }

    /** Draw a cursor in the panel for drag-n-drop so the
     * user knows where the drop will occur.
     */
    private static class JCursorPanel extends JPanel {
        /** Draw no cursor (common case).
         */
        public static final int NO_CURSOR = -1;

        private int _cursor = NO_CURSOR;
        
        /** Set the cursor to NO_CURSOR and repaint.
         */
        public void clearCursor() {
            setCursor(NO_CURSOR);
        }

        /** Set the cursor to the given position.  The
         * cursor will be drawn before the i'th child.
         */
        public void setCursor(int cursor) {
            _cursor = cursor;
            repaint();
        }

        /** Paint the cursor and then paint the children.
         */
        public void paint(Graphics g) {
            super.paint(g);
            if(_cursor >= 0) {
                Graphics2D g2d = (Graphics2D)g;
                int y = 0;
                Component[] comps = getComponents();
                if(_cursor < getComponentCount()) {
                    Component comp = comps[_cursor];
                    y = comp.getY();
                }
                else {
                    Component comp = comps[comps.length-1];
                    y = comp.getY()+comp.getHeight();
                }
                g2d.setColor(Color.red);
                g2d.setStroke(PaintedPath.getStroke(4.0f));
                g2d.drawLine(0,y,getWidth(),y);
            }
        }
    }

    /**
     * The plain-text flavor that we will be using for our
     * basic drag-and-drop protocol.
     */
    public static final DataFlavor TEXT_FLAVOR = DataFlavor.plainTextFlavor;
    public static final DataFlavor STRING_FLAVOR = DataFlavor.stringFlavor;
}

