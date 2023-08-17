package uk.ac.starlink.topcat.plot2;

import gnu.jel.CompilationException;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ListModel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.topcat.BasicAction;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatListener;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Styles;
import uk.ac.starlink.ttools.plot2.PlotType;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ColorConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.data.AreaCoord;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.FloatingCoord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.geom.MatrixPlotType;
import uk.ac.starlink.ttools.plot2.layer.HealpixPlotter;
import uk.ac.starlink.ttools.plot2.task.CoordSpec;
import uk.ac.starlink.util.Loader;

/**
 * Control manager that uses FormLayerControls to provide
 * panels that allow you to enter the position values once
 * for a given table and then go to other tabs in the control
 * to customise the layers generated.
 * 
 * @author   Mark Taylor
 * @since    15 Mar 2013
 */
public class GroupControlManager<P,A> implements ControlManager {

    private final ControlStack stack_;
    private final PlotType<P,A> plotType_;
    private final PlotTypeGui<P,A> plotTypeGui_;
    private final ListModel<TopcatModel> tablesModel_;
    private final ZoneFactory zfact_;
    private final MultiConfigger baseConfigger_;
    private final TopcatListener tcListener_;
    private final NextSupplier nextSupplier_;
    private final Map<CoordsType,List<Plotter<?>>> plotterMap_;
    private final Action[] stackActs_;

    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.topcat.plot2" );

    /**
     * System property that may contain a colon-separated list of
     * Plotter implementation class names (with no-arg constructors)
     * to plug in at runtime.
     * This is a bit scrappy - they will show up in all plot types,
     * which probably is not appropriate.
     */
    public static final String PLOTTERS_PROP = "plot2.plotters";

    /**
     * Constructor.
     *
     * @param   stack  control stack which this object will manage
     * @param   plotType  defines basic plot characteristics
     * @param   plotTypeGui  defines GUI-specific plot characteristics
     * @param   tablesModel   list of available tables
     * @param   zfact   zone id factory
     * @param   baseConfigger  configuration source for some global config
     *                        options
     * @param   tcListener listener for TopcatEvents; this manager will arrange
     *                     for it to listen to whatever is the currently
     *                     selected TopcatModel
     */
    public GroupControlManager( ControlStack stack, PlotType<P,A> plotType,
                                PlotTypeGui<P,A> plotTypeGui,
                                ListModel<TopcatModel> tablesModel,
                                ZoneFactory zfact, MultiConfigger baseConfigger,
                                TopcatListener tcListener ) {
        stack_ = stack;
        plotType_ = plotType;
        plotTypeGui_ = plotTypeGui;
        tablesModel_ = tablesModel;
        zfact_ = zfact;
        baseConfigger_ = baseConfigger;
        tcListener_ = tcListener;
        nextSupplier_ = new NextSupplier();
        nextSupplier_.putValues( StyleKeys.COLOR,
                                 ColorConfigKey.getPlottingColors() );
        List<Action> stackActList = new ArrayList<Action>();

        /* Split the list up by the number of positional coordinates
         * they have. */
        plotterMap_ = new LinkedHashMap<CoordsType,List<Plotter<?>>>();
        for ( CoordsType ctyp : CoordsType.values() ) {
            plotterMap_.put( ctyp, new ArrayList<Plotter<?>>() );
        }
        for ( Plotter<?> plotter : plotType_.getPlotters() ) {
            CoordsType ctyp = getCoordsType( plotter );
            plotterMap_.get( ctyp ).add( plotter );
        }
        for ( Plotter<?> plotter :
              Loader.getClassInstances( PLOTTERS_PROP, Plotter.class ) ) {
            CoordsType ctyp = getCoordsType( plotter );
            plotterMap_.get( ctyp ).add( plotter );
        }

        /* Add group controls grouping suitably categorised plotters. */
        for ( CoordsType ctyp : CoordsType.values() ) {
            if ( ctyp.getIcon() != null &&
                 ! plotterMap_.get( ctyp ).isEmpty() ) {
                String actName = "Add " + ctyp.getTypeTitle() + " Control";
                String actDescrip = "Add a new " + ctyp.getTypeDescription()
                                  + " plot control to the stack";
                final CoordsType ctyp0 = ctyp;
                stackActList.add( new LayerControlAction( actName,
                                                          getIcon( ctyp ),
                                                          actDescrip,
                                                          (Plotter) null,
                                                          stack_ ) {
                    public LayerControl createLayerControl() {
                        return createGroupControl( ctyp0, true );
                    }
                } );
            }
        }

        /* Add single controls for miscellaneous plotters. */
        assert CoordsType.MISC.getIcon() == null;
        for ( Plotter<?> plotter : plotterMap_.get( CoordsType.MISC ) ) {
            Action stackAct =
                LayerControlAction
               .createPlotterAction( plotTypeGui_, plotter, stack, tablesModel_,
                                     zfact_, nextSupplier_, tcListener_,
                                     baseConfigger_ );
            if ( stackAct != null ) {
                stackActList.add( stackAct );
            }
            else {
                logger_.warning( "No GUI available for plotter "
                               + plotter.getPlotterName() );
            }
        }
        stackActs_ = stackActList.toArray( new Action[ 0 ] );
    }

