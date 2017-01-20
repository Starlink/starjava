package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
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
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.ttools.convert.ValueConverter;
import uk.ac.starlink.ttools.jel.CustomCompilationException;
import uk.ac.starlink.util.gui.WeakTableColumnModelListener;

/**
 * ComboBoxModel for holding table per-row expressions.
 * These may represent either actual columns or JEL expressions 
 * evaluated against columns.
 * Elements of the model which contain usable data are instances of
 * {@link uk.ac.starlink.table.ColumnData}.
 * The selected item may be of some other type (currently String),
 * and this should be ignored (treated as null) for the purposes
 * of data access.
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
    private final Filter filter_;
    private final TableColumnModel colModel_;
    private final boolean hasNone_;
    private final boolean hasIndex_;
    private List activeColumns_;
    private List modelColumns_;
    private Object selected_;

    private static final Logger logger =
        Logger.getLogger( "uk.ac.starlink.topcat" );
    private static final ValueInfo INDEX_INFO =
        new DefaultValueInfo( "index", Long.class, "Row index" );

    /**
     * Constructs a model with a specified column metadata filter.
     *
     * @param   tcModel   table model containing columns
     * @param   filter    determines which columns are permitted
     * @param   hasNone   true iff you want a null entry in the selector model
     * @param   hasIndex  true iff you want an index column entry in the
     *                    selector model
     */
    public ColumnDataComboBoxModel( TopcatModel tcModel, Filter filter,
                                    boolean hasNone, boolean hasIndex ) {
        tcModel_ = tcModel;
        filter_ = filter;
        colModel_ = tcModel.getColumnModel();
        hasNone_ = hasNone;
        hasIndex_ = hasIndex;

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
        if ( hasIndex_ ) {
            activeColumns_.add( new IndexColumnData( tcModel ) );
        }
        for ( int i = 0; i < colModel_.getColumnCount(); i++ ) {
            StarTableColumn tcol = (StarTableColumn) colModel_.getColumn( i );
            SelectedColumnData cdata = getColumnData( tcModel, tcol );
            modelColumns_.add( cdata );
            if ( filter.acceptColumn( cdata.getColumnInfo() ) ) {
                activeColumns_.add( cdata );
            }
        }
    }

    /**
     * Constructs a model for a given content class, optionally with
     * a blank entry and an entry for the magic 'index' column.
     *
     * @param   tcModel   table model containing columns
     * @param   dataClazz content class of permitted columns
     * @param   hasNone   true iff you want a null entry in the selector model
     * @param   hasIndex  true iff you want an index column entry in the
     *                    selector model
     */
    public ColumnDataComboBoxModel( TopcatModel tcModel, final Class dataClazz,
                                    boolean hasNone, boolean hasIndex ) {
        this( tcModel,
              new Filter() {
                   public boolean acceptColumn( ValueInfo info ) {
                       return dataClazz
                      .isAssignableFrom( info.getContentClass() );
                   }
               },
               hasNone, hasIndex );
    }

    /**
     * Constructs a model for a given content class,
     * optionally with a blank entry.
     *
     * @param   tcModel   table model containing columns
     * @param   hasNone   true iff you want a null entry in the selector model
     */
    public ColumnDataComboBoxModel( TopcatModel tcModel, Class dataClazz,
                                    boolean hasNone ) {
        this( tcModel, dataClazz, hasNone, false );
    }

    public Object getElementAt( int index ) {
        return activeColumns_.get( index );
    }

    /**
     * Returns the element at a given index as a typed object.
     *
     * @param   index   requested index
     * @return   value at index as a ColumnData, or null
     */
    public ColumnData getColumnDataAt( int index ) {
        Object el = getElementAt( index );
        return el instanceof ColumnData ? (ColumnData) el : null;
    }

    public int getSize() {
        return activeColumns_.size();
    }

    public Object getSelectedItem() {
        return selected_;
    }

    /**
     * Converts a string value to a ColumnData value suitable for selection
     * by this model.  If it cannot be done, a CompilationException is thrown.
     *
     * @param   txt  string expression (or column name) for data
     * @return  corresponding ColumnData object
     * @throws  CompilationException  if <code>txt</code> is not valid
     */
    public ColumnData stringToColumnData( final String txt )
            throws CompilationException {

        /* No text, no column data. */
        if ( txt == null || txt.trim().length() == 0 ) {
            return null;
        }

        /* See if the string is a column name.  Try case-sensitive first,
         * then case-insensitive. */
        int ncol = getSize();
        for ( int i = 0; i < ncol; i++ ) {
            ColumnData item = getColumnDataAt( i );
            if ( item != null && txt.equals( item.toString() ) ) {
                return item;
            }
        }
        for ( int i = 0; i < ncol; i++ ) {
            ColumnData item = getColumnDataAt( i );
            if ( item != null && txt.equalsIgnoreCase( item.toString() ) ) {
                return item;
            }
        }

        /* Otherwise, try to interpret the string as a JEL expression. */
        ColumnData cdata = new SyntheticColumnData( tcModel_, txt );
        Class clazz = cdata.getColumnInfo().getContentClass();
        if ( filter_.acceptColumn( cdata.getColumnInfo() ) ) {
            return cdata;
        }
        else {
            throw new CustomCompilationException( "Wrong data type for \""
                                                + txt + "\"" );
        }
    }

    public void setSelectedItem( Object item ) {
        if ( item == null ? selected_ != null 
                          : ! item.equals( selected_ ) ) {
            selected_ = item;

            /* This bit of magic is copied from the J2SE1.4
             * DefaultComboBoxModel implementation - seems to be necessary
             * to send the right events, but not otherwise documented. */
            fireContentsChanged( this, -1, -1 );
        }
    }

    /**
     * Attempts to locate and return a member of this model which matches
     * the given <code>info</code>.  Exactly how the matching is done is
     * not defined - presumably grubbing about with UCDs or column names etc.
     *
     * @param  info  metadata item to match
     * @return   object suitable for selection in this model which matches
     *           <code>info</code>, or null if nothing suitable can be found
     */
    public ColumnData getBestMatchColumnData( ValueInfo info ) {
        int bestScore = 0;
        ColumnData bestData = null;
        for ( int i = 0; i < getSize(); i++ ) {
            ColumnData cdata = getColumnDataAt( i );
            if ( cdata != null ) {
                int score = match( info, cdata.getColumnInfo() );
                if ( score > bestScore ) {
                    bestScore = score;
                    bestData = cdata;
                }
            }
        }
        return bestData;
    }

    /**
     * Attempts to determine whether two ValueInfo objects appear to be
     * referring to the same physical quantity.  The higher the returned
     * value, the better the match.  Zero is returned for no discernible
     * match at all.
     *
     * @param   targetInfo   metadata object we want to be like
     * @param   testInfo     metadata object to assess
     * @return  integer indicating match quality
     */
    private int match( ValueInfo targetInfo, ValueInfo testInfo ) {
        int score = 0;

        String targetName = targetInfo.getName();
        String testName = testInfo.getName();
        if ( targetName != null && testName != null ) {
            targetName = targetName.toLowerCase();
            testName = testName.toLowerCase();
            if ( testName.equals( targetName ) ) {
                score += 5;
            }
            else if ( testName.startsWith( targetName ) ) {
                score += 2;
            }
        }

        String targetUcd = targetInfo.getUCD();
        String testUcd = testInfo.getUCD();
        if ( targetUcd != null && testUcd != null ) {
            targetUcd = targetUcd.replace( '_', '.' ).toLowerCase();
            testUcd = testUcd.replace( '_', '.' ).toLowerCase();
            if ( testUcd.equals( targetUcd ) ) {
                score += 100;
            }
            else {
                String[] targetWords = targetUcd.split( "\\." );
                String[] testWords = testUcd.split( "\\." );
                int nword = Math.min( targetWords.length, testWords.length );
                for ( int i = 0; i < nword; i++ ) {
                    if ( targetWords[ i ].equals( testWords[ i ] ) ) {
                        score += 10;
                    }
                }
            }
        }
        return score;
    }

    /**
     * Attempts to locate and return a member of this model which
     * is the only match for a given <code>info</code>.
     * If no good match can be found, or if multiple equally good matches
     * are found, null is returned.
     * Exactly how the matching is done is
     * not defined - presumably grubbing about with UCDs or column names etc.
     *
     * @param  info  metadata item to match
     * @return   object suitable for selection in this model which matches
     *           <code>info</code>, or null if nothing suitable can be found
     */
    public ColumnData getUniqueMatchColumnData( ValueInfo info ) {
        int nc = getSize();
        ColumnInfo[] infos0 = new ColumnInfo[ nc ];
        for ( int i = 0; i < nc; i++ ) {
            ColumnData cdata = getColumnDataAt( i );
            ColumnInfo info0 = cdata == null ? null : cdata.getColumnInfo();
            infos0[ i ] = info0 == null ? new ColumnInfo( (String) null )
                                        : info0;
        }

        /* Try to find unique matched name. */
        String name1 = info.getName();
        if ( name1 != null ) {
            int imatch = 0;
            int nmatch = 0;
            for ( int i = 0; i < nc; i++ ) {
                if ( name1.equalsIgnoreCase( infos0[ i ].getName() ) ) {
                    imatch = i;
                    nmatch++;
                }
            }
            if ( nmatch == 1 ) {
                return getColumnDataAt( imatch );
            }
        }

        /* Try to find unique matched UCD. */
        String ucd1 = info.getUCD();
        if ( ucd1 != null ) {
            int imatch = 0;
            int nmatch = 0;
            for ( int i = 0; i < nc; i++ ) {
                if ( ucd1.equalsIgnoreCase( infos0[ i ].getUCD() ) ) {
                    imatch = i;
                    nmatch++;
                }
            }
            if ( nmatch == 1 ) {
                return getColumnDataAt( imatch );
            }
        }

        /* Try to find unique matched Utype. */
        String utype1 = info.getUtype();
        if ( utype1 != null ) {
            int imatch = 0;
            int nmatch = 0;
            for ( int i = 0; i < nc; i++ ) {
                if ( ucd1.equalsIgnoreCase( infos0[ i ].getUtype() ) ) {
                    imatch = i;
                    nmatch++;
                }
            }
            if ( nmatch == 1 ) {
                return getColumnDataAt( imatch );
            }
        }

        /* No luck. */
        return null;
    }

    /*
     * Implementation of the TableColumnModelListener interface.
     * These methods watch for changes in the TableColumnModel and 
     * adjust this model's state accordingly.
     */

    public void columnAdded( TableColumnModelEvent evt ) {
        int index = evt.getToIndex();
        StarTableColumn tcol = (StarTableColumn) colModel_.getColumn( index );
        ColumnData cdata = getColumnData( tcModel_, tcol );
        modelColumns_.add( cdata );
        if ( filter_.acceptColumn( cdata.getColumnInfo() ) ) {
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
            if ( hasIndex_ ) {
                activeColumns_.add( new IndexColumnData( tcModel_ ) );
            }
            for ( int i = 0; i < colModel_.getColumnCount(); i++ ) {
                StarTableColumn tcol =
                    (StarTableColumn) colModel_.getColumn( i );
                SelectedColumnData cdata = getColumnData( tcModel_, tcol );
                modelColumns_.add( cdata );
                if ( oldActive.contains( cdata ) ) {
                    activeColumns_.add( cdata );
                }
            }
            int index0 = 0;
            if ( hasNone_ ) {
                index0++;
            }
            if ( hasIndex_ ) {
                index0++;
            }
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
     * Creates a ColumnData object simply representing a single column
     * of a table.
     * Behaviour is undefined if tcol is not associated with the model.
     *
     * @param  tcModel  topcat model
     * @param   tcol   column in model
     * @return   column data object
     */
    public static ColumnData createSimpleColumnData( TopcatModel tcModel,
                                                     StarTableColumn tcol ) {
        return new SelectedColumnData( tcModel, tcol );
    }

    /**
     * Returns a ColumnData associated with a given column of a table.
     *
     * @param   tcModel   topcat model that the column is from
     * @param   tcol   column in tcModel
     * @return  column data for column <code>icol</code>
     */
    private static SelectedColumnData getColumnData( TopcatModel tcModel, 
                                                     StarTableColumn tcol ) {
        ColumnInfo info = tcol.getColumnInfo();
        if ( Number.class.isAssignableFrom( info.getContentClass() ) ) {
            return new SelectedColumnData( tcModel, tcol );
        }
        else {
            ValueConverter conv = (ValueConverter)
                info.getAuxDatumValue( TopcatUtils.NUMERIC_CONVERTER_INFO,
                                       ValueConverter.class );
            if ( conv != null ) {
                return new ConvertedColumnData( tcModel, tcol, conv );
            }
            else {
                return new SelectedColumnData( tcModel, tcol );
            }
        }
    }

    /**
     * Determines what columns are acceptable for this model.
     */
    public interface Filter {

        /**
         * Indicates whether a given data type is suitable to be offered
         * as an option for this model.
         *
         * @param   info   column metadata
         * @return  true iff info describes OK data content
         *          acceptable for this model
         */
        boolean acceptColumn( ValueInfo info );
    }

    /**
     * ComboBoxEditor implementation suitable for use with a 
     * ColumnDataComboBoxModel.
     */
    private static class ColumnDataEditor extends BasicComboBoxEditor {

        private final ColumnDataComboBoxModel model_;
        private final Component parent_;
        private final ComboBoxEditor base_;
        private final Color okColor_;
        private final Color errColor_;
        private String text_;
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
            Component editor = base_.getEditorComponent();
            okColor_ = editor.getForeground();
            errColor_ = Color.GRAY;
        }

        public void setItem( Object obj ) {
            base_.setItem( toStringOrData( obj ) );
        }

        public Object getItem() {
            return toStringOrData( base_.getItem() );
        }

        /**
         * Configures the editor component to give a visual indication
         * of whether a legal column data object is currently visible.
         *
         * @param  isOK  true for usable data, false for not
         */
        private void setOk( boolean isOk ) {
            base_.getEditorComponent()
                 .setForeground( isOk ? okColor_ : errColor_ );
        }

        /**
         * Takes an object which is a possible content of this editor and
         * returns a corresponding ColumnData object if possible,
         * otherwise a string.
         * This method informs the user via a popup if a string cannot
         * be converted to data.  It caches values so that it does not
         * keep showing the same popup for the same string.
         *
         * @param  item   input object
         * @return   output object, should be of type String or
         *           (preferably) ColumnData
         */
        private Object toStringOrData( Object item ) {
            if ( item instanceof ColumnData ) {
                text_ = null;
                data_ = (ColumnData) item;
            }
            else if ( item instanceof String ) {
                String txt = (String) item;
                if ( ! txt.equals( text_ ) ) {
                    ColumnData colData;
                    CompilationException err;
                    try {
                        colData = model_.stringToColumnData( txt );
                        err = null;
                    }
                    catch ( CompilationException e ) {
                        colData = null;
                        err = e;
                    }
                    text_ = txt;
                    data_ = colData;
                    if ( err != null ) {
                        setOk( false );
                        JOptionPane
                       .showMessageDialog( parent_, err.getMessage(),
                                           "Evaluation Error",
                                           JOptionPane.ERROR_MESSAGE );
                    }
                }
            }
            else if ( item == null ) {
                data_ = null;
                text_ = null;
            }
            setOk( data_ != null || text_ == null
                                 || text_.trim().length() == 0 );
            return data_ != null ? data_ : text_;
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
            super( new DefaultValueInfo( expr ), expr, null,
                   tcModel.createJELRowReader() );
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
         * @param   tcol  table column in tcModel's column model
         */
        SelectedColumnData( TopcatModel tcModel, StarTableColumn tcol ) {
            super( tcol.getColumnInfo() );
            tcModel_ = tcModel;
            icol_ = tcol.getModelIndex();
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

    /**
     * ColumnData implementation for a column out of a table which is
     * modified by a converter.  The equals() method is correct on the
     * assumption that only one converter is ever used for a given 
     * column of a given table.
     */
    private static class ConvertedColumnData extends SelectedColumnData {

        private final ValueConverter conv_;

        /**
         * Constructor.
         *
         * @param   tcModel  topcat model that the column is from
         * @param   tcol  table column in tcModel's column model
         */
        ConvertedColumnData( TopcatModel tcModel, StarTableColumn tcol,
                             ValueConverter conv ) {
            super( tcModel, tcol );
            ColumnInfo cinfo = new ColumnInfo( conv.getOutputInfo() );
            cinfo.setAuxDatum( new DescribedValue( TopcatUtils
                                                  .NUMERIC_CONVERTER_INFO,
                                                  conv ) );
            setColumnInfo( cinfo );
            conv_ = conv;
        }

        public Object readValue( long irow ) throws IOException {
            return conv_.convert( super.readValue( irow ) );
        }

        public boolean equals( Object o ) {
            return o instanceof ConvertedColumnData && super.equals( o );
        }
    }

    /**
     * ColumnData implementation which yields the table row number.
     */
    private static class IndexColumnData extends ColumnData {
        final TopcatModel tcModel_;

        IndexColumnData( TopcatModel tcModel ) {
            super( INDEX_INFO );
            tcModel_ = tcModel;
        }

        public Object readValue( long irow ) {
            return new Long( irow + 1 );
        }

        public String toString() {
            return "index";
        }

        public boolean equals( Object o ) {
            return o instanceof IndexColumnData
                && ((IndexColumnData) o).tcModel_ == this.tcModel_;
        }

        public int hashCode() {
            return tcModel_.hashCode();
        }
    }
}
