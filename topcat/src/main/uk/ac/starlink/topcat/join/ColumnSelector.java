package uk.ac.starlink.topcat.join;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.table.TableColumnModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
import uk.ac.starlink.topcat.TopcatModel;

/**
 * Graphical component which can select a column from a table.
 * As well as simple selection of the column, it also permits GUI
 * selection of the units in which the column is specified.
 *
 * @author   Mark Taylor (Starlink)
 * @since    17 Sep 2003 (Starlink)
 */
public class ColumnSelector extends JComponent implements ActionListener {

    private final ValueInfo info_;
    private final JComboBox colChooser_;
    private final JComboBox conversionChooser_;
    private final ColumnConverter converter0_;
    private final Map colModelMap_ = new WeakHashMap();
    private final Map converterMap_ = new HashMap();
    private TopcatModel tcModel_;

    /**
     * Constructs a new column selector which will allow selection of a 
     * column suitable for a given value type.
     *
     * @param  info  description of the kind of column that must be chosen
     */
    public ColumnSelector( ValueInfo info ) {
        info_ = info;
        String units = info.getUnitString();

        /* Set up the label. */
        JLabel label = new JLabel( info.getName() + " column:" );
        label.setToolTipText( "Select column for " + info.getDescription() );

        /* Set up the main column selection combo box. */
        colChooser_ = new JComboBox();
        colChooser_.setRenderer( new ColumnCellRenderer( colChooser_ ) );
        colChooser_.addActionListener( this );

        /* Set up a value conversion combo box if appropriate. */
        ColumnConverter[] converters = ColumnConverter.getConverters( info );
        if ( converters.length > 1 ) {
            converter0_ = null;
            conversionChooser_ = new JComboBox( converters );
            conversionChooser_.setSelectedIndex( 0 );
            conversionChooser_.setToolTipText( "Units for column " + 
                                               info.getName() );
            conversionChooser_.addActionListener( this );
        }
        else {
            conversionChooser_ = null;
            converter0_ = converters[ 0 ];
        }

        /* Lay out components. */
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        add( label );
        add( Box.createHorizontalStrut( 5 ) );
        add( colChooser_ );
        if ( conversionChooser_ != null ) {
            add( Box.createHorizontalStrut( 5 ) );
            add( conversionChooser_ );
        }
        else if ( units != null && units.trim().length() > 0 ) {
            add( Box.createHorizontalStrut( 5 ) );
            add( new JLabel( " (" + units.trim() + ")" ) );
        }
    }

    /**
     * Sets this selector to choose columns from a given TopcatModel.
     * If it has had this table before, it will remember the settings
     * from last time as a user convenience.
     *
     * @param  tcModel  topcat model representing the table whose columns
     *         can be selected
     */
    public void setTable( TopcatModel tcModel ) {
        tcModel_ = tcModel;
        if ( ! colModelMap_.containsKey( tcModel ) ) {
            colModelMap_.put( tcModel, 
                              makeColumnSelectionModel( tcModel, info_ ) );
        }
        colChooser_.setModel( (ComboBoxModel) colModelMap_.get( tcModel ) );

        /* Force an update event. */
        colChooser_.setSelectedIndex( colChooser_.getSelectedIndex() );
    }

    /**
     * Returns the column value converter object currently selected by the
     * user.
     *
     * @return converter
     */
    private ColumnConverter getConverter() {
        if ( converter0_ != null ) {
            return converter0_;
        }
        else {
            return (ColumnConverter) conversionChooser_.getSelectedItem();
        }
    }

    /**
     * Returns the (effective) column currently selected by the user.
     * It takes into account the column and (if any) conversion selected
     * by the user.
     * This column is based on the data in the currently installed table.
     *
     * @return  ColumnData representing the currently-selected column,
     *          or null if none is selected
     */
    public ColumnData getSelectedColumn() {
        StarTableColumn tcol = (StarTableColumn) colChooser_.getSelectedItem();
        if ( tcol == null ) {
            return null;
        }
        final int icol = tcol.getModelIndex();
        final ColumnConverter colConverter = getConverter();
        final StarTable table = tcModel_.getDataModel();
        assert colConverter != null;
        return new ColumnData( tcol.getColumnInfo() ) {
            public Object readValue( long irow ) throws IOException {
                return colConverter.convertValue( table.getCell( irow, icol ) );
            }
        };
    }