    public Action[] getStackActions() {
        return stackActs_;
    }

    public Control createDefaultControl( TopcatModel tcModel ) {

        /* For the special case where the table is known to contain HEALPix
         * indices, and one of the layer controls can plot those, use that
         * control.  In most cases, the other controls will not be appropriate
         * for such a table. */
        if ( tcModel != null &&
             HealpixTableInfo.isHealpix( tcModel.getDataModel()
                                                .getParameters() ) ) {
            for ( Action stackAct : stackActs_ ) {
                if ( stackAct instanceof LayerControlAction ) {
                    LayerControlAction cact = (LayerControlAction) stackAct;
                    if ( cact.getPlotter() instanceof HealpixPlotter ) {
                        LayerControl control = cact.createLayerControl();
                        assert control instanceof HealpixLayerControl;
                        if ( control instanceof HealpixLayerControl ) {
                            ((HealpixLayerControl) control)
                                                  .setTopcatModel( tcModel );
                            return control;
                        }
                    }
                }
            }
        }

        /* Otherwise, just pick the first control. */
        for ( Action stackAct : stackActs_ ) {
            if ( stackAct instanceof LayerControlAction ) {
                LayerControl control =
                    ((LayerControlAction) stackAct).createLayerControl();
                if ( control instanceof FormLayerControl ) {
                    ((FormLayerControl) control).setTopcatModel( tcModel );
                }
                return control;
            }
        }
        return null;
    }

    public void addLayer( LayerCommand<?> lcmd ) throws LayerException {
        logger_.info( "Add layer: " + lcmd );
        getGroupControl( lcmd ).addLayer( lcmd );
    }

    /**
     * Returns a control to which a specified layer can be added.
     * If a suitable control is currently in the stack, that will be returned.
     * Otherwise, a new control will be constructed and placed into the
     * stack.
     *
     * @param   lcmd  specifies a layer that wants to be added
     * @return  control in the stack for which <code>addLayer(lcmd)</code>
     *          will work
     */
    private MultiFormLayerControl getGroupControl( LayerCommand<?> lcmd )
            throws LayerException {
        ControlStackModel stackModel = stack_.getStackModel();

        /* Try to find and return an existing compatible control. */
        for ( int ic = 0; ic < stackModel.getSize(); ic++ ) {
            Control control = stackModel.getControlAt( ic );
            if ( control instanceof MultiFormLayerControl ) {
                MultiFormLayerControl groupControl =
                    (MultiFormLayerControl) control;
                if ( isCompatible( groupControl, lcmd ) ) {
                    return groupControl;
                }
            }
        }

        /* If there wasn't one, create a new one, add it to the stack,
         * and return it. */
        MultiFormLayerControl control = createGroupControl( lcmd );
        stack_.addControl( control );
        return control;
    }

