package uk.ac.starlink.ttools.plottask;

import gnu.jel.CompilationException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.task.BooleanParameter;
import uk.ac.starlink.task.ChoiceParameter;
import uk.ac.starlink.task.DoubleParameter;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.ExecutionException;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.task.ParameterValueException;
import uk.ac.starlink.task.TaskException;
import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.ttools.plot.DataBounds;
import uk.ac.starlink.ttools.plot.MarkShape;
import uk.ac.starlink.ttools.plot.MarkStyle;
import uk.ac.starlink.ttools.plot.MarkStyles;
import uk.ac.starlink.ttools.plot.MultiPlotData;
import uk.ac.starlink.ttools.plot.PlotData;
import uk.ac.starlink.ttools.plot.PlotState;
import uk.ac.starlink.ttools.plot.Shader;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot.SubsetSelectionPlotData;
import uk.ac.starlink.ttools.plot.TablePlot;
import uk.ac.starlink.ttools.plot.WrapperPlotData;
import uk.ac.starlink.ttools.task.ConsumerTask;
import uk.ac.starlink.ttools.task.DefaultMultiParameter;
import uk.ac.starlink.ttools.task.FilterParameter;
import uk.ac.starlink.ttools.task.InputTableParameter;
import uk.ac.starlink.ttools.task.TableProducer;

/**
 * Obtains a {@link uk.ac.starlink.ttools.plot.PlotState} and associated
 * {@link uk.ac.starlink.ttools.plot.PlotData} from the execution environment.
 * It sets up and interrogates a lot of parameters which describe how the
 * plot should be done, and organises this information into a single 
 * object, a PlotState.  It is subclassed for different plot types.
 *
 * @author   Mark Taylor
 * @since    22 Apr 2008
 */
public class PlotStateFactory {

    /** Symbolic suffix representing a table in per-table parameter names. */
    public static final String TABLE_VARIABLE = "N";

    /** Symbolic suffix representing a subset in per-subset parameter names. */
    public static final String SUBSET_VARIABLE = "S";

    /** Symbolic suffix representing an auxiliary axis in per-aux 
     * parameter names.  Currently blank, as only one auxiliary axis is
     * provided for. */
    public static final String AUX_VARIABLE = "";

    private static final String TABLE_PREFIX = "in";
    private static final String FILTER_PREFIX = "cmd";
    private static final String SUBSET_PREFIX = "subset";
    private static final String AUX_PREFIX = "aux";
    private static final String STYLE_PREFIX = "";
    private static final double PAD_RATIO = 0.02;

    private final String[] mainDimNames_;
    private final boolean useAux_;
    private final boolean useLabel_;
    private final int errNdim_;
    private final BooleanParameter gridParam_;
    private final DefaultMultiParameter seqParam_;
    private final BooleanParameter aaParam_;

    /**
     * Constructor.
     *
     * @param  dimNames names of main plot dimensions (typically "X", "Y", etc);
     * @param  useAux  whether auxiliary axes are used
     * @param  useLabel  whether point text labelling is used
     * @param  errNdim  number of axes for which errors can be plotted
     */
    public PlotStateFactory( String[] dimNames, boolean useAux,
                             boolean useLabel, int errNdim ) {
        mainDimNames_ = dimNames;
        useAux_ = useAux;
        useLabel_ = useLabel;
        errNdim_ = errNdim;

        gridParam_ = new BooleanParameter( "grid" );
        gridParam_.setPrompt( "Draw grid lines?" );
        gridParam_.setDescription( new String[] {
            "<p>If true, grid lines are drawn on the plot.",
            "If false, they are absent.",
            "</p>",
        } );
        gridParam_.setDefault( true );

        seqParam_ = new DefaultMultiParameter( "sequence", ',' );
        seqParam_.setPrompt( "Defines plot order of subsets" );
        seqParam_.setUsage( "<suffix>,<suffix>,..." );
        seqParam_.setDescription( new String[] {
            "<p>Can be used to control the sequence in which different",
            "datasets and subsets are plotted.",
            "This will affect which symbols are plotted on top of,",
            "and so potentially obscure,",
            "which other ones.",
            "The value of this parameter is a comma-separated list of the",
            "\"<code>" + TABLE_VARIABLE + SUBSET_VARIABLE + "</code>\"",
            "suffixes which appear on the",
            "parameters which apply to subsets.",
            "The sets which are named",
            "will be plotted in order, so the first-named one will be",
            "at the bottom (most likely to be obscured).",
            "Note that if this parameter is supplied, then only those sets",
            "which are named will be plotted,",
            "so this parameter may also be used to restrict which plots appear",
            "(though it may not be the most efficient way of doing this).",
            "If no explicit value is supplied for this parameter,",
            "sets will be plotted in some sequence decided by STILTS",
            "(probably alphabetic by suffix).",
            "</p>",
        } );
        seqParam_.setNullPermitted( true );

        aaParam_ = new BooleanParameter( "antialias" );
        aaParam_.setPrompt( "Use antialiasing for lines?" );
        aaParam_.setDescription( new String[] {
            "<p>Controls whether lines are drawn using antialiasing,",
            "where applicable.",
            "If lines are drawn to a bitmapped-type graphics output format",
            "setting this parameter to true smooths the lines out by",
            "using gradations of colour for diagonal lines, and setting it",
            "false simply sets each pixel in the line to on or off.",
            "For vector-type graphics output formats, or for cases in which",
            "no diagonal lines are drawn, the setting of this parameter",
            "has no effect.",
            "Setting it true may slow the plot down slightly.",
            "</p>",
        } );
        aaParam_.setDefault( true );
    }

