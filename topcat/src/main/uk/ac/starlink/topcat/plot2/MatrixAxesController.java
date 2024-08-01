package uk.ac.starlink.topcat.plot2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JPanel;
import uk.ac.starlink.table.ColumnData;
import uk.ac.starlink.topcat.ActionForwarder;
import uk.ac.starlink.topcat.ColumnDataComboBoxModel;
import uk.ac.starlink.topcat.LineBox;
import uk.ac.starlink.topcat.ResourceIcon;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.config.BooleanConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMeta;
import uk.ac.starlink.ttools.plot2.config.ConfigKey;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.config.OptionConfigKey;
import uk.ac.starlink.ttools.plot2.config.Specifier;
import uk.ac.starlink.ttools.plot2.config.StyleKeys;
import uk.ac.starlink.ttools.plot2.config.ToggleNullConfigKey;
import uk.ac.starlink.ttools.plot2.geom.MatrixGanger;
import uk.ac.starlink.ttools.plot2.geom.MatrixGangerFactory;
import uk.ac.starlink.ttools.plot2.geom.MatrixPlotType;
import uk.ac.starlink.ttools.plot2.geom.MatrixShape;
import uk.ac.starlink.ttools.plot2.geom.PlaneAspect;
import uk.ac.starlink.ttools.plot2.geom.PlaneSurfaceFactory;
import uk.ac.starlink.util.StreamUtil;

/**
 * AxesController implementation for the matrix plot.
 *
 * @author   Mark Taylor
 * @since    19 Sep 2023
 */