    /**
     * Determines whether a given control can have a specified layer
     * added to it.
     *
     * @param  control  existing control
     * @param  lcmd   specifies layer to add
     * @return  true iff <code>control.addLayer(lcmd)</code> will work
     */
    private static boolean isCompatible( MultiFormLayerControl control,
                                         LayerCommand<?> lcmd ) {

        /* Note the implementation of this method is closely tied to the
         * implementation of MultiFormLayerControl.addLayer. */

        /* Must have the same table data. */
        if ( lcmd.getTopcatModel() != control.getTopcatModel() ) {
            return false;
        }

        /* Must have the same positional coordinates.
         * This test currently requires all the coordinates to be the same,
         * I think that could be relaxed to just the positional ones. */
        Map<String,String> ivals = new HashMap<>();
        GuiCoordContent[] contents =
            control.getPositionCoordPanel().getContents();
        for ( CoordSpec cspec : GuiCoordContent.getCoordSpecs( contents ) ) {
            ivals.put( cspec.getInputName(), cspec.getValueExpr() );
        }
        if ( ! lcmd.getInputValues().equals( ivals ) ) {
            return false;
        }

        /* Must have the same, single, subset.  It could be possible
         * under some circumstances to add to a group control with
         * multiple subsets, but the logic is tricky, so this is not
         * currently supported by addLayer. */
        RowSubset rset = lcmd.getRowSubset();
        RowSubset.Key rsKey = rset.getKey();
        SubsetStack subStack = control.getSubsetStack();
        if ( ! Arrays.equals( subStack.getSelectedSubsets(),
                              new RowSubset[] { rset } ) ) {
            return false;
        }

        /* Any config options specified for the new layer must not conflict
         * with config options (probably colour) currently in force for
         * the new layer's subset. */
        SubsetConfigManager subManager = control.getSubsetManager();
        if ( subManager.hasConfigger( rsKey ) ) {
            ConfigMap ctrlConfig = subManager.getConfigger( rsKey ).getConfig();
            ConfigMap cmdConfig = lcmd.getConfig();
            for ( ConfigKey<?> key : cmdConfig.keySet() ) {
                if ( ctrlConfig.keySet().contains( key ) &&
                     ! PlotUtil.equals( ctrlConfig.get( key ),
                                        cmdConfig.get( key ) ) ) {
                    return false;
                }
            }
        }

        /* If it passes all those tests, it's compatible. */
        return true;
    }

    /**
     * Constructs a new group layer control which is capable of having a
     * specified layer added to it.
     *
     * @param  lcmd  layer specification
     * @return  new control for which <code>addLayer(lcmd)</code> will work
     */
    private MultiFormLayerControl createGroupControl( LayerCommand<?> lcmd )
            throws LayerException {

        /* Create the control. */
        CoordsType ctyp = getCoordsType( lcmd.getPlotter() );
        MultiFormLayerControl control = createGroupControl( ctyp, false );

        /* Set the table. */
        control.setTopcatModel( lcmd.getTopcatModel() );

        /* Set up the positional coordinates. */
        PositionCoordPanel posCoordPanel = control.getPositionCoordPanel();
        Coord[] posCoords = posCoordPanel.getCoords();
        Map<String,String> inputValues = lcmd.getInputValues();
        for ( int ic = 0; ic < posCoords.length; ic++ ) {
            Input[] inputs = posCoords[ ic ].getInputs();
            for ( int iu = 0; iu < inputs.length; iu++ ) {
                String name = LayerCommand.getInputName( inputs[ iu ] );
                String value = inputValues.get( name );
                if ( value != null ) {
                    ColumnDataComboBoxModel colModel =
                        posCoordPanel.getColumnSelector( ic, iu );
                    ColumnData colData;
                    try {
                        colData = colModel.stringToColumnData( value );
                    }
                    catch ( CompilationException e ) {
                        throw new LayerException( "Can't compile: " + value,
                                                  e );
                    }
                    posCoordPanel.getColumnSelector( ic, iu )
                                 .setSelectedItem( colData );
                }
            }
        }

        /* Set up per-subset configuration. */
        control.getSubsetManager()
               .setConfig( lcmd.getRowSubset().getKey(), lcmd.getConfig() );

        /* Return. */
        return control;
    }