    /**
     * Returns the parameters associated with this object.
     * The returned list is intended for external use in documentation;
     * the parameter objects returned may or may not be those used for
     * obtaining values from a particular execution environment.
     * For this reason they may have names which are symbolic,
     * that is, represent possible parameter names.  Since actual parameter
     * names are dynamically determined from other parameter names, 
     * it is not possible to return an exhaustive list.
     *
     * @return   array of parameters to be used for documentation
     */
    public Parameter[] getParameters() {

        /* Create and return a list of parameters some of which have
         * "example" suffixes.  In some cases the parameters which 
         * supply actual values to this factory are constructed as 
         * required elsewhere in this class. */
        String tSuffix = TABLE_VARIABLE;
        String stSuffix = TABLE_VARIABLE + SUBSET_VARIABLE;
        String auxAxName = AUX_PREFIX + AUX_VARIABLE;

        /* Per-table input parameters. */
        InputTableParameter inParam = createTableParameter( tSuffix );
        FilterParameter filterParam = createFilterParameter( tSuffix );
        List paramList = new ArrayList();
        paramList.add( inParam );
        paramList.add( inParam.getFormatParameter() );
        paramList.add( inParam.getStreamParameter() );
        paramList.add( filterParam );

        /* Per-axis parameters. */
        List axParamSetList = new ArrayList();
        for ( int idim = 0; idim < mainDimNames_.length; idim++ ) {
            axParamSetList.add( new AxisParameterSet( mainDimNames_[ idim ] ) );
        }
        if ( useAux_ ) {
            axParamSetList.add( new AxisParameterSet( auxAxName ) );
        }
        AxisParameterSet[] axParamSets =
            (AxisParameterSet[])
            axParamSetList.toArray( new AxisParameterSet[ 0 ] );
        int allNdim = axParamSets.length;
        Parameter[][] axScalarParams = new Parameter[ allNdim ][];
        for ( int idim = 0; idim < allNdim; idim++ ) {
            AxisParameterSet axParamSet = axParamSets[ idim ];
            paramList.add( axParamSet.createCoordParameter( tSuffix ) );
            axScalarParams[ idim ] = axParamSet.getScalarParameters();
        }
        int nScalarParam = axScalarParams[ 0 ].length;  // same for all elements
        for ( int ip = 0; ip < nScalarParam; ip++ ) {
            for ( int idim = 0; idim < allNdim; idim++ ) {
                paramList.add( axScalarParams[ idim ][ ip ] );
            }
        }
        for ( int idim = 0; idim < errNdim_; idim++ ) {
            paramList.add( createErrorParameter( mainDimNames_[ idim ],
                                                 tSuffix ) );
        }
        if ( useAux_ ) {
            Parameter shaderParam =
                createShaderParameters( new String[] { AUX_VARIABLE } )[ 0 ];
            assert shaderParam.getDefault() != null;
            paramList.add( shaderParam );
        }

        /* Other parameters. */
        if ( useLabel_ ) {
             paramList.add( createLabelParameter( tSuffix ) );
        }
        paramList.add( createSubsetExpressionParameter( stSuffix ) );
        paramList.add( createSubsetNameParameter( stSuffix ) );
        paramList.addAll( Arrays.asList( createStyleFactory( STYLE_PREFIX )
                                        .getParameters( stSuffix ) ) );
        paramList.add( gridParam_ );
        paramList.add( aaParam_ );
        paramList.add( seqParam_ );
        return (Parameter[]) paramList.toArray( new Parameter[ 0 ] );
    }

    /**
     * Returns a configured PlotState obtained from parameter values
     * specified by the given execution environment.
     *
     * @param   env  execution environment
     */
    public PlotState getPlotState( Environment env ) throws TaskException {
        PlotState state = createPlotState();
        configurePlotState( state, env );
        return state;
    }

    /**
     * Creates a new unconfigured PlotState object suitable for configuration
     * by this factory.
     *
     * @return   plot state
     */
    protected PlotState createPlotState() {
        return new PlotState();
    }

