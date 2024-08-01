package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JToggleButton;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * ComboBox for selecting DomainMappers appropriate for a given Domain.
 * It works alongside a ColumnData selector, since the default and available
 * DomainMappers will be a function of the currently selected ColumnData
 * as well as of the Domain.
 *
 * @author   Mark Taylor
 * @since    15 Apr 2020
 */
public class DomainMapperComboBox extends JComboBox<DomainMapper> {

    private final Domain<?> domain_;
    private final JComboBox<ColumnData> columnSelector_;
    private final JComponent line_;
    private final List<DomainMapper> mappers_;
    private Object coldata_;

    /**
     * Constructor.
     *
     * @param  domain  domain for which mappers can be selected
     * @param  columnSelector   column selector with which this selector
     *                          is associated
     */
    @SuppressWarnings("this-escape")
    public DomainMapperComboBox( Domain<?> domain,
                                 JComboBox<ColumnData> columnSelector ) {
        domain_ = domain;
        columnSelector_ = columnSelector;
        final JToggleButton lockButton = new JToggleButton( ResourceIcon.LOCK );
        lockButton.setToolTipText( "Lock " + domain.getDomainName()
                                 + " type selector" );
        lockButton.setMargin( new Insets( 0, 0, 0, 0 ) );
        lockButton.setSelected( false );
        lockButton.addActionListener( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( ! lockButton.isSelected() ) {
                    updateMapper();
                }
            }
        } );
        mappers_ = new ArrayList<DomainMapper>();
        if ( domain.getMappers().length > 1 ) {
            mappers_.add( null );
        }
        mappers_.addAll( Arrays.asList( domain.getMappers() ) );
        for ( DomainMapper mapper : mappers_ ) {
            addItem( mapper );
        }
        setRenderer( new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent( JList<?> list,
                                                           Object value,
                                                           int index,
                                                           boolean isSel,
                                                           boolean hasFocus ) {
                Component c =
                    super.getListCellRendererComponent( list, value, index,
                                                        isSel, hasFocus );
                if ( c instanceof JLabel ) {
                    ((JLabel) c).setText( value instanceof DomainMapper
                                        ? ((DomainMapper) value).getSourceName()
                                        : " " );
                }
                return c;
            }
        } );
        columnSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                if ( ! lockButton.isSelected() ) {
                    updateMapper();
                }
            }
        } );
        updateMapper();
        line_ = Box.createHorizontalBox();
        line_.add( lockButton );
        line_.add( Box.createHorizontalStrut( 5 ) );
        line_.add( new ShrinkWrapper( this ) );
        line_.add( Box.createHorizontalStrut( 5 ) );
        line_.add( new ComboBoxBumper( this ) );
    }

    /**
     * Returns the component containing this selector plus some decorations
     * and auxiliary controls.
     *
     * @return   display component
     */
    public JComponent getComponent() {
        return line_;
    }

    /**
     * Updates the state of this component to correspond to the current
     * selection status of its associated column selector.
     */
    private void updateMapper() {
        Object cdata = columnSelector_.getSelectedItem();
        if ( cdata != coldata_ ) {
            coldata_ = cdata;
            removeAllItems();
            ColumnInfo info = getSelectedColumnInfo();
            Class<?> clazz = info == null ? null : info.getContentClass();
            for ( DomainMapper mapper : mappers_ ) {
                if ( mapper == null || clazz == null ||
                     mapper.getSourceClass().isAssignableFrom( clazz ) ) {
                    addItem( mapper );
                }
            }
            DomainMapper auto = getAutoValue();
            if ( ! mappers_.contains( auto ) ) {
                addItem( auto );
            }
            setSelectedItem( auto );
        }
    }

    /**
     * Returns the default DomainMapper for the currently selected column.
     * 
     * @return  default domain mapper for current state
     */
    private DomainMapper getAutoValue() {
        ColumnInfo info = getSelectedColumnInfo();
        if ( info != null ) {
            DomainMapper dm = domain_.getProbableMapper( info );
            if ( dm == null ) {
                dm = domain_.getPossibleMapper( info );
            }
            return dm;
        }
        return null;
    }

    /**
     * Returns the metadata for the selected column, if any.
     * 
     * @return  value info for selected column, or null
     */
    private ColumnInfo getSelectedColumnInfo() {
        Object sel = columnSelector_.getSelectedItem();
        return sel instanceof ColumnData
             ? ((ColumnData) sel).getColumnInfo()
             : null;
    }
}
