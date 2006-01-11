package uk.ac.starlink.topcat.plot;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.CheckBoxStack;
import uk.ac.starlink.topcat.OptionsListModel;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBoxModel;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Abstract component for choosing a table, a number of columns, 
 * and a selection of row subsets.  The details of the column selection
 * are left to concrete subclasses.
 *
 * @author   Mark Taylor
 * @since    28 Oct 2005
 */
public abstract class PointSelector extends JPanel implements TopcatListener {

    private final JComboBox tableSelector_;
    private final JScrollPane subsetScroller_;
    private final OrderedSelectionRecorder subSelRecorder_;
    private final SelectionForwarder selectionForwarder_;
    private final ListSelectionListener listActioner_;
    private final List topcatListeners_;
    private final JPanel colPanel_;
    private final Map subsetLabels_;
    protected final ActionForwarder actionForwarder_;
    private boolean initialised_;
    private MutableStyleSet styles_;
    private TopcatModel tcModel_;
    private ListSelectionModel subSelModel_;
    private StyleAnnotator annotator_;
    private StyleWindow styleWindow_;
    private String selectorLabel_;

    private static final int ICON_SIZE = 11;

    /**
     * Constructor.
     */
    public PointSelector() {
        super( new BorderLayout() );

        /* Set up a map of labels for the subsets controlled by this selector.
         * Its keys are Integers (giving the subset index) and its values
         * are label strings.  A default label is used for subsets with
         * no entry. */
        subsetLabels_ = new HashMap();

        /* Set up some listeners. */
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

        /* Construct and place the main box for input. */
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

        /* Place the table selection box. */
        JComponent tPanel = Box.createHorizontalBox();
        tPanel.add( new JLabel( " Table: " ) );
        tPanel.add( new ShrinkWrapper( tableSelector_ ) );
        tPanel.add( Box.createHorizontalGlue() );
        entryBox.add( tPanel );
 
        /* Place the panel which will hold the column selector boxes.
         * This will be filled later by calling an abstract method. */
        colPanel_ = new JPanel();
        colPanel_.setLayout( new BoxLayout( colPanel_, BoxLayout.X_AXIS ) );
        entryBox.add( colPanel_ );
        entryBox.add( Box.createVerticalGlue() );

        /* Make a container for the subset selector. */
        subsetScroller_ = new JScrollPane( new CheckBoxStack() ) {
            public Dimension getPreferredSize() {
                return new Dimension( super.getPreferredSize().width,
                                      entryBox.getPreferredSize().height );
            }
        };
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

        /* Initialise the table, which may be necessary to initialise the
         * state properly via listeners. */
        TopcatModel tcModel =
            (TopcatModel) tableSelector_.getSelectedItem();
        if ( tcModel != null ) {
            configureForTable( tcModel );
        }
    }

    /**
     * Returns the panel which contains column selectors and any other
     * UI compoenents that the concrete subclass wants to place.
     *
     * @return   column selector panel
     */
    protected abstract JComponent getColumnSelectorPanel();

    /**
     * Returns the number of columns in the table that {@link #getData} will 
     * return;
     * 
     * @return  dimensionality
     */
    public abstract int getNdim();

    /**
     * Indicates whether this selector has enough state filled in to be
     * able to specify some point data.
     *
     * @return   true iff properly filled in
     */
    public abstract boolean isValid();

    /**
     * Set up column selectors correctly for the given model.
     * This will involve setting the column selector models appropriately.
     * If the submitted table is null, then the selector models should be
     * unselected.
     *
     * @param  tcModel   table for which selectors must be configured
     */
    protected abstract void configureSelectors( TopcatModel tcModel );

    /**
     * Hint to set up the values of the column selectors to a 
     * sensible value.  An implementation which does nothing is legal.
     */
    protected abstract void initialiseSelectors();

    /**
     * Returns a StarTable which corresponds to the data in the columns
     * selected by the current selections on this object.
     *
     * <p>Note: for performance reasons, it is <em>imperative</em> that
     * two tables returned from this method must match according to the
     * {@link java.lang.Object#equals} method if they are known to 
     * contain the same cell data (i.e. if the state of this selector
     * has not changed in the mean time).  Don't forget to do 
     * <code>hashCode</code> too.
     *
     * @return   table containing the data from the current selection
     */
    public abstract StarTable getData();

