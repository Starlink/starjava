package uk.ac.starlink.topcat.plot2;

import gnu.jel.CompilationException;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.table.TableColumn;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.gui.StarTableColumn;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnCheckBoxList;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.TopcatJELUtils;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.geom.MatrixGangerFactory;
import uk.ac.starlink.ttools.plot2.geom.MatrixPlotType;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.util.Bi;
import uk.ac.starlink.util.StreamUtil;

/**
 * CoordPanel for specifying matrix plots.
 * This has a variable number of coordinates, under the control of the user.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2023
 */
public class MatrixPositionCoordPanel extends SimplePositionCoordPanel {

    private final VariableCoordStack stack_;
    private final FillPanel fillPanel_;
    private final ActionForwarder forwarder_;
    private final Action increaseAct_;
    private final Action reduceAct_;
    private int nCoord_;

    private static final int INITIAL_NCOORD = 4;
    private static final int MAX_NCOORD = MatrixGangerFactory.MAX_NCOORD_GUI;
    private static final FloatingCoord[] COORDS = createCoords( MAX_NCOORD );
    private static final int ONLY_IU = 0;

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public MatrixPositionCoordPanel() {
        super( COORDS.clone(), new ConfigKey<?>[ 0 ],
               new VariableCoordStack(), PlaneDataGeom.INSTANCE );
        stack_ = (VariableCoordStack) getStack();
        stack_.setCoordPanel( this );
        stack_.showItems( -1 );
        fillPanel_ = new FillPanel();
        nCoord_ = INITIAL_NCOORD;
        forwarder_ = getActionForwarder();
        increaseAct_ = createAdjustCoordinateCountAction( true );
        reduceAct_ = createAdjustCoordinateCountAction( false );
        addButtons( new Action[] { increaseAct_, reduceAct_ } );
        updateCount();
    }

    /**
     * Returns the number of coordinate entry fields currently visible.
     *
     * @return  number of coord entry fields
     */
    public int getVisibleCoordCount() {
        return nCoord_;
    }

    @Override
    public List<Bi<String,JComponent>> getExtraTabs() {
        return Collections
              .singletonList( new Bi<String,JComponent>( "Fill", fillPanel_ ) );
    }

    @Override
    public boolean isPreferredCoord( Coord coord ) {

        /* None of the coordinates is required on its own, but at least
         * one of them must be filled in for a viable plot, so mark them
         * all Preferred. */
        return coord.isRequired()
            || Arrays.asList( COORDS ).contains( coord );
    }

    @Override
    public void autoPopulate() {
        ValueInfo[] allInfos = getInfos( getColumnSelector( 0, ONLY_IU ) );
        for ( int ic = 0; ic < Math.min( nCoord_, allInfos.length ); ic++ ) {
            populate( getColumnSelector( ic, ONLY_IU ), allInfos[ ic ] );
        }
    }

    @Override
    public Coord[] getCoords() {
        Coord[] allCoords = super.getCoords();
        if ( allCoords == null ) {
            return null;
        }
        Coord[] visibleCoords = new Coord[ nCoord_ ];
        System.arraycopy( allCoords, 0, visibleCoords, 0, nCoord_ );
        return visibleCoords;
    }

    @Override
    public Coord[] getAdditionalManagedCoords() {
        return new Coord[] { PlaneDataGeom.X_COORD, PlaneDataGeom.Y_COORD, };
    }

    @Override
    public GuiCoordContent[] getContents() {
        GuiCoordContent[] allContents = super.getContents();
        if ( allContents == null ) {
            return null;
        }
        GuiCoordContent[] visibleContents = new GuiCoordContent[ nCoord_ ];
        System.arraycopy( allContents, 0, visibleContents, 0, nCoord_ );
        return visibleContents;
    }

    @Override
    public void setTable( TopcatModel tcModel, boolean autoFill ) {
        super.setTable( tcModel, autoFill );
        fillPanel_.setTable( tcModel );
    }

