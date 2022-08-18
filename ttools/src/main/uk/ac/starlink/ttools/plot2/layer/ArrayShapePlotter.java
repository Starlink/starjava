package uk.ac.starlink.ttools.plot2.layer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import uk.ac.starlink.ttools.plot.Range;
import uk.ac.starlink.ttools.plot2.DataGeom;
import uk.ac.starlink.ttools.plot2.PlotLayer;
import uk.ac.starlink.ttools.plot2.RangeCollector;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.FloatingArrayCoord;
import uk.ac.starlink.ttools.plot2.data.Input;
import uk.ac.starlink.ttools.plot2.data.Tuple;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * ShapePlotter subclass that plots multiple shapes for each row,
 * based on array-valued coordinates.
 * This class provides some additional functionality
 * specific to array-valued positions.
 *
 * <p>This plotter does not report positions and point clouds in the
 * usual way, since each row typically corresponds to a large region
 * of the plot surface, and reporting a single point is not very helpful.
 * Instead, the PlotLayers it supplies are doctored to adjust the
 * coordinate ranges to cover the whole of the relevant area for
 * the plotted rows.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2021
 */
public class ArrayShapePlotter extends ShapePlotter {

    private final ShapeForm form_;
    private final FloatingArrayCoord xsCoord_;
    private final FloatingArrayCoord ysCoord_;
    private final int icXs_;
    private final int icYs_;

    /**
     * Constructor.
     *
     * @param   name  plotter name
     * @param   form  multiple shape determiner
     * @param   mode  colour determiner
     */
    public ArrayShapePlotter( String name, ShapeForm form, ShapeMode mode ) {
        super( name, form, mode, createArrayCoordGroup( form, mode ) );
        form_ = form;
        xsCoord_ = FloatingArrayCoord.X;
        ysCoord_ = FloatingArrayCoord.Y;
        CoordGroup cgrp = getCoordGroup();
        icXs_ = 0;
        icYs_ = 1;
    }

    @Override
    public int getModeCoordsIndex( DataGeom geom ) {
        return 2 + form_.getExtraCoords().length;
    }

    @Override
    public PlotLayer createLayer( DataGeom pointDataGeom,
                                  final DataSpec dataSpec,
                                  ShapeStyle style ) {
        final PlotLayer baseLayer =
            super.createLayer( pointDataGeom, dataSpec, style );
        if ( baseLayer == null ) {
            return null;
        }
        final Function<Tuple,XYArrayData> xyReader =
            createXYArrayReader( xsCoord_, ysCoord_, icXs_, icYs_, dataSpec );
        return new WrapperPlotLayer( baseLayer ) {
            @Override
            public void extendCoordinateRanges( Range[] ranges,
                                                boolean[] logFlags,
                                                DataStore dataStore ) {
                super.extendCoordinateRanges( ranges, logFlags, dataStore );
                RangeCollector<TupleSequence> rangeCollector =
                        new RangeCollector<TupleSequence>( 2 ) {
                    public void accumulate( TupleSequence tseq,
                                            Range[] ranges ) {
                        extendRanges( tseq, xyReader, ranges[0], ranges[1] );
                    }
                };
                Range[] arrayRanges =
                    dataStore
                   .getTupleRunner()
                   .collect( rangeCollector,
                             () -> dataStore.getTupleSequence( dataSpec ) );
                rangeCollector.mergeRanges( ranges, arrayRanges );
            }
        };
    }

    /**
     * Creates an array of ArrayShapePlotters, using all combinations of the
     * specified list of ShapeForms and ShapeModes.
     * Since these implement the {@link ModePlotter} interface,
     * other parts of the UI may be able to group them.
     *
     * @param  forms  array of shape forms
     * @param  modes  array of shape modes
     * @return <code>forms.length*modes.length</code>-element array of plotters
     */
    public static ArrayShapePlotter[]
            createArrayShapePlotters( ShapeForm[] forms, ShapeMode[] modes ) {
        int nf = forms.length;
        int nm = modes.length;
        ArrayShapePlotter[] plotters = new ArrayShapePlotter[ nf * nm ];
        int ip = 0;
        for ( ShapeMode mode : modes ) {
            for ( ShapeForm form : forms ) {
                String name = form.getFormName() + "-" + mode.getModeName();
                plotters[ ip++ ] = new ArrayShapePlotter( name, form, mode );
            }
        }
        assert ip == plotters.length;
        return plotters;
    }

    /**
     * Utility method that identifies whether an Input corresponds to a
     * named axis.  This is an ad hoc method put in place to
     * assist in working out how to annotate axes on which array plots
     * are represented.
     * 
     * @param  axName  geometric axis name, e.g. "X"
     * @param  input   coordinate input specification
     * @return   true iff the input corresponds to an array value specifier
     *           intended for the named axis
     */
    public static boolean matchesAxis( String axName, Input input ) {
        return ( "X".equals( axName ) &&
                 input.getMeta().getLongName()
                .equals( FloatingArrayCoord.X
                        .getInput().getMeta().getLongName() ) )
            || ( "Y".equals( axName ) &&
                 input.getMeta().getLongName()
                .equals( FloatingArrayCoord.Y
                        .getInput().getMeta().getLongName() ) );
    }

