package uk.ac.starlink.vo;

import javax.swing.event.UndoableEditEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.undo.CompoundEdit;

/**
 * Document implementation that works around a misfeature of PlainDocument.
 *
 * <p>In PlainDocument, a replace call (triggered by e.g. JTextArea.setText())
 * is logged as two Undoable edits: one that deletes all the old text
 * and another that adds all the new text.
 * That means that a user action implemented using setText ends up
 * requiring two user Undo actions, which doesn't match user expectations.
 * This PlainDocument subclass makes sure that such replace calls
 * aggregate their corresponding edits into a single one, so only a single
 * user Undo action is required to revert, giving a better user experience.
 *
 * <p>This code was copied from
 * <a href="https://stackoverflow.com/questions/24433089">StackOverflow</a>.
 *
 * @author   <a href="https://stackoverflow.com/users/177145/aterai">aterai</a>
 * @author   Mark Taylor
 */
class CustomReplacePlainDocument extends PlainDocument {

    private CompoundEdit compoundEdit_;

    @Override
    protected void fireUndoableEditUpdate( UndoableEditEvent evt ) {
        if ( compoundEdit_ == null ) {
            super.fireUndoableEditUpdate( evt );
        }
        else {
            compoundEdit_.addEdit( evt.getEdit() );
        }
    }

    @Override
    public void replace( int offset, int length, String txt, AttributeSet atts )
            throws BadLocationException {
        if ( length == 0 ) {
            super.replace( offset, length, txt, atts );
        }
        else {
            compoundEdit_ = new CompoundEdit();
            super.fireUndoableEditUpdate(
                      new UndoableEditEvent( this, compoundEdit_ ) );
            super.replace( offset, length, txt, atts );
            compoundEdit_.end();
            compoundEdit_ = null;
        }
    }
}
