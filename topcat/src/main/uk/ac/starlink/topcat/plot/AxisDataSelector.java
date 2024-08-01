package uk.ac.starlink.topcat.plot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnDataComboBox;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ToggleButtonModel;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.ErrorMode;
import uk.ac.starlink.util.gui.ComboBoxBumper;
import uk.ac.starlink.util.gui.ShrinkWrapper;

/**
 * Graphical component allowing selection of one or more columns 
 * required for plotting data along one axis of a graph.  
 * The principal column is the one which defines the centre of the
 * plotted points along the axis in question, but additional
 * ones may be required according to whether error bars will be drawn.
 *
 * @author   Mark Taylor
 * @since    23 Feb 2007
 */
@SuppressWarnings({"unchecked","rawtypes"})
public class AxisDataSelector extends JPanel {

    private final JComponent extrasBox_;
    private final JComboBox atSelector_;
    private final JComboBox loSelector_;
    private final JComboBox hiSelector_;
    private final JComboBox lhSelector_;
    private final Map<ErrorMode.Extent,JComboBox> extentMap_;
    private final ActionForwarder actionForwarder_;
    private ErrorMode errorMode_ = ErrorMode.NONE;

    private static final ColumnSelectionTracker colSelectionTracker_ =
        new ColumnSelectionTracker();
    private static final Pattern ERR_NAME_REGEX = 
        Pattern.compile( "[\\-\\._ ]*" +
                         "(err|error|sig|sigma|sd|stdev|st.dev)" +
                         "[\\-\\._ ]*",
                         Pattern.CASE_INSENSITIVE );
    private static final Pattern ERR_UCD_REGEX =
        Pattern.compile( ".*" +
                         "(stat\\.error|stat\\.stdev|meta\\.code\\.error)" +
                         ".*",
                         Pattern.CASE_INSENSITIVE );

    /** Error type denoting the lower bound error. */
    private static final ErrorType LO = new ErrorType( "Lower" ) {
        public JComboBox getSelector( AxisDataSelector axSel ) {
            return axSel.loSelector_;
        }
    };

    /** Error type denoting the upper bound error. */
    private static final ErrorType HI = new ErrorType( "Upper" ) {
        public JComboBox getSelector( AxisDataSelector axSel ) {
            return axSel.hiSelector_;
        }
    };

    /** Error type denoting the symmetric lower/upper bound error. */
    private static final ErrorType LH = new ErrorType( "LowerUpper" ) {
        public JComboBox getSelector( AxisDataSelector axSel ) {
            return axSel.lhSelector_;
        }
    };

    /**
     * Constructor.
     *
     * @param  axisName   name of the axis, used for user labels
     * @param  toggleNames  names of an array of toggle buttons to be
     *                      displayed in this component
     * @param  toggleModels toggle button models to be displayed in this
     *                      component (same length as <code>toggleNames</code>)
     */
    @SuppressWarnings("this-escape")
    public AxisDataSelector( String axisName, String[] toggleNames,
                             ToggleButtonModel[] toggleModels ) {
        setLayout( new BoxLayout( this, BoxLayout.X_AXIS ) );
        actionForwarder_ = new ActionForwarder();

        /* Construct all the column selectors which may be needed
         * (though possibly some will never be used). */
        atSelector_ = new ColumnDataComboBox();
        loSelector_ = new ColumnDataComboBox();
        hiSelector_ = new ColumnDataComboBox();
        lhSelector_ = new ColumnDataComboBox();
        JComboBox[] selectors = getSelectors();
        assert selectors.length == 4;
        for ( int i = 0; i < selectors.length; i++ ) {
            selectors[ i ].addActionListener( actionForwarder_ );
        }
        atSelector_.addActionListener( new ColumnSelectionListener( null ) );
        loSelector_.addActionListener( new ColumnSelectionListener( LO ) );
        hiSelector_.addActionListener( new ColumnSelectionListener( HI ) );
        lhSelector_.addActionListener( new ColumnSelectionListener( LH ) );

        /* Cache information about which selectors represent which extents. */
        extentMap_ = new HashMap<ErrorMode.Extent,JComboBox>();
        extentMap_.put( ErrorMode.LOWER_EXTENT, loSelector_ );
        extentMap_.put( ErrorMode.UPPER_EXTENT, hiSelector_ );
        extentMap_.put( ErrorMode.BOTH_EXTENT, lhSelector_ );

        /* Place the main column selector. */
        placeSelector( this, axisName + " Axis:", atSelector_ );
        int ntog = toggleNames != null ? toggleNames.length : 0;

        /* Prepare a container to contain the auxiliary column selectors. */
        extrasBox_ = Box.createHorizontalBox();
        add( extrasBox_ );

        /* Place the toggle buttons. */
        for ( int itog = 0; itog < ntog; itog++ ) {
            String name = toggleNames[ itog ];
            ToggleButtonModel model = toggleModels[ itog ];
            if ( name != null && model != null ) {
                JCheckBox checkBox = model.createCheckBox();
                checkBox.setText( name );
                add( Box.createHorizontalStrut( 5 ) );
                add( checkBox );
            }
        }

        /* Pad. */
        add( Box.createHorizontalGlue() );
    }