    /**
     * Creates a new empty group layer control.
     *
     * @param   ctyp  common coordinate set type
     * @param   autoPlot  true to attempt a plot without further
     *                    user interaction
     * @return   group control, or null if it would be useless
     */
    private MultiFormLayerControl createGroupControl( CoordsType ctyp,
                                                      boolean autoPlot ) {
        List<Plotter<?>> plotterList = plotterMap_.get( ctyp );
        if ( plotterList != null && plotterList.size() > 0 ) {
            PositionCoordPanel coordPanel =
                ctyp.createPositionCoordPanel( plotType_, plotTypeGui_ );
            Specifier<ZoneId> zs0 = zfact_.createZoneSpecifier();
            Configger zoneConfigger = baseConfigger_.layerConfigger( zs0 );
            Specifier<ZoneId> zsel = zfact_.isSingleZone() ? null : zs0;
            boolean autoPop = ctyp.isAutoPopulate();
            MultiFormLayerControl control = 
                new MultiFormLayerControl( plotTypeGui_, coordPanel,
                                           tablesModel_, zsel, autoPop,
                                           nextSupplier_, tcListener_,
                                           getIcon( ctyp ),
                                           plotterList
                                          .toArray( new Plotter<?>[ 0 ] ),
                                           zoneConfigger );
            if ( autoPlot ) {
                control.addDefaultLayers();
            }
            return control;
        }
        else {
            return null;
        }
    }

    /**
     * Returns the appropriate CoordsType for use with a given plotter.
     *
     * @param   plotter  plotter
     * @return   coords type for plotter, not null
     */
    private CoordsType getCoordsType( Plotter<?> plotter ) {
        CoordGroup cgrp = plotter.getCoordGroup();
        int npos = cgrp.getBasicPositionCount();
        Coord[] extraCoords = cgrp.getExtraCoords();

        /* Treat HealpixPlotter as a special case since although it has
         * positional coordinates, they are not the standard coordinates
         * for its plot type (it's a pixel index, not a lon/lat pair).
         * This special handling is a bit messy and should really be
         * generalised, but since it's the only such special case so far,
         * it's not clear how best to do that generalisation.
         * If other instances of this kind of requirement come up,
         * consider this more carefully and generalise the handling
         * as appropriate. */
        if ( plotter instanceof HealpixPlotter ) {
            return CoordsType.MISC;
        }

        /* For other layer types, examine their declared characteristics
         * to decide how they are categorised. */
        else if ( npos == 0 &&
                  extraCoords.length > 0 &&
                  extraCoords[ 0 ] instanceof AreaCoord ) {
            return CoordsType.AREA;
        }
        else if ( npos == 0 &&
                  extraCoords.length >= 2 &&
                  extraCoords[ 0 ] instanceof FloatingArrayCoord &&
                  extraCoords[ 1 ] instanceof FloatingArrayCoord ) {
            return CoordsType.XYARRAY;
        }
        else if ( npos == 1 ) {
            return CoordsType.SINGLE_POS;
        }
        else if ( npos == 2 ) {
            return CoordsType.DOUBLE_POS;
        }
        else if ( npos == 4 ) {
            return CoordsType.QUAD_POS;
        }
        else if ( cgrp.isSinglePartialPosition() &&
                  cgrp.getExtraCoords().length == 2 &&
                  cgrp.getExtraCoords()[ 1 ]
                       == FloatingCoord.WEIGHT_COORD ) {
            if ( plotTypeGui_.hasExtraHistogram() ) {
                return CoordsType.SINGLE_POS;
            }
            else {
                return CoordsType.WEIGHTED_HISTO;
            }
        }
        else {
            return CoordsType.MISC;
        }
    }

    /**
     * Nasty hack to yield a different icon for the Matrix Plot
     * single position control.  If more special cases need to be added
     * on top of this, rework it in a more principled way.
     *
     * @param  ctyp  coordinate type
     * @return  icon for coordinate position control
     */
    private Icon getIcon( CoordsType ctyp ) {
        return ctyp == CoordsType.SINGLE_POS &&
                       plotType_ instanceof MatrixPlotType
             ? ResourceIcon.PLOT_MATRIX
             : ctyp.getIcon();
    }

    /**
     * Categorises the kind of plot.
     * This categorisation is used to determine what kinds of
     * plots can be grouped together in the same GroupControl.
     */
    private enum CoordsType {

        /** Plotter with a single positional coordinate. */
        SINGLE_POS( ResourceIcon.PLOT_DATA, "Position", "positional", true ) {
            public <P,A> PositionCoordPanel
                    createPositionCoordPanel( PlotType<P,A> plotType,
                                              PlotTypeGui<P,A> plotTypeGui ) {
                return plotTypeGui.createPositionCoordPanel( 1 );
            }
        },