    /**
     * Configures a PlotState object by examining parameter values in a
     * given execution environment.  Such an object was presumably 
     * previously created by a call to {@link #createPlotState}.
     *
     * @param   state  plot state to configure
     * @param   env   execution environment
     */
    protected void configurePlotState( PlotState state, Environment env )
            throws TaskException {
        int mainNdim = mainDimNames_.length;
        state.setMainNdim( mainNdim );
        String[] paramNames = env.getNames();

        /* Work out which parameter suffixes are being used to identify
         * different tables.  This is done by finding all the parameters
         * which start "table" and pulling off their suffixes.
         * These suffixes are then applied to other parameter stems
         * for obtaining other per-table parameter values. */
        String tPrefix = TABLE_PREFIX;
        String[] tableLabels = getSuffixes( paramNames, tPrefix );
        if ( tableLabels.length == 0 ) {
            tableLabels = new String[] { "" };
        }
        Arrays.sort( tableLabels );
        int nTable = tableLabels.length;

        /* Get a list of all the axis names being used.  This includes the
         * main axes and any auxiliary ones. */
        List axNameList = new ArrayList();
        axNameList.addAll( Arrays.asList( mainDimNames_ ) );
        String[] auxLabels;
        if ( useAux_ ) {
            auxLabels = AxisParameterSet.getAuxAxisNames( paramNames );
            Arrays.sort( auxLabels );
            for ( int ia = 0; ia < auxLabels.length; ia++ ) {
                axNameList.add( "aux" + auxLabels[ ia ] );
            }
        }
        else {
            auxLabels = new String[ 0 ];
        }
        String[] allAxNames = (String[]) axNameList.toArray( new String[ 0 ] );
        int allNdim = allAxNames.length;

        /* Assemble parameter groups corresponding to each axis. */
        AxisParameterSet[] axParamSets = new AxisParameterSet[ allNdim ];
        for ( int idim = 0; idim < allNdim; idim++ ) {
            axParamSets[ idim ] = new AxisParameterSet( allAxNames[ idim ] );
        }

        /* Construct a PlotData object for the data obtained from each table. */
        PlotData[] datas = new PlotData[ nTable ];
        String[] coordExprs0 = null;
        StyleFactory styleFactory = createStyleFactory( STYLE_PREFIX );
        List setLabelList = new ArrayList();
        for ( int itab = 0; itab < nTable; itab++ ) {
            String tlabel = tableLabels[ itab ];
            StarTable table = getInputTable( env, tlabel );
            String[] coordExprs = new String[ allNdim ];
            for ( int idim = 0; idim < allNdim; idim++ ) {
                Parameter coordParam =
                    axParamSets[ idim ].createCoordParameter( tlabel );
                if ( idim >= mainNdim ) {
                    coordParam.setNullPermitted( true );
                }
                coordExprs[ idim ] = coordParam.stringValue( env );
            }
            String[] errExprs = new String[ errNdim_ ];
            for ( int idim = 0; idim < errNdim_; idim++ ) {
                errExprs[ idim ] = 
                    createErrorParameter( mainDimNames_[ idim ], tlabel )
                   .stringValue( env );
            }
            String labelExpr =
                useLabel_ ? createLabelParameter( tlabel ).stringValue( env )
                          : null;
            SubsetDef[] subsetDefs =
                getSubsetDefinitions( env, tlabel, styleFactory );
            int nset = subsetDefs.length;
            String[] setExprs = new String[ nset ];
            String[] setNames = new String[ nset ];
            Style[] setStyles = new Style[ nset ];
            for ( int is = 0; is < nset; is++ ) {
                SubsetDef sdef = subsetDefs[ is ];
                setLabelList.add( sdef.label_ );
                setExprs[ is ] = sdef.expression_;
                setNames[ is ] = sdef.name_;
                setStyles[ is ] = sdef.style_;
            }
            try {
                TablePlotData plotData =
                    createPlotData( env, tlabel, table,
                                    setExprs, setNames, setStyles,
                                    labelExpr, coordExprs, errExprs );
                plotData.checkExpressions();
                datas[ itab ] = plotData;
            }
            catch ( CompilationException e ) {
                throw new TaskException( e.getMessage(), e ); 
            }
            if ( itab == 0 ) {
                coordExprs0 = coordExprs;
            }
        }

        /* Set up a plot data object which is an aggregation of the data
         * objects from all the input tables. */
        PlotData plotData = new MultiPlotData( datas );

        /* Rearrange the set plot order if required. */
        StringBuffer seqbuf = new StringBuffer();
        for ( Iterator it = setLabelList.iterator(); it.hasNext(); ) {
            seqbuf.append( it.next() );
            if ( it.hasNext() ) {
                seqbuf.append( seqParam_.getValueSeparator() );
            }
        }
        String seqDefault = seqbuf.toString();
        seqParam_.setDefault( seqDefault );
        String seqString = seqParam_.stringValue( env );
        if ( seqString != null && ! seqString.equals( seqDefault ) ) {
            String[] setLabels = seqParam_.stringValue( env )
                                .split( "\\Q" + seqParam_.getValueSeparator()
                                      + "\\E" );
            int nset = setLabels.length;
            for ( int is = 0; is < nset; is++ ) {
                setLabels[ is ] = setLabels[ is ].trim();
            }
            int[] isets = new int[ nset ];
            for ( int is = 0; is < nset; is++ ) {
                String label = setLabels[ is ];
                isets[ is ] = setLabelList.indexOf( label );
                if ( isets[ is ] < 0 ) {
                    String msg = "Unknown set identifier \"" + label + "\"; "
                               + "known labels are " + setLabelList;
                    throw new ParameterValueException( seqParam_, msg );
                }
            }
            plotData = new SubsetSelectionPlotData( plotData, isets );
        }

        /* Store the calculated plot data object in the plot state. */
        state.setPlotData( plotData );

        /* Configure other per-axis properties. */
        boolean[] logFlags = new boolean[ allNdim ];
        boolean[] flipFlags = new boolean[ allNdim ];
        double[][] ranges = new double[ allNdim ][];
        String[] labels = new String[ allNdim ];
        for ( int idim = 0; idim < allNdim; idim++ ) {
            AxisParameterSet axParamSet = axParamSets[ idim ];
            logFlags[ idim ] = axParamSet.logParam_.booleanValue( env );
            flipFlags[ idim ] = axParamSet.flipParam_.booleanValue( env );
            ranges[ idim ] = new double[] {
                axParamSet.loParam_.doubleValue( env ),
                axParamSet.hiParam_.doubleValue( env ),
            };
            String labelDefault = coordExprs0[ idim ];
            if ( labelDefault == null || labelDefault.trim().length() == 0 ) {
                labelDefault = idim < mainNdim
                             ? mainDimNames_[ idim ]
                             : "Aux " + ( idim - mainNdim + 1 );
            }
            axParamSet.labelParam_.setDefault( labelDefault );
            labels[ idim ] = axParamSet.labelParam_.stringValue( env );
        }
        state.setLogFlags( logFlags );
        state.setFlipFlags( flipFlags );
        state.setRanges( ranges );
        state.setAxisLabels( labels );

        /* Configure per-auxiliary axis properties. */
        Shader[] shaders = new Shader[ auxLabels.length ];
        ShaderParameter[] shaderParams = createShaderParameters( auxLabels );
        for ( int ia = 0; ia < auxLabels.length; ia++ ) {
            shaders[ ia ] = shaderParams[ ia ].shaderValue( env );
        }
        state.setShaders( shaders );

        /* Configure other properties. */
        state.setGrid( gridParam_.booleanValue( env ) );
        state.setAntialias( aaParam_.booleanValue( env ) );
    }

