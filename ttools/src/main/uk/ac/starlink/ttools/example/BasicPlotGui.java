package uk.ac.starlink.ttools.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Domain;
import uk.ac.starlink.table.DomainMapper;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.gui.LabelledComponentStack;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.PlotCaching;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.ShadeAxisFactory;
import uk.ac.starlink.ttools.plot2.Span;
import uk.ac.starlink.ttools.plot2.Surface;
import uk.ac.starlink.ttools.plot2.SurfaceFactory;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.DataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.InputMeta;
import uk.ac.starlink.ttools.plot2.data.SimpleDataStoreFactory;
import uk.ac.starlink.ttools.plot2.data.TupleRunner;
import uk.ac.starlink.ttools.plot2.geom.PlanePlotType;
import uk.ac.starlink.ttools.plot2.paper.Compositor;
import uk.ac.starlink.ttools.plot2.paper.PaperTypeSelector;
import uk.ac.starlink.ttools.plot2.task.CoordValue;
import uk.ac.starlink.ttools.plot2.task.JELDataSpec;
import uk.ac.starlink.ttools.plot2.task.PlotDisplay;
import uk.ac.starlink.ttools.plot2.task.PointSelectionEvent;
import uk.ac.starlink.ttools.plot2.task.PointSelectionListener;

/**
 * This is a basic interactive GUI plotter.
 * You supply it with a fixed table to provide input data
 * and a single fixed plot surface type and layer type to define the
 * kind of plot to make, and it sets up a GUI to gather
 * the plot data and plot layer configuration.
 * It does this using the self-describing features of the
 * {@link uk.ac.starlink.ttools.plot2.PlotType PlotType} and
 * {@link uk.ac.starlink.ttools.plot2.Plotter Plotter} classes;
 * the code here does not need to understand any of the details of
 * particular plot types (scatter plots, histograms, 3d plots etc),
 * and it could be changed to present a different plot type by just
 * changing the supplied Plotter and/or PlotType, though some of
 * the subtleties that apply to only certain plot layers may be
 * skated over here.
 *
 * <p>As currently implemented, you need to hit the "Plot" button at
 * the bottom of the window to (re)plot the data.
 * This gives you a plot in the upper half which you can navigate
 * around using the mouse.
 * 
 * <p>It is definitely possible to fix it so that the replot happens
 * when any of the GUI controls is changed, rather than requiring
 * an explicit user replot action as shown here.  In fact, much of
 * the plot2 infrastructure is designed with this in mind.
 * However, ensuring that gets done with maximal efficiency
 * (intelligent caching) requires some careful work.
 * See the class <code>uk.ac.starlink.topcat.plot2.PlotPanel</code>
 * from the TOPCAT application for an example of how to do it.
 *
 * <p>This class could fairly easily be adapted to plot multi-layer plots,
 * for instance where the user assembles different layers from a menu
 * of available Plotters.
 *
 * <p>A <code>main</code> method is supplied to run up this GUI
 * from the command line.
 *
 * @author   Mark Taylor
 * @since    28 Sep 2015
 */
public class BasicPlotGui<P,A,S extends Style> extends JPanel {

    private final PlotType<P,A> plotType_;
    private final SurfaceFactory<P,A> sfact_;
    private final Plotter<S> plotter_;
    private final DataGeom geom_;
    private final DataStoreFactory dstoreFact_;
    private final InputPanel inputPanel_;
    private PlotDisplay<P,A> plotDisplay_;
    private DataStore dataStore_;

