package uk.ac.starlink.ttools.plot2;

import java.awt.Point;
import java.lang.Iterable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NoSuchElementException;
import uk.ac.starlink.ttools.plot2.data.DataSpec;
import uk.ac.starlink.ttools.plot2.data.DataStore;
import uk.ac.starlink.ttools.plot2.data.TupleSequence;

/**
 * Represents an unordered set of data positions forming part of a plot.
 * An iterator over the points is provided.
 * Instances of this class may also be compared for equality:
 * if instances compare equal, the iterators will dispense the same positions,
 * though not necessarily in the same order.
 *
 * <p>The unordered semantics of this class is imposed by the equality
 * requirement.  Most of the ordering could be restored by rewording the
 * contract a bit differently if that becomes useful, but at time of
 * writing it's not needed.
 *
 * @author   Mark Taylor
 * @since    13 Feb 2013
 */
@Equality
public class PointCloud {

    private final int ndim_;
    private final Collection<SubCloud> subClouds_;

    /**
     * Constructs a point cloud from an array of data layers.
     *
     * <p>If the <code>deduplicate</code> parameter is set true,
     * then if there are any groups of layers which can be determined to
     * return the same position sequences as each other, duplicates
     * will be culled leaving only one from each group.
     * This should be set true if you're interested only in the position
     * values, not how many times each one appears.
     *
     * @param  layers  plot layers
     * @param  deduplicate  true to cull duplicate layers
     */
    public PointCloud( PlotLayer[] layers, boolean deduplicate ) {
        this( createSubClouds( layers, deduplicate ) );
    }

    /**
     * Constructs a point cloud from a single layer.
     *
     * @param  layer   plot layer
     */
    public PointCloud( PlotLayer layer ) {
        this( layer.getDataGeom(), layer.getDataSpec(),
              layer.getPlotter().getPositionCount(), true );
    }

    /**
     * Constructs a point cloud from a geometry/dataspec pair.
     * The geometry information is assumed to start at the first column.
     *
     * @param  geom  data geom
     * @param  spec  data spec
     * @param  npos  number of positions in the data spec
     * @param  deduplicate  true to cull duplicated entries
     */
    public PointCloud( DataGeom geom, DataSpec spec, int npos,
                       boolean deduplicate ) {
        this( createSubClouds( geom, spec, npos, deduplicate ) );
    }

    /**
     * Constructs a point cloud from a collection of subclouds.
     *
     * @return  subcloud collection
     */
    public PointCloud( Collection<SubCloud> subClouds ) {
        subClouds_ = subClouds;
        Iterator<SubCloud> it = subClouds.iterator();
        ndim_ = it.hasNext() ? it.next().geom_.getDataDimCount() : 0;
    }

    /**
     * Convenience method to return an iterable over data positions.
     * Just calls {@link #createDataPosIterator}.
     *
     * @param   dataStore  data storage object
     * @return  iterable over data positions
     */
    public Iterable<double[]>
           createDataPosIterable( final DataStore dataStore ) {
        return new Iterable<double[]>() {
            public Iterator<double[]> iterator() {
                return createDataPosIterator( dataStore );
            }
        };
    }

    /**
     * Returns an iterator over data positions.
     * Iterates over <code>dataDimCount</code>-element arrays
     * giving position in data space.  The same <code>double[]</code>
     * array object is returned each time with different contents,
     * so beware of storing it between iterations.
     *
     * @param   dataStore  data storage object
     * @return  iterator over data positions
     */
    public Iterator<double[]> createDataPosIterator( DataStore dataStore ) {
        return new DataPosIterator( dataStore );
    }

    /**
     * Returns an array of the subclouds in this point cloud.
     *
     * @return  subcloud array
     */
    public SubCloud[] getSubClouds() {
        return subClouds_.toArray( new SubCloud[ 0 ] );
    }

