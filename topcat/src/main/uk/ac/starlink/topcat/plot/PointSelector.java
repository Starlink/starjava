package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JPanel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.CheckBoxStack;
import uk.ac.starlink.topcat.ColumnCellRenderer;
import uk.ac.starlink.topcat.ColumnComboBoxModel;
import uk.ac.starlink.topcat.OptionsListModel;
import uk.ac.starlink.topcat.RestrictedColumnComboBoxModel;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBoxModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Component for choosing a table, a number of columns, and a selection
 * of row subsets.
 *
 * @author   Mark Taylor
 * @since    28 Oct 2005
 */
public class PointSelector extends JPanel implements TopcatListener {

    private final int ndim_;
    private final JComboBox tableSelector_;
    private final JComboBox[] colSelectors_;
    private final JScrollPane subsetScroller_;
    private final OrderedSelectionRecorder subSelRecorder_;
    private final ActionForwarder actionForwarder_;
    private final SelectionForwarder selectionForwarder_;
    private final ListSelectionListener listActioner_;
    private final List topcatListeners_;
    private MarkStyleProfile markStyles_;
    private TopcatModel tcModel_;
    private ListSelectionModel subSelModel_;

    /**
     * Constructs a selector optionally with a table which will be the
     * only one it operates on.  If <code>fixedTable</code> is non-null
     * then table selection will not be permitted.
     *
     * @param   axisNames  labels for the columns to choose
     * @param   markStyles  default marker style profile
     * @param   fixedTable  optionally, the identity of a table which 
     *          is the one on which this selector will operate
     */
    public PointSelector( String[] axisNames, MarkStyleProfile markStyles,
                          TopcatModel fixedTable ) {
        super( new BorderLayout() );
        ndim_ = axisNames.length;
        markStyles_ = markStyles;
        actionForwarder_ = new ActionForwarder();
        selectionForwarder_ = new SelectionForwarder();
        listActioner_ = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                actionForwarder_
               .actionPerformed( new ActionEvent( this, 0, "Selection" ) );
            }
        };
        topcatListeners_ = new ArrayList();
        final JComponent controlBox = Box.createHorizontalBox();
        add( controlBox, BorderLayout.SOUTH );

        final Box entryBox = new Box( BoxLayout.Y_AXIS );
        entryBox.setBorder( AuxWindow.makeTitledBorder( "Data" ) );
        controlBox.add( entryBox );
        controlBox.add( Box.createHorizontalStrut( 5 ) );

        /* Prepare a selection box for the table. */
        TablesListComboBoxModel tablesModel = new TablesListComboBoxModel();
        tableSelector_ = new JComboBox( tablesModel );
        tableSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                configureForTable( (TopcatModel)
                                   tableSelector_.getSelectedItem() );
            }
        } );
        tableSelector_.addActionListener( actionForwarder_ );

        /* Place the table selection box if we don't have a fixed table. */
        if ( fixedTable == null ) {
            JComponent tPanel = Box.createHorizontalBox();
            tPanel.add( new JLabel( " Table: " ) );
            tPanel.add( new ShrinkWrapper( tableSelector_ ) );
            tPanel.add( Box.createHorizontalGlue() );
            entryBox.add( tPanel );
        }
 
        /* Prepare and place panels for selection and configuration of each
         * axis. */
        colSelectors_ = new JComboBox[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            String aName = axisNames[ i ];
            JComponent cPanel = Box.createHorizontalBox();
            cPanel.add( new JLabel( " " + aName + " Axis: " ) );
            entryBox.add( Box.createVerticalStrut( 5 ) );
            entryBox.add( cPanel );

            /* Add and configure the column selector. */
            colSelectors_[ i ] = new JComboBox();
            colSelectors_[ i ]
                .setRenderer( new ColumnCellRenderer( colSelectors_[ i ] ) );
            colSelectors_[ i ].addActionListener( actionForwarder_ );
            cPanel.add( new ShrinkWrapper( colSelectors_[ i ] ) );
            cPanel.add( Box.createHorizontalStrut( 5 ) );
            cPanel.add( new ComboBoxBumper( colSelectors_[ i ] ) );
            cPanel.add( Box.createHorizontalGlue() );
            colSelectors_[ i ].setEnabled( false );

            /* Without this border, the bumpers come out one pixel larger
             * than the combo box.  I have absolutely no idea why, or why
             * this fixes it. */
            cPanel.setBorder( BorderFactory.createEmptyBorder( 1, 1, 1, 1 ) );
        }
        entryBox.add( Box.createVerticalStrut( 5 ) );

        /* Make a container for the subset selector. */
        subsetScroller_ = new JScrollPane( new CheckBoxStack() );
        subsetScroller_.setBorder( AuxWindow
                                  .makeTitledBorder( "Row Subsets" ) );
        controlBox.add( subsetScroller_ );

        /* Arrange for a record to be kept of the order in which selections
         * are made. */
        subSelRecorder_ = new OrderedSelectionRecorder() {
            public boolean[] getModelState() {
                return getSubsetSelection();
            }
        };
        selectionForwarder_.add( subSelRecorder_ );
        selectionForwarder_.add( listActioner_ );

        /* Initialise the table; either set it to the fixed value if one
         * has been specified, or set it to the intial value of the
         * selector, which may be necessary to initialise the state
         * properly via listeners. */
        if ( fixedTable != null ) {
            setTable( fixedTable, true );
        }
        else {
            TopcatModel tcModel =
                (TopcatModel) tableSelector_.getSelectedItem();
            if ( tcModel != null ) {
                configureForTable( tcModel );
            }
        }
    }

    public void setVisible( boolean visible ) {
        super.setVisible( visible );
        if ( visible ) {
            revalidate();
            repaint();
        }
    }

    /**
     * Constructs a selector in which the user can choose the table.
     *
     * @param   axisNames  labels for the columns to choose
     * @param   markStyles  default marker style profile
     */
    public PointSelector( String[] axisNames, MarkStyleProfile markStyles ) {
        this( axisNames, markStyles, null );
    }

    /**
     * Returns the number of axes this component will deal with.
     * 
     * @return  dimensionality
     */
    public int getNdim() {
        return ndim_;
    }

    /**
     * Indicates whether this selector has enough state filled in to be
     * able to specify some point data.
     *
     * @return   true iff properly filled in
     */
    public boolean isValid() {
        if ( getTable() == null ) {
            return false;
        }
        StarTableColumn[] cols = getColumns();
        for ( int i = 0; i < cols.length; i++ ) {
            if ( cols[ i ] == null ||
                 cols[ i ] == ColumnComboBoxModel.NO_COLUMN ) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the currently selected table.
     *
     * @param  topcat model of the currently selected table
     */
    public TopcatModel getTable() {
        return tcModel_;
    }

    /**
     * Sets the table to which this selector currently applies.
     *
     * @param   tcModel   table
     * @param   init   whether to initialise the columns with sensible
     *          starting values
     */
    public void setTable( TopcatModel tcModel, boolean init ) {
        tableSelector_.setSelectedItem( tcModel );

        /* Initialise the column selectors.  This isn't necesary, but it's
         * what the old plot window used to do so may confuse people less.
         * Possibly change this action in the future though. */
        if ( init ) {
            for ( int i = 0; i < ndim_; i++ ) {
                if ( i + 1 < colSelectors_[ i ].getItemCount() ) {
                    colSelectors_[ i ].setSelectedIndex( i + 1 );
                }
            }
        }
    }

    /**
     * Returns an array of the selected columns, one for each axis.
     *
     * @return  columns array
     */
    public StarTableColumn[] getColumns() {
        StarTableColumn[] cols = new StarTableColumn[ ndim_ ];
        for ( int i = 0; i < ndim_; i++ ) {
            cols[ i ] = (StarTableColumn) colSelectors_[ i ].getSelectedItem();
        }
        return cols;
    }

    /**
     * Sets the current selection pattern for row subsets.
     *
     * @param   selected  array of selection flags for subsets
     */
    public void setSubsetSelection( boolean[] selected ) {
        subSelModel_.setValueIsAdjusting( true );
        subSelModel_.clearSelection();
        int nset = getTable().getSubsets().getSize();
        for ( int i = 0; i < nset; i++ ) {
            if ( selected[ i ] ) {
                subSelModel_.addSelectionInterval( i, i );
            }
        }
        subSelModel_.setValueIsAdjusting( false );
    }

    /**
     * Returns an array of flags indicating which of the row subsets have
     * been selected.
     *
     * @return  subset selection flags
     */
    public boolean[] getSubsetSelection() {
        TopcatModel tcModel = getTable();
        if ( tcModel == null ) {
            return new boolean[ 0 ];
        }
        else {
            int nset = tcModel.getSubsets().getSize();
            boolean[] flags = new boolean[ nset ];
            for ( int i = 0; i < nset; i++ ) {
                flags[ i ] = subSelModel_.isSelectedIndex( i );
            }
            return flags;
        }
    }

    /**
     * Returns a list of indices giving the selected subsets.
     * This contains substantially the same information as in 
     * {@link #getSubsetSelection}, but in a different form and with
     * the additional information of what order the selections were
     * made in.
     *
     * @return  array of selected subset indices 
     */
    public int[] getOrderedSubsetSelection() {
        return subSelRecorder_.getOrderedSelection();
    }

    /**
     * Returns the marker style to use for a given subset index.
     *
     * @param  isub  subset index
     * @return  marker style
     */
    public MarkStyle getStyle( int isub ) {
        return markStyles_.getStyle( isub );
    }

    /**
     * Returns the mark style profile used by this selector.
     *
     * @return  style profile
     */
    public MarkStyleProfile getStyles() {
        return markStyles_;
    }

    /**
     * Adds an action listener.  It will be notified every time something
     * interesting happens to this selector.
     *
     * @param  listener   listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addListener( listener );
    }

    /**
     * Removes an action listener which was previously added.
     *
     * @param   listener  listener to remove
     * @see  #addActionListener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeListener( listener );
    }

    /**
     * Adds a ListSelectionListener which will be notified when the
     * subset selection changes.  Note that the source of the 
     * {@link javax.swing.event.ListSelectionEvent}s which are sent
     * will be this PointSelector.
     *
     * @param  listener  listener to add
     */
    public void addSubsetSelectionListener( ListSelectionListener listener ) {
        selectionForwarder_.add( listener );
    }

    /**
     * Removes a ListSelectionListener which was previously added.
     *
     * @param  listner  listener to remove
     * @see    #addSubsetSelectionListener
     */
    public void removeSubsetSelectionListener( ListSelectionListener listner ) {
        selectionForwarder_.remove( listner );
    }

    /**
     * Adds a TopcatListener which will be notified when changes occur to
     * the TopcatModel associated with this selector.
     *
     * @param   listener  listener to add
     */
    public void addTopcatListener( TopcatListener listener ) {
        topcatListeners_.add( listener );
    }
    
    /**
     * Removes a TopcatListener which was previously added.
     *
     * @param  listener  listener to remove
     * @see    #addTopcatListener
     */
    public void removeTopcatListener( TopcatListener listener ) {
        topcatListeners_.remove( listener );
    }

    /**
     * Defines what columns will appear as possibles in the column selectors.
     *
     * @param  cinfo  column metadata
     * @return   true iff a column like <code>cinfo</code> should be 
     *           choosable
     */
    protected boolean acceptColumn( ColumnInfo cinfo ) {
        Class clazz = cinfo.getContentClass();
        return Number.class.isAssignableFrom( clazz )
            || Date.class.isAssignableFrom( clazz );
    }

    /**
     * Sets this selector to work from a table described by a given
     * TopcatModel.
     *
     * @param  tcModel  table to work with
     */
    private void configureForTable( TopcatModel tcModel ) {

        /* Reset our record of the topcat model, ensuring that we continue
         * to listen to the right one. */
        if ( tcModel_ != null ) {
            tcModel_.removeTopcatListener( this );

            /* Reset column selector models to prevent unhelpful events being
             * triggered while we're reconfiguring. */
            for ( int i = 0; i < ndim_; i++ ) {
                colSelectors_[ i ].setSelectedItem( null );
            }
        }
        tcModel_ = tcModel; 
        tcModel_.addTopcatListener( this );

        /* Install a new subset selector component. */
        CheckBoxStack subStack = new CheckBoxStack( tcModel.getSubsets() );
        subsetScroller_.setViewportView( subStack );

        /* Reset our record of the subset selection model, keeping listeners
         * informed. */
        int oldMin = 0;
        int oldMax = 0;
        if ( subSelModel_ != null ) {
            subSelModel_.removeListSelectionListener( selectionForwarder_ );
            ListSelectionEvent evt =
                new ListSelectionEvent( subSelModel_,
                                        subSelModel_.getMinSelectionIndex(),
                                        subSelModel_.getMaxSelectionIndex(),
                                        false );
            selectionForwarder_.forwardEvent( evt );
        }
        subSelModel_ = subStack.getSelectionModel();
        subSelModel_.addListSelectionListener( selectionForwarder_ );

        /* Initialise the subset selection. */
        setDefaultSubsetSelection();

        /* Configure the column selectors. */
        for ( int i = 0; i < ndim_; i++ ) {
            colSelectors_[ i ].setModel(
                new RestrictedColumnComboBoxModel( tcModel.getColumnModel(),
                                                   true ) {
                    public boolean acceptColumn( ColumnInfo cinfo ) {
                        return PointSelector.this.acceptColumn( cinfo );
                    }
                }
            );
            colSelectors_[ i ].setEnabled( true );
        }

        /* Repaint. */
        revalidate();
        repaint();
    }

    /**
     * Sets the default selection for the subset selection component
     * based on the state of the current TopcatModel.
     */
    private void setDefaultSubsetSelection() {
        RowSubset currentSet = tcModel_.getSelectedSubset();
        OptionsListModel subsets = tcModel_.getSubsets();
        subSelModel_.setValueIsAdjusting( true );
        subSelModel_.clearSelection();
        int nrsets = subsets.size();
        for ( int i = 0; i < nrsets; i++ ) {
            if ( subsets.get( i ) == currentSet ) {
                subSelModel_.addSelectionInterval( i, i );
            }
        }
        subSelModel_.setValueIsAdjusting( false );
    }

    /*
     * TopcatListener implementation.
     */
    public void modelChanged( TopcatEvent evt ) {
        assert evt.getModel() == tcModel_;
        int code = evt.getCode();
        if ( code == TopcatEvent.SUBSET ) {
            setDefaultSubsetSelection();
        }

        /* Forward the event to other listeners. */
        for ( Iterator it = topcatListeners_.iterator(); it.hasNext(); ) {
            ((TopcatListener) it.next()).modelChanged( evt );
        }
    }

    /**
     * ListSelectionListener implementation which forwards events to 
     * client listeners.
     */
    private class SelectionForwarder implements ListSelectionListener {
        final List listeners_ = new ArrayList();
        void add( ListSelectionListener listener ) {
            listeners_.add( listener );
        }
        void remove( ListSelectionListener listener ) {
            listeners_.remove( listener );
        }
        public void valueChanged( ListSelectionEvent evt ) {
            forwardEvent( evt );
        }
        void forwardEvent( ListSelectionEvent evt ) {
            ListSelectionEvent fEvt =
                new ListSelectionEvent( PointSelector.this, evt.getFirstIndex(),
                                        evt.getLastIndex(),
                                        evt.getValueIsAdjusting() );
            for ( Iterator it = listeners_.iterator(); it.hasNext(); ) {
                ((ListSelectionListener) it.next()).valueChanged( fEvt );
            }
        }
    }
}