    /**
     * Constructor.
     *
     * @param  plotType  plot type
     * @param  plotter   plotter defining the single plot layer
     *                   this component plots
     * @param  table    table containing input data to plot
     */
    public BasicPlotGui( PlotType<P,A> plotType, Plotter<S> plotter,
                         StarTable table ) {
        super( new BorderLayout() );
        plotType_ = plotType;
        sfact_ = plotType.getSurfaceFactory();
        plotter_ = plotter;
        geom_ = plotType.getPointDataGeoms()[ 0 ];

        /* Note a CachedDataStoreFactory or some other implementation
         * could be more appropriate here depending on performance
         * and scalability constraints etc. */
        dstoreFact_ = new SimpleDataStoreFactory( TupleRunner.DEFAULT );

        /* Prepare GUI. */
        inputPanel_ = createInputPanel( plotter, geom_, sfact_, table );

        /* Prepare display. */
        final JComponent plotPanel = new JPanel( new BorderLayout() );

        /* Prepare listener for when a point is clicked. */
        final PointSelectionListener psl = new PointSelectionListener() {
            public void pointSelected( PointSelectionEvent evt ) {
                reportPoint( evt );
            }
        };

        /* Prepare a button to initiate the plot. */
        Action plotAction = new AbstractAction( "(Re)Plot" ) {
            public void actionPerformed( ActionEvent evt ) {
                if ( plotDisplay_ != null ) {
                    plotDisplay_.removePointSelectionListener( psl );
                    plotPanel.remove( plotDisplay_ );
                    plotDisplay_ = null;
                }
                try {
                    plotDisplay_ = createPlotDisplay();
                }
                catch ( Exception e ) {
                    System.err.println( "Plot error: " + e );
                    return;
                }
                plotDisplay_.addPointSelectionListener( psl );
                plotPanel.add( plotDisplay_, BorderLayout.CENTER );
                plotPanel.revalidate();
                plotPanel.repaint();
            }
        };

        /* Layout. */
        plotPanel.setPreferredSize( new Dimension( 400, 300 ) );
        inputPanel_.setPreferredSize( new Dimension( 400, 300 ) );
        JSplitPane splitter = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
        splitter.setBottomComponent( inputPanel_ );
        splitter.setTopComponent( plotPanel );
        add( splitter, BorderLayout.CENTER );
        add( new JButton( plotAction ), BorderLayout.SOUTH );
    }

    /**
     * Creates a new plot display based on the current state of the GUI.
     *
     * @return   new plot display component
     */
    private PlotDisplay<P,A> createPlotDisplay()
            throws ConfigException, IOException, InterruptedException {

        /* Acquire general key/value configuration from the GUI. */
        ConfigMap config = inputPanel_.getConfig();

        /* Acquire information about selected column data from the GUI. */
        DataSpec dataSpec = inputPanel_.getDataSpec();

        /* Determine plot layer style from the acquired config. */
        S style = plotter_.createStyle( config );

        /* Construct a plot layer object, which defines what is plotted. */
        DataSpec[] dataSpecs = new DataSpec[] { dataSpec };
        PlotLayer layer = plotter_.createLayer( geom_, dataSpec, style );
        PlotLayer[] layers = new PlotLayer[] { layer };

        /* Read, and possibly cache, the data required to perform the plot. */
        DataStore prevStore = dataStore_;
        DataStore dataStore = dstoreFact_.readDataStore( dataSpecs, prevStore );
        dataStore_ = dataStore;

        /* Perform a load of other setup required for the plot.
         * We are skipping some optional items such as legends here.
         * Some layer types would require more work. */
        Icon legend = null;
        float[] legPos = null;
        String title = null;
        ShadeAxisFactory shadeFact = null;
        Span shadeFixSpan = null;
        PaperTypeSelector ptSel = plotType_.getPaperTypeSelector();
        Compositor compositor = Compositor.SATURATION;
        Padding padding = new Padding();
        boolean surfaceAuxRange = true;
        boolean navigable = true;
        PlotCaching caching = PlotCaching.createFullyCached();

        /* Create and return the live, navigable plot display object.
         * See the implementation in that class for the various bits of
         * magic this involves. */
        return PlotDisplay
              .createPlotDisplay( layers, sfact_, config,
                                  legend, legPos, title, shadeFact,
                                  shadeFixSpan, ptSel, compositor,
                                  padding, dataStore, navigable, caching );
    }

