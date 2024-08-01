package uk.ac.starlink.topcat;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import uk.ac.starlink.table.ColumnData;

/**
 * Panel for displaying the results of a classification of column contents.
 * It can turn these results into an array of RowSubsets, based on
 * user interaction including selection of particular categories and
 * subset name assignment.
 *
 * @author   Mark Taylor
 * @since    3 Feb 2015
 */
public class ClassifyReportPanel extends JPanel {

    private final GridBagLayout gridder_;
    private ColumnData cdata_;
    private String prefix_;
    private int ncat_;
    private Item[] items_;

    /** Maximum length of default subset name value identification string. */
    private static final int MAXLEN_VALSTR = 16;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public ClassifyReportPanel() {
        gridder_ = new GridBagLayout();
        setLayout( gridder_ );
        prefix_ = "";
        ncat_ = 0;
        setItems( new Item[ 0 ] );
    }

    /**
     * Sets the maximum number of categories that will be displayed.
     * The most populous ones will be displayed first.
     * Calling this method affects the way that setData is carried out,
     * it does not affect the current display.
     *
     * @param  ncat  maximum number of categories
     */
    public void setMaxCount( int ncat ) {
        ncat_ = ncat;
    }

    /**
     * Sets the standard prefix used for subset names.
     * Calling this method affects the way that setData is carried out,
     * it does not affect the current display.
     *
     * @param  prefix  new prefix
     */
    public void setPrefix( String prefix ) {
        prefix_ = prefix;
    }

    /**
     * Sets the classification data for display by this panel,
     * and updates the display according to the current state.
     * If the parameters are null, the display will be cleared.
     *
     * @param  cdata  column data used for classification
     * @param  classifier   classification results
     */
    public void setData( ColumnData cdata, Classifier<?> classifier ) {
        cdata_ = cdata;
        final List<Item> itemList = new ArrayList<Item>();
        if ( classifier != null && cdata != null ) {

            /* Arrange that de/selection of the category rows will recalculate
             * the item count corresoponding to None Of The Above. */
            final Item otherItem = new Item( null, prefix_ );
            final long nrow = classifier.getItemCount();
            ActionListener flagListener = new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    long nother = nrow;
                    for ( Item item : itemList ) {
                        if ( item.cval_ != null &&
                             item.flagBox_.isSelected() ) {
                            nother -= item.cval_.getCount();
                        }
                    }
                    otherItem.countLabel_.setText( Long.toString( nother ) );
                }
            };

            /* Add a new item for each category. */
            for ( Classifier.CountedValue<?> cv :
                  classifier.getTopValues( ncat_ ) ) {
                Item item = new Item( cv, prefix_ );
                item.flagBox_.addActionListener( flagListener );
                itemList.add( item );
            }

            /* If the number of categories is not zero, add an extra item
             * corresponding to None Of The Above. */
            if ( itemList.size() > 0 ) {
                itemList.add( otherItem );
            }

            /* Make sure that all the subset names are unique. */
            Set<String> nameSet = new HashSet<String>();
            for ( Item item : itemList ) {
                nameSet.add( item.txtField_.getText() );
            }
            if ( nameSet.size() != itemList.size() ) {
                int i = 0;
                for ( Item item : itemList ) {
                    if ( item != otherItem ) {
                        item.txtField_.setText( prefix_ + ( ++i ) );
                    }
                }
            }