    /**
     * Extends X and Y coordinate ranges to cover all the positions
     * represented by array-valued X and Y coordinates in a TupleSequence.
     *
     * @param  tseq   tuple sequence
     * @param  xyReader  reader for array data
     * @param  xRange   X range to adjust
     * @param  yRange   Y range to adjust
     */
    private void extendRanges( TupleSequence tseq,
                               Function<Tuple,XYArrayData> xyReader,
                               Range xRange, Range yRange ){
        while ( tseq.next() ) {
            XYArrayData xyData = xyReader.apply( tseq );
            if ( xyData != null ) {
                int np = xyData.getLength();
                for ( int ip = 0; ip < np; ip++ ) {
                    xRange.submit( xyData.getX( ip ) );
                    yRange.submit( xyData.getY( ip ) );
                }
            }
        }
    }

   /**
     * Returns a reader for matched X/Y array data for use with array plotters.
     * If null is returned from this function, no plotting should be done.
     *
     * @param  xsCoord  coordinate for X array
     * @param  ysCoord  coordinate for Y array
     * @param  icXs   X array coordinate index in group
     * @param  icYs   Y array coordinate index in group
     * @param  dataSpec  data specification
     * @return  thread-safe function to map tuples to XYArrayData;
     *          the function returns null for tuples
     *          that should not be plotted/accumulated
     */
    public static Function<Tuple,XYArrayData>
            createXYArrayReader( FloatingArrayCoord xsCoord,
                                 FloatingArrayCoord ysCoord,
                                 int icXs, int icYs, DataSpec dataSpec ) {
        boolean hasX = ! dataSpec.isCoordBlank( icXs );
        boolean hasY = ! dataSpec.isCoordBlank( icYs );
        if ( hasX && hasY ) {
            return tuple -> {
                int np = xsCoord.getArrayCoordLength( tuple, icXs );
                if ( np > 0 &&
                     np == ysCoord.getArrayCoordLength( tuple, icYs ) ) {
                    double[] xs = xsCoord.readArrayCoord( tuple, icXs );
                    double[] ys = ysCoord.readArrayCoord( tuple, icYs );
                    return new XYArrayData() {
                        public int getLength() {
                            return np;
                        }
                        public double getX( int i ) {
                            return xs[ i ];
                        }
                        public double getY( int i ) {
                            return ys[ i ];
                        }
                    };
                }
                else {
                    return null;
                }
            };
        }
        else if ( hasX ) {
            return tuple -> {
                double[] xs = xsCoord.readArrayCoord( tuple, icXs );
                return xs != null && xs.length > 0
                     ? new XYArrayData() {
                           public int getLength() {
                               return xs.length;
                           }
                           public double getX( int i ) {
                               return xs[ i ];
                           }
                           public double getY( int i ) {
                               return (double) i;
                           }
                       }
                     : null;
            };
        }
        else if ( hasY ) {
            return tuple -> {
                double[] ys = ysCoord.readArrayCoord( tuple, icYs );
                return ys != null && ys.length > 0
                     ? new XYArrayData() {
                           public int getLength() {
                               return ys.length;
                           }
                           public double getX( int i ) {
                               return (double) i;
                           }
                           public double getY( int i ) {
                               return ys[ i ];
                           }
                       }
                     : null;
            };
        }
        else {
            return null;
        }
    }

    /**
     * Prepares a CoordGroup suitable for use with an ArrayShapePlotter
     * based on an array-consuming ShapeForm and a ShapeMode.
     *
     * @param  form  shape form that uses X and Y array-valued coordinates
     * @param  mode  shading mode
     * @return  coord group
     */
    private static CoordGroup createArrayCoordGroup( ShapeForm form,
                                                     ShapeMode mode ) {
        List<Coord> clist = new ArrayList<>();
        clist.add( FloatingArrayCoord.X );
        clist.add( FloatingArrayCoord.Y );
        clist.addAll( Arrays.asList( form.getExtraCoords() ) );
        clist.addAll( Arrays.asList( mode.getExtraCoords() ) );
        Coord[] coords = clist.toArray( new Coord[ 0 ] );
        boolean[] rangeCoordFlags = new boolean[ coords.length ];
        rangeCoordFlags[ 0 ] = true;
        rangeCoordFlags[ 1 ] = true;
        return CoordGroup
              .createNoBasicCoordGroup( coords, form.getExtraPositionCount(),
                                        rangeCoordFlags );
    }
}