    /**
     * Reports a selected point.
     * Currently it prints the row index and position to standard output.
     *
     * @param  evt  point selection event
     */
    private void reportPoint( PointSelectionEvent evt ) {
        long irow = evt.getClosestRows()[ 0 ];  // only one layer
        String txt = irow >= 0 ? ( "Point #" + irow ) : "(no point)";
        Point gpos = evt.getPoint();
        int isurf = evt.getSurfaceIndex();
        Surface surface = plotDisplay_.getScene().getSurfaces()[ isurf ];
        if ( surface != null ) {
            double[] dpos = surface.graphicsToData( gpos, null );
            if ( dpos != null ) {
                txt += " at (" + surface.formatPosition( dpos ) + ")";
            }
        }
        System.err.println( "\t" + txt );
    }

    /**
     * Creates a BasicPlotGui instance.
     * This just invokes the constructor, but takes care of the
     * parameterised types.
     *
     * @param  plotType  plot type
     * @param  plotter   plotter defining the single plot layer
     *                   this component plots
     * @param  table    table containing input data to plot
     */
    public static <P,A,S extends Style> BasicPlotGui<P,A,S>
            createBasicPlotGui( PlotType<P,A> plotType, Plotter<S> plotter,
                                StarTable table ) {
        return new BasicPlotGui<P,A,S>( plotType, plotter, table );
    }

    /**
     * Constructs an InputPanel instance that acquires configuration
     * and data input values required for a certain plot type.
     *
     * @param   plotter   defines the plot layer type
     * @param   geom      defines the positional coordinate mapping
     * @param   sfact     defines the type of plot surface
     * @param   table     supplies the data columns
     * @return   GUI component to acquire plot information from the user
     */
    private static InputPanel createInputPanel( Plotter<?> plotter,
                                                DataGeom geom,
                                                SurfaceFactory<?,?> sfact,
                                                StarTable table ) {

        /* Work out the data values required to plot the layer. */
        CoordGroup cgrp = plotter.getCoordGroup();
        List<Coord> coordList = new ArrayList<Coord>();
        for ( int ipos = 0; ipos < cgrp.getBasicPositionCount(); ipos++ ) {
            coordList.addAll( Arrays.asList( geom.getPosCoords() ) );
        }
        coordList.addAll( Arrays.asList( cgrp.getExtraCoords() ) );

        /* Construct and return an input panel that solicits all required
         * information from the user. */
        InputPanel inputPanel = new InputPanel();
        inputPanel.addCoords( "Data", coordList.toArray( new Coord[ 0 ] ),
                              table );
        inputPanel.addConfigKeys( "Style", plotter.getStyleKeys() ); 
        inputPanel.addConfigKeys( "Profile", sfact.getProfileKeys() );
        inputPanel.addConfigKeys( "Aspect", sfact.getAspectKeys() );
        inputPanel.addConfigKeys( "Navigator", sfact.getNavigatorKeys() );
        return inputPanel;
    }

    /**
     * Component that can acquire config and coord information from the user.
     */
    private static class InputPanel extends JPanel {
        private final JTabbedPane tabber_;
        private final List<KeySpec<?>> klist_;
        private final List<CoordInput> clist_;

        /**
         * Constructor.
         */
        InputPanel() {
            super( new BorderLayout() );
            tabber_ = new JTabbedPane();
            klist_ = new ArrayList<KeySpec<?>>();
            clist_ = new ArrayList<CoordInput>();
            add( tabber_, BorderLayout.CENTER );
        }