    @Override
    public int hashCode() {
        int code = 74433;
        code = 23 * code + unorderedHashCode( subClouds_ );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof PointCloud ) {
            PointCloud other = (PointCloud) o;
            assert unorderedEquals( this.subClouds_, other.subClouds_ )
                == unorderedEquals( other.subClouds_, this.subClouds_ );
            return unorderedEquals( this.subClouds_, other.subClouds_ );
        }
        else {
            return false;
        }
    }

    /**
     * Returns a hashcode for a collection which depends on the elements
     * it contains, but not on their order.
     *
     * @param   c  collection
     * @return  hash code
     */
    private static <T> int unorderedHashCode( Collection<T> c ) {
        int code = 0;
        for ( T item : c ) {
            code += c.hashCode();
        }
        return code;
    }

    /**
     * Equality determination for two collections based on the elements
     * they contain, but not on their order.
     *
     * @param  c1  one collection
     * @param  c2  other collection
     * @return  true iff c1 and c2 have the same elements
     */
    private static <T> boolean unorderedEquals( Collection<T> c1,
                                                Collection<T> c2 ) {
        if ( c1.equals( c2 ) ) {
            return true;
        }
        else if ( c1.size() != c2.size() ) {
            return false;
        }
        else {
            List<Object> l2 = new ArrayList<Object>( c2 );
            for ( Object item : c1 ) {
                if ( ! l2.remove( item ) ) {
                    return false;
                }
            }
            assert l2.isEmpty();
            return true;
        }
    }

    /**
     * Returns a collection of subclouds from a list of layers,
     * with optional deduplication.
     *
     * @param  layers  plot layers
     * @param  deduplicate  true to cull duplicate layers
     * @return subcloud collection
     */
    private static Collection<SubCloud> createSubClouds( PlotLayer[] layers,
                                                         boolean deduplicate ) {
        Collection<SubCloud> subClouds = deduplicate
                                       ? new LinkedHashSet<SubCloud>()
                                       : new ArrayList<SubCloud>();
        int nl = layers.length;
        int ndim0 = -1;
        for ( int il = 0; il < nl; il++ ) {
            PlotLayer layer = layers[ il ];
            DataGeom geom = layer.getDataGeom();
            DataSpec spec = layer.getDataSpec();
            int npos = layer.getPlotter().getPositionCount();
            if ( geom != null && spec != null && npos > 0 ) {

                /* Add an entry. */
                subClouds.addAll( createSubClouds( geom, spec, npos,
                                                   deduplicate ) );

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
        return subClouds;
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
    private static Collection<SubCloud> createSubClouds( DataGeom geom,
                                                         DataSpec spec,
                                                         int npos,
                                                         boolean deduplicate ) {
        Collection<SubCloud> subClouds = deduplicate
                                       ? new LinkedHashSet<SubCloud>()
                                       : new ArrayList<SubCloud>();
        if ( geom != null && spec != null && npos > 0 ) {
            int npc = geom.getPosCoords().length;
            for ( int ip = 0; ip < npos; ip++ ) {
                subClouds.add( new SubCloud( geom, spec, ip * npc ) );
            }
        }
        return subClouds;
    }

    /**
     * Iterator over data positions.
     */
    private class DataPosIterator implements Iterator<double[]> {
        private final DataStore dataStore_;
        private final Iterator<SubCloud> cloudIt_;
        private final double[] dpos_;
        private final double[] dpos1_;
        private final Point gp_;
        private DataGeom geom_;
        private int iPosCoord_;
        private TupleSequence tseq_;
        private boolean hasNext_;

        /**
         * Constructor.
         *
         * @param   dataStore  data storage object
         */
        DataPosIterator( DataStore dataStore ) {
            dataStore_ = dataStore;
            cloudIt_ = subClouds_.iterator();
            dpos_ = new double[ ndim_ ];
            dpos1_ = new double[ ndim_ ];
            gp_ = new Point();
            tseq_ = PlotUtil.EMPTY_TUPLE_SEQUENCE;
            hasNext_ = advance();
        }

        public boolean hasNext() {
            return hasNext_;
        }

        public double[] next() {
            if ( hasNext_ ) {
                System.arraycopy( dpos_, 0, dpos1_, 0, ndim_ );
                hasNext_ = advance();
                return dpos1_;
            }
            else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Does work for the next iteration.
         */
        private boolean advance() {
            while ( tseq_.next() ) {
                if ( geom_.readDataPos( tseq_, iPosCoord_, dpos_ ) ) {
                    return true;
                }
            }
            while ( cloudIt_.hasNext() ) {
                SubCloud cloud = cloudIt_.next();
                geom_ = cloud.geom_;
                iPosCoord_ = cloud.iPosCoord_;
                tseq_ = dataStore_.getTupleSequence( cloud.spec_ );
                return advance();
            }
            return false;
        }
    }

    /**
     * Aggregates the DataGeom and the geometry-specific parts of the
     * DataSpec for a layer.
     * This determines what data positions it will return.
     */
    @Equality
    public static class SubCloud {
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
        SubCloud( DataGeom geom, DataSpec spec, int iPosCoord ) {
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
            code = 23 * code + getGeomSpecId().hashCode();
            return code;
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof SubCloud ) {
                SubCloud other = (SubCloud) o;
                return this.geom_.equals( other.geom_ )
                    && this.getGeomSpecId().equals( other.getGeomSpecId() );
            }
            else {
                return false;
            }
        }

        private Object getGeomSpecId() {
            int nc = geom_.getPosCoords().length;
            int ni = nc + 2;
            List<Object> list = new ArrayList<Object>( ni );
            list.add( spec_.getSourceTable() );
            list.add( spec_.getMaskId() );
            for ( int ic = 0; ic < nc; ic++ ) {
                list.add( spec_.getCoordId( iPosCoord_ + ic ) );
            }
            assert list.size() == ni;
            return list;
        }
    }
}