    /**
     * Constructs a PlotData object.
     * Called by {@link #configurePlotState}; may be overridden by subclasses.
     *
     * @param   env  execution environment
     * @param   tLabel  table identifier suffix
     * @param   table  input table
     * @param   setExprs  nset-element JEL boolean-valued expression array
     *                    for set inclusion
     * @param   setNames  nset-element set name array
     * @param   setStyles  nset-elemnt set style array
     * @param   labelExpr  JEL expression for text label
     * @param   coordExprs  ndim-element JEL double-valued expression array
     *                      for coordinate values
     * @param   errExprs    nerr-element expression(s) array for error values
     * @return   new PlotData object based on parameters
     */
    protected TablePlotData createPlotData( Environment env, String tLabel,
                                            StarTable table, String[] setExprs,
                                            String[] setNames,
                                            Style[] setStyles, String labelExpr,
                                            String[] coordExprs,
                                            String[] errExprs )
            throws TaskException, CompilationException {
        return new CartesianTablePlotData( table, setExprs, setNames, setStyles,
                                           labelExpr, coordExprs, errExprs );
    }

    /**
     * Performs additional plot state configuration which may require 
     * a pass through the data.  This may do zero or more of the following:
     * 
     * <ol>
     * <li>Configure the range attributes of the given state, ensuring 
     *     that they have non-NaN values.
     * </ol>
     *
     * @param   state  plot state whose ranges will to be configured
     * @param   plot   table plot for which configuration is to be done
     */
    public void configureFromData( PlotState state, TablePlot plot )
            throws TaskException, IOException {
        if ( requiresConfigureFromBounds( state ) ) {
            configureFromBounds( state, calculateBounds( state, plot ) );
        }
    }

    /**
     * Calculates data bounds for a given data set as appropriate for the
     * given plot.
     *
     * @param  state plot state
     * @param  plot  plot object
     */
    public DataBounds calculateBounds( PlotState state, TablePlot plot ) {
        return plot.calculateBounds( state.getPlotData(), state );
    }

    /**
     * Indicates whether it is necessary to calculate the DataBounds for a
     * given PlotState before it is ready to be used.
     * Iff true is returned, then {@link #configureFromBounds} will be called
     * later.
     *
     * @param   state  plot state
     * @return  whether configureFromBounds should be called
     */
    protected boolean requiresConfigureFromBounds( PlotState state ) {
        PlotData plotData = state.getPlotData();

        /* See if any of the data ranges are missing, and hence need
         * calculation; if so the answer is true. */
        int ndim = plotData.getNdim();
        for ( int idim = 0; idim < ndim; idim++ ) {
            double[] range = state.getRanges()[ idim ];
            if ( Double.isNaN( range[ 0 ] ) || Double.isNaN( range[ 1 ] ) ) {
                return true;
            }
        }

        /* See if any of the plot styles need information from data bounds;
         * if so the answer is true. */
        int nset = plotData.getSetCount();
        for ( int is = 0; is < nset; is++ ) {
            if ( requiresAdjustFromData( plotData.getSetStyle( is ) ) ) {
                return true;
            }
        }

        /* If we've got this far, the answer is false. */
        return false;
    }