    /**
     * Adds an action listener.
     *
     * @param   listener  action listener
     */
    public void addActionListener( ActionListener listener ) {
        actionForwarder_.addActionListener( listener );
    }

    /**
     * Removes an action listener.
     *
     * @param  listener  action listener
     */
    public void removeActionListener( ActionListener listener ) {
        actionForwarder_.removeActionListener( listener );
    }

    public void setEnabled( boolean enabled ) {
        super.setEnabled( enabled );
        JComponent[] selectors = getSelectors();
        for ( int i = 0; i < selectors.length; i++ ) {
            selectors[ i ].setEnabled( enabled );
        }
    }

    /**
     * Returns the main column selector associated with this selector.
     * This is the one which defines the centre of the plotted points.
     *
     * @return  principal column selector
     */
    public JComboBox getMainSelector() {
        return atSelector_;
    }

    /**
     * Returns the currently displayed column selectors which provide
     * error information.  Which these are will depend on the current 
     * error mode.
     *
     * @return   displayed selectors apart from the main one
     */
    public JComboBox[] getErrorSelectors() {
        ErrorMode.Extent[] extents = errorMode_.getExtents();
        int nex = extents.length;
        JComboBox[] selectors = new JComboBox[ nex ];
        for ( int i = 0; i < nex; i++ ) {
            selectors[ i ] = extentMap_.get( extents[ i ] );
        }
        return selectors;
    }

    /**
     * Sets the error mode.  This controls which column selectors are
     * displayed.
     *
     * @param  errorMode  error mode
     */
    public void setErrorMode( ErrorMode errorMode ) {
        if ( errorMode.equals( errorMode_ ) ) {
            return;
        }
        extrasBox_.removeAll();
        ErrorMode.Extent[] extents = errorMode.getExtents();
        for ( int i = 0; i < extents.length; i++ ) {
            ErrorMode.Extent extent = extents[ i ];
            placeSelector( extrasBox_, extent.getLabel(),
                           extentMap_.get( extent ) );
        }
        errorMode_ = errorMode;
        revalidate();
    }

    /**
     * Configures this component for a given table, populating the 
     * column selectors accordingly.
     *
     * @param  tcModel  new table (may be null)
     */
    public void setTable( TopcatModel tcModel ) {
        JComboBox[] selectors = getSelectors();
        for ( int i = 0; i < selectors.length; i++ ) {
            JComboBox s = selectors[ i ];
            if ( tcModel == null ) {
                s.setSelectedItem( null );
                s.setEnabled( false );
            }
            else {
                s.setModel( new ColumnDataComboBoxModel( tcModel, Number.class,
                                                         true ) );
                s.setEnabled( true );
            }
        }
    }

    /**
     * Returns an array of all the column selectors which may be displayed
     * by this component.
     *
     * @return   array of combo boxes
     */
    public JComboBox[] getSelectors() {
        return new JComboBox[] {
            atSelector_, loSelector_, hiSelector_, lhSelector_,
        };
    }

    /**
     * Places a column selector within a container.
     *
     * @param  box  container
     * @param  label  text label describing the selector
     * @param  selector  combo box to place
     */
    private void placeSelector( JComponent box, String label, 
                                JComboBox selector ) {
        box.add( Box.createHorizontalStrut( 5 ) );
        box.add( new JLabel( label + " " ) );
        box.add( new ShrinkWrapper( selector ) );
        box.add( Box.createHorizontalStrut( 5 ) );
        box.add( new ComboBoxBumper( selector ) );
        box.add( Box.createHorizontalStrut( 5 ) );
    }