        /** Plotter with two positional coordinates. */
        DOUBLE_POS( ResourceIcon.PLOT_PAIR, "Pair", "pair position", true ) {
            public <P,A> PositionCoordPanel
                    createPositionCoordPanel( PlotType<P,A> plotType,
                                              PlotTypeGui<P,A> plotTypeGui ) {
                return plotTypeGui.createPositionCoordPanel( 2 );
            }
        },

        /** Plotter with shape information but no positional coordinate. */
        AREA( ResourceIcon.PLOT_AREA, "Area", "area", true ) {
            public <P,A> PositionCoordPanel
                    createPositionCoordPanel( PlotType<P,A> plotType,
                                              PlotTypeGui<P,A> plotTypeGui ) {
                return plotTypeGui.createAreaCoordPanel();
            }
        },

        /** Plotter with array-valued X and Y coordinates. */
        XYARRAY( ResourceIcon.PLOT_VECTOR, "XYArray", "array pair", true ) {
            public <P,A> PositionCoordPanel
                    createPositionCoordPanel( PlotType<P,A> plotType,
                                              PlotTypeGui<P,A> plotTypeGui ) {
                return new XYArrayCoordPanel();
            }
        },

        /** Plotter with four positional coordinates. */
        QUAD_POS( ResourceIcon.PLOT_QUAD, "Quad", "quadrilateral", true ) {
            public <P,A> PositionCoordPanel
                    createPositionCoordPanel( PlotType<P,A> plotType,
                                              PlotTypeGui<P,A> plotTypeGui ) {
                return plotTypeGui.createPositionCoordPanel( 4 );
            }
        },

        /** Histogram-like plotter. */
        WEIGHTED_HISTO( ResourceIcon.PLOT_HISTO, "Histogram",
                        "optionally weighted histogram", true ) {
            public <P,A> PositionCoordPanel
                    createPositionCoordPanel( PlotType<P,A> plotType,
                                              PlotTypeGui<P,A> plotTypeGui ) {
                Coord[] coords = {
                    plotType.getPointDataGeoms()[ 0 ].getPosCoords()[ 0 ],
                    FloatingCoord.WEIGHT_COORD,
                };
                return new SimplePositionCoordPanel( coords, null );
            }
        },

        /** Plotter not covered by other categories. */
        MISC( null, null, null, false ) {
            public <P,A> PositionCoordPanel
                    createPositionCoordPanel( PlotType<P,A> plotType,
                                              PlotTypeGui<P,A> plotTypeGui ) {
                return null;
            }
        };

        private final Icon icon_;
        private final String tName_;
        private final String tname_;
        private final boolean isAutoPop_;

        /**
         * Constructor.
         *
         * @param  icon  icon
         * @param  tName  short (one word?) label describing this type
         * @param  tname  short phrase describing this type
         * @param  isAutoPop  whether to autopopulate coordinates for this type
         */
        CoordsType( Icon icon, String tName, String tname, boolean isAutoPop ) {
            icon_ = icon;
            tName_ = tName;
            tname_ = tname;
            isAutoPop_ = isAutoPop;
        }

        /**
         * Returns a generic icon suitable for this type of coordinate set.
         *
         * @return  icon
         */
        public Icon getIcon() {
            return icon_;
        }

        /**
         * Returns a short (one-word?), capitalised string labelling this type.
         *
         * @return  title
         */
        public String getTypeTitle() {
            return tName_;
        }

        /**
         * Returns a short phrase describing this type.
         *
         * @return  short description
         */
        public String getTypeDescription() {
            return tname_;
        }

        /**
         * Indicates whether GUIs associated with this plot type are
         * suitable for autopopulating with default columns.
         *
         * @return  true iff autopopulation is a good idea
         */
        public boolean isAutoPopulate() {
            return isAutoPop_;
        }

        /**
         * Returns a new PositionCoordPanel that can be common to all plotters
         * grouped in this category.
         *
         * @param   plotType  plot specifics
         * @param   plotTypeGui  gui plot specifics
         */
        public abstract <P,A> PositionCoordPanel
                createPositionCoordPanel( PlotType<P,A> plotType,
                                          PlotTypeGui<P,A> plotTypeGui );
    }
}
