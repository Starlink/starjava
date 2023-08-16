package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.GangContext;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.GangerFactory;
import uk.ac.starlink.ttools.plot2.Padding;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.IntegerConfigKey;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;

/**
 * GangerFactory for use with a square matrix of Plane plots.
 * Histogram-like plots are expected on the diagonal, and scatter-plot
 * like plots on the off-diagonal cells.
 *
 * @author   Mark Taylor
 * @since    1 Jun 2023
 */
public class MatrixGangerFactory
        implements GangerFactory<PlaneSurfaceFactory.Profile,PlaneAspect> {
    
    /** Config key for number of input coordinates. */
    public static final IntegerConfigKey NCOORD_KEY = createCoordCountKey();

    /** Config key for matrix format. */
    public static final ConfigKey<MatrixFormat> FORMAT_KEY =
        createMatrixFormatKey();

    /** Config key for whether cells are constrained to be square. */
    public static final BooleanConfigKey SQUARES_KEY = createSquaresKey();

    /** Config key for gap between matrix cells. */
    public static final IntegerConfigKey CELLGAP_KEY = createCellGapKey();

    /** Maximum number of coords offered by NCOORD_KEY Specifier. */
    public static final int MAX_NCOORD_GUI = 64;

    /** Sole instance.  @see {@link #instance}. */
    @SuppressWarnings("rawtypes")
    public static final MatrixGangerFactory INSTANCE =
        new MatrixGangerFactory();

    /**
     * Private sole constructor prevents external instantiation of
     * singleton class.
     */
    private MatrixGangerFactory() {
    }

    public boolean hasIndependentZones() {
        return false;
    }

    public ConfigKey<?>[] getGangerKeys() {
        return new ConfigKey<?>[] {
            NCOORD_KEY,
            FORMAT_KEY,
            CELLGAP_KEY,
            SQUARES_KEY,
        };
    }

    public Ganger<PlaneSurfaceFactory.Profile,PlaneAspect>
            createGanger( Padding padding, ConfigMap config,
                          GangContext context ) {
        MatrixShape shape = getShape( config, context.getPlotters() );
        int cellGap = config.get( CELLGAP_KEY ).intValue();
        boolean isSquares = config.get( SQUARES_KEY );
        return new MatrixGanger( shape, padding, isSquares, cellGap );
    }

    /**
     * Returns the matrix shape that this factory will use given a
     * set of plot types and a configuration map.
     *
     * @param  config  config map
     * @param  plotters   plotters generating plots on the matrix
     * @return   matrix shape
     */
    public MatrixShape getShape( ConfigMap config, Plotter<?>[] plotters ) {
        int ncoord = config.get( NCOORD_KEY ).intValue();
        MatrixFormat format = config.get( FORMAT_KEY );
        boolean hasOnDiag = false;
        boolean hasOffDiag = false;
        for ( Plotter<?> plotter : plotters ) {
            CoordGroup cgrp = plotter.getCoordGroup();
            hasOnDiag = hasOnDiag || MatrixFormat.isOnDiagonal( cgrp );
            hasOffDiag = hasOffDiag || MatrixFormat.isOffDiagonal( cgrp );
        }
        return format.getShape( ncoord, hasOnDiag, hasOffDiag );
    }

    /**
     * Returns the sole instance of this class.
     *
     * @return  factory instance
     */
    public static MatrixGangerFactory instance() {
        return INSTANCE;
    }

    /**
     * Creates the config key for the number of input variables.
     *
     * @return  new config key
     */
    private static IntegerConfigKey createCoordCountKey() {
        ConfigMeta meta = new ConfigMeta( "nvar", "Variables" );
        meta.setShortDescription( "Number of variables" );
        meta.setXmlDescription( new String[] {
            "<p>Gives the number of quantities to be plotted against",
            "each other,",
            "which will be the number of cells along each side of the",
            "scatter plot matrix.",
            "</p>",
        } );
        return IntegerConfigKey.createSpinnerKey( meta, 3, 2, MAX_NCOORD_GUI );
    }

    /**
     * Creates the config key for selecting matrix format.
     *
     * @return  new config key
     */
    private static ConfigKey<MatrixFormat> createMatrixFormatKey() {
        ConfigMeta meta = new ConfigMeta( "matrixformat", "Matrix Format" );
        meta.setShortDescription( "Shape variant of plot grid" );
        meta.setXmlDescription( new String[] {
            "<p>Configures which cells of the matrix grid will be filled in.",
            "Below-diagonal, above-diagonal, or the full matrix can be chosen.",
            "Given grid cells will only appear if there are",
            "appropriate plot layers specified, i.e.",
            "2-coordinate (scatter-plot-like) plots for the off-diagonal cells",
            "and 1-coordinate (histogram-like) plots for the diagonal cells.",
            "</p>",
        } );
        MatrixFormat[] options = MatrixFormat.values();
        boolean useRadio = true;
        return new OptionConfigKey<MatrixFormat>( meta, MatrixFormat.class,
                                                  options, options[ 0 ],
                                                  useRadio ) {
            public String getXmlDescription( MatrixFormat fmt ) {
                return fmt.getDescription();
            }
        }.addOptionsXml();
    }

    /**
     * Creates the config key for constraining matrix cells to be square.
     *
     * @return  new config key
     */
    private static BooleanConfigKey createSquaresKey() {
        ConfigMeta meta = new ConfigMeta( "squares", "Square Panels" );
        meta.setShortDescription( "Force square matrix elements?" );
        meta.setXmlDescription( new String[] {
            "<p>If true, each of the plotted panels in the matrix",
            "will have the same vertical and horizontal dimension.",
            "If false, the shape of each panel will be determined",
            "by the shape of the overall plotting area.",
            "</p>",
        } );
        return new BooleanConfigKey( meta, false );
    }

    /**
     * Creates the config key for configuring the displayed gap
     * between matrix cells.
     *
     * @return  new config key
     */
    private static IntegerConfigKey createCellGapKey() {
        ConfigMeta meta = new ConfigMeta( "cellgap", "Cell Gap" );
        meta.setShortDescription( "Gap between matrix cells" );
        meta.setXmlDescription( new String[] {
            "<p>Gives the number of pixels between cells in the",
            "displayed matrix of plots.",
            "</p>",
        } );
        return IntegerConfigKey.createSpinnerKey( meta, 4, 0, 32 );
    }
}
