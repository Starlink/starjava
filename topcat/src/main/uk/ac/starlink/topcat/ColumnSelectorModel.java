package uk.ac.starlink.topcat;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;

/**
 * Model for a {@link ColumnSelector}.  Contains information about
 * how you get a value of a given type (such as Right Ascension)
 * from a table.
 *
 * @author   Mark Taylor (Starlink)
 * @since    6 Oct 2004
 */
public class ColumnSelectorModel {

    private final ValueInfo info_;
    private final ComboBoxModel<ColumnConverter> convChooser_;
    private final ColumnConverter converter0_;
    private final Map<ColumnData,ColumnConverter> convMap_ =
        new HashMap<ColumnData,ColumnConverter>();
    private final SelectionListener selectionListener_;
    private TopcatModel tcModel_;
    private ComboBoxModel<ColumnData> colChooser_;

    /**
     * Constructs a new model for a given table and value type.
     *
     * @param  tcModel  table model
     * @param  info   description of the kind of column which is required
     */
    @SuppressWarnings("this-escape")
    public ColumnSelectorModel( TopcatModel tcModel, ValueInfo info ) {
        info_ = info;
        selectionListener_ = new SelectionListener();

        /* Get a suitable model for selection of the unit converter, if
         * appropriate. */
        String units = info.getUnitString();
        ColumnConverter[] converters = ColumnConverter.getConverters( info );
        if ( converters.length > 1 ) {
            converter0_ = null;
            convChooser_ =
                new DefaultComboBoxModel<ColumnConverter>( converters );
            convChooser_.addListDataListener( selectionListener_ );
        }
        else {
            convChooser_ = null;
            converter0_ = converters[ 0 ];
        }

        setTable( tcModel );
    }

    /**
     * Sets the table that this selector model is configured for.
     *
     * @param  tcModel  new table
     */
    public void setTable( TopcatModel tcModel ) {
        tcModel_ = tcModel;

        /* Get a suitable model for selection of the base column from the 
         * table. */
        if ( colChooser_ != null ) {
            colChooser_.removeListDataListener( selectionListener_ );
        }
        colChooser_ = makeColumnModel( tcModel_, info_ );
        colChooser_.addListDataListener( selectionListener_ );

        /* Force an update to make sure that the correct converter for any
         * currently-selected column is selected. */
        Object item = colChooser_.getSelectedItem();
        if ( item instanceof ColumnData ) {
            columnSelected( (ColumnData) item );
        }
    }

    /**
     * Returns the currently selected column converter.
     *
     * @return  converter
     */
    private ColumnConverter getConverter() {
        if ( converter0_ != null ) {
            return converter0_;
        }
        else {
            return (ColumnConverter) convChooser_.getSelectedItem();
        }
    }

    /**
     * Returns this model's value description.
     *
     * @return  value info
     */
    public ValueInfo getValueInfo() {
        return info_;
    }

    /**
     * Returns the model used for choosing columns.
     * Elements of the model which contain usable data will be instances of
     * {@link uk.ac.starlink.table.ColumnData}
     * and will not take account of any selected converter.
     * The selected item may be of some other type (currently String),
     * and this should be ignored (treated as null) for the purposes
     * of data access.
     *
     * @return  columns combo box model
     */
    public ComboBoxModel<ColumnData> getColumnModel() {
        return colChooser_;
    }

    /**
     * Returns the model used for choosing converters.  May be null if
     * there is no choice.
     *
     * @return  converter combo box model, or null
     */
    public ComboBoxModel<ColumnConverter> getConverterModel() {
        return convChooser_;
    }

    /**
     * Returns the (effective) column currently selected by the user.
     * It takes into account the column and (if any) conversion selected
     * by the user.
     *
     * <p>The returned ColumnData object has an intelligent implementation
     * of <code>equals</code> (and <code>hashCode</code>), in that 
     * two invocations of this method without any intervening change of
     * of state of this model will evaluate equal.
     *
     * @return  ColumnData representing the currently-selected column,
     *          or null if none is selected
     */
    public ColumnData getColumnData() {
        Object item = colChooser_.getSelectedItem();
        return item instanceof ColumnData
             ? new ConvertedColumnData( (ColumnData) item, getConverter() )
             : null;
    }