public class MatrixAxesController
        extends AbstractAxesController<PlaneSurfaceFactory.Profile,PlaneAspect>{

    private final ConfigControl mainControl_;
    private final Map<MatrixShape.Cell,
                      ZoneController<PlaneSurfaceFactory.Profile,PlaneAspect>>
            controllerMap_;
    private final ConfigKey<Integer> NCOORD_KEY =
        new ToggleNullConfigKey<Integer>( MatrixGangerFactory.NCOORD_KEY,
                                          "Auto", true );
    private final CoordsConfigPanel coordsConfigPanel_;
    private final SpecifierArrayPanel<String> labelSpecifiersPanel_;
    private final Specifier<Labelling> labellingSpecifier_;
    private int ncoord_;
    private MatrixPositionCoordPanel[] matrixPanels_;
    private static final boolean XDIAG = MatrixGanger.XDIAG;
    private static final ConfigKey<Boolean> LOG_KEY =
        new BooleanConfigKey( new ConfigMeta( "log", "Log" ) );
    private static final ConfigKey<Boolean> FLIP_KEY =
        new BooleanConfigKey( new ConfigMeta( "flip", "Flip" ) );

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public MatrixAxesController() {
        mainControl_ = new ConfigControl( "Axes", ResourceIcon.AXIS_CONFIG );
        controllerMap_ =
            new HashMap<MatrixShape.Cell,
                        ZoneController<PlaneSurfaceFactory.Profile,
                                       PlaneAspect>>();
        matrixPanels_ = new MatrixPositionCoordPanel[ 0 ];

        /* Coord count can be changed by NCOORD_KEY, so need to listen
         * to possible changes.  Only forward the event if it actually is
         * relevant, otherwise an infinite loop can result. */
        addActionListener( new ActionListener() {
            Integer nc0_;
            public void actionPerformed( ActionEvent evt ) {
                Integer nc1 = getConfig().get( NCOORD_KEY );
                if ( ! Objects.equals( nc1, nc0_ ) ) {
                    nc0_ = nc1;
                    configureForPanels( matrixPanels_ );
                }
            }
        } );

        /* Matrix tab. */
        List<ConfigKey<?>> matrixKeyList = new ArrayList<>();
        for ( ConfigKey<?> key :
              MatrixGangerFactory.INSTANCE.getGangerKeys() ) {
            matrixKeyList.add( key == MatrixGangerFactory.NCOORD_KEY
                                   ? NCOORD_KEY
                                   : key );
        }
        ConfigKey<?>[] matrixKeys =
            matrixKeyList.toArray( new ConfigKey<?>[ 0 ] );
        mainControl_.addSpecifierTab( "Matrix",
                                      new ConfigSpecifier( matrixKeys ) );
        
        /* Coords tab. */
        coordsConfigPanel_ = new CoordsConfigPanel();
        mainControl_.addControlTab( "Coords", coordsConfigPanel_.panel_, true );
        mainControl_.getConfigSpecifiers()
                    .add( coordsConfigPanel_.scalarSpecifier_ );
        coordsConfigPanel_.forwarder_.addActionListener( mainControl_
                                                        .getActionForwarder() );

        /* Range tab. */

        /* Grid tab. */
        List<ConfigKey<?>> gridKeyList = new ArrayList<>();
        gridKeyList.add( PlaneSurfaceFactory.GRID_KEY );
        gridKeyList.addAll( Arrays
                           .asList( StyleKeys.GRIDCOLOR_KEYSET.getKeys() ) );
        gridKeyList.addAll( Arrays.asList( new ConfigKey<?>[] {
            StyleKeys.AXLABEL_COLOR,
            StyleKeys.MINOR_TICKS,
            StyleKeys.SHADOW_TICKS,
            PlaneSurfaceFactory.XCROWD_KEY,
            PlaneSurfaceFactory.YCROWD_KEY,
        } ) );
        ConfigKey<?>[] gridKeys = gridKeyList.toArray( new ConfigKey<?>[ 0 ] );
        mainControl_.addSpecifierTab( "Grid", new ConfigSpecifier( gridKeys ) );

        /* Labels tab. */
        LabelsPanel labelsPanel = new LabelsPanel();
        labelSpecifiersPanel_ = labelsPanel.specifiersPanel_;
        labelSpecifiersPanel_.addActionListener( mainControl_
                                                .getActionForwarder() );
        labellingSpecifier_ = labelsPanel.labellingSpecifier_;
        labellingSpecifier_
            .addActionListener( evt -> configureForPanels( matrixPanels_ ) );
        mainControl_.addControlTab( "Labels", labelsPanel, true );

        /* Font tab. */
        mainControl_.addSpecifierTab( "Font",
                                      new ConfigSpecifier( StyleKeys.CAPTIONER
                                                          .getKeys() ) );
        addControl( mainControl_ );
    }

    public void configureForLayers( LayerControl[] layerControls ) {
        matrixPanels_ =
            Arrays.stream( layerControls )
           .flatMap( StreamUtil.keepInstances( FormLayerControl.class ) )
           .map( flc -> flc.getPositionCoordPanel() )
           .flatMap( StreamUtil.keepInstances( MatrixPositionCoordPanel.class ))
           .toArray( n -> new MatrixPositionCoordPanel[ n ] );
        configureForPanels( matrixPanels_ );
    }

    public ConfigMap getConfig() {
        ConfigMap config = mainControl_.getConfig();
        config.put( MatrixGangerFactory.NCOORD_KEY,
                    Integer.valueOf( Math.max( 2, ncoord_ ) ) );
        return config;
    }

    public List<ZoneController<PlaneSurfaceFactory.Profile,PlaneAspect>>
            getZoneControllers( Ganger<PlaneSurfaceFactory.Profile,
                                       PlaneAspect> ganger ) {
        MatrixShape shape = ((MatrixGanger) ganger).getShape();
        List<ZoneController<PlaneSurfaceFactory.Profile,PlaneAspect>>
            controllers = new ArrayList<>();
        Function<MatrixShape.Cell,ZoneController<PlaneSurfaceFactory.Profile,
                                                 PlaneAspect>> zcFact =
            cell -> new MatrixZoneController( this, cell );
        for ( MatrixShape.Cell cell : shape ) {
            controllers.add( controllerMap_.computeIfAbsent( cell, zcFact ) );
        }
        return controllers;
    }

    /**
     * Updates the GUI according to the current state of a set of
     * MatrixPositionCoordPanels that are active for this controller.
     *
     * @param  matrixPanels  coord panels supplying positional information
     *                       to this controller
     */
    private void configureForPanels( MatrixPositionCoordPanel[] matrixPanels ) {

        /* Get the number of coordinates, which may be explicitly set or
         * inferred from the currently specified coords. */
        Integer configNcoord = getConfig().get( NCOORD_KEY );
        ncoord_ = configNcoord == null ? getActiveCoordCount( matrixPanels )
                                       : configNcoord.intValue();

        /* Update the GUI to show configuration for the number of coordinates
         * we have established. */
        coordsConfigPanel_.arraySpecifier_.showElements( ncoord_ );
        labelSpecifiersPanel_.showElements( ncoord_ );

        /* Fix it so that the default values for axis labels are taken
         * from coordinate labels (table column names or expressions). */
        MatrixPositionCoordPanel leadMatrixPanel = matrixPanels.length > 0
                                                 ? matrixPanels[ 0 ]
                                                 : null;
        String[] dfltAxisLabels = getAxisLabels( leadMatrixPanel );
        for ( int ic = 0; ic < dfltAxisLabels.length; ic++ ) {
            ((AutoSpecifier<String>) labelSpecifiersPanel_.getSpecifier( ic ))
                .setAutoValue( dfltAxisLabels[ ic ] );
        }
    }

    /**
     * Determines from a list of coord panels what is apparently the
     * number of spatial coordinates intended for use.
     * What it actually does is take the highest-numbered non-blank
     * coordinate, since blank coords appearing earlier than non-blank
     * ones are assumed to be intentional.
     *
     * @param  matrixPanels   coord panels
     * @return   apparent number of coordinates for matrix
     */
    private int getActiveCoordCount( MatrixPositionCoordPanel[] matrixPanels ) {
        int ncoord = 0;
        BitSet cmask = new BitSet();
        for ( MatrixPositionCoordPanel matrixPanel : matrixPanels ) {
            int nc = matrixPanel.getVisibleCoordCount();
            for ( int ic = 0; ic < nc; ic++ ) {
                ColumnDataComboBoxModel colSelector =
                    matrixPanel.getColumnSelector( ic, 0 );
                if ( colSelector.getSelectedItem() instanceof ColumnData ) {
                    cmask.set( ic );
                }
            }
        }
        return cmask.length();
    }

    /**
     * Returns an array of the labels that should be used for matrix plot
     * spatial coordinates.
     *
     * @param  panel  coord panel
     * @return  ncoord-element array giving suggested axis labels
     */
    private String[] getAxisLabels( MatrixPositionCoordPanel panel ) {
        GuiCoordContent[] contents = panel == null
                                   ? new GuiCoordContent[ 0 ]
                                   : panel.getContents();
        return labellingSpecifier_.getSpecifiedValue()
                                  .getDefaultLabels( contents, ncoord_ );
    }

    /**
     * ZoneController implementation for use with this class.
     */
    private static class MatrixZoneController
            extends SingleAdapterZoneController<PlaneSurfaceFactory.Profile,
                                                PlaneAspect> {

        private final MatrixAxesController axesController_;
        private final MatrixShape.Cell cell_;

        /**
         * Constructor.
         *
         * @param  axesController  axes controller
         * @param  cell   position in matrix for zone
         */
        MatrixZoneController( MatrixAxesController axesController,
                              MatrixShape.Cell cell ) {
            super( new PlaneAxisController() );
            axesController_ = axesController;
            cell_ = cell;
        }

        @Override
        public ConfigMap getConfig() {
            ConfigMap config = new ConfigMap();

            /* Here I'm inheriting behaviour from the per-zone
             * PlaneAxisController, which includes profile things
             * like grid and font that are supposed to be global for
             * all matrix zones, so are then overwritten by the
             * MatrixAxesController.  This works but is a bit messy;
             * it might be cleaner to implement ZoneController from scratch,
             * but that would mean duplication of the somewhat hairy
             * range updating code that I'd rather not have in more places
             * than necessary, so leave it like this for now. */
            config.putAll( super.getConfig() );
            config.putAll( axesController_.getConfig() );

            /* Add per-cell specifics. */
            int ix = cell_.getX();
            int iy = cell_.getY();

            /* Fixed aspect ratio does not make sense for histogram-like
             * plots on the leading diagonal. */
            if ( ix == iy ) {
                config.keySet().remove( PlaneSurfaceFactory.XYFACTOR_KEY );
            }

            /* Configure the X and Y behaviour of the surface in this zone
             * according to spatial coordinates determined by the identity
             * of the current matrix cell.
             * Take care on the diagonal: avoid treating the non-spatial
             * coordinate of histogram-like cells, like spatial coordinates. */
            SpecifierListArrayPanel slap =
                axesController_.coordsConfigPanel_.arraySpecifier_;
            ConfigMap xconfig = slap.getConfig( ix );
            ConfigMap yconfig = slap.getConfig( iy );
            SpecifierArrayPanel<String> labelSpecifiersPanel =
                axesController_.labelSpecifiersPanel_;
            if ( ix != iy || XDIAG ) {
                config.put( PlaneSurfaceFactory.XLOG_KEY,
                            xconfig.get( LOG_KEY ) );
                config.put( PlaneSurfaceFactory.XFLIP_KEY,
                            xconfig.get( FLIP_KEY ) );
                config.put( PlaneSurfaceFactory.XLABEL_KEY,
                            labelSpecifiersPanel.getSpecifier( ix )
                                                .getSpecifiedValue() );
            }
            if ( ix != iy || !XDIAG ) {
                config.put( PlaneSurfaceFactory.YLOG_KEY,
                            yconfig.get( LOG_KEY ) );
                config.put( PlaneSurfaceFactory.YFLIP_KEY,
                            yconfig.get( FLIP_KEY ) );
                config.put( PlaneSurfaceFactory.YLABEL_KEY,
                            labelSpecifiersPanel.getSpecifier( iy )
                                                .getSpecifiedValue() );
            }
            return config;
        }
    }

    /**
     * Panel for display in the Coords tab.
     */
    private static class CoordsConfigPanel {

        final ConfigSpecifier scalarSpecifier_;
        final SpecifierListArrayPanel arraySpecifier_;
        final ActionForwarder forwarder_;
        final JComponent panel_;

        CoordsConfigPanel() {
            ConfigKey<?>[] scalarKeys = {};
            ConfigKey<?>[] arrayKeys = { LOG_KEY, FLIP_KEY };
            scalarSpecifier_ = new ConfigSpecifier( scalarKeys );
            arraySpecifier_ =
                new SpecifierListArrayPanel( arrayKeys,
                                             MatrixPlotType::getCoordName );
            panel_ = Box.createVerticalBox();
            panel_.add( scalarSpecifier_.getComponent() );
            panel_.add( arraySpecifier_.getComponent() );
            forwarder_ = new ActionForwarder();
            scalarSpecifier_.addActionListener( forwarder_ );
            arraySpecifier_.addActionListener( forwarder_ );
        }
    }

    /**
     * Specifies the policy that provides default labels for matrix plot axes.
     */
    private enum Labelling {

        /** Uses user-entered column/expression as label. */
        VALUE( "Value",
               contents -> Arrays.stream( contents )
                          .map( c -> {
                               String label = c.getDataLabels()[ 0 ];
                               return label == null ? getCoordName( c ) : label;
                           } )
                          .toArray( n -> new String[ n ] ) ),

        /** Uses user-entered column/expression plus units where available. */
        VALUE_UNIT( "Value/Unit",
                    contents -> Arrays.stream( contents )
                               .map( c -> GuiCoordContent
                                         .getCoordLabel( getCoordName( c ),
                                                         contents ) )
                               .toArray( n -> new String[ n ] ) ),

        /** Uses the coordinate name itself as label. */
        COORD( "Xn", contents -> Arrays.stream( contents )
                                .map( c -> getCoordName( c ) )
                                .toArray( n -> new String[ n ] ) );

        final String name_;
        final Function<GuiCoordContent[],String[]> mapper_;

        /**
         * Constructor.
         *
         * @param  name  option name
         * @param  mapper  maps an array of GuiCoordContents to
         *                 default axis labels
         */
        Labelling( String name, Function<GuiCoordContent[],String[]> mapper ) {
            name_ = name;
            mapper_ = mapper;
        }

        /**
         * Gets default axis labels given an array of GuiCoordContents.
         *
         * @param  contents  plot coordinate information array
         * @param  ncoord   required length of output array
         * @return  ncoord-element default axis label array
         */
        public String[] getDefaultLabels( GuiCoordContent[] contents,
                                          int ncoord ) {
            String[] outLabels = new String[ ncoord ];
            String[] contentLabels = mapper_.apply( contents );
            int ic = 0; 
            for ( ; ic < Math.min( ncoord, contentLabels.length ); ic++ ) {
                outLabels[ ic ] = contentLabels[ ic ];
            }
            for ( ; ic < ncoord; ic++ ) {
                outLabels[ ic ] = MatrixPlotType.getCoordName( ic );
            }
            return outLabels;
        }

        @Override
        public String toString() {
            return name_;
        }

        /**
         * Extracts the coordinate name from a GuiCoordContent.
         *
         * @param  content  coordinate and value information
         * @param  return  coordinate name
         */
        private static String getCoordName( GuiCoordContent content ) {
            return content.getCoord().getInputs()[ 0 ].getMeta().getLongName();
        }
    }

    /**
     * Panel presenting user selection GUI for axis labelling.
     */
    private static class LabelsPanel extends JPanel {
        final SpecifierArrayPanel<String> specifiersPanel_;
        final Specifier<Labelling> labellingSpecifier_;
        LabelsPanel() {
            super( new BorderLayout() );
            Box box = Box.createVerticalBox();
            ConfigKey<String> xlabelKey = PlaneSurfaceFactory.XLABEL_KEY;
            specifiersPanel_ =
                new SpecifierArrayPanel<String>(
                    xlabelKey, i -> MatrixPlotType.getCoordName( i ) + " Label",
                    i -> new AutoSpecifier<String>
                                          ( xlabelKey.createSpecifier() ) );

            ConfigMeta labellingMeta =
                new ConfigMeta( "defaults", "Default Labels" );
            labellingSpecifier_ = 
                new OptionConfigKey<Labelling>( labellingMeta, Labelling.class,
                                                Labelling.values(),
                                                Labelling.VALUE, true ) {
                    // Not used except for display; no user-level documentation
                    // is required.
                    public String getXmlDescription( Labelling labelling ) {
                        return "";
                    }
                }
               .createSpecifier();
            JComponent policyLine =
                new LineBox( labellingMeta.getLongName(),
                             labellingSpecifier_.getComponent() );
            box.add( policyLine );
            box.add( Box.createVerticalStrut( 5 ) );
            box.add( specifiersPanel_.getComponent() );
            add( box, BorderLayout.NORTH );
        }
    }
}
