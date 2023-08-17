package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import uk.ac.starlink.topcat.RowSubset;
import uk.ac.starlink.topcat.TopcatModel;
import uk.ac.starlink.ttools.plot.Style;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.Ganger;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.PlotUtil;
import uk.ac.starlink.ttools.plot2.Plotter;
import uk.ac.starlink.ttools.plot2.config.ConfigException;
import uk.ac.starlink.ttools.plot2.config.ConfigMap;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.geom.MatrixFormat;
import uk.ac.starlink.ttools.plot2.geom.MatrixGanger;
import uk.ac.starlink.ttools.plot2.geom.MatrixShape;
import uk.ac.starlink.ttools.plot2.geom.PlaneDataGeom;

/**
 * LayerManager implementation for use with the Matrix plot.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2023
 */
public class MatrixLayerManager implements ZoneLayerManager {

    private final FormLayerControl flc_;

    /**
     * Constructor.
     *
     * @param  flc  manages coordinates and subsets
     */
    public MatrixLayerManager( FormLayerControl flc ) {
        flc_ = flc;
    }

    public boolean hasLayers() {
        // Not totally bulletproof, since some MatrixLayers may end up
        // producing PlotLayers that are all null.  But false positives
        // are not forbidden, so it's probably good enough.
        return getMatrixLayers().length > 0;
    }

    public Map<RowSubset,List<Style>> getStylesBySubset() {
        Map<RowSubset,List<Style>> map = new LinkedHashMap<>();
        for ( MatrixLayer mlayer : getMatrixLayers() ) {
            RowSubset rset = mlayer.rset_;
            Style style = mlayer.getStyle();
            if ( rset != null && style != null ) {
                map.computeIfAbsent( rset, m -> new ArrayList<Style>() )
                   .add( style );
            }
        }
        return map;
    }

    public Map<FormControl,List<PlotLayer>>
            getLayersByControl( Ganger<?,?> ganger ) {
        MatrixShape shape = ((MatrixGanger) ganger).getShape();
        Map<FormControl,List<PlotLayer>> map = new LinkedHashMap<>();
        for ( MatrixLayer mlayer : getMatrixLayers() ) {
            List<PlotLayer> list =
                map.computeIfAbsent( mlayer.fc_,
                                     fc -> new ArrayList<PlotLayer>() );
            TopcatLayer tclayer = mlayer.toTopcatLayer( shape );
            if ( tclayer != null ) {
                for ( PlotLayer player : tclayer.getPlotLayers() ) {
                    if ( player != null ) {
                        list.add( player );
                    }
                }
            }
        }
        return map;
    }

    public TopcatLayer[] getLayers( Ganger<?,?> ganger ) {
        MatrixShape shape = ((MatrixGanger) ganger).getShape();
        return Arrays.stream( getMatrixLayers() )
                     .map( ml -> ml.toTopcatLayer( shape ) )
                     .filter( tcl -> tcl != null )
                     .toArray( n -> new TopcatLayer[ n ] );
    }

    /**
     * Returns the state of this component in the form of an array of
     * MatrixLayer objects.
     *
     * @returm  matrix layers specified by the current configuration
     */
    private MatrixLayer[] getMatrixLayers() {
        RowSubset[] rsets = flc_.getSubsetStack().getSelectedSubsets();
        PositionCoordPanel posCoordPanel = flc_.getPositionCoordPanel();
        GuiCoordContent[] posContents = posCoordPanel.getContents();
        TopcatModel tcModel = flc_.getTopcatModel();
        if ( tcModel == null || posContents == null || rsets == null ) {
            return new MatrixLayer[ 0 ];
        }
        DataGeom geom = posCoordPanel.getDataGeom();
        ConfigMap coordConfig = posCoordPanel.getConfig();
        FormControl[] fcs = flc_.getActiveFormControls();
        List<MatrixLayer> layerList = new ArrayList<>();
        for ( RowSubset rset : rsets ) {
            String leglabel = flc_.getLegendLabel( rset );
            for ( FormControl fc : fcs ) {
                GuiCoordContent[] extraContents = fc.getExtraCoordContents();
                if ( extraContents != null ) {
                    ConfigMap config = new ConfigMap();
                    config.putAll( coordConfig );
                    config.putAll( fc.getExtraConfig() );
                    config.putAll( fc.getStylePanel()
                                     .getConfig( rset.getKey() ) );
                    MatrixLayer matrixLayer =
                        new MatrixLayer( fc, config, leglabel, tcModel,
                                         posContents, extraContents, geom,
                                         rset );
                    layerList.add( matrixLayer );
                }
            }
        }
        return layerList.toArray( new MatrixLayer[ 0 ] );
    }