    /**
     * Sets the content of this model given textual values for the 
     * column specification and for the converter.
     * This is done on a best-efforts basis; the return status indicates
     * whether it worked.
     *
     * @param  colTxt   text content for column specification
     * @param  convTxt   name of converter required; may be null
     * @return   true iff configuration was successful
     */
    public boolean setTextValue( String colTxt, String convTxt ) {
        if ( colChooser_ instanceof ColumnDataComboBoxModel ) {
            ColumnData cdata;
            try {
                cdata = ((ColumnDataComboBoxModel) colChooser_)
                       .stringToColumnData( colTxt );
            }
            catch ( CompilationException e ) {
                return false;
            }
            colChooser_.setSelectedItem( cdata );
            if ( convTxt != null ) {
                ColumnConverter convItem = null;
                for ( int i = 0; i < convChooser_.getSize(); i++ ) {
                    ColumnConverter conv = convChooser_.getElementAt( i );
                    if ( conv.toString().equals( convTxt ) ) {
                        convItem = conv;
                    }
                }
                if ( convItem != null ) {
                    convChooser_.setSelectedItem( convItem );
                    return true;
                }
                else {
                    return false;
                }
            }
            else {
                return true;
            }
        }
        else {
            return false;
        }
    }

    /**
     * Called when the column selection is changed.
     *
     * @param  col  new column (not null)
     */
    private void columnSelected( ColumnData cdata ) {
        if ( convChooser_ != null ) {
            ColumnConverter storedConverter = convMap_.get( cdata );

            /* If we've used this column before, set the converter type
             * to the one that was in effect last time. */
            if ( storedConverter != null ) {
                convChooser_.setSelectedItem( storedConverter );
            }

            /* Otherwise, try to guess the converter type on the basis
             * of the selected column. */
            else {
                ColumnConverter conv = guessConverter( cdata.getColumnInfo() );
                if ( conv != null ) {
                    convChooser_.setSelectedItem( conv );
                }
            }
        }
    }

    /**
     * Called when the converter selection is changed.
     *
     * @param  conv  new converter (not null)
     */
    private void converterSelected( ColumnConverter conv ) {

        /* Remember what converter was chosen for the current column. */
        convMap_.put( (ColumnData) colChooser_.getSelectedItem(), conv );
    }

    /**
     * Returns a best guess for the conversion to use for a given selected
     * column.  This will be one of the ones associated with this selector
     * (i.e. one of the ones in the conversion selector or the sole one
     * if there is no conversion selector).
     *
     * @param  cinfo  column description
     * @return  suitable column converter
     */
    private ColumnConverter guessConverter( ColumnInfo cinfo ) {

        /* If there is only one permissible converter, return that. */
        if ( converter0_ != null ) {
            return converter0_;
        }

        /* Otherwise, try to get clever.  This is currently done on a
         * case-by-case basis rather than using an extensible framework
         * because there's a small number (1) of conversions that we know
         * about.  If there were many, a redesign might be in order. */
        String units = info_.getUnitString();
        String cunits = cinfo.getUnitString();
        if ( units != null && cunits != null ) {
            units = units.toLowerCase();
            cunits = cunits.toLowerCase();
            int nconv = convChooser_.getSize();
            if ( units.equals( "radian" ) || units.equals( "radians" ) ||
                 units.equals( "rad" ) ) {
                if ( cunits.startsWith( "rad" ) ) {
                    for ( int i = 0; i < nconv; i++ ) {
                        ColumnConverter conv = convChooser_.getElementAt( i );
                        if ( conv.toString().toLowerCase()
                                            .startsWith( "rad" ) ) {
                            return conv;
                        }
                    }
                }
                else if ( cunits.startsWith( "deg" ) ) {
                    for ( int i = 0; i < nconv; i++ ) {
                        ColumnConverter conv = convChooser_.getElementAt( i );
                        if ( conv.toString().toLowerCase()
                                            .startsWith( "deg" ) ) {
                            return conv;
                        }
                    }
                }
                else if ( cunits.startsWith( "hour" ) ||
                          cunits.equals( "hr" ) || cunits.equals( "hrs" ) ) {
                    for ( int i = 0; i < nconv; i++ ) {
                        ColumnConverter conv = convChooser_.getElementAt( i );
                        if ( conv.toString().toLowerCase()
                                            .startsWith( "hour" ) ) {
                            return conv;
                        }
                    }
                }
                else if ( cunits.startsWith( "arcmin" ) ) {
                    for ( int i = 0; i < nconv; i++ ) {
                        ColumnConverter conv = convChooser_.getElementAt( i );
                        if ( conv.toString().toLowerCase()
                                            .startsWith( "arcmin" ) ) {
                            return conv;
                        }
                    }
                }
                else if ( cunits.startsWith( "arcsec" ) ) {
                    for ( int i = 0; i < nconv; i++ ) {
                        ColumnConverter conv = convChooser_.getElementAt( i );
                        if ( conv.toString().toLowerCase()
                                            .startsWith( "arcsec" ) ) {
                            return conv;
                        }
                    }
                }
            }
        }

        /* Return null if we haven't found a match yet. */
        return null;
    }

