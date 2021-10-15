package uk.ac.starlink.topcat.join;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.ListModel;
import uk.ac.starlink.table.JoinFixAction;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.table.join.MatchEngine;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ControlWindow;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.topcat.TupleSelector;
import uk.ac.starlink.topcat.plot2.ControlManager;
import uk.ac.starlink.topcat.plot2.CubePlotWindow;
import uk.ac.starlink.topcat.plot2.LayerCommand;
import uk.ac.starlink.topcat.plot2.LayerException;
import uk.ac.starlink.topcat.plot2.PlanePlotWindow;
import uk.ac.starlink.topcat.plot2.SkyPlotWindow;
import uk.ac.starlink.topcat.plot2.SpherePlotWindow;
import uk.ac.starlink.topcat.plot2.StackPlotWindow;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ColorConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.geom.CubeDataGeom;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SkyDataGeom;
import uk.ac.starlink.ttools.plot2.geom.SphereDataGeom;
import uk.ac.starlink.ttools.plot2.layer.MarkForm;
import uk.ac.starlink.ttools.plot2.layer.MarkerShape;
import uk.ac.starlink.ttools.plot2.layer.PairLinkForm;
import uk.ac.starlink.ttools.plot2.layer.ShapeMode;
import uk.ac.starlink.ttools.plot2.layer.ShapePlotter;
import uk.ac.starlink.ttools.plot2.layer.ShapeStyle;
import uk.ac.starlink.util.gui.ErrorDialog;

/**
 * Prepares a plot based on the inputs and outputs of a crossmatch operation.
 *
 * @author   Mark Taylor
 * @since    20 Dec 2013
 */
public abstract class MatchPlotter {

    private static final MarkerShape[] SINGLE_SHAPES = new MarkerShape[] {
        MarkerShape.CROXX,
        MarkerShape.CROSS,
        MarkerShape.OPEN_TRIANGLE_UP,
        MarkerShape.OPEN_TRIANGLE_DOWN,
    };
    private static final Color[] SINGLE_COLORS =
        ColorConfigKey.getPlottingColors();

    /**
     * Posts a plot window representing data from tables input to a match
     * and the output table.  The input tables are represented as points,
     * and the output table is represented as links between the
     * corresponding input positions.
     *
     * @param  parent  parent component
     * @param  tselectors  selectors used to specify match input
     *                     tables and values, one for each input table
     * @param  fixActs   options for column name disambiguation, 
     *                   one for each input table
     * @param  result   output (matched) table
     * @throws  LayerException  if the plot cannot be constructed
     */
    public abstract void showPlot( Component parent, TupleSelector[] tselectors,
                                   JoinFixAction[] fixActs,
                                   TopcatModel result )
            throws LayerException;

