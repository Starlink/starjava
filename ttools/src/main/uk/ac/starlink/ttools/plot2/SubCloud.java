package uk.ac.starlink.ttools.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import uk.ac.starlink.ttools.plot2.data.Coord;
import uk.ac.starlink.ttools.plot2.data.CoordGroup;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;

/**
 * Aggregates the DataGeom and the geometry-specific parts of the
 * DataSpec for a layer.
 * This defines one set of positions that it will return for a given
 * tuple sequence (one position per tuple).
 *
 * <p>Equality is implemented so that two equal subclouds have the same
 * geom and coordinate columns.  This means they will iterate over
 * the same data positions for a given tuple sequence,
 * but not necessarily that they have the same
 * DataSpec objects or coordinate index positions.
 *
 * @author   Mark Taylor
 * @since    29 Nov 2013
 */
@Equality
public class SubCloud {

    private final DataGeom geom_;
    private final DataSpec spec_;
    private final int iPosCoord_;

    /**
     * Constructor.
     *
     * @param  geom  data geom
     * @param  spec  data spec
     * @param  iPosCoord  index of coordinate at which position information
     *                    starts in the DataSpec
     */
    public SubCloud( DataGeom geom, DataSpec spec, int iPosCoord ) {
        geom_ = geom;
        spec_ = spec;
        iPosCoord_ = iPosCoord;
    }

    /**
     * Returns the data geom for this subcloud.
     *
     * @return  geom
     */
    public DataGeom getDataGeom() {
        return geom_;
    }

    /**
     * Returns the data spec for this subcloud.
     *
     * @return  spec
     */
    public DataSpec getDataSpec() {
        return spec_;
    }

    /**
     * Returns the index of the data spec coordinate at which the
     * position information starts for this subcloud.
     *
     * @return  position coordinate index
     */
    public int getPosCoordIndex() {
        return iPosCoord_;
    }

    @Override
    public int hashCode() {
        int code = 7701;
        code = 23 * code + geom_.hashCode();
        code = 23 * code + spec_.getSourceTable().hashCode();
        code = 23 * code + spec_.getMaskId().hashCode();
        code = 23 * code + Arrays.hashCode( getCoordIds() );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof SubCloud ) {
            SubCloud other = (SubCloud) o;
            return this.geom_.equals( other.geom_ )
                && this.spec_.getSourceTable()
                             .equals( other.spec_.getSourceTable() )
                && this.spec_.getMaskId().equals( other.spec_.getMaskId() )
                && Arrays.equals( this.getCoordIds(), other.getCoordIds() );
        }
        else {
            return false;
        }
    }

    /**
     * Returns an array of the coord IDs for the coordinates that determine
     * the position information for this cloud.
     *
     * @return   coord column identifier array
     */
    private Object[] getCoordIds() {
        int npc = geom_.getPosCoords().length;
        Object[] ids = new Object[ npc ];
        for ( int ipc = 0; ipc < npc; ipc++ ) {
            ids[ ipc ] = spec_.getCoordId( iPosCoord_ + ipc );
        }
        return ids;
    }

    /**
     * Returns an array of subclouds from a list of layers,
     * with optional deduplication.
     *
     * @param  layers  plot layers
     * @param  deduplicate  true to cull duplicate subclouds
     * @return subclouds
     */
    public static SubCloud[] createSubClouds( PlotLayer[] layers,
                                              boolean deduplicate ) {
        Collection<SubCloud> subClouds = createCollection( deduplicate );
        int nl = layers.length;
        int ndim0 = -1;
        for ( int il = 0; il < nl; il++ ) {
            PlotLayer layer = layers[ il ];
            DataGeom geom = layer.getDataGeom();
            DataSpec spec = layer.getDataSpec();
            CoordGroup cgrp = layer.getPlotter().getCoordGroup();
            if ( geom != null && spec != null ) {
                for ( int ipos = 0; ipos < cgrp.getBasicPositionCount();
                      ipos++ ) {
                    int icPos = cgrp.getPosCoordIndex( ipos, geom );
                    subClouds.add( new SubCloud( geom, spec, icPos ) );
                }
                for ( int ipos = 0; ipos < cgrp.getExtraPositionCount();
                      ipos++ ) {
                     int icPos = cgrp.getExtraCoordIndex( ipos, geom );
                     subClouds.add( new SubCloud( geom, spec, icPos ) );
                }

                /* Check consistency. */
                int ndim = geom.getDataDimCount();
                if ( ndim0 < 0 ) {
                    ndim0 = ndim;
                }
                else if ( ndim != ndim0 ) {
                    throw new IllegalArgumentException( "dimensionality "
                                                      + "mismatch" );
                }
            }
        }
        return subClouds.toArray( new SubCloud[ 0 ] );
    }

    /**
     * Returns a collection of subclouds from a list of layers,
     * but including only those layers whose positions are partial.
     * This means that the data position coordinate arrays will have
     * some elements NaN, indicating something other than an actual
     * point in the data space - for instance a vertical or horizontal
     * line.
     *
     * @param  layers  plot layers
     * @param  deduplicate  true to cull duplicate subclouds
     * @return subclouds from partial position layers only
     */
    public static SubCloud[] createPartialSubClouds( PlotLayer[] layers,
                                                     boolean deduplicate ) {
        Collection<SubCloud> subClouds = createCollection( deduplicate );
        int nl = layers.length;
        for ( int il = 0; il < nl; il++ ) {
            PlotLayer layer = layers[ il ];
            DataGeom dataGeom = layer.getDataGeom();
            DataSpec dataSpec = layer.getDataSpec();
            if ( dataGeom != null && dataSpec != null ) {
                CoordGroup cgrp = layer.getPlotter().getCoordGroup();
                if ( cgrp.isSinglePartialPosition() ) {
                    assert cgrp.getBasicPositionCount() == 0;
                    subClouds.add( new SubCloud( dataGeom, dataSpec, 0 ) );
                }
            }
        }
        return subClouds.toArray( new SubCloud[ 0 ] );
    }

    /**
     * Returns a collection of subclouds for a number of positions from
     * a data spec.
     *
     * @param  geom  data geom
     * @param  spec  data spec
     * @param  npos  number of positions in the data spec
     * @param  deduplicate  true to cull duplicate layers
     * @return  collection of subclouds
     */
    public static SubCloud[] createSubClouds( DataGeom geom, DataSpec spec,
                                              int npos, boolean deduplicate ) {
        Collection<SubCloud> subClouds = createCollection( deduplicate );
        if ( geom != null && spec != null && npos > 0 ) {
            int npc = geom.getPosCoords().length;
            for ( int ip = 0; ip < npos; ip++ ) {
                subClouds.add( new SubCloud( geom, spec, ip * npc ) );
            }
        }
        return subClouds.toArray( new SubCloud[ 0 ] );
    }

    /**
     * Returns a SubCloud collection that will handle addition of equal
     * items appropriately for a given deduplication strategy.
     * If <code>deduplicate</code> is false, all added items will be in
     * the result, but if true, then the result will only contain one
     * instance of each equal item.
     *
     * @param  deduplicate  whether deduplication is required
     * @return  new mutable collection to which subclouds can be added
     */
    private static Collection<SubCloud>
            createCollection( boolean deduplicate ) {
        return deduplicate ? new LinkedHashSet<SubCloud>()
                           : new ArrayList<SubCloud>();
    }
}
