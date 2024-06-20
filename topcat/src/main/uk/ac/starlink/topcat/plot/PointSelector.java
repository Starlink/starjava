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
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListSelectionModel;
import javax.swing.DefaultListModel;
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
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnStarTable;
import uk.ac.starlink.table.DefaultValueInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.AuxWindow;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.CheckBoxStack;
import uk.ac.starlink.topcat.OptionsListModel;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TablesListComboBox;
import uk.ac.starlink.topcat.TopcatEvent;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.WeakTopcatListener;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.StyleSet;
import uk.ac.starlink.util.IconUtils;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Component for choosing a table, a number of columns and a selection of
 * row subsets.  The details of the column selection are handled by 
 * a consitituent {@link AxesSelector} object.
 *
 * @author   Mark Taylor
 * @since    28 Oct 2005
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class PointSelector extends JPanel {

    private final AxesSelector axesSelector_;
    private final JComboBox tableSelector_;
    private final JScrollPane subsetScroller_;
    private final JScrollPane entryScroller_;
    private final OrderedSelectionRecorder subSelRecorder_;
    private final SelectionForwarder selectionForwarder_;
    private final List<TopcatListener> topcatListeners_;
    private final Map<Integer,SubsetFluff> subsetFluffs_;
    private final TopcatListener tcListener_;
    private final TopcatListener weakTcListener_;
    private final ActionForwarder actionForwarder_;
    private final ActionListener errorModeListener_;
    private MutableStyleSet styles_;
    private TopcatModel tcModel_;
    private ListSelectionModel subSelModel_;
    private StyleAnnotator annotator_;
    private StyleWindow styleWindow_;
    private String selectorLabel_;

    /** ActionEvent command string indicating a change of table. */
    public static final String TABLE_CHANGED = "TABLE_CHANGED";

    public PointSelector( AxesSelector axesSelector, MutableStyleSet styles ) {
        super( new BorderLayout() );
        axesSelector_ = axesSelector;
        styles_ = styles;

        /* Set up a map of labels for the subsets controlled by this selector.
         * Its keys represent subsets (as supplied by indexToKey)
         * and its values are SubsetFluff objects giving relevant
         * information about the labelling of styles.
         * A default label is used for subsets with no entry. */
        subsetFluffs_ = new HashMap<Integer,SubsetFluff>();

        /* Set up some listeners. */
        actionForwarder_ = new ActionForwarder();
        selectionForwarder_ = new SelectionForwarder();
        final ListSelectionListener listActioner = new ListSelectionListener() {
            public void valueChanged( ListSelectionEvent evt ) {
                actionForwarder_
               .actionPerformed( new ActionEvent( this, 0, "Selection" ) );
            }
        };
        errorModeListener_ = new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( annotator_ != null ) {
                    annotator_.updateStyles();
                }
            }
        };
        axesSelector_.addActionListener( actionForwarder_ );
        topcatListeners_ = new ArrayList<TopcatListener>();
        final JComponent controlBox = Box.createHorizontalBox();
        add( controlBox, BorderLayout.SOUTH );

        /* Set up a listener for TopcatEvents.  A reference is kept in this
         * object, but the listener registered with any TopcatModels only
         * contains a weak reference to it.  This prevents the TopcatModel
         * keeping alive references to this PointSelector (and hence its
         * parent GraphicsWindow, which is typically expensive on memory)
         * after the GraphicsWindow disappears. */
        tcListener_ = new TopcatListener() {
            public void modelChanged( TopcatEvent evt ) {
                assert evt.getModel() == tcModel_;
                int code = evt.getCode();

                /* If the current subset changes, fix to plot only that one. */
                if ( code == TopcatEvent.CURRENT_SUBSET ) {
                    setDefaultSubsetSelection();
                }

                /* If we get a request to highlight a given subset, make sure
                 * it's set to plot and force a replot. */
                else if ( code == TopcatEvent.SHOW_SUBSET ) {
                    RowSubset rset = (RowSubset) evt.getDatum();
                    OptionsListModel<RowSubset> subsets = tcModel_.getSubsets();
                    int nrsets = subsets.size();
                    for ( int is = 0; is < nrsets; is++ ) {
                        if ( subsets.get( is ) == rset ) {

                            /* As well as making sure that the given subset
                             * is selected, we need to force a replot since
                             * its content might have changed.  We do this
                             * here by unselecting it then selecting it.
                             * This has the desired effect, but also means
                             * that the replot gets done twice.  It would be
                             * more efficient to cause a replot directly
                             * somehow, but the required objects are not
                             * currently available from this class. */
                            subSelModel_.removeSelectionInterval( is, is );
                            subSelModel_.addSelectionInterval( is, is );
                        }
                    }
                }

                /* Forward the event to other listeners. */
                for ( TopcatListener l : topcatListeners_ ) {
                    l.modelChanged( evt );
                }
            }
        };
        weakTcListener_ = new WeakTopcatListener( tcListener_ );

        /* Construct the box for input of axes etc, and insert it into a
         * somewhat customised scroll pane. */
        final Box entryBox = new Box( BoxLayout.Y_AXIS );
        entryScroller_ = new JScrollPane( entryBox ) {

            /**
             * Validate root is false. This means that the 
             * scrollpane is allowed to fix its own size, based on the 
             * preferred size of its contents, if there is enough space
             * available.  This is the effect that we want to achieve here.
             *
             * @return  false
             */
            public boolean isValidateRoot() {
                return false;
            }

            /**
             * Fix it so that the preferred size takes account of scrollbars
             * if they may be present.  You can achieve a similar effect
             * by just setting scrollbar policies to *_SCROLLBAR_ALWAYS,
             * but that leaves an empty scrollbar when it's not necessary,
             * which is (IMHO) ugly.
             */
            public Dimension getPreferredSize() {
                Dimension size =
                    new Dimension( entryBox.getPreferredSize() );
                Insets insets = getInsets();
                size.width += insets.left + insets.right;
                size.height += insets.top + insets.bottom;
                if ( getVerticalScrollBarPolicy()
                     != VERTICAL_SCROLLBAR_NEVER ) {
                    size.width +=
                      + getVerticalScrollBar().getPreferredSize().width;
                }
                if ( getHorizontalScrollBarPolicy()
                     != HORIZONTAL_SCROLLBAR_NEVER ) {
                    size.height +=
                      + getHorizontalScrollBar().getPreferredSize().height;
                }
                return size;
            }
        };

        /* Install horizontal scrollbar only by default. */
        setHorizontalEntryScrolling( true );
        setVerticalEntryScrolling( false );

        /* Decorate and place entry box. */
        entryScroller_.setBorder( AuxWindow.makeTitledBorder( "Data" ) );
        controlBox.add( entryScroller_ );
        controlBox.add( Box.createHorizontalStrut( 5 ) );

        /* Prepare a selection box for the table. */
        tableSelector_ = new TablesListComboBox( 0 );
        tableSelector_.addItemListener( new ItemListener() {
            public void itemStateChanged( ItemEvent evt ) {
                TopcatModel tcModel =
                    (TopcatModel) tableSelector_.getSelectedItem();
                if ( tcModel == null ) {
                    tcModel = TopcatModel.createDummyModel();
                }
                configureForTable( tcModel );
            }
        } );
        tableSelector_.addActionListener( actionForwarder_ );

        /* Place the table selection box. */
        JComponent tPanel = Box.createHorizontalBox();
        tPanel.add( new JLabel( " Table: " ) );
        tPanel.add( new ShrinkWrapper( tableSelector_ ) );
        tPanel.add( Box.createHorizontalGlue() );
        entryBox.add( tPanel );

        /* Place the panel which will hold the column selector boxes. */
        JComponent colPanel = new JPanel();
        colPanel.setLayout( new BoxLayout( colPanel, BoxLayout.X_AXIS ) );
        colPanel.add( axesSelector_.getColumnSelectorPanel() );
        entryBox.add( colPanel );
        entryBox.add( Box.createVerticalGlue() );

        /* Make a container for the subset selector. */
        subsetScroller_ = new JScrollPane( new CheckBoxStack() ) {
            public Dimension getPreferredSize() {
                return new Dimension( super.getPreferredSize().width,
                                      entryScroller_.getPreferredSize()
                                                    .height );
            }
            public Dimension getMaximumSize() {
                return getPreferredSize();
            }
            public Dimension getMinimumSize() {
                return getPreferredSize();
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
        selectionForwarder_.add( listActioner );

        /* Initialise the table, which may be necessary to initialise the
         * state properly via listeners. */
        TopcatModel tcModel =
            (TopcatModel) tableSelector_.getSelectedItem();
        if ( tcModel != null ) {
            configureForTable( tcModel );
        }
    }

    /**
     * Returns the AxesSelector used by this PointSelector.
     *
     * @return  axes selector
     */
    public AxesSelector getAxesSelector() {
        return axesSelector_;
    }

    /** 
     * Indicates whether this selector has enough state filled in to be
     * able to specify some point data.
     * 
     * @return   true iff properly filled in
     */
    public boolean isReady() {
        return axesSelector_.isReady();
    }

    /**
     * Determines whether the component containing the column selectors
     * will scroll horizontally if required or not.
     *
     * @param  isScroll   true iff the entry box should scroll horizontally
     */
    public void setHorizontalEntryScrolling( boolean isScroll ) {
        int policy = isScroll ? JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
                              : JScrollPane.HORIZONTAL_SCROLLBAR_NEVER;
        entryScroller_.setHorizontalScrollBarPolicy( policy );
    }

    /**
     * Determines whether the component containing the column selectors
     * will scroll vertically if required or not.
     *
     * @param  isScroll  true iff the entry box should scroll vertically
     */
    public void setVerticalEntryScrolling( boolean isScroll ) {
        int policy = isScroll ? JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                              : JScrollPane.VERTICAL_SCROLLBAR_NEVER;
        entryScroller_.setVerticalScrollBarPolicy( policy );
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
     * Returns the label for this selector.
     *
     * @return  selector label
     */
    public String getLabel() {
        return selectorLabel_;
    }

    /**
     * Returns the currently selected table.
     *
     * @return  topcat model of the currently selected table
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

        /* Set the table selector accordingly.  This will cause the 
         * axes selector to be set as well. */
        tableSelector_.setSelectedItem( tcModel );

        /* Initialise the column selectors.  This isn't necesary, but it's
         * what the old plot window used to do so may confuse people less.
         * Possibly change this action in the future though. */
        if ( init ) {
            axesSelector_.initialiseSelectors();
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
        return styles_.getStyle( indexToId( isub ) );
    }

    /**
     * Resets a single style.
     *
     * @param  isub  subset index
     * @param  style new style
     * @param  label new subset name
     * @param  hidden  true iff the representation of this style is to be
     *         excluded from plot legends
     */
    private void resetStyle( int isub, Style style, String label,
                             boolean hidden ) {
        styles_.setStyle( indexToId( isub ), style );
        subsetFluffs_.put( indexToKey( isub ),
                           new SubsetFluff( label, hidden ) );
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

            /* Hide the style editor if it's visible so it doesn't display
             * the wrong thing.  This is a bit lazy - probably ought to ensure
             * that it's displaying the right thing instead. */
            StyleWindow swin = getStyleWindow();
            if ( swin != null ) {
                swin.dispose();
            }
        }
    }

    /**
     * Returns the icon used to represent a given style in legends for this
     * selector.
     *
     * @param  style  style to represent
     * @return  icon
     */
    public Icon getStyleLegendIcon( Style style ) {
        return style instanceof MarkStyle
            ? ((MarkStyle) style).getLegendIcon( axesSelector_.getErrorModes() )
            : style.getLegendIcon();
    }

    /**
     * Returns a listener which should be informed every time the error
     * mode changes.
     *
     * @return  listener
     */
    public ActionListener getErrorModeListener() {
        return errorModeListener_;
    }

    /**
     * Adds an action listener.  It will be notified every time something
     * interesting happens to this selector.
     *
     * @param  listener   listener to add
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes an action listener which was previously added.
     *
     * @param   listener  listener to remove
     * @see  #addActionListener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
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
    public void configureForTable( TopcatModel tcModel ) {

        /* Reset our record of the topcat model, ensuring that we continue
         * to listen to the right one. */
        if ( tcModel_ != null ) {
            tcModel_.removeTopcatListener( weakTcListener_ );

            /* Reset column selector models to prevent unhelpful events being
             * triggered while we're reconfiguring. */
            axesSelector_.setTable( null );
        }
        tcModel_ = tcModel;
        tcModel_.addTopcatListener( weakTcListener_ );

        /* Hide the style editor if visible, to avoid problems with it
         * having the wrong idea of what table it refers to. */
        StyleWindow styler = getStyleWindow();
        if ( styler != null ) {
            styler.dispose();
        }

        /* Install a new subset selector component. */
        OptionsListModel<RowSubset> subsets = tcModel.getSubsets();
        DefaultListSelectionModel selModel = new DefaultListSelectionModel();
        annotator_ = new StyleAnnotator( subsets, selModel );
        CheckBoxStack subStack = new CheckBoxStack( subsets, annotator_ );
        subStack.setSelectionModel( selModel );
        Component oldStack = subsetScroller_.getViewport().getView();
        if ( oldStack instanceof CheckBoxStack ) {
            ((CheckBoxStack) oldStack).setListModel( new DefaultListModel() );
        }
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
        axesSelector_.setTable( tcModel );

        /* Notify listeners. */
        actionForwarder_.actionPerformed( new ActionEvent( this, 0,
                                                           TABLE_CHANGED ) );

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
        final StyleAnnotator annotator = annotator_;
        final StyleEditor editor = styler.getEditor();
        styler.setTarget( new ActionListener() {
            public void actionPerformed( ActionEvent evt ) {
                if ( annotator_ == annotator ) {
                    resetStyle( index, editor.getStyle(), editor.getLabel(),
                                editor.getHideLegend() );
                }
            }
        } );
        editor.setState( getStyle( index ), getSubsetLabel( index ),
                         getSubsetHidden( index ) );
        editor.setSetId( new SetId( this, index ) );
        styler.setVisible( true );
    }

    /**
     * Translates from subset index (which may change for a given subset)
     * to subset ID, which is fixed for a given subset.
     *
     * @param  isub  current index of subset
     * @return  subset id
     */
    private int indexToId( int isub ) {
        return tcModel_ == null
             ? -1
             : tcModel_.getSubsets().indexToId( isub );
    }

    /**
     * Generates an object which can serve as a unique, unchanging 
     * key for a subset.
     *
     * @param  isub  current index of subset
     * @return   unique object representing subset
     */
    private Integer indexToKey( int isub ) {
        return tcModel_ == null
             ? null
             : Integer.valueOf( tcModel_.getSubsets().indexToId( isub ) );
    }

    /**
     * Determines the current index in the subsets list of the subset
     * with a given key.
     *
     * @param  key  subset key obtained from {@link #indexToKey}
     * @return  current index of subset
     */
    private int keyToIndex( Object key ) {
        if ( key == null || tcModel_ == null ) {
            return -1;
        }
        else {
            int id = ((Integer) key).intValue();
            return tcModel_.getSubsets().idToIndex( id );
        }
    }

    /**
     * Returns the label which is to be used in a plot for annotating one
     * of the subsets controlled by this selector.
     *
     * @param   isub  subset index
     * @return  subset label
     */
    private String getSubsetLabel( int isub ) {
        SubsetFluff fluff = subsetFluffs_.get( indexToKey( isub ) );
        String label = fluff == null ? null : fluff.label_;
        if ( label == null ) {
            label = getTable().getSubsets().get( isub ).getName();
            if ( selectorLabel_ != null ) {
                label = selectorLabel_ + "." + label;
            }
        }
        return label;
    }

    /**
     * Returns the subset label to be used for annotating one of the subsets
     * controlled by this selector in a plot legend.  If the return value
     * is the empty string then the subset should be excluded from the legend.
     *
     * @param   isub  subset index
     * @return  label, or null for hidden
     */
    public String getSubsetDisplayLabel( int isub ) {
        return getSubsetHidden( isub )
             ? ""
             : getSubsetLabel( isub );
    }

    /**
     * Indicates whether one of the subsets controlled by this selector
     * should be excluded from the plot legend.
     *
     * @param   isub  subset index
     * @return   true to hide the subset
     */
    private boolean getSubsetHidden( int isub ) {
        SubsetFluff fluff = subsetFluffs_.get( indexToKey( isub ) );
        return fluff != null && fluff.hidden_;
    }

    /**
     * Sets the default selection for the subset selection component
     * based on the state of the current TopcatModel.
     */
    private void setDefaultSubsetSelection() {
        RowSubset currentSet = tcModel_.getSelectedSubset();
        OptionsListModel<RowSubset> subsets = tcModel_.getSubsets();
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

    /**
     * Struct-type utility class which aggregates a subset name and a flag
     * indicating whether it should be hidden from the plot legend.
     */
    private static class SubsetFluff {

        final String label_;
        final boolean hidden_;

        /**
         * Constructor.
         *
         * @param  label   subset label
         * @param  hidden   true to hide label
         */
        SubsetFluff( String label, boolean hidden ) {
            label_ = label;
            hidden_ = hidden;
        }
    }

    /**
     * Defines how to draw the annotating buttons which accompany each
     * subset in the subset checkbox stack.
     */
    private class StyleAnnotator implements CheckBoxStack.Annotator,
                                            ListSelectionListener {
        private final List<RowSubset> list_;
        private final ListSelectionModel selModel_;
        private final Map<Integer,Action> actions_;
        private final Icon blankIcon_;

        /**
         * Constructor.
         *
         * @param  list  list of subsets to be annotated
         * @param  selModel  selection model describing which subsets
         *                   are currently selected
         */
        StyleAnnotator( List<RowSubset> list, ListSelectionModel selModel ) {
            list_ = list;
            selModel_ = selModel;
            actions_ = new HashMap<Integer,Action>();
            selModel_.addListSelectionListener( this );
            Icon sampleIcon = getStyle( 0 ).getLegendIcon();
            blankIcon_ = IconUtils.emptyIcon( sampleIcon.getIconWidth(),
                                              sampleIcon.getIconHeight() );
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
            for ( Integer key : actions_.keySet() ) {
                clearStyle( key.intValue() );
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
        private Action getAction( int index ) {
            final Integer key = indexToKey( index );
            if ( ! actions_.containsKey( key ) ) {
                Action act = new BasicAction( null, blankIcon_,
                                              "Edit style for subset " +
                                              list_.get( index ) ) {
                    public void actionPerformed( ActionEvent evt ) {
                        editStyle( keyToIndex( key ) );
                    }
                };
                actions_.put( key, act );
            }
            return actions_.get( key );
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
         * Resets the icons for each style.  This may be required if something
         * has happened to change the way that icons are displayed.
         */
        public void updateStyles() {
            for ( int i = 0; i < list_.size(); i++ ) {
                if ( hasStyle( i ) ) {
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
            getAction( index ).putValue( Action.SMALL_ICON,
                                         getStyleLegendIcon( style ) );
        }

        /**
         * Determines whether a list item has had a style assigned to it
         * before or not.
         *
         * @param  index  item index
         * @return  true iff the item has ever had a style
         */
        private boolean hasStyle( int index ) {
            Integer key = indexToKey( index );
            return actions_.containsKey( key )
                && actions_.get( key ).getValue( Action.SMALL_ICON )
                   != blankIcon_;
        }

        /**
         * Resets the style to null for a given index.
         *
         * @param  index  index to clear
         */
        private void clearStyle( int index ) {
            Object key = indexToKey( index );
            if ( actions_.containsKey( key ) ) {
                actions_.get( key ).putValue( Action.SMALL_ICON, blankIcon_ );
            }
        }
    }

    /**
     * ListSelectionListener implementation which forwards events to
     * client listeners.
     */
    private class SelectionForwarder implements ListSelectionListener {
        final List<ListSelectionListener> listeners_ =
            new ArrayList<ListSelectionListener>();
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
            for ( ListSelectionListener l : listeners_ ) {
                l.valueChanged( fEvt );
            }
        }
    }
}