    public void setVisible( boolean visible ) {
        if ( visible ) {
            revalidate();
            repaint();
        }
        if ( ! initialised_ ) {
            colPanel_.add( getColumnSelectorPanel() );
            initialised_ = true;
        }
        super.setVisible( visible );
    }

    /**
     * Sets the style window associated with this selector.
     * This should be called soon after construction (before this selector
     * is displayed), and should not be called subsequently.
     *
     * @param  styler   style window
     */
    public void setStyleWindow( StyleWindow styler ) {
        styleWindow_ = styler;
    }

    /**
     * Returns this selector's style window.
     *
     * @return  style window
     */
    public StyleWindow getStyleWindow() {
        return styleWindow_;
    }

    /**
     * Sets a label for this selector.  This should be a short string;
     * it's used to disambiguate subsets from those controlled by other
     * selectors.
     *
     * @param   label   label string
     */
    public void setLabel( String label ) {
        selectorLabel_ = label;
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
            initialiseSelectors();
        }
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
     * Returns the style to use for a given subset index.
     *
     * @param  isub  subset index
     * @return   subset style
     */
    public Style getStyle( int isub ) {
        return styles_.getStyle( isub );
    }

    /**
     * Resets a single style.
     *
     * @param  isub  subset index 
     * @param  style new style
     * @param  label new subset name
     */
    private void resetStyle( int isub, Style style, String label ) {
        styles_.setStyle( isub, style );
        subsetLabels_.put( new Integer( isub ), label );
        annotator_.setStyleIcon( isub, style );
        actionForwarder_
            .actionPerformed( new ActionEvent( this, 0, "Style change" ) );
    }

    /**
     * Returns the style set used by this selector.
     *
     * @return  style set
     */
    public StyleSet getStyles() {
        return styles_;
    }

    /**
     * Resets the style set to be used by this selector.
     *
     * @param  styles   new style set
     */
    public void setStyles( MutableStyleSet styles ) {
        styles_ = styles;
        if ( annotator_ != null ) {
            annotator_.resetStyles( styles_ );
        }
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
            configureSelectors( null );
        }
        tcModel_ = tcModel; 
        tcModel_.addTopcatListener( this );

        /* Hide the style editor if visible, to avoid problems with it
         * having the wrong idea of what table it refers to. */
        getStyleWindow().dispose();

        /* Install a new subset selector component. */
        OptionsListModel subsets = tcModel.getSubsets();
        ListSelectionModel selModel = new DefaultListSelectionModel();
        annotator_ = new StyleAnnotator( subsets, selModel );
        CheckBoxStack subStack = new CheckBoxStack( subsets, annotator_ );
        subStack.setSelectionModel( selModel );
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
        configureSelectors( tcModel );