        /**
         * Adds a group of configuration specifiers to the GUI.
         * Each added key defines an item of information required from the user.
         *
         * @param  title  short title for this group of items
         * @param  keys  config keys
         */
        public void addConfigKeys( String title, ConfigKey<?>[] keys ) {
            LabelledComponentStack stack = new LabelledComponentStack();
            for ( ConfigKey<?> key : keys ) {
                KeySpec<?> kspec = createKeySpec( key );
                klist_.add( kspec );
                ConfigMeta meta = key.getMeta();

                /* More key metadata exists;
                 * you could add tooltips etc here if you wanted to. */
                stack.addLine( meta.getShortName(),
                               kspec.specifier_.getComponent() );
            }
            addTab( title, stack );
        }

        /**
         * Returns the configuration acquired from the user.
         * In general this contains an entry for each of the ConfigKeys
         * added earlier.
         *
         * @return   user-supplied configuration information
         */
        public ConfigMap getConfig() {
            ConfigMap map = new ConfigMap();
            for ( KeySpec<?> k : klist_ ) {
                k.putValue( map );
            }
            return map;
        }

        /**
         * Adds a group of coordinate selectors to the GUI.
         * Each added coord defines a data item (column of the table)
         * required from the user.
         *
         * @param  title  short title for this group of coordinates
         * @param  coords   coordinate specifiers
         * @param  table   input table from which data can be read
         */
        public void addCoords( String title, Coord[] coords, StarTable table ) {
            LabelledComponentStack stack = new LabelledComponentStack();
            for ( Coord coord : coords ) {
                CoordInput cinput = new CoordInput( coord, table );
                clist_.add( cinput );
                Input[] inputs = coord.getInputs();
                for ( int ii = 0; ii < inputs.length; ii++ ) {
                    InputMeta meta = inputs[ ii ].getMeta();

                    /* More input-field metadata exists;
                     * you could add tooltips etc here if you wanted to. */
                    stack.addLine( meta.getShortName(),
                                   cinput.entryBoxes_[ ii ] );
                }
            }
            JComponent box = Box.createVerticalBox();
            box.add( new JLabel( "Choose columns or enter expressions" ) );
            box.add( stack );
            addTab( title, box );
        }

        /**
         * Returns the data specifier that encapsulates the input coordinate
         * information specified by the user.
         *
         * @return   user-supplied data spec
         */
        public DataSpec getDataSpec() {
            int nc = clist_.size();
            CoordValue[] cvals = new CoordValue[ nc ];
            for ( int ic = 0; ic < nc; ic++ ) {
                CoordInput cinput = clist_.get( ic );
                JComboBox<?>[] entryBoxes = cinput.entryBoxes_;
                int nin = entryBoxes.length;
                String[] inExprs = new String[ nin ];
                DomainMapper[] dms = new DomainMapper[ nin ];
                for ( int ii = 0; ii < nin; ii++ ) {
                    inExprs[ ii ] = (String) entryBoxes[ ii ].getSelectedItem();
                }
                cvals[ ic ] = new CoordValue( cinput.coord_, inExprs, dms );
            }
            StarTable table = clist_.get( 0 ).table_;  // must be same for all
            try {
                return new JELDataSpec( table, null, cvals );
            }
            catch ( TaskException e ) {
                JOptionPane.showMessageDialog( this, e.getMessage(),
                                               "Bad Coordinate Value",
                                               JOptionPane.ERROR_MESSAGE );
                return null;
            }
        }

        /**
         * Adds a component under a heading.
         *
         * @param  title  short title for GUI section
         * @param  comp   component to add
         */
        private void addTab( String title, JComponent comp ) {
            JPanel panel = new JPanel( new BorderLayout() );
            panel.add( comp, BorderLayout.NORTH );
            tabber_.add( title, new JScrollPane( panel ) );
        }

        /**
         * Constructs a KeySpec object given a ConfigKey.
         *
         * @param  key  key
         * @return  keySpec
         */
        private static <T> KeySpec<T> createKeySpec( ConfigKey<T> key ) {
            return new KeySpec<T>( key );
        }

        /**
         * Typed class that aggregates a config key and a GUI component
         * that can acquire its value (a Specifier).
         */
        private static class KeySpec<T> {
            final ConfigKey<T> key_;
            final Specifier<T> specifier_;