    /**
     * Makes a guess at the best error column to use for a given main column.
     *
     * @param   mainCol  column whose errors are to be determined
     * @param   type   error type
     * @param   selector  the combo box containing the possible columns for
     *          the main column (which is assumed to be the list of possible
     *          selections for the error column)
     * @return  guess for error column, or null if nothing looks suitable
     */
    private static ColumnData guessColumn( ColumnData mainCol, ErrorType type,
                                           JComboBox selector ) {
        ColumnInfo mainInfo = mainCol.getColumnInfo();
        int ncol = selector.getItemCount();
        int iMain = -1;
        int bestScore = 0;
        ColumnData bestCol = null;
        for ( int icol = 0; icol < ncol; icol++ ) {
            Object dat = selector.getItemAt( icol );
            ColumnData auxCol = dat instanceof ColumnData ? (ColumnData) dat
                                                          : null;
            if ( auxCol == null ) {
            }
            else if ( auxCol.equals( mainCol ) ) {
                iMain = icol;
            }
            else {
                int score =
                    type.getLikeness( mainInfo, auxCol.getColumnInfo() );
                if ( score > 0 && iMain >= 0 && icol - iMain <= 2 ) {
                    score += 4;
                }
                if ( score > bestScore ) {
                    bestScore = score;
                    bestCol = auxCol;
                }
            }
        }
        return bestCol;
    }

    /**
     * Returns an integer indicating how likely it is that <code>auxInfo</code>
     * describes a column which contains the errors of <code>mainInfo</code>.
     * Zero means it is not likely, and larger numbers mean it is more likely.
     *
     * @param  mainInfo  metadata for main column
     * @param  auxInfo   metadata for other column
     * @return  indication of whether <code>auxInfo</code> gives errors for
     *          <code>mainInfo</code>
     */
    private static int getErrorLikeness( ColumnInfo mainInfo,
                                         ColumnInfo auxInfo ) {
        int score = 0;
        String nameDiff = getInsertion( mainInfo.getName(), auxInfo.getName() );
        if ( nameDiff != null &&
             ERR_NAME_REGEX.matcher( nameDiff ).matches() ) {
            score += 5;
        }
        String ucdDiff = getInsertion( mainInfo.getUCD(), auxInfo.getUCD() );
        if ( ucdDiff != null &&
             ERR_UCD_REGEX.matcher( ucdDiff ).matches() ) {
            score += 8;
        }
        return score;
    }

    /**
     * Returns the string fragment which has been added to a string 
     * <code>s0</code> to turn it into a longer string <code>s1</code>.
     * The string may have been added at either end or in the middle.
     * If no such string exists, or if it is of zero length, null
     * is returned.
     * For instance <code>getInsertion("AB","AXXB")</code>
     * would return <code>"XX"</code>.
     *
     * @param  s0  basic (shorter) string
     * @param  s1  string which may be s0 plus an insertion
     * @return  insertion, or null
     */
    private static String getInsertion( String s0, String s1 ) {
        if ( s0 == null || s1 == null || s0.length() == 0 || s1.length() == 0 ||
             s0.length() >= s1.length() ) {
            return null;
        }
        int l0 = s0.length();
        int l1 = s1.length();
        int nPre = 0;
        for ( int i = 0; i < l0 && s0.charAt( i ) == s1.charAt( i ); i++ ) {
            nPre++;
        }
        int nPost = 0;
        for ( int i = 0;
              i < l0 && s0.charAt( l0 - 1 - i ) == s1.charAt( l1 - 1 - i );
              i++ ) {
            nPost++;
        }
        return nPost + nPre == l0 ? s1.substring( nPre, l1 - nPost )
                                  : null;
    }

    /**
     * Action listener which converts events on individual column selectors 
     * into suitable calls to the Column Selection Tracker object.
     */
    private class ColumnSelectionListener implements ActionListener {

        private final ErrorType errorType_;
        private ColumnData lastCol_;

        /**
         * Constructor.
         *
         * @param   errorType  indicates the type of error selector this 
         *          object is listening to; null indicates the main selector
         */
        public ColumnSelectionListener( ErrorType errorType ) {
            errorType_ = errorType;
        }