    /**
     * Returns a combobox model which allows selection of columns
     * from a table model suitable for a given argument.
     * The elements of the model are instances of
     * ColumnData (or null).
     */
    private static ComboBoxModel<ColumnData>
            makeColumnModel( TopcatModel tcModel, ValueInfo argInfo ) {

        /* With no table, the model is empty. */
        if ( tcModel == null ) {
            return new DefaultComboBoxModel<ColumnData>( new ColumnData[ 1 ] );
        }

        /* Make the model. */
        ComboBoxModel<ColumnData> model = 
            new ColumnDataComboBoxModel( tcModel, argInfo.getContentClass(),
                                         argInfo.isNullable() );

        /* Have a guess what will be a good value for the initial
         * selection.  There is scope for doing this better. */
        ColumnData selected = null;
        String ucd = argInfo.getUCD();
        if ( ucd != null ) {
            for ( int i = 0; i < model.getSize() && selected == null; i++ ) {
                Object item = model.getElementAt( i );
                if ( item instanceof ColumnData ) {
                    ColumnData cdata = (ColumnData) item;
                    ColumnInfo info = cdata.getColumnInfo();
                    if ( info.getUCD() != null && 
                         matchUcds( info.getUCD(), ucd ) ) {
                        selected = cdata;
                    }
                }
            }
        }
        String name = argInfo.getName().toLowerCase();
        if ( name != null ) {
            for ( int i = 0; i < model.getSize() && selected == null; i++ ) {
                Object item = model.getElementAt( i );
                if ( item instanceof ColumnData ) {
                    ColumnData cdata = (ColumnData) item;
                    ColumnInfo info = cdata.getColumnInfo();
                    String cname = info.getName();
                    if ( cname != null &&
                         cname.toLowerCase().startsWith( name ) ) {
                        selected = cdata;
                    }
                }
            }
        }
        if ( selected != null ) { 
            model.setSelectedItem( selected );
        }
        return model;
    }

    /**
     * Determines whether two UCDs appear to match.
     *
     * @param  u1  first UCD
     * @param  u2  second UCD
     */
    private static boolean matchUcds( String u1, String u2 ) {

        /* Low-rent UCD1 -> UCD1+ conversion. */
        u1 = u1.replace( '_', '.' ).toLowerCase();
        u2 = u2.replace( '_', '.' ).toLowerCase();
        if ( u1.indexOf( u2 ) >= 0 ) {
            return true;
        }
        return false;
    }

    /**
     * Implements ListDataListener to react when the column selector or
     * the converter selector changes.
     */
    private class SelectionListener implements ListDataListener {
        public void intervalAdded( ListDataEvent evt ) {
        }
        public void intervalRemoved( ListDataEvent evt ) {
        }
        public void contentsChanged( ListDataEvent evt ) {

            /* Contrary to API documentation, this is called when the selection
             * on a ComboBoxModel is changed. */
            if ( evt.getSource() == colChooser_ ) {
                Object item = colChooser_.getSelectedItem();
                if ( item instanceof ColumnData ) {
                    columnSelected( (ColumnData) item );
                }
            }
            else if ( evt.getSource() == convChooser_ ) {
                ColumnConverter conv =
                    (ColumnConverter) convChooser_.getSelectedItem();
                if ( conv != null ) {
                    converterSelected( conv );
                }
            }
            else {
                assert false;
            }
        }
    }

    /**
     * ColumnData implementation which gives the result of the virtual
     * column described by the current state of this component.
     * It has non-trivial implementations of equals and hashCode.
     */
    private static class ConvertedColumnData extends ColumnData {

        private final ColumnData base_;
        private final ColumnConverter converter_;

        /**
         * Constructor.
         *
         * @param   base   base column data
         * @param   converter   converter
         */
        ConvertedColumnData( ColumnData base, ColumnConverter converter ) {
            super( base.getColumnInfo() );
            base_ = base;
            converter_ = converter;
        }

        public Object readValue( long irow ) throws IOException {
            return converter_.convertValue( base_.readValue( irow ) );
        }

        public boolean equals( Object o ) {
            if ( o instanceof ConvertedColumnData ) {
                ConvertedColumnData other = (ConvertedColumnData) o;
                return this.base_ == other.base_
                    && this.converter_ == other.converter_;
            }
            else {
                return false;
            }
        }

        public int hashCode() {
            int code = 9997;
            code = 23 * code + base_.hashCode();
            code = 23 * code + converter_.hashCode();
            return code;
        }
    }
}