    /**
     * Updates a plot state generated by this factory with information 
     * generated from a first pass through the data.
     *
     * @param  state  plot state to update
     * @param  bounds  data bounds calculated by a pass through the data
     */
    protected void configureFromBounds( PlotState state, DataBounds bounds )
            throws TaskException {
        PlotData plotData = state.getPlotData();
        int ndim = bounds.getRanges().length;
        int mainNdim = mainDimNames_.length;

        /* Update plot state range limits as required. */
        for ( int idim = 0; idim < ndim; idim++ ) {
            boolean logFlag = state.getLogFlags()[ idim ];
            double[] stateRange = state.getRanges()[ idim ];
            double[] calcRange = bounds.getRanges()[ idim ]
                                       .getFiniteBounds( logFlag );
            boolean loCalc = Double.isNaN( stateRange[ 0 ] );
            boolean hiCalc = Double.isNaN( stateRange[ 1 ] );
            String dimName = idim < mainNdim ? mainDimNames_[ idim ] : "Aux";
            if ( loCalc ) {
                if ( ! hiCalc && stateRange[ 1 ] <= calcRange[ 0 ] ) {
                    String msg =
                        "Supplied " + dimName + " upper bound (" +
                        stateRange[ 1 ] +
                        ") is less than data lower bound (" +
                        calcRange[ 0 ] +
                        ")";
                    throw new ExecutionException( msg );
                }
                stateRange[ 0 ] = calcRange[ 0 ];
            }
            if ( hiCalc ) {
                if ( ! loCalc && stateRange[ 0 ] >= calcRange[ 1 ] ) {
                    String msg = 
                        "Supplied " + dimName + " lower bound (" +
                        stateRange[ 0 ] +
                        ") is greater than data upper bound (" +
                        calcRange[ 1 ] +
                        ")";
                    throw new ExecutionException( msg );
                }
                stateRange[ 1 ] = calcRange[ 1 ];
            }
            assert stateRange[ 0 ] <= stateRange[ 1 ];

            /* If lower and upper bounds are equal, nudge them down and up
             * respectively by an arbitrary amount. */
            if ( stateRange[ 0 ] == stateRange[ 1 ] ) {
                double val = stateRange[ 0 ];
                if ( val == Math.floor( val ) ) {
                    stateRange[ 0 ]--;
                    stateRange[ 1 ]++;
                }
                else {
                    stateRange[ 0 ] = Math.floor( val );
                    stateRange[ 1 ] = Math.ceil( val );
                }
            }

            /* Otherwise, introduce padding for calculated bounds
             * for non-auxiliary axes only. */
            else {
                if ( idim < mainNdim ) {
                    if ( logFlag ) {
                        double pad =
                            Math.pow( stateRange[ 1 ] / stateRange[ 0 ],
                                      PAD_RATIO );
                        if ( loCalc ) {
                            stateRange[ 0 ] /= pad;
                        }
                        if ( hiCalc ) {
                            stateRange[ 1 ] *= pad;
                        }
                    }
                    else {
                        double pad =
                            ( stateRange[ 1 ] - stateRange[ 0 ] ) * PAD_RATIO;
                        if ( loCalc ) {
                            stateRange[ 0 ] -= pad;
                        }
                        if ( hiCalc ) {
                            stateRange[ 1 ] += pad;
                        }
                    }
                }
            }
            assert state.getRanges()[ idim ][ 0 ] 
                 < state.getRanges()[ idim ][ 1 ];
        }

        /* Update style configurations as required. */
        int nset = plotData.getSetCount();
        final Style[] styles = new Style[ nset ];
        int nAdjust = 0;
        for ( int is = 0; is < nset; is++ ) {
            Style style = plotData.getSetStyle( is );
            if ( requiresAdjustFromData( style ) ) {
                styles[ is ] = adjustFromData( style, is, bounds );
                nAdjust++;
            }
            else {
                styles[ is ] = style;
            }
        }
        if ( nAdjust > 0 ) {
            state.setPlotData( new WrapperPlotData( plotData ) {
                public Style getSetStyle( int is ) {
                    return styles[ is ];
                }
            } );
        }
    }

