package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.util.gui.WeakTableColumnModelListener;

/**
 * ComboBoxModel for holding table per-row expressions.
 * These may represent either actual columns or JEL expressions 
 * evaluated against columns.
 * All members of the model are {@link uk.ac.starlink.table.ColumnData}
 * objects.
 * 
 * <p>The {@link #createComboBox} method provides a JComboBox which is a
 * suitable host for instances of this class.
 *
 * @author   Mark Taylor
 * @since    23 Jan 2005
 */
public class ColumnDataComboBoxModel
        extends AbstractListModel
        implements TableColumnModelListener, ComboBoxModel {

    private final TopcatModel tcModel_;
    private final TableColumnModel colModel_;
    private final boolean hasNone_;
    private List activeColumns_;
    private List modelColumns_;
    private ColumnData selected_;

    /**
     * Constructor.
     *
     * @param   tcModel   table model containing columns
     * @param   hasNone   true iff you want a null entry in the selector model
     */
    public ColumnDataComboBoxModel( TopcatModel tcModel, boolean hasNone ) {
        tcModel_ = tcModel;
        colModel_ = tcModel.getColumnModel();
        hasNone_ = hasNone;

        /* Listen to the table's column model so that we can update the
         * contents of this model.  Do it using a weak reference so that
         * the listener won't prevent this model from being garbage
         * collected. */
        colModel_.addColumnModelListener(
            new WeakTableColumnModelListener( this ) );

        /* Set up a list of all the columns in the column model, and all the
         * ones we're using for this model. */
        activeColumns_ = new ArrayList();
        modelColumns_ = new ArrayList();
        if ( hasNone_ ) {
            activeColumns_.add( null );
        }
        for ( int i = 0; i < colModel_.getColumnCount(); i++ ) {
            ColumnData cdata = new SelectedColumnData( tcModel_, i );
            modelColumns_.add( cdata );
            if ( acceptType( cdata.getColumnInfo().getContentClass() ) ) {
                activeColumns_.add( cdata );
            }
        }
    }

    /**
     * Determines whether a given class is acceptable for the content of
     * the expressions contained in this model 
     * (<code>DataColumn.getColumnInfo().getContentClass()</code>).
     * This method can be overridden to change the classes which are 
     * permitted, however be careful since this method is called in
     * the constructor.
     *
     * <p>The default implementation OKs Number or any of its subclasses.
     *
     * @param   clazz  class for possible inclusion
     * @return  true iff clazz is OK as a data column type for this model
     */
    public boolean acceptType( Class clazz ) {
        return Number.class.isAssignableFrom( clazz );
    }

    public Object getElementAt( int index ) {
        return activeColumns_.get( index );
    }

    public int getSize() {
        return activeColumns_.size();
    }

    public Object getSelectedItem() {
        return selected_;
    }

    public void setSelectedItem( Object item ) {
        if ( item == null ? selected_ != null 
                          : ! item.equals( selected_ ) ) {
            selected_ = (ColumnData) item;

            /* This bit of magic is copied from the J2SE1.4
             * DefaultComboBoxModel implementation - seems to be necessary
             * to send the right events, but not otherwise documented. */
            fireContentsChanged( this, -1, -1 );
        }
    }

    /*
     * Implementation of the TableColumnModelListener interface.
     * These methods watch for changes in the TableColumnModel and 
     * adjust this model's state accordingly.
     */

    public void columnAdded( TableColumnModelEvent evt ) {
        int index = evt.getToIndex();
        ColumnData cdata = new SelectedColumnData( tcModel_, index );
        modelColumns_.add( cdata );
        if ( acceptType( cdata.getColumnInfo().getContentClass() ) ) {
            int pos = activeColumns_.size();
            activeColumns_.add( cdata );
            fireIntervalAdded( this, pos, pos );
        }
    }

    public void columnRemoved( TableColumnModelEvent evt ) {
        int index = evt.getFromIndex();
        ColumnData cdata = (ColumnData) modelColumns_.get( index );
        modelColumns_.remove( cdata );
        int pos = activeColumns_.indexOf( cdata );
        if ( pos >= 0 ) {
            activeColumns_.remove( pos );
            fireIntervalRemoved( this, pos, pos );
        }
    }

    public void columnMoved( TableColumnModelEvent evt ) {
        int from = evt.getFromIndex();
        if ( activeColumns_.contains( modelColumns_.get( from ) ) ) {
            List oldActive = activeColumns_;
            activeColumns_ = new ArrayList();
            modelColumns_ = new ArrayList();
            if ( hasNone_ ) {
                activeColumns_.add( null );
            }
            for ( int i = 0; i < colModel_.getColumnCount(); i++ ) {
                ColumnData cdata = new SelectedColumnData( tcModel_, i );
                modelColumns_.add( cdata );
                if ( oldActive.contains( cdata ) ) {
                    activeColumns_.add( cdata );
                }
            }
            int index0 = hasNone_ ? 1 : 0;
            int index1 = activeColumns_.size() - 1;
            fireContentsChanged( this, index0, index1 );
        }
    }

    public void columnMarginChanged( ChangeEvent evt ) {}

    public void columnSelectionChanged( ListSelectionEvent evt ) {}

    /**
     * Constructs and returns a JComboBox suitable for use with
     * a <code>ColumnDataComboBoxModel</code>.  It installs 
     * (and deinstalls as appropriate)
     * {@link javax.swing.ComboBoxEditor}s which allow for 
     * textual expressions to be interpreted as JEL expressions based
     * on the TopcatModel on which this model is based.
     * This facility is only available/useful in the case that the 
     * combo box is editable; so the returned combo box is editable.
     * Currently no default renderer is required or installed.
     *
     * @return   new custom combo box
     */
    public static JComboBox createComboBox() {
        JComboBox comboBox = new JComboBox() {
            public void setModel( ComboBoxModel model ) {
                super.setModel( model );
                if ( model instanceof ColumnDataComboBoxModel ) {
                    ColumnDataComboBoxModel emodel =
                        (ColumnDataComboBoxModel) model;
                    setEditor( new ColumnDataEditor( emodel, this ) );
                }
            }
        };
        comboBox.setEditable( true );
        return comboBox;
    }

    /**
     * ComboBoxEditor implementation suitable for use with a 
     * ColumnDataComboBoxModel.
     */
    private static class ColumnDataEditor extends BasicComboBoxEditor {

        private final ColumnDataComboBoxModel model_;
        private final Component parent_;
        private final ComboBoxEditor base_;
        private ColumnData data_;

        /**
         * Constructor.
         *
         * @param  model   model which this editor can work with
         */
        public ColumnDataEditor( ColumnDataComboBoxModel model,
                                 Component parent ) {
            model_ = model;
            parent_ = parent;
            base_ = new JComboBox().getEditor();
        }

        public void setItem( Object obj ) {
            base_.setItem( obj == null ? null : obj.toString() );
            data_ = (ColumnData) obj;
        }

        public Object getItem() {
            String txt = (String) base_.getItem();

            /* No text - no selection. */
            if ( txt == null || txt.trim().length() == 0 ) {
                return null;
            }

            /* If the text matches the stringified version of the last known
             * selection, return it unchanged. */
            else if ( data_ != null && txt.equals( data_.toString() ) ) {
                return data_;
            }

            /* Otherwise, go looking for the column name in the contents
             * of this selection model. */
            else {
                int ncol = model_.getSize();
                for ( int i = 0; i < ncol; i++ ) {
                    ColumnData item = (ColumnData) model_.getElementAt( i );
                    if ( item != null && 
                         txt.equalsIgnoreCase( item.toString() ) ) {
                        return item;
                    }
                }
            }

            /* If none of the above have worked, then try to interpret 
             * the text as a JEL (synthetic column) expression. */
            String msg;
            try {
                ColumnData cdata =
                    new SyntheticColumnData( model_.tcModel_, txt );
                Class clazz = cdata.getColumnInfo().getContentClass();
                if ( model_.acceptType( clazz ) ) {
                    return cdata;
                }
                else {
                    msg = "Expression has wrong type: " + clazz.getName();
                }
            }
            catch ( CompilationException e ) {
                msg = e.toString();
            }

            /* No luck - inform user and return null. */
            base_.setItem( null );
            JOptionPane.showMessageDialog( parent_, msg, "Evaluation error",
                                           JOptionPane.ERROR_MESSAGE );
            return null;
        }

        public Component getEditorComponent() {
            return base_.getEditorComponent();
        }

        public void selectAll() {
            base_.selectAll();
        }

        public void removeActionListener( ActionListener listener ) {
            base_.removeActionListener( listener );
        }

        public void addActionListener( ActionListener listener ) {
            base_.addActionListener( listener );
        }
    }

    /**
     * ColumnData implementation for a column defined by a JEL expression.
     * This just extends SyntheticColumn so that it can provide sensible
     * equals() and toString() methods.
     */
    private static class SyntheticColumnData extends SyntheticColumn {

        private final TopcatModel tcModel_;
        private String expr_;

        /**
         * Constructor.
         *
         * @param  tcModel  topcat model against which to evaluate expression
         * @param  expr   expression for value
         * @throws   CompilationException  if expr can't be compiled
         */
        SyntheticColumnData( TopcatModel tcModel, String expr )
                throws CompilationException {
            super( new DefaultValueInfo( expr ), tcModel.getDataModel(),
                   tcModel.getSubsets(), expr, null );
            tcModel_ = tcModel;
            expr_ = expr;
        }

        public String toString() {
            return expr_;
        }

        public boolean equals( Object o ) {
            if ( o instanceof SyntheticColumnData ) {
                SyntheticColumnData other = (SyntheticColumnData) o;
                return other.tcModel_ == this.tcModel_
                    && other.expr_.equals( this.expr_ );
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = tcModel_.hashCode();
            code = code * 23 + expr_.hashCode();
            return code;
        }
    }

    /**
     * ColumnData implementation for a column out of a table.
     * Provides sensible equals() and toString() methods.
     */
    private static class SelectedColumnData extends ColumnData {

        private final TopcatModel tcModel_;
        private final int icol_;
        private final StarTable dataModel_;

        /**
         * Constructor.
         *
         * @param   tcModel   topcat model that the column is from
         * @param   icol  index into tcModel's columnModel of the 
         *          column to represent
         */
        SelectedColumnData( TopcatModel tcModel, int icol ) {
            super( ((StarTableColumn) tcModel.getColumnModel()
                                     .getColumn( icol )).getColumnInfo() );
            tcModel_ = tcModel;
            icol_ = tcModel.getColumnModel().getColumn( icol ).getModelIndex();
            dataModel_ = tcModel_.getDataModel();
        }

        public Object readValue( long irow ) throws IOException {
            return dataModel_.getCell( irow, icol_ );
        }

        public String toString() {
            return getColumnInfo().getName();
        }

        public boolean equals( Object o ) {
            if ( o instanceof SelectedColumnData ) {
                SelectedColumnData other = (SelectedColumnData) o;
                return other.icol_ == this.icol_
                    && other.tcModel_ == this.tcModel_;
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = icol_;
            code = code * 23 + tcModel_.hashCode();
            return code;
        }
    }
}