    /**
     * Acquires an instance of this class suitable for a given match engine.
     *
     * @param  engine   match criterion
     * @return  match plotter instance, or null if we don't know how to do it
     */
    public static MatchPlotter getMatchPlotter( MatchEngine engine ) {
        final ListModel<TopcatModel> tablesModel =
            ControlWindow.getInstance().getTablesListModel();

        /* Get the names of the coordinates which are required by the
         * matching criterion. */
        ValueInfo[] tupleInfos = engine.getTupleInfos();
        int nc = tupleInfos.length;
        String[] cNames = new String[ nc ];
        for ( int ic = 0; ic < nc; ic++ ) {
            cNames[ ic ] = tupleInfos[ ic ].getName();
        }

        /* Now we use these names to identify what kind of plot makes sense.
         * This is not bulletproof, but currently seems to work in most cases.
         * At present we just plot the basic positional coordinates.
         * Additional coordinates (e.g. error ellipse parameters) are
         * just ignored.  We could get smarter than this, and actually
         * plot error ellipses (etc) for the various more complicated kinds
         * of matches. */

        /* Sphere plot.  Possibly this should be used for Sky + X as well? */
        if ( sameFirstNames( cNames,
                             new String[] { "RA", "Dec", "Distance" } ) ||
             sameFirstNames( cNames,
                             new String[] { "Lon", "Lat", "Radius" } ) ) {
            return new BasicMatchPlotter( SphereDataGeom.INSTANCE, false ) {
                public StackPlotWindow<?,?>
                        createPlotWindow( Component parent ) {
                    return new SpherePlotWindow( parent, tablesModel );
                }
            };
        }

        /* Sky plot. */
        else if ( sameFirstNames( cNames, new String[] { "RA", "Dec" } ) ||
                  sameFirstNames( cNames, new String[] { "Lon", "Lat" } ) ) {
            return new BasicMatchPlotter( SkyDataGeom.createGeom( null, null ),
                                          true ) {
                public StackPlotWindow<?,?>
                        createPlotWindow( Component parent ) {
                    return new SkyPlotWindow( parent, tablesModel );
                }
            };
        }

        /* Cube plot. */
        else if ( sameFirstNames( cNames, new String[] { "X", "Y", "Z" } ) ) {
            return new BasicMatchPlotter( CubeDataGeom.INSTANCE, false ) {
                public StackPlotWindow<?,?>
                        createPlotWindow( Component parent ) {
                    return new CubePlotWindow( parent, tablesModel );
                }
            };
        }

        /* Plane plot. */
        else if ( sameFirstNames( cNames, new String[] { "X", "Y" } ) ) {
            return new BasicMatchPlotter( PlaneDataGeom.INSTANCE, true ) {
                public StackPlotWindow<?,?>
                        createPlotWindow( Component parent ) {
                    return new PlanePlotWindow( parent, tablesModel );
                }
            };
        }

        /* Don't know - can't do it. */
        return null;
    }