    /**
     * Invoked when the selected item from one of the combo boxes is changed.
     */
    public void actionPerformed( ActionEvent evt ) {
        Object source = evt.getSource();
        if ( source == colChooser_ && 
             colChooser_.getSelectedItem() != null  ) {
            columnSelected( (StarTableColumn) colChooser_.getSelectedItem() );
        }
        else if ( source == conversionChooser_ && 
                  conversionChooser_.getSelectedItem() != null ) {
            converterSelected( (ColumnConverter)
                               conversionChooser_.getSelectedItem() );
        }
    }

    /**
     * Invoked when a new column has been selected.
     *
     * @param  tcol  newly selected column
     */
    private void columnSelected( StarTableColumn tcol ) {
        if ( conversionChooser_ != null ) {
            ColumnConverter storedConverter =
               (ColumnConverter) converterMap_.get( tcol );

            /* If we've used this column before, set the converter type
             * to the one that was in effect last time. */
            if ( storedConverter != null ) {
                conversionChooser_.setSelectedItem( storedConverter );
            }

            /* Otherwise, try to guess the converter type on the basis
             * of the selected column. */
            else {
                conversionChooser_
               .setSelectedItem( guessConverter( tcol.getColumnInfo() ) );
            }
        }
    }

    /**
     * Invoked when a new converter has been selected.
     * 
     * @param  conv  newly selected column converter
     */
    private void converterSelected( ColumnConverter conv ) {

        /* Remember what converter was chosen for the current column. */
        converterMap_.put( colChooser_.getSelectedItem(), conv );
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
            int nconv = conversionChooser_.getItemCount();

            /* Known converters for radians are radian or degree. */
            if ( units.equalsIgnoreCase( "radian" ) ||
                 units.equalsIgnoreCase( "radians" ) ) {
                if ( cunits.toLowerCase().startsWith( "rad" ) ) {
                    for ( int i = 0; i < nconv; i++ ) {
                        ColumnConverter conv =
                            (ColumnConverter) conversionChooser_.getItemAt( i );
                        if ( conv.toString().toLowerCase()
                                            .startsWith( "rad" ) ) {
                            return conv;
                        }
                    }
                }
                else if ( cunits.toLowerCase().startsWith( "deg" ) ) {
                    for ( int i = 0; i < nconv; i++ ) {
                        ColumnConverter conv = 
                            (ColumnConverter) conversionChooser_.getItemAt( i );
                        if ( conv.toString().toLowerCase()
                                            .startsWith( "deg" ) ) {
                            return conv;
                        }
                    }
                }
            }
        }

        /* Return default one if we haven't found a match yet. */
        return (ColumnConverter) conversionChooser_.getItemAt( 0 );
    }

    /** 
     * Returns a combobox model which allows selection of columns
     * from a table model suitable for a given argument.
     */ 
    private static ComboBoxModel makeColumnSelectionModel( TopcatModel tcModel,
                                                           ValueInfo argInfo ) {

        /* Make the model. */
        TableColumnModel columnModel = tcModel.getColumnModel();
        RestrictedColumnComboBoxModel model =
            RestrictedColumnComboBoxModel 
           .makeClassColumnComboBoxModel( columnModel, argInfo.isNullable(),
                                          argInfo.getContentClass() );
        
        /* Have a guess what will be a good value for the initial
         * selection.  There is scope for doing this better. */
        int selection = -1;
        ColumnInfo[] cinfos =
            Tables.getColumnInfos( tcModel.getApparentStarTable() );
        int ncol = cinfos.length;
        String ucd = argInfo.getUCD();
        if ( ucd != null ) { 
            for ( int i = 0; i < ncol && selection < 0; i++ ) {
                if ( model.acceptColumn( cinfos[ i ] ) &&
                     cinfos[ i ].getUCD() != null &&
                     cinfos[ i ].getUCD().indexOf( ucd ) >= 0 ) {
                    selection = i;
                }
            }
        }
        String name = argInfo.getName().toLowerCase();
        if ( name != null && selection < 0 ) {
            for ( int i = 0; i < ncol && selection < 0; i++ ) {
                if ( model.acceptColumn( cinfos[ i ] ) ) {
                    String cname = cinfos[ i ].getName();
                    if ( cname != null &&
                         cname.toLowerCase().startsWith( name ) ) {
                        selection = i;
                    }
                }
            }
        }
        if ( selection >= 0 ) {
            model.setSelectedItem( columnModel.getColumn( selection ) );
        }
        return model;
    }

}
