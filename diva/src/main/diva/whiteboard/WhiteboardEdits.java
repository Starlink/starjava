/*
 * $Id: WhiteboardEdits.java,v 1.13 2001/07/22 22:02:27 johnr Exp $
 *
 * Copyright (c) 1998-2001 The Regents of the University of California.
 * All rights reserved. See the file COPYRIGHT for details.
 */
package diva.whiteboard;
import javax.swing.undo.*;
import diva.sketch.SketchListener;
import diva.sketch.SketchModel;
import diva.sketch.Symbol;
import diva.gui.MultipageModel;
import diva.gui.Page;
import java.awt.Color;
import java.util.Iterator;

/**
 * WhiteboardEdits contains the set of undoable editing operations
 * that the whiteboard application uses to support undo. <p>
 *
 * Issues:
 * <ul>
 *   <li> how to restore the editor to its previous state
 *        when we undo (e.g. to go to the right page where the
 *        edit ocurred)?
 *   </li>
 * </ul>
 *
 * @author Michael Shilman  (michaels@eecs.berkeley.edu)
 * @author Heloise Hse      (hwawen@eecs.berkeley.edu)
 * @version $Revision: 1.13 $
 * @rating Red
 */
public class WhiteboardEdits {
    /** Add a stroke to the 
     */
    public static class AddStrokeEdit extends AbstractUndoableEdit {
        private SketchModel _sketchModel;
        private Symbol _symbol;
        private MultipageModel _multipageModel;
        private Page _curPage;//the page that the symbol is on
        
        public AddStrokeEdit(SketchModel sm, Symbol s, MultipageModel mm){
            _sketchModel = sm;
            _symbol = s;
            _multipageModel = mm;
            _curPage = _multipageModel.getCurrentPage();
        }

        public void redo() throws CannotRedoException {
            _sketchModel.addSymbol(_symbol);
            if(_multipageModel.getCurrentPage() != _curPage){
                _multipageModel.setCurrentPage(_curPage);
            }
        }

        public void undo() throws CannotUndoException {
            _sketchModel.removeSymbol(_symbol);
            if(_multipageModel.getCurrentPage() != _curPage){            
                _multipageModel.setCurrentPage(_curPage);
            }
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "Add Stroke";
        }
    }

    public static class DeleteStrokeEdit extends AbstractUndoableEdit {
        private SketchModel _sketchModel;
        private Symbol _symbol;
        private MultipageModel _multipageModel;
        private Page _curPage;//the page that the symbol is on
        
        public DeleteStrokeEdit(SketchModel sm, Symbol s, MultipageModel mm){
            _sketchModel = sm;
            _symbol = s;
            _multipageModel = mm;
            _curPage = _multipageModel.getCurrentPage();
        }

        public void redo() throws CannotRedoException {
            _sketchModel.removeSymbol(_symbol);
            if(_multipageModel.getCurrentPage() != _curPage){            
                _multipageModel.setCurrentPage(_curPage);
            }
        }

        public void undo() throws CannotUndoException {
            _sketchModel.addSymbol(_symbol);
            if(_multipageModel.getCurrentPage() != _curPage){            
                _multipageModel.setCurrentPage(_curPage);
            }
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "Delete Stroke";
        }
    }

    public static class DeleteGroupedStrokeEdit extends CompoundEdit {
        
        public DeleteGroupedStrokeEdit(){}

        public String getUndoPresentationName(){
            return "Undo Delete Strokes";
        }

        public String getRedoPresentationName(){
            return "Redo Delete Strokes";
        }

        public String getPresentationName(){
            return "Delete Strokes";
        }
    }
    
    public static class CutEdit extends CompoundEdit {
        
        public CutEdit(){}

        /*
        public void redo() throws CannotRedoException {
            for(Iterator i = edits.iterator(); i.hasNext();){
                UndoableEdit edit = (UndoableEdit)i.next();
                edit.redo();
            }
        }

        public void undo() throws CannotUndoException {
            for(Iterator i = edits.iterator(); i.hasNext();){
                UndoableEdit edit = (UndoableEdit)i.next();
                edit.undo();
            }
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        */
        
        public String getUndoPresentationName(){
            return "Undo Cut";
        }

        public String getRedoPresentationName(){
            return "Redo Cut";
        }
        
        public String getPresentationName(){
            return "Cut";
        }
    }
    
    public static class PasteEdit extends CompoundEdit {

        public PasteEdit(){}

        public void redo() throws CannotRedoException {
            for(Iterator i = edits.iterator(); i.hasNext();){
                UndoableEdit edit = (UndoableEdit)i.next();
                edit.redo();
            }
        }

        public void undo() throws CannotUndoException {
            for(Iterator i = edits.iterator(); i.hasNext();){
                UndoableEdit edit = (UndoableEdit)i.next();
                edit.undo();
            }
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }

        public String getUndoPresentationName(){
            return "Undo Paste";
        }

        public String getRedoPresentationName(){
            return "Redo Paste";
        }
        