    /**
     * Creates an action which can be used to post a plot for a given
     * completed match operation.
     * This is a utility method which acquires a suitable instance and
     * invokes it within an action.
     *
     * <p>It's not always possible to do this.  In the case that no plot
     * can be made, a non-null action is still returned, but invoking it
     * will pop up an error message.
     *
     * @param   parent  parent component
     * @param   engine  match engine determining match criteria
     * @param   tselectors   populated GUI components specifying input
     *                       tables and coordinates, corresponding to
     *                       match engine requirements
     * @param  fixActs   options for column name disambiguation, 
     *                   one for each input table
     * @param  result   output (matched) table
     * @return   action to plot the result
     */
    public static Action createPlotAction( final Component parent,
                                           final MatchEngine engine,
                                           final TupleSelector[] tselectors,
                                           final JoinFixAction[] fixActs,
                                           final TopcatModel result ) {
        final MatchPlotter plotter = getMatchPlotter( engine );
        Action act = new BasicAction( "Plot Result", ResourceIcon.MATCHPLOT,
                                "Plot the input tables and match links" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( plotter == null ) {
                    String msg = "No plotter for matcher " + engine;
                    JOptionPane.showMessageDialog( parent, msg, "No Match Plot",
                                                   JOptionPane.ERROR_MESSAGE );
                }
                else {
                    try {
                        plotter.showPlot( parent, tselectors, fixActs, result );
                    }
                    catch ( LayerException e ) {
                        ErrorDialog.showError( parent, "Plot Failure", e,
                                               "Cannot plot match result" );
                    }
                }
            }
        };
        return act;
    }

    /**
     * Determines whether the first few elements of a values array are the
     * same as the elements of a targets array.  String matching is
     * case-insensitive.  If the values array is shorter, the result is
     * always false.
     *
     * @param   values   strings to test
     * @param   targets  strings to test against
     * @return  true iff the first <code>targets.length</code>
     *          elements of <code>values</code> matches the corresponding
     *          elements of <code>targets</code>, case-insensitive
     */
    private static boolean sameFirstNames( String[] values, String[] targets ) {
        int n = targets.length;
        if ( values.length < n ) {
            return false;
        }
        for ( int i = 0; i < n; i++ ) {
            if ( ! values[ i ].equalsIgnoreCase( targets[ i ] ) ) {
                return false;
            }
        }
        return true;
    }

    /**
     * MatchPlotter implementation.
     */
    private static abstract class BasicMatchPlotter extends MatchPlotter {

        private final String[] userPosCoordNames_;
        private final ShapeMode markMode1_;
        private final ShapeMode markModeN_;

        /**
         * Constructor.
         *
         * @param   geom   maps positional parameters to graphics coordinates
         * @param   is2d   true iff plot is 2-d like (no depth)
         */
        BasicMatchPlotter( DataGeom geom, boolean is2d ) {
            markMode1_ = is2d ? ShapeMode.FLAT2D : ShapeMode.FLAT3D;
            markModeN_ = is2d ? ShapeMode.AUTO : ShapeMode.FLAT3D;
            Coord[] posCoords = geom.getPosCoords();
            List<String> inameList = new ArrayList<String>();
            for ( Coord posCoord : geom.getPosCoords() ) {
                for ( Input input : posCoord.getInputs() ) {
                    inameList.add( LayerCommand.getInputName( input ) );
                }
            }
            userPosCoordNames_ = inameList.toArray( new String[ 0 ] );
        }

        /**
         * Returns a new plot window of a suitable type for layers to be
         * added by this plotter.
         *
         * @param   parent  parent component
         */
        abstract StackPlotWindow<?,?> createPlotWindow( Component parent );

        /**
         * Returns style configuration for single-point markers.
         *
         * @param  iin  index of input table
         * @return  config map
         */
        public ConfigMap createMarkConfig1( int iin ) {
            ConfigMap config = new ConfigMap();
            config.put( StyleKeys.COLOR,
                        SINGLE_COLORS[ iin % SINGLE_COLORS.length ] );
            config.put( StyleKeys.MARKER_SHAPE,
                        SINGLE_SHAPES[ iin % SINGLE_SHAPES.length ] );
            config.put( StyleKeys.SIZE, 2 );
            return config;
        }

        /**
         * Returns style configuration for result (multi-point) markers.
         *
         * @return   config map
         */
        public ConfigMap createMarkConfigN() {
            ConfigMap config = new ConfigMap();
            config.put( StyleKeys.COLOR, Color.gray );
            config.put( StyleKeys.MARKER_SHAPE, MarkerShape.OPEN_CIRCLE );
            config.put( StyleKeys.SIZE, 3 );
            return config;
        }

        /**
         * Returns style configuration for result pair links.
         *
         * @return  config map
         */
        public ConfigMap createPairLinkConfig() {
            ConfigMap config = new ConfigMap();
            config.put( StyleKeys.COLOR, Color.gray );
            return config;
        }

        public void showPlot( Component parent, TupleSelector[] tselectors,
                              JoinFixAction[] fixActs, TopcatModel result )
                throws LayerException {
            int nin = tselectors.length;
            int nupc = userPosCoordNames_.length;
            String[][] exprs = new String[ nupc ][ nin ];
            StackPlotWindow<?,?> win = createPlotWindow( parent );
            ControlManager controlManager = win.getControlManager();

            /* For each input table, add a layer giving single positions.
             * We have to find the position expressions (user Coord values)
             * by looking at the values the user entered into the
             * TupleSelectors to specify the match. */
            Plotter<ShapeStyle> plotter1 =
                new ShapePlotter( "input", MarkForm.SINGLE, markMode1_ );
            for ( int iin = 0; iin < nin; iin++ ) {
                TupleSelector tsel = tselectors[ iin ];
                String[] texprs = tsel.getTupleExpressions();
                for ( int iuc = 0; iuc < nupc; iuc++ ) {
                    exprs[ iuc ][ iin ] = texprs[ iuc ];
                }
                Map<String,String> coordVals =
                    new LinkedHashMap<String,String>();
                for ( int iuc = 0; iuc < nupc; iuc++ ) {
                    coordVals.put( userPosCoordNames_[ iuc ], texprs[ iuc ] );
                }
                ConfigMap config = createMarkConfig1( iin );
                TopcatModel tcModel = tsel.getTable();
                LayerCommand<?> lcmd =
                    new LayerCommand<ShapeStyle>( plotter1, tcModel, coordVals,
                                                  config,
                                                  tcModel.getSelectedSubset() );
                controlManager.addLayer( lcmd );
            }

            /* Now try to add layers with multi-positions got from the
             * result table. */
            Map<String,String> coordValuesN =
                new LinkedHashMap<String,String>();

            /* Try to get the expressions to use for the positions in the
             * result table.  This is not easy, since the columns referenced
             * in the original expressions may have been renamed for
             * inclusion in the result table.  The possible cases are:
             *   - column not renamed
             *   - column renamed
             *   - expression made up of columns not renamed
             *   - expression made up of columns some of which are renamed
             *   - expression made up of columns referenced by $ID
             * The first two have a good chance of being handled
             * correctly by this code. The third one might work.
             * The others probably won't, and
             * would require either parsing of the expressions by this
             * code, or columns explicitly inserted into result table
             * giving the join columns.  The latter might be a good idea,
             * since it would allow auto-plotting of joined tables when
             * added to a plot, rather than just doing it here. */
            for ( int iin = 0; iin < nin; iin++ ) {

                /* First, prepare to work out how the column names in
                 * the input table whose expressions we are looking at
                 * in this iteration will have been mangled for deduplication
                 * in the joined result table we have.  Amongst other things
                 * this requires a list of the column names from the
                 * current input table and another list of the column names
                 * from all the other tables. */
                JoinFixAction fixAct = fixActs[ iin ];
                Collection<String> colNames0 = new HashSet<String>();
                Collection<String> colNamesOther = new HashSet<String>();
                for ( int jin = 0; jin < nin; jin++ ) {
                    Collection<String> list = jin == iin ? colNames0
                                                         : colNamesOther;
                    StarTable table =
                        tselectors[ jin ].getTable().getDataModel();
                    int ncol = table.getColumnCount();
                    for ( int ic = 0; ic < ncol; ic++ ) {
                        list.add( table.getColumnInfo( ic ).getName() );
                    }
                }

                /* Then, put together a map of position coordinate names
                 * to expressions that can be resolved in the context
                 * of the result table. */
                String suffix = PlotUtil.getIndexSuffix( iin );
                String[] texprs = tselectors[ iin ].getTupleExpressions();
                for ( int iuc = 0; iuc < nupc; iuc++ ) {

                    /* Get the coordinate name. */
                    String cname = userPosCoordNames_[ iuc ] + suffix;

                    /* Get the deduplicated expression. */
                    String baseExpr = texprs[ iuc ];
                    Collection<String> otherNames = new HashSet<String>();
                    otherNames.addAll( colNames0 );
                    otherNames.remove( baseExpr );
                    otherNames.addAll( colNamesOther );
                    String cvalue = fixAct.getFixedName( baseExpr, otherNames );

                    /* Store them together in a map. */
                    coordValuesN.put( cname, cvalue );
                }
            }

            /* Use the calculated information to set up a new layer creation
             * command. */
            Plotter<ShapeStyle> markPlotterN =
                new ShapePlotter( "result", MarkForm.createMarkForm( nin ),
                                  markModeN_ );
            LayerCommand<?> markCmd =
                new LayerCommand<ShapeStyle>( markPlotterN, result,
                                              coordValuesN, createMarkConfigN(),
                                              RowSubset.ALL );
            controlManager.addLayer( markCmd );
            if ( nin == 2 ) {
                Plotter<ShapeStyle> linkPlotter =
                    new ShapePlotter( "result", PairLinkForm.getInstance(),
                                      markModeN_ );
                LayerCommand<?> linkCmd =
                    new LayerCommand<ShapeStyle>( linkPlotter, result,
                                                  coordValuesN,
                                                  createPairLinkConfig(),
                                                  RowSubset.ALL );
                controlManager.addLayer( linkCmd );
            }
            win.setVisible( true );
        }
    }
}