    /**
     * Updates the current state of this panel to match the number of
     * visible coordinates.
     * Should be called if that value may have changed.
     */
    private void updateCount() {
        increaseAct_.setEnabled( nCoord_ < MAX_NCOORD );
        reduceAct_.setEnabled( nCoord_ > 1 );
        stack_.showItems( nCoord_ );
    }

    /**
     * Creates an action that can increment or decrement the number of
     * visible coordinates.
     *
     * @param  isAdd  true to increment, false to decrement
     * @return  new action
     */
    private Action createAdjustCoordinateCountAction( boolean isAdd ) {
        return new BasicAction( isAdd ? "Add" : "Remove",
                                isAdd ? ResourceIcon.ADD
                                      : ResourceIcon.SUBTRACT,
                                ( isAdd ? "Add a new entry to "
                                        : "Subtract the last entry from " )
                                + "the list of coordinates" ) {
            public void actionPerformed( ActionEvent evt ) {
                int nCoord = nCoord_ + ( isAdd ? +1 : -1 );
                if ( nCoord >= 1 && nCoord <= MAX_NCOORD ) {
                    nCoord_ = nCoord;
                    updateCount();
                    forwarder_.actionPerformed( evt );
                }
            }
        };
    }

    /**
     * Creates an array of coordinates suitable for use as matrix
     * positional coordinates.
     *
     * @param  nc  number of coordinates required
     * @return  array of coords
     */
    private static FloatingCoord[] createCoords( int nc ) {
        boolean isRequired = false;
        return IntStream
              .range( 0, nc )
              .mapToObj( ic -> FloatingCoord
                              .createCoord( MatrixPlotType.getCoordMeta( ic ),
                                            isRequired ) )
              .toArray( n -> new FloatingCoord[ n ] );
    }

    /**
     * Creates a column mapper that does a one-to-one mapping from
     * input columns to output coordinate values.
     *
     * @return  new mapper
     */
    private static ColumnMapper createSingleColumnMapper() {
        return new ColumnMapper() {
            public String getName() {
                return "Selected";
            }
            public String getDescription() {
                return "Use selected columns";
            }
            public String[] columnsToExpressions( StarTableColumn[] tcols ) {
                return Arrays.stream( tcols )
                      .map( tc -> tc.getColumnInfo().getName() )
                      .toArray( n -> new String[ n ] );
            }
        };
    }

    /**
     * Creates a column mapper that maps from input columns to
     * combinations of all possible distinct pairs.
     * Combination is either differences (Xa-Xb) or ratios (Xa/Xb).
     *
     * @param  isRatio  true for ratios, false for differences
     * @return  new mapper
     */
    private static ColumnMapper createPairColumnMapper( boolean isRatio ) {
        return new ColumnMapper() {
            public String getName() {
                return isRatio ? "Pair Ratios" : "Pair Differences";
            }
            public String getDescription() {
                return "Use column " + ( isRatio ? "ratios" : "differences" )
                     + " for all pair combinations of selected columns";
            }
            public String[] columnsToExpressions( StarTableColumn[] tcols ) {
                int nin = tcols.length;
                String[] inExprs = new String[ nin ];
                for ( int i = 0; i < nin; i++ ) {
                    StarTableColumn tcol = tcols[ i ];
                    String cname = tcol.getColumnInfo().getName();
                    inExprs[ i ] = TopcatJELUtils.isJelIdentifier( cname )
                                 ? cname
                                 : TopcatJELUtils.getColumnId( tcol );
                }
                List<String> outList = new ArrayList<>();
                for ( int i = 0; i < nin; i++ ) {
                    for ( int j = i + 1; j < nin; j++ ) {
                        outList.add( new StringBuffer()
                                    .append( inExprs[ i ] )
                                    .append( isRatio ? "/" : "-" )
                                    .append( inExprs[ j ] )
                                    .toString() );
                    }
                }
                return outList.toArray( new String[ 0 ] );
            }
        };
    }

    /**
     * Helper panel that provides an alternative way to fill in the
     * coordinates from the Position tab.
     * Generally it can be done with less pointing and clicking.
     */
    private class FillPanel extends JPanel {