        public String getPresentationName(){
            return "Paste";
        }
    }
    
    public static class NewPageEdit extends AbstractUndoableEdit {
        private MultipageModel _model;
        private Page _page;
        /**
         * The current page before the new page was added
         */
        private Page _curPage; 
        
        public NewPageEdit(MultipageModel m, Page p, Page curPage){
            _model = m;
            _page = p;
            _curPage = curPage;
        }

        public void redo() throws CannotRedoException {
            _model.addPage(_page);
            _model.setCurrentPage(_page);
        }

        public void undo() throws CannotUndoException {
            _model.removePage(_page);
            _model.setCurrentPage(_curPage);
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "New Page";
        }
    }

    public static class ReorderPageEdit extends AbstractUndoableEdit {
        private MultipageModel _model;
        private Page _page;
        private int _srcIndex;
        private int _dstIndex;
        /**
         * The current page before the new page was added
         */
        private Page _curPage; 
        
        public ReorderPageEdit(MultipageModel model, Page page, Page curPage, int srcIndex, int dstIndex){
            _model = model;
            _page = page;
            _curPage = curPage;
            _srcIndex = srcIndex;
            _dstIndex = dstIndex;
        }

        public void redo() throws CannotRedoException {
            _model.removePage(_page);
            _model.insertPage(_page, _dstIndex);
            _model.setCurrentPage(_curPage);
        }

        public void undo() throws CannotUndoException {
            _model.removePage(_page);
            _model.insertPage(_page, _srcIndex);
            _model.setCurrentPage(_curPage);
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "Reorder Page";
        }
    }
    
    public static class DeletePageEdit extends AbstractUndoableEdit {
        private MultipageModel _model;
        private Page _page;
        private int _index;
        /**
         * The current page before the new page was added
         */
        private Page _curPage; 
        
        public DeletePageEdit(MultipageModel m, Page p, Page curPage, int index){
            _model = m;
            _page = p;
            _curPage = curPage;
            _index = index;
        }

        public void redo() throws CannotRedoException {
            _model.removePage(_page);
            _model.setCurrentPage(_curPage);
        }

        public void undo() throws CannotUndoException {
            _model.insertPage(_page, _index);
            _model.setCurrentPage(_curPage);
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "Delete Page";
        }
    }
    

    /**
     * An edit on a stroke's line width.
     */
    public static class StrokeWidthEdit extends AbstractUndoableEdit {
        SketchModel _sketchModel;
        Symbol _symbol;
        float _oldWidth;
        float _newWidth;

        public StrokeWidthEdit(SketchModel skm, Symbol s, float oldWidth, float newWidth){
            _sketchModel = skm;
            _symbol = s;
            _oldWidth = oldWidth;
            _newWidth = newWidth;
        }
        
        public void redo() throws CannotRedoException {
            _symbol.setLineWidth(_newWidth);
            _sketchModel.updateSymbol(_symbol);
        }

        public void undo() throws CannotUndoException {
            _symbol.setLineWidth(_oldWidth);
            _sketchModel.updateSymbol(_symbol);
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "Stroke Width";
        }
    }

    /**
     * An edit on a stroke's outline color.
     */
    public static class StrokeOutlineColorEdit extends AbstractUndoableEdit {
        SketchModel _sketchModel;
        Symbol _symbol;
        Color _oldColor;
        Color _newColor;
        
        public StrokeOutlineColorEdit(SketchModel skm, Symbol s, Color oldColor, Color newColor){
            _sketchModel = skm;
            _symbol = s;
            _oldColor = oldColor;
            _newColor = newColor;
            System.out.println("Outline color edit: old(" + _oldColor + "), new(" + _newColor +")");
        }
        
        public void redo() throws CannotRedoException {
            _symbol.setOutline(_newColor);
            _sketchModel.updateSymbol(_symbol);
        }

        public void undo() throws CannotUndoException {
            _symbol.setOutline(_oldColor);
            _sketchModel.updateSymbol(_symbol);            
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "Stroke Outline Color";
        }

    }

    /**
     * An edit on a stroke's fill color.
     */
    public static class StrokeFillColorEdit extends AbstractUndoableEdit {
        SketchModel _sketchModel;
        Symbol _symbol;
        Color _oldColor;
        Color _newColor;
        
        public StrokeFillColorEdit(SketchModel skm, Symbol s, Color oldColor, Color newColor){
            _sketchModel = skm;
            _symbol = s;
            _oldColor = oldColor;
            _newColor = newColor; 
        }
        
        public void redo() throws CannotRedoException {
            _symbol.setFill(_newColor);
            _sketchModel.updateSymbol(_symbol);
        }

        public void undo() throws CannotUndoException {
            _symbol.setFill(_oldColor);
            _sketchModel.updateSymbol(_symbol);            
        }

        public boolean canRedo(){
            return true;
        }

        public boolean canUndo(){
            return true;
        }
        
        public String getPresentationName(){
            return "Stroke Fill Color";
        }
    }
}