    /**
     * Indicates whether a given style generated by this factory needs 
     * to be updated with information from a first pass through the data.
     * Iff true is returned, then {@link #adjustFromData} will be called later.
     *
     * @param   style  plot style to consider
     * @return  true iff adjustFromData should be called on <code>style</code>
     */
    public boolean requiresAdjustFromData( Style style ) {
        if ( style instanceof MarkStyle ) {
            MarkStyle mstyle = (MarkStyle) style;
            if ( mstyle.getSize() < 0 ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates a plotting style generated by this factory with information
     * generated from a first pass through the data.
     *
     * @param  style  plot style to update
     * @param  iset   set index for which style is used
     * @param  bounds  data bounds calculated by a pass through the data
     */
    public Style adjustFromData( Style style, int iset, DataBounds bounds ) {
        int npoint = bounds.getPointCounts()[ iset ];
        if ( style instanceof MarkStyle ) {
            MarkStyle mstyle = (MarkStyle) style;
            if ( mstyle.getSize() < 0 ) {
                int size;
                if ( npoint > 100000 ) {
                    size = 0;
                    if ( mstyle.getOpaqueLimit() == 1 ) {
                        mstyle.setOpaqueLimit( npoint / 5000 );
                    }
                }
                else if ( npoint > 10000 ) {
                    size = 0;
                }
                else if ( npoint > 2000 ) {
                    size = 1;
                }
                else if ( npoint > 200 ) {
                    size = 2;
                }
                else if ( npoint > 20 ) {
                    size = 3;
                }
                else {
                    size = 4;
                }
                if ( size != mstyle.getSize() ) {
                    MarkShape shape = size > 0 ? mstyle.getShapeId()
                                               : MarkShape.POINT;
                    MarkStyle style1 =
                        shape.getStyle( mstyle.getColor(), size );
                    style1.setLine( mstyle.getLine() );
                    style1.setLineWidth( mstyle.getLineWidth() );
                    style1.setDash( mstyle.getDash() );
                    style1.setHidePoints( mstyle.getHidePoints() );
                    style1.setOpaqueLimit( mstyle.getOpaqueLimit() );
                    style1.setErrorRenderer( mstyle.getErrorRenderer() );
                    mstyle = style1;
                }
            }
            return mstyle;
        }
        else {
            return style;
        }
    }

    /**
     * Constructs a style factory which can retrieve a plotting style suitable
     * for use with this factory from the environment.
     *
     * @param  prefix  prefix to use for all style-type variables
     */
    protected StyleFactory createStyleFactory( String prefix ) {
        return new MarkStyleFactory( prefix, errNdim_ );
    }

    /**
     * Obtains a table with a given table label from the environment.
     *
     * @param   env   execution environment
     * @param   tlabel  table parameter label
     * @return   input table table
     */
    private StarTable getInputTable( Environment env, String tlabel )
            throws TaskException {
        TableProducer producer =
            ConsumerTask.createProducer( env, createFilterParameter( tlabel ),
                                              createTableParameter( tlabel ) );
        try {
            return producer.getTable();
        }
        catch ( IOException e ) {
            throw new ExecutionException( "Table processing error", e );
        }
    }

    /**
     * Obtains the subset definition object from the environment
     * for a given table.
     *
     * @param  env  execution environment
     * @param  tlabel   table parameter label
     * @param  styleFactory   factory which can examine the environment
     *         for plotting style information
     */
    private SubsetDef[] getSubsetDefinitions( Environment env, String tlabel,
                                              StyleFactory styleFactory )
            throws TaskException {

        /* Work out which parameter suffixes are being used to identify
         * different subsets for the table with parameter suffix tlabel.
         * This is done by finding all the parameters which start 
         * "subset"+tlabel and pulling off their suffixes.  These suffixes
         * are then applied to other parameter stems for obtaining other
         * per-subset parameter values. */
        String[] paramNames = env.getNames();
        String stPrefix = SUBSET_PREFIX + tlabel;
        String[] subLabels = getSuffixes( paramNames, stPrefix );
        Arrays.sort( subLabels );
        int nset = subLabels.length;

        /* If there are no subsets for this table, consider it the same as
         * a single subset with inclusion of all points. */
        if ( nset == 0 ) {
            Parameter nameParam = createSubsetNameParameter( tlabel );
            nameParam.setDefault( tlabel );
            String name = nameParam.stringValue( env );
            return new SubsetDef[] {
                new SubsetDef( tlabel, "true", name,
                               styleFactory.getStyle( env, tlabel ) ),
            };
        }

        /* If there is at least one subset, gather the information required
         * to construct a SubsetDef object describing its characteristics. */
        else {
            SubsetDef[] sdefs = new SubsetDef[ nset ];
            for ( int is = 0; is < nset; is++ ) {
                String stLabel = tlabel + subLabels[ is ];
                String expr = createSubsetExpressionParameter( stLabel )
                             .stringValue( env );
                Parameter nameParam = createSubsetNameParameter( stLabel );
                nameParam.setDefault( expr );
                String name = nameParam.stringValue( env );
                sdefs[ is ] =
                    new SubsetDef( stLabel, expr, name,
                                   styleFactory.getStyle( env, stLabel ) );
            }
            return sdefs;
        }
    }

    /**
     * Constructs an input table parameter with a given suffix.
     *
     * @param   tlabel  table parameter label
     * @return  new input table parameter
     */
    private InputTableParameter createTableParameter( String tlabel ) {
        return new InputTableParameter( TABLE_PREFIX + tlabel );
    }

    /**
     * Constructs an input filter parameter with a given suffix.
     *
     * @param   tlabel  table parameter label
     * @return  new table filter parameter
     */
    private FilterParameter createFilterParameter( String tlabel ) {
        return new FilterParameter( FILTER_PREFIX + tlabel );
    }

    /**
     * Returns a parameter giving expressions for the error values associated
     * with a given coordinate axis, indexed by a table-identifying label.
     * The value taken by this parameter is a string which is either a single
     * (symmetric) numeric error expression or two comma-separated 
     * error expressions.
     *
     * @param   axName  axis label
     * @param   tlabel  table identifier
     * @return   error pair parameter
     */
    private Parameter createErrorParameter( String axName, String tlabel ) {
        Parameter param =
            new Parameter( axName.toLowerCase() + "error" + tlabel );
        param.setUsage( "<expr>|[<lo-expr>],[<hi-expr>]" );
        param.setPrompt( "Error bound(s) in " + axName + " for table "
                       + tlabel );
        param.setNullPermitted( true );
        param.setDescription( new String[] {
            "<p>Gives expressions for the errors on " + axName,
            "coordinates for table " + tlabel + ".",
            "The following forms are permitted:",
            "<ul>",
            "<li><code>&lt;expr&gt;</code>: symmetric error value</li>",
            "<li><code>&lt;lo-expr&gt;,&lt;hi-expr&gt;</code>:" +
                 "distinct lower and upper error values</li>",
            "<li><code>&lt;lo-expr&gt;,</code>: lower error value only</li>",
            "<li><code>,&lt;hi-expr&gt;</code>: upper error value only</li>",
            "<li><code>null</code>: no errors</li>",
            "</ul>",
            "The expression in each case is a numeric algebraic expression",
            "based on column names",
            "as described in <ref id='jel'/>.",
            "</p>",
        } );
        return param;
    }

    /**
     * Constructs a new text label expression parameter.
     *
     * @param  tlabel   table parameter label
     * @return  new text label expression parameter
     */
    private Parameter createLabelParameter( String tlabel ) {
        Parameter labelParam = new Parameter( "txtlabel" + tlabel );
        labelParam.setPrompt( "Label annotating each plotted point" );
        labelParam.setDescription( new String[] {
            "<p>Gives an expression which will label each plotted point.",
            "If given, the text (or number) resulting from evaluating",
            "the expression will be written near each point which is",
            "plotted.",
            "</p>",
        } );
        labelParam.setNullPermitted( true );
        return labelParam;
    }

    /**
     * Constructs a new subset inclusion expression parameter.
     *
     * @param  stlabel  table/subset parameter label
     * @return  new subset expression parameter
     */
    private Parameter createSubsetExpressionParameter( String stlabel ) {
        Parameter param = new Parameter( SUBSET_PREFIX + stlabel );
        param.setPrompt( "Selection criterion for subset " + stlabel );
        param.setDescription( new String[] {
            "<p>Gives the selection criterion for the subset labelled",
            "\"<code>" + stlabel + "</code>\".",
            "This is a boolean expression which may be the name of",
            "a boolean-valued column or any other boolean-valued expression.",
            "Rows for which the expression evaluates true will be included",
            "in the subset, and those for which it evaluates false will not.",
            "</p>",
        } );
        param.setUsage( "<expr>" );
        return param;
    }

    /**
     * Constructs a new subset name parameter. 
     *
     * @param  stlabel  table/subset parameter label
     * @return  new subset name parameter
     */
    private Parameter createSubsetNameParameter( String stlabel ) {
        Parameter nameParam = new Parameter( STYLE_PREFIX + "name" + stlabel );
        nameParam.setNullPermitted( true );
        nameParam.setPrompt( "Label for subset " + stlabel );
        nameParam.setDescription( new String[] {
            "<p>Provides a name to use for a subset with the symbolic label",
            stlabel + ".",
            "This name will be used for display in the legend,",
            "if one is displayed.",
            "</p>",
        } );
        return nameParam;
    }

    /**
     * Constructs one or more parameters for selecting shaders on auxiliary
     * axes.
     *
     * @param  auxlabels  labels for auxiliary axes for which shaders are
     *                    required
     * @return   shader parameter array (one for each auxlabel)
     */
    private ShaderParameter[] createShaderParameters( String[] auxlabels ) {
        int nparam = auxlabels.length;
        ShaderParameter[] params = new ShaderParameter[ nparam ];
        String[] dflts = ShaderParameter.getDefaultValues( nparam );
        for ( int i = 0; i < nparam; i++ ) {
            params[ i ] =
                new ShaderParameter( AUX_PREFIX + auxlabels[ i ] + "shader" );
            params[ i ].setDefault( dflts[ i ] );
        }
        return params;
    }

    /**
     * Returns an array of unique identifying suffixes associated with a
     * given prefix from a list of strings.  Each string in <code>names</code>
     * is examined, and if it starts with <code>prefix</code> the prefix
     * part is removed and the remaining part is stored in a list for return.
     *
     * @param  names  list of strings for analysis
     * @param  prefix  common prefix from which to take suffixes
     * @return  array of unique suffix values
     */
    private static String[] getSuffixes( String[] names, String prefix ) {
        List suffixList = new ArrayList();
        for ( int i = 0; i < names.length; i++ ) {
            if ( names[ i ].toLowerCase().startsWith( prefix.toLowerCase() ) ) {
                String suffix = names[ i ].substring( prefix.length() );
                if ( ! suffixList.contains( suffix ) ) {
                    suffixList.add( suffix );
                }
            }
        }
        return (String[]) suffixList.toArray( new String[ 0 ] );
    }

    /**
     * Aggregates the parameters which pertain to a single axis.
     */
    private static class AxisParameterSet {
        final String axName_;
        final DoubleParameter loParam_;
        final DoubleParameter hiParam_;
        final BooleanParameter logParam_;
        final BooleanParameter flipParam_;
        final Parameter labelParam_;

        private static Pattern auxAxisRegex_;

        /**
         * Constructor.
         *
         * @param  axName  axis name
         */
        public AxisParameterSet( String axName ) {
            axName_ = axName.toLowerCase();
            loParam_ = new DoubleParameter( axName_ + "lo" );
            loParam_.setPrompt( "Lower bound for " + axName_ + " axis" );
            loParam_.setDescription( new String[] {
                "<p>The lower limit for the plotted " + axName_ + " axis.",
                "If not set, a value will be chosen which is low enough",
                "to accommodate all the data.",
                "</p>",
            } );
            loParam_.setNullPermitted( true );

            hiParam_ = new DoubleParameter( axName_ + "hi" );
            hiParam_.setPrompt( "Upper bound for " + axName_ + " axis" );
            hiParam_.setDescription( new String[] {
                "<p>The upper limit for the plotted " + axName_ + " axis.",
                "If not set, a value will be chosen which is high enough",
                "to accommodate all the data.",
                "</p>",
            } );
            hiParam_.setNullPermitted( true );

            logParam_ = new BooleanParameter( axName_ + "log" );
            logParam_.setPrompt( "Logarithmic scale on " + axName_
                               + " axis?" );
            logParam_.setDescription( new String[] {
                "<p>If false (the default), the scale on the " + axName_,
                "axis is linear; if true it is logarithmic.",
                "</p>",
            } );
            logParam_.setDefault( "false" );

            flipParam_ = new BooleanParameter( axName_ + "flip" );
            flipParam_.setPrompt( "Reversed direction on " + axName_
                                + "axis?" );
            flipParam_.setDescription( new String[] {
                "<p>If set true, the scale on the " + axName_ + " axis",
                "will increase in the opposite sense from usual",
                "(e.g. right to left rather than left to right).",
                "</p>",
            } );
            flipParam_.setDefault( "false" );

            labelParam_ = new Parameter( axName_ + "label" );
            labelParam_.setPrompt( "Label for axis " + axName_ );
            labelParam_.setDescription( new String[] {
                "<p>Specifies a label to be used for annotating axis "
                + axName_ + ".",
                "A default values based on the plotted data will be used",
                "if no value is supplied for this parameter.",
                "</p>",
            } );
            labelParam_.setNullPermitted( true );
        }

        /**
         * Returns an array of the parameters which have a one-to-one 
         * correspondence with this axis.
         * A corresponding array will be returned for all instances of this
         * class.
         *
         * @return   array of parameters used by this axis
         */
        public Parameter[] getScalarParameters() {
            return new Parameter[] {
                loParam_,
                hiParam_,
                logParam_,
                flipParam_,
                labelParam_,
            };
        }

        /**
         * Returns a parameter giving an expression for the coordinate values
         * used by this axis.  It is indexed by a table-identifying label.
         *
         * @param  tlabel  table identifier
         * @return   parameter giving JEL expression for coordinate data
         */
        public Parameter createCoordParameter( String tlabel ) {
            Parameter param = new Parameter( axName_ + "data" + tlabel );
            param.setUsage( "<expr>" );
            param.setPrompt( "Value to plot on " + axName_ + " axis"
                           + " for table " + tlabel );
            param.setDescription( new String[] {
                "<p>Gives a column name or expression for the " + axName_,
                "axis data for table " + tlabel + ".",
                "The expression is a numeric algebraic expression",
                "based on column names",
                "as described in <ref id=\"jel\"/>",
                "</p>",
            } );
            return param;
        }

        /**
         * Returns a regular expression which will pull out auxiliary axis
         * names from a parameter name.  The aux axis name is the first
         * match group of a match using the returned regular expression.
         *
         * @return   regular expression pattern
         */
        private static Pattern getAuxAxisRegex() {
            if ( auxAxisRegex_ == null ) {

                /* Assemble list of base parameter names (no prefixes). */
                AxisParameterSet paramSet = new AxisParameterSet( "" );
                List nameList = new ArrayList();
                nameList.add( paramSet.createCoordParameter( "" ).getName() );
                Parameter[] scalarParams = paramSet.getScalarParameters();
                for ( int ip = 0; ip < scalarParams.length; ip++ ) {
                    nameList.add( scalarParams[ ip ].getName() );
                }

                /* Assemble regular expression string. */
                StringBuffer sbuf = new StringBuffer()
                    .append( "\\Q" )
                    .append( AUX_PREFIX )
                    .append( "\\E" )
                    .append( '(' )
                    .append( ".*" )
                    .append( ')' )
                    .append( '(' );
                for ( Iterator it = nameList.iterator(); it.hasNext(); ) {
                    String baseName = (String) it.next();
                    sbuf.append( "\\Q" )
                        .append( baseName )
                        .append( "\\E" );
                    if ( it.hasNext() ) {
                        sbuf.append( '|' );
                    }
                }
                sbuf.append( ')' )
                    .append( ".*" );

                /* Compile the resulting regular expression. */
                auxAxisRegex_ =
                    Pattern.compile( sbuf.toString(),
                                     Pattern.CASE_INSENSITIVE );
            }
            return auxAxisRegex_;
        }

        /**
         * Returns any auxiliary axis names indicated by the parameters in
         * a given list of parameter names.  This is found by examining
         * each given parameter name to see if it matches a pattern like
         * "aux*data" and noting the infix parts.
         *
         * @param  paramNames  array of existing parameter names
         * @return  array of auxiliary axis names
         */
        public static String[] getAuxAxisNames( String[] paramNames ) {
            Pattern regex = getAuxAxisRegex();
            Set auxNameSet = new HashSet();
            for ( int in = 0; in < paramNames.length; in++ ) {
                Matcher matcher = regex.matcher( paramNames[ in ] );
                if ( matcher.matches() ) {
                    auxNameSet.add( matcher.group( 1 ) );
                }
            }
            return (String[]) auxNameSet.toArray( new String[ 0 ] );
        }
    }

    /**
     * Utility class which aggregates information about a Row Subset.
     */
    private static class SubsetDef {
        final String label_;
        final String expression_;
        final String name_;
        final Style style_;

        /**
         * Constructor.
         *
         * @param  label    label defined from parameter suffix
         * @param  expression   boolean JEL expression defining inclusion
         * @param  name     subset name
         * @param  style    subset style
         */
        SubsetDef( String label, String expression, String name,
                   Style style ) {
            label_ = label;
            expression_ = expression;
            name_ = name;
            style_ = style;
        }
    }
}