        /* Repaint. */
        revalidate();
        repaint();
    }

    /**
     * Invoked when the user wants to edit the style of one of the subsets
     * controlled by this selector.
     *
     * @param  index  index of the subset to edit
     */
    private void editStyle( final int index ) {
        final StyleWindow styler = getStyleWindow();
        TopcatModel tcModel = getTable();
        RowSubset rset = (RowSubset) tcModel.getSubsets().get( index );
        final StyleAnnotator annotator = annotator_;
        final StyleEditor editor = styler.getEditor();
        styler.setTarget( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( annotator_ == annotator ) {
                    resetStyle( index, editor.getStyle(), editor.getLabel() );
                }
            }
        } );
        editor.setState( getStyle( index ), getSubsetLabel( index ) );
        editor.setSetId( new SetId( this, index ) );
        styler.show();
    }

    /**
     * Returns the label which is to be used in a plot for annotating one
     * of the subsets controlled by this selector.
     *
     * @param   isub  subset index
     * @return  subset label
     */
    public String getSubsetLabel( int isub ) {
        String label = (String) subsetLabels_.get( new Integer( isub ) );
        if ( label == null ) {
            label = ((RowSubset) getTable().getSubsets().get( isub )).getName();
            if ( selectorLabel_ != null ) {
                label = selectorLabel_ + "." + label;
            }
        }
        return label;
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
     * Defines how to draw the annotating buttons which accompany each
     * subset in the subset checkbox stack.
     */
    private class StyleAnnotator implements CheckBoxStack.Annotator,
                                            ListSelectionListener {
        private final List list_;
        private final ListSelectionModel selModel_;
        private final Map actions_;
        private final Icon BLANK_ICON =
            Styles.getLegendIcon( null, ICON_SIZE, ICON_SIZE );

        /**
         * Constructor.
         *
         * @param  list  list of subsets to be annotated
         * @param  selModel  selection model describing which subsets
         *                   are currently selected
         */
        StyleAnnotator( List list, ListSelectionModel selModel ) {
            list_ = list;
            selModel_ = selModel;
            actions_ = new HashMap();
            selModel_.addListSelectionListener( this );
        }

        public Component createAnnotation( Object item ) {
            int ix = list_.indexOf( item );
            if ( ix >= 0 ) {
                JButton butt = new JButton( getAction( ix ) );
                butt.setMargin( new Insets( 0, 0, 0, 0 ) );
                return butt;
            }
            else {
                return null;
            }
        }

        /**
         * For any style annotated by this object which has been 
         * assigned a non-blank style, it's reset in accordance 
         * with a new StyleSet.
         *
         * @param  styles   new styleset 
         */
        void resetStyles( StyleSet styles ) {
            for ( Iterator it = actions_.keySet().iterator(); it.hasNext(); ) {
                int index = ((Integer) it.next()).intValue();
                clearStyle( index );
            }
            valueChanged( null );
        }

        /**
         * Returns the action associated with the annotation button.
         * This has an icon which displays the current style (if any 
         * has been assigned) and an action which allows the user to
         * change it.
         * 
         * @param  index  index into the list of the subset
         * @return  action relating to entry <code>index</code>
         */
        private Action getAction( final int index ) {
            Integer key = new Integer( index );
            if ( ! actions_.containsKey( key ) ) {
                Action act = new BasicAction( null, BLANK_ICON,
                                              "Edit style for subset " +
                                              list_.get( index ) ) {
                    public void actionPerformed( ActionEvent evt ) {
                        editStyle( index );
                    }
                };
                actions_.put( key, act );
            }
            return (Action) actions_.get( key );
        }

        /**
         * ListSelectionListener implementation.  This keeps track of which
         * items in the list have ever been selected.  The ones that have
         * get an annotation and the others don't.  The reason for this is
         * that we don't want to use up all the styles on subsets which 
         * never get plotted.
         */
        public void valueChanged( ListSelectionEvent evt ) {
            for ( int i = selModel_.getMinSelectionIndex();
                  i <= selModel_.getMaxSelectionIndex(); i++ ) {
                if ( selModel_.isSelectedIndex( i ) && ! hasStyle( i ) ) {
                    setStyleIcon( i, getStyle( i ) );
                }
            }
        }

        /**
         * Sets the icon on the action button for a given index.
         *
         * @param   index  index of item to change
         * @param   style  style which has icon
         */
        void setStyleIcon( int index, Style style ) {
            Icon icon = Styles.getLegendIcon( style, ICON_SIZE, ICON_SIZE );
            getAction( index ).putValue( Action.SMALL_ICON, icon );
        }

        /**
         * Determines whether a list item has had a style assigned to it
         * before or not.
         *
         * @param  index  item index
         * @return  true iff the item has ever had a style 
         */
        private boolean hasStyle( int index ) {
            Integer key = new Integer( index );
            return actions_.containsKey( key ) 
                && ((Action) actions_.get( key )).getValue( Action.SMALL_ICON )
                   != BLANK_ICON;
        }

        /**
         * Resets the style to null for a given index.
         *
         * @param  index  index to clear
         */
        private void clearStyle( int index ) {
            Integer key = new Integer( index );
            if ( actions_.containsKey( key ) ) {
                ((Action) actions_.get( key )).putValue( Action.SMALL_ICON,
                                                         BLANK_ICON );
            }
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