        private final JLabel tableLabel_;
        private final ColumnCheckBoxList colList_;
        private final FillAction[] fillActs_;
        private TopcatModel tcModel_;

        /**
         * Constructor.
         */
        FillPanel() {
            super( new BorderLayout() );
            JComponent topBox = Box.createVerticalBox();
            tableLabel_ = new JLabel();

            /* Prepare a checkboxlist containing all suitable columns
             * of the current table. */
            Predicate<TableColumn> isNumeric = tc ->
                tc instanceof StarTableColumn &&
                Number.class
               .isAssignableFrom( ((StarTableColumn) tc)
                                 .getColumnInfo().getContentClass() );
            colList_ = new ColumnCheckBoxList( false, isNumeric );
            topBox.add( new LineBox( "Table", tableLabel_ ) );

            /* Prepare actions that modify the content of the
             * column checkboxlist. */
            Action[] controlActs = {
                createResetAction(),
                createSelectAllAction( true ),
                createSelectAllAction( false ),
            };

            /* Prepare actions that use the content of the column
             * checkboxlist to fill in the Position tab. */
            ColumnMapper[] colMappers = {
                createSingleColumnMapper(),
                createPairColumnMapper( false ),
                createPairColumnMapper( true ),
            };
            fillActs_ = Arrays.stream( colMappers )
                       .map( m -> new FillAction( colList_, m ) )
                       .toArray( n -> new FillAction[ n ] );

            /* Place components. */
            JComponent box1 = Box.createHorizontalBox();
            for ( Action act : controlActs ) {
                JButton butt = new JButton( act );
                butt.setHideActionText( true );
                butt.setMargin( new Insets( 2, 2, 2, 2 ) );
                box1.add( butt );
                box1.add( Box.createHorizontalStrut( 5 ) );
            }
            box1.add( Box.createHorizontalGlue() );
            JComponent box2 = Box.createVerticalBox();
            box2.add( new LineBox( "Populate Position tab", null ) );
            for ( Action act : fillActs_ ) {
                box2.add( Box.createVerticalStrut( 5 ) );
                Box line = Box.createHorizontalBox();
                line.add( Box.createHorizontalStrut( 15 ) );
                JButton butt = new JButton( act );
                line.add( butt );
                line.add( Box.createHorizontalGlue() );
                box2.add( line );
            }
            Box ctrlBox = Box.createVerticalBox();
            ctrlBox.add( box1 );
            ctrlBox.add( Box.createVerticalStrut( 5 ) );
            ctrlBox.add( box2 );
            ctrlBox.add( Box.createVerticalGlue() );
            ctrlBox.setBorder( BorderFactory.createEmptyBorder( 0, 5, 0, 0 ) );
            JPanel ctrlPanel = new JPanel( new BorderLayout() );
            ctrlPanel.add( ctrlBox, BorderLayout.NORTH );
            JScrollPane listScroller = new JScrollPane( colList_ );
            listScroller.getVerticalScrollBar().setUnitIncrement( 32 );
            add( topBox, BorderLayout.NORTH );
            add( listScroller, BorderLayout.CENTER );
            add( ctrlPanel, BorderLayout.EAST );
        }

        /**
         * Sets the current table for this tab.
         *
         * @param  tcModel  topcat model
         */
        public void setTable( TopcatModel tcModel ) {
            tableLabel_.setText( tcModel == null ? null : tcModel.toString() );
            colList_.setTableColumnModel( tcModel == null
                                              ? null
                                              : tcModel.getColumnModel() );
            tcModel_ = tcModel;
        }

        /**
         * Creates an action that repopulates the column checkboxlist
         * from the topcat model.
         *
         * @return  new reset action
         */
        private Action createResetAction() {
            return BasicAction.create( "Reset", ResourceIcon.REDO,
                                       "Repopulate column list from table",
                                       evt -> setTable( tcModel_ ) );
        }