            /* Initialise state. */
            flagListener.actionPerformed( null );
        }

        /* Update the GUI. */
        setItems( itemList.toArray( new Item[ 0 ] ) );
    }

    /**
     * Returns the number of subsets corresponding to this component's
     * current configuration.
     *
     * @return  number of items returned by a call to <code>createSubsets</code>
     */
    public int getSubsetCount() {
        int nset = 0;
        for ( Item item : items_ ) {
            if ( item.flagBox_.isSelected() ) {
                nset++;
            }
        }
        return nset;
    }

    /**
     * Returns a list of RowSubsets corresponding to the current state
     * of this component.
     *
     * @return   row subsets
     */
    public RowSubset[] createSubsets() {
        final Set<Object> includeSet = new HashSet<Object>();
        Item otherItem = null;
        List<RowSubset> rsets = new ArrayList<RowSubset>();
        for ( Item item : items_ ) {
            if ( item.flagBox_.isSelected() ) {
                String name = item.txtField_.getText();
                if ( name != null && name.trim().length() > 0 ) {
                    Classifier.CountedValue<?> cval = item.cval_;
                    if ( cval == null ) {
                        assert otherItem == null;
                        otherItem = item;
                    }
                    else {
                        Object value = cval.getValue();
                        includeSet.add( value );
                        rsets.add( createSubset( name, value, cdata_ ) );
                    }
                }
            }
        }

        /* Add an entry for none-of-the-above if required. */
        if ( otherItem != null ) {
            String name = otherItem.txtField_.getText();
            rsets.add( new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return !includeSet.contains( cdata_.readValue( lrow ) );
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            } );
        }
        return rsets.toArray( new RowSubset[ 0 ] );
    }

    /**
     * Sets the current list of result items and displays them in the GUI.
     *
     * @param  new item list
     */
    private void setItems( Item[] items ) {
        items_ = items;
        removeAll();
        addTitle();
        int iy = 1;
        for ( Item item : items_ ) {
            addItem( item, iy++ );
        }
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = iy;
        gc.weighty = 1.0;
        gc.fill = GridBagConstraints.BOTH;
        addGridComponent( Box.createVerticalGlue(), gc );
        revalidate();
        repaint();
    }

    /**
     * Displays the title row.
     */
    private void addTitle() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.ipadx = 4;
        gc.ipady = 2;
        gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.gridx = 0;
        addGridComponent( createTitleLabel( "Count" ), gc );
        gc.gridx++;
        addGridComponent( createTitleLabel( "Value" ), gc );
        gc.gridx++;
        gc.weightx = 1;
        addGridComponent( createTitleLabel( "Subset Name" ), gc );
        gc.weightx = 0;
        gc.gridx++;
        addGridComponent( createTitleLabel( "Add Subset?" ), gc );
    }

    /**
     * Returns a label suitable for use as a column header.
     *
     * @param  txt  label text
     * @return  label component
     */
    private JComponent createTitleLabel( String txt ) {
        JComponent label = new JLabel( txt );
        label.setBorder( BorderFactory.createCompoundBorder(
                             BorderFactory.createEtchedBorder(),
                             BorderFactory.createEmptyBorder( 2, 2, 2, 2 ) ) );
        return label;
    }

    /**
     * Displays a single item.
     *
     * @param  item  result item to display
     * @param  index   index of item being displayed
     */
    private void addItem( Item item, int index ) {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets( 0, 5, 0, 5 );
        gc.gridy = index;
        gc.gridx = 0;
        gc.anchor = GridBagConstraints.EAST;
        addGridComponent( item.countLabel_, gc );
        gc.anchor = GridBagConstraints.WEST;
        gc.gridx++;
        addGridComponent( item.labelLabel_, gc );
        gc.gridx++;
        gc.weightx = 1;
        gc.insets = new Insets( 0, 0, 0, 0 );
        gc.fill = GridBagConstraints.HORIZONTAL;
        addGridComponent( item.txtField_, gc );
        gc.weightx = 0;
        gc.insets = new Insets( 0, 5, 0, 5 );
        gc.fill = GridBagConstraints.NONE;
        gc.gridx++;
        addGridComponent( item.flagBox_, gc );
    }

    /**
     * Adds a component to this container.
     *
     * @param  comp  component to add
     * @param  gc   constraints object
     */
    private void addGridComponent( Component comp, GridBagConstraints gc ) {
        gridder_.setConstraints( comp, gc );
        add( comp, gc );
    }

    /**
     * Creates a subset defined by existence of a given value in a column.
     *
     * <p>It would be nice to return SyntheticRowSubset instances here,
     * since they can be reported with their expressions in the subsets
     * window, so it's easy to see where they came from.
     * This would also be better for session serialization,
     * and for STILTS command generation.
     * But the current implementation does not do that.
     *
     * <p>The problem is that it's quite hard to come up with robust
     * JEL expressions corresponding to these classification categories
     * (serialising java equality conditions into java source code).
     * Problems include working out a suitable column name,
     * coping with null testing for potentially primitive column types,
     * and serialising the object value.  It would certainly be possible,
     * though maybe fiddly, to solve some of these problems some of the time.
     * This method acts as a placeholder in case somebody gets round
     * to doing that one day.
     *
     * @param  name  subset name
     * @param  value   value which the column must have for inclusion
     * @parma  cdata   column data
     */
    private static RowSubset createSubset( String name, final Object value,
                                           final ColumnData cdata ) {
        if ( value == null ) {
           return new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return cdata.readValue( lrow ) == null;
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            };
        }
        else {
            return new RowSubset( name ) {
                public boolean isIncluded( long lrow ) {
                    try {
                        return value.equals( cdata.readValue( lrow ) );
                    }
                    catch ( IOException e ) {
                        return false;
                    }
                }
            };
        }
    }

    /**
     * Defines a result item which will be displayed to the user and
     * which can be turned into a RowSubset.
     */
    private static class Item {

        final Classifier.CountedValue<?> cval_;
        final JLabel countLabel_;
        final JLabel labelLabel_;
        final JTextField txtField_;
        final JCheckBox flagBox_;

        /**
         * Constructor.
         *
         * @param  cval  counted value containing item data
         * @param  prefix   default prefix for subset name
         */
        Item( Classifier.CountedValue<?> cval, String prefix ) {
            cval_ = cval;
            boolean isOther = cval == null;
            countLabel_ =
                new JLabel( isOther ? null
                                    : String.valueOf( cval.getCount() ) );
            String label = isOther ? "other"
                                   : String.valueOf( cval.getValue() );
            labelLabel_ = new JLabel( label );
            txtField_ = new JTextField();
            txtField_.setText( prefix +
                               ClassifyWindow.sanitiseText( label, false,
                                                            MAXLEN_VALSTR ) );
            flagBox_ = new JCheckBox();
            flagBox_.setSelected( cval != null );
            flagBox_.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent evt ) {
                    updateItemState();
                }
            } );
            updateItemState();
        }

        private void updateItemState() {
            boolean isEnabled = flagBox_.isSelected();
            countLabel_.setEnabled( isEnabled );
            labelLabel_.setEnabled( isEnabled );
            txtField_.setEnabled( isEnabled );
        }
    }
}