        public void actionPerformed( ActionEvent evt ) {
            JComboBox src = (JComboBox) evt.getSource();
            Object dat = src.getSelectedItem();
            ColumnData col = dat instanceof ColumnData ? (ColumnData) dat
                                                       : null;
            if ( col == lastCol_ ) {
                return;
            }
            lastCol_ = col;
            AxisDataSelector axSel = AxisDataSelector.this;
            if ( errorType_ == null ) {
                colSelectionTracker_.mainSelected( axSel, col );
            }
            else {
                colSelectionTracker_.auxSelected( axSel, errorType_, col );
            }
        }
    }

    /**
     * Enumeration class which describes a type of error quantity that
     * can be selected.
     */
    private static abstract class ErrorType {

        private final String name_;

        /**
         * Constructor.
         *
         * @param  error type description
         */
        private ErrorType( String name ) {
            name_ = name;
        }

        /**
         * Returns the combo box corresponding to this error type 
         * for a given AxisDataSelector object.
         *
         * @param  axSel   axis data selector
         * @return  combo box in <code>axSel</code> for this type
         */
        public abstract JComboBox getSelector( AxisDataSelector axSel );

        /**
         * Returns a score indicating how much <code>auxInfo</code> looks
         * like a column which plays the role of this type with respect
         * to <code>mainInfo</code>.
         * A return of zero means it doesn't look like one; higher values
         * are progressively more likely.
         *
         * <p>The default implementation just decides how much it looks like
         * an error column; subclasses may override this if they can 
         * do something more specific.
         *
         * @param  mainInfo  description of main column
         * @param  auxInfo   description of auxiliary column
         * @return  non-negative value giving likelihood of error relation
         */
        public int getLikeness( ColumnInfo mainInfo, ColumnInfo auxInfo ) {
            return getErrorLikeness( mainInfo, auxInfo );
        }

        /**
         * Returns error type description.
         */
        public String toString() {
            return name_;
        }
    }

    /**
     * Keeps track of which columns serve as error values for which other
     * columns.  There could be one of these per table, but it is currently
     * more convenient to have one application-wide instance.
     */
    private static class ColumnSelectionTracker {

        /**
         * Structure containing the state of this object.
         * It is a map from (main ColumnData, ErrorType) -> aux ColumnData
         * (see {@link #getKey}).  Entries exist where the value of an
         * auxiliary (error) selector is known for a given main selector value.
         * A null entry means the value should be blank; this differs from
         * an absent entry which means it is unknown.
         */
        private final Map<List<Object>,ColumnData> errColMap_ =
            new HashMap<List<Object>,ColumnData>();

        /**
         * Called when the main selector in an AxisDataSelector has a new
         * column chosen.  This may cause changes to the selections of the
         * auxiliary selectors.
         *
         * @param  axSel  the axis data selector which this event concerns
         * @param  col    the newly-selected column
         */
        public void mainSelected( final AxisDataSelector axSel,
                                  ColumnData mainCol ) {
            if ( mainCol == null ) {
                axSel.loSelector_.setSelectedItem( null );
                axSel.hiSelector_.setSelectedItem( null );
                axSel.lhSelector_.setSelectedItem( null );
            }
            else {
                for ( ErrorType type : new ErrorType[] { LO, HI, LH, } ) {
                    List<Object> key = getKey( mainCol, type );
                    JComboBox selector = type.getSelector( axSel );
                    ColumnData col = errColMap_.containsKey( key )
                                   ? errColMap_.get( key )
                                   : guessColumn( mainCol, type, selector );
                    selector.setSelectedItem( col );
                }
            }
        }

        /**
         * Called when an auxiliary selector in an AxisDataSelector has
         * a new column chosen.  This will not affect the selections in
         * any other selectors, but the selection may be remembered for
         * re-use later.
         *
         * @param  axSel  the axis data selector which this event concerns
         * @param  type   the type of selector whose selection has changed
         * @param  col    the new value for the selector
         */
        public void auxSelected( AxisDataSelector axSel, ErrorType type,
                                 ColumnData col ) {
            Object mainDat = axSel.atSelector_.getSelectedItem();
            if ( mainDat instanceof ColumnData ) {
                List<Object> key = getKey( (ColumnData) mainDat, type );
                if ( col != null || errColMap_.containsKey( key ) ) {
                    errColMap_.put( key, col );
                }
            }
        }

        /**
         * Returns a key for use in this object's <code>errColMap_</code>.
         *
         * @param   mainCol   main column
         * @param   type      error type
         * @return  opaque key object
         */
        private static List<Object> getKey( ColumnData mainCol,
                                            ErrorType type ) {
            return Arrays.asList( new Object[] { mainCol, type, } );
        }
    }
}