    /**
     * Encapsulates state captured by this GUI that can produce a TopcatLayer.
     */
    private static class MatrixLayer {
        private final FormControl fc_;
        private final ConfigMap config_;
        private final String leglabel_;
        private final TopcatModel tcModel_;
        private final GuiCoordContent[] posContents_;
        private final GuiCoordContent[] extraContents_;
        private final DataGeom geom_;
        private final RowSubset rset_;

        /**
         * Constructor.
         *
         * @param  fc  form control
         * @param  config  configuration applying to layer
         * @param  leglabel  legend label for this layer
         * @param  tcModel  topcat model
         * @param  posContents  information about supplied positional
         *                      coordinates
         * @param  extraContents  information about supplied non-positional
         *                        coordinates
         * @param  geom   data geom
         * @param  rset   row subset supplying data
         */
        MatrixLayer( FormControl fc, ConfigMap config, String leglabel,
                     TopcatModel tcModel, GuiCoordContent[] posContents,
                     GuiCoordContent[] extraContents, DataGeom geom,
                     RowSubset rset ) {
            fc_ = fc;
            config_ = config;
            leglabel_ = leglabel;
            tcModel_ = tcModel;
            posContents_ = posContents;
            extraContents_ = extraContents;
            geom_ = geom;
            rset_ = rset;
        }

        /**
         * Returns the Style associated with this layer.
         *
         * @return  layer style
         */
        public Style getStyle() {
            try {
                return fc_.getPlotter().createStyle( config_ );
            }
            catch ( ConfigException e ) {
                return null;
            }
        }

        /**
         * Creates a TopcatLayer based on the state of this object
         * as applied to a given MatrixShape.
         *
         * @param  shape  matrix shape
         * @return  topcat layer for use with shape
         */
        public TopcatLayer toTopcatLayer( MatrixShape shape ) {
            Plotter<?> plotter = fc_.getPlotter();
            CoordGroup cgrp = plotter.getCoordGroup();
            int nExtra = cgrp.getExtraCoords().length;
            boolean isOnDiag = MatrixFormat.isOnDiagonal( cgrp );
            boolean isOffDiag = MatrixFormat.isOffDiagonal( cgrp );
            int npos = cgrp.getBasicPositionCount();
            boolean hasPos = npos + cgrp.getExtraPositionCount() > 0;
            int ncell = shape.getCellCount();
            PlotLayer[] plotLayers = new PlotLayer[ ncell ];
            for ( int icell = 0; icell < ncell; icell++ ) {
                MatrixShape.Cell cell = shape.getCell( icell );
                int ix = cell.getX();
                int iy = cell.getY();
                List<GuiCoordContent> cellContentList = new ArrayList<>();
                final boolean hasCell;
                if ( ix < posContents_.length && iy < posContents_.length ) {
                    if ( isOffDiag ) {
                        if ( ix != iy ) {
                            hasCell = true;
                            cellContentList.add( posContents_[ ix ] );
                            cellContentList.add( posContents_[ iy ] );
                        }
                        else {
                            hasCell = false;
                        }
                    }
                    else if ( isOnDiag ) {
                        if ( ix == iy ) {
                            hasCell = true;
                            cellContentList.add( posContents_[ ix ] );
                        }
                        else {
                            hasCell = false;
                        }
                    }
                    else {
                        hasCell = true;
                    }
                }
                else {
                    hasCell = false;
                }
                if ( hasCell ) {
                    cellContentList.addAll( Arrays.asList( extraContents_ ) );
                    GuiCoordContent[] cellContents =
                        cellContentList.toArray( new GuiCoordContent[ 0 ] );
                    DataSpec dspec =
                        new GuiDataSpec( tcModel_, rset_, cellContents );
                    plotLayers[ icell ] =
                        fc_.createLayer( geom_, dspec, rset_ );
                }
            }
            if ( Arrays.stream( plotLayers ).anyMatch( l -> l != null ) ) {
                GuiCoordContent[] matrixContents =
                    PlotUtil.arrayConcat( posContents_, extraContents_ );
                return new TopcatLayer( plotLayers, config_, leglabel_,
                                        tcModel_, matrixContents, rset_ );
            }
            else {
                return null;
            }
        }
    }
}