        /**
         * Creates an action that selects or unselects all the checkboxes
         * in the column checkboxlist.
         *
         * @param  isSet  true to select, false to unselect
         * @return   new action
         */
        private Action createSelectAllAction( final boolean isSet ) {
            return BasicAction
                  .create( ( isSet ? "Select" : "Unselect" ) + " All",
                           ( isSet ? ResourceIcon.REVEAL_ALL
                                   : ResourceIcon.HIDE_ALL ),
                           "Set all columns in this display " +
                           ( isSet ? "selected" : "unselected" ),
                           evt -> {
                               colList_.setCheckedAll( isSet );
                               colList_.repaint();
                           } );
        }
    }

    /**
     * Defines a mapping from a list of table columns to
     * a list of coordinate expressions for plotting.
     */
    private static interface ColumnMapper {

        /**
         * Returns a name of this mapper.
         *
         * @return  action name
         */
        String getName();

        /**
         * Returns a description for this mapper.
         *
         * @return  action basic short description
         */
        String getDescription();

        /**
         * Maps a list of columns to a list of JEL-friendly
         * coordinate expressions.
         *
         * @param  tcols  input column array
         * @return  output coordinate expression array
         */
        String[] columnsToExpressions( StarTableColumn[] tcols );
    }

    /**
     * Action that fills in the Position tab with expressions obtained
     * by applying a ColumnMapper to the currently selected list of
     * columns in a ColumnCheckBoxList.
     */
    private class FillAction extends AbstractAction {

        final ColumnCheckBoxList colList_;
        final ColumnMapper mapper_;

        /**
         * Constructor.
         *
         * @param  colList  list supplying column selections
         * @param  mapper   defines mapping from columns to coord expressions
         */
        FillAction( ColumnCheckBoxList colList, ColumnMapper mapper ) {
            super( mapper.getName() );
            colList_ = colList;
            mapper_ = mapper;
            colList_.addListDataListener( new ListDataListener() {
                public void contentsChanged( ListDataEvent evt ) {
                    updateAction();
                }
                public void intervalAdded( ListDataEvent evt ) {
                    updateAction();
                }
                public void intervalRemoved( ListDataEvent evt ) {
                    updateAction();
                }
            } );
            updateAction();
        }

        public void actionPerformed( ActionEvent evt ) {
            StarTableColumn[] tcols = getSelectedColumns();
            String[] outExprs = mapper_.columnsToExpressions( tcols );
            int nout = outExprs.length;
            if ( nout < MAX_NCOORD ) {
                nCoord_ = nout;
                for ( int ic = 0; ic < MAX_NCOORD; ic++ ) {
                    ColumnDataComboBoxModel model =
                        getColumnSelector( ic, ONLY_IU );
                    ColumnData cdata;
                    if ( ic < nout ) {
                        try {
                            cdata = model.stringToColumnData( outExprs[ ic ] );
                        }
                        catch ( CompilationException e ) {
                            // shouldn't happen
                            cdata = null;
                        }
                    }
                    else {
                        cdata = null;
                    }
                    model.setSelectedItem( cdata );
                }
                updateCount();
                forwarder_.actionPerformed( new ActionEvent( this, 0, "" ) );
            }
        }

        /**
         * Acquires the list of selected columns from the checkboxlist.
         *
         * @return  selected StarTableColumns
         */
        private StarTableColumn[] getSelectedColumns() {

            /* Filter the list so that all elements are guaranteed to be
             * StarTableColumn instances.  They should be in any case. */
            return colList_.getCheckedItems().stream()
                  .flatMap( StreamUtil.keepInstances( StarTableColumn.class ) )
                  .toArray( n -> new StarTableColumn[ n ] );
        }

        /**
         * Updates this state of this action for current status.
         * Should be called if the column list or selections in it
         * might have changed.
         */
        private void updateAction() {
            StarTableColumn[] tcols = getSelectedColumns();
            int nin = tcols.length;
            int nout = mapper_.columnsToExpressions( tcols ).length;
            putValue( SHORT_DESCRIPTION,
                      new StringBuffer()
                         .append( mapper_.getDescription() )
                         .append( " (" )
                         .append( nin )
                         .append( " selected -> " )
                         .append( nout )
                         .append( " coords)" )
                         .toString() );
            setEnabled( nout > 1 && nout < MAX_NCOORD );
        }
    }
}