            /**
             * Constructor.
             *
             * @param   key  key
             */
            KeySpec( ConfigKey<T> key ) {
                key_ = key;
                specifier_ = key_.createSpecifier();
                specifier_.setSpecifiedValue( key_.getDefaultValue() );
            }

            /**
             * Transfers the currently set value of this object's GUI
             * specifier to a supplied map.
             *
             * @param   map  destination map
             */
            void putValue( ConfigMap map ) {
                map.put( key_, specifier_.getSpecifiedValue() );
            }
        }

        /**
         * Aggregates a coordinate description and a GUI component
         * that can acquire a value for it in the context of a given table.
         */
        private static class CoordInput {
            final StarTable table_;
            final Coord coord_;
            final JComboBox<?>[] entryBoxes_;

            /**
             * Constructor.
             *
             * @param   coord  coordinate description
             * @param  table  source for coordinate data
             */
            CoordInput( Coord coord, StarTable table ) {
                coord_ = coord;
                table_ = table;
                Input[] inputs = coord.getInputs();
                entryBoxes_ = new JComboBox<?>[ inputs.length ];
                for ( int i = 0; i < inputs.length; i++ ) {
                    entryBoxes_[ i ] =
                        createColumnEntryBox( table, inputs[ i ].getDomain() );
                }
            }

            /** 
             * Returns a combo box that can acquire a typed value in the
             * context of a given table.
             *
             * @param   table  table supplying data
             * @param   domain   value domain for data column entries
             */
            private static JComboBox<?>
                    createColumnEntryBox( StarTable table, Domain<?> domain ) {

                /* Add an item to the combo box for each column with a
                 * value of the right type.  But you can also type in
                 * expressions using the JEL expression language. */
                int ncol = table.getColumnCount();
                List<String> cnameList = new ArrayList<String>();
                cnameList.add( null );
                for ( int ic = 0; ic < ncol; ic++ ) {
                    ColumnInfo info = table.getColumnInfo( ic );
                    if ( domain.getPossibleMapper( info ) != null ) {
                        cnameList.add( info.getName() );
                    }
                }
                JComboBox<?> combo =
                    new JComboBox<Object>( cnameList
                                          .toArray( new String[ 0 ] ) );
                combo.setEditable( true );
                combo.setSelectedItem( null );
                return combo;
            }
        }
    }

    /**
     * Main method.
     * Invoke it with the name of a (FITS or VOTable) table as the first
     * argument.
     */
    public static void main( String[] args ) throws IOException {
        // Turn off logging here if you want to.
        // Logger.getLogger( "uk.ac.starlink" ).setLevel( Level.WARNING );

        if ( args.length != 1 ) {
            System.err.println( "usage: " + BasicPlotGui.class.getName() + " "
                              + "<table-location>" );
            System.exit( 1 );
        }
        StarTable table = new StarTableFactory().makeStarTable( args[ 0 ] );

        /* Plane plot - could be something different. */
        PlotType<?,?> plotType = PlanePlotType.getInstance();

        /* Pick the first plotter it provides - could be something different. */
        Plotter<?> plotter = plotType.getPlotters()[ 0 ];

        /* Create and post GUI. */
        BasicPlotGui<?,?,?> gui =
            createBasicPlotGui( plotType, plotter, table );
        JFrame frm = new JFrame();
        frm.getContentPane().add( gui );
        frm.pack();
        frm.setVisible( true );

        /* Instructions. */
        System.out.println();
        System.out.println( "  PlotType: " + plotType.getClass().getName() );
        System.out.println( "  Plot layer: " + plotter.getPlotterName() );
        System.out.println( "  --> Select columns, configure options, then "
                          + "hit the \"(Re)Plot\" button" );
        System.out.println( "  --> You can navigate the plot using the mouse" );
        System.out.println( "  --> Click on a point to identify it on stdout" );
        System.out.println();
    }
}
