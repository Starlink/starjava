package uk.ac.starlink.topcat.plot2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import uk.ac.starlink.table.ValueInfo;

/**
 * Defines criteria for identifying a matching tuple of coordinates
 * from a list of ValueInfos.
 *
 * <p>Some implementations are also provided.  They are not bulletproof
 * (what they are trying to do is really an AI task)
 * but will probably do the right thing often enough to be useful
 * rather than annoying.
 *
 * @author   Mark Taylor
 * @since    11 Mar 2019
 */
public abstract class CoordSpotter {

    private final int ntype_;

    /**
     * Constructor.
     *
     * @param   ntype  number of entries per tuple
     */
    protected CoordSpotter( int ntype ) {
        ntype_ = ntype;
    }

    /**
     * Returns the number of entries in the tuples found by this object.
     *
     * @return  number of different coordinate types that can be identified
     */
    public int getTypeCount() {
        return ntype_;
    }

    /**
     * Returns an index indicating which coordinate type known by this spotter,
     * if any, the given value info corresponds to.
     * If it doesn't look like one of the coordinates this object knows about,
     * a negative value is returned.
     *
     * @param  info  metadata item to test
     * @return  integer from 0 to <code>ntype</code>
     *          if info is recognised, -1 if not
     */
    public abstract int getCoordType( ValueInfo info );

    /**
     * Tries to find a number of, ideally matching, entries in a list
     * of ValueInfos that corresponds to a fixed number of coordinate tuples,
     * using this spotter's coordinate identification criteria.
     *
     * @param  npos  number of tuples required
     * @param  infos  list of available metadata items
     * @return  if successful, an npos*ntype-element array giving
     *          (a1,b1,...a2,b2,..aN,bN),
     *          or null on failure
     */
    public ValueInfo[] findCoordGroups( int npos, ValueInfo[] infos ) {
        ValueInfo[] foundInfos = new ValueInfo[ ntype_ * npos ];
        int[] ifounds = new int[ ntype_ ];
        for ( ValueInfo info : infos ) {
            int itype = getCoordType( info );
            if ( itype >= 0 ) {
                if ( ifounds[ itype ] < npos ) {
                    foundInfos[ ifounds[ itype ] * ntype_ + itype ] = info;
                    ifounds[ itype ]++;
                }
            }
        }
        for ( int ifound : ifounds ) {
            if ( ifound < npos ) {
                return null;
            }
        }
        return foundInfos;
    }

    /**
     * Tries to find a number of, ideally matching, entries in a list
     * of ValueInfos that corresponds to a fixed number of coordinate tuples,
     * using this coordinate identification criteria from a number of
     * different spotter instances.  Each one is tried in turn until
     * one succeeds.
     *
     * @param  npos  number of tuples required
     * @param  infos  list of available metadata items
     * @param  spotters  list of spotter implementations to try
     * @return  if successful, an npos*ntype-element array giving
     *          (a1,b1,...a2,b2,..aN,bN),
     *          or null on failure
     */
    public static ValueInfo[] findCoordGroups( int npos,
                                               ValueInfo[] infos,
                                               CoordSpotter[] spotters ) {
        for ( CoordSpotter spotter : spotters ) {
            ValueInfo[] foundInfos = spotter.findCoordGroups( npos, infos );
            if ( foundInfos != null ) {
                return foundInfos;
            }
        }
        return null;
    }

    /**
     * Returns a CoordSpotter instance that looks at info name prefixes
     * or suffixes.  The supplied pre/suffixes must be strictly alphabetic,
     * since non-alphabetic characters are used as word boundaries.
     *
     * @param  alphaPrefixes   list of case-insensitive alphabetic column name
     *                         prefixes/suffixes, one to identify
     *                         each element of a coordinate
     *                         group tuple
     * @param  isPrefix   true to look for prefixes, false for suffixes
     * @return   new spotter
     */
    public static CoordSpotter
            createNamePrefixSpotter( final String[] alphaPrefixes,
                                     final boolean isPrefix ) {
        final List<String> prefList = new ArrayList<String>();
        for ( String p : alphaPrefixes ) {
            prefList.add( p.toLowerCase() );
        }
        final String eraseRegex = isPrefix ? "[^a-z].*" : "^.*[^a-z]";
        return new CoordSpotter( alphaPrefixes.length ) {
            public int getCoordType( ValueInfo info ) {
                String name = info.getName();
                if ( name != null ) {
                    String namePart =
                        name.toLowerCase().replaceFirst( eraseRegex, "" );
                    return prefList.indexOf( namePart );
                }
                else {
                    return -1;
                }
            }
        };
    }

    /**
     * Returns a CoordSpotter instance that looks at UCDs.
     *
     * @param  root   UCD root (do not include trailing ".")
     * @param  tails  list of strings to append to the root+"." to make
     *                a UCD for each tuple element to be identified
     * @param  allowSuffix  if false, UCD matching must be exact
     *                      (apart from case); if true, trailing text after
     *                      the matched part is allowed
     */
    public static CoordSpotter createUcdSpotter( String root, String[] tails,
                                                 boolean allowSuffix ) {
        final int nu = tails.length;
        final String[] ucds = new String[ nu ];
        for ( int iu = 0; iu < nu; iu++ ) {
            ucds[ iu ] = ( root + "." + tails[ iu ] ).toLowerCase();
        }
        final List<String> ucdList = Arrays.asList( ucds );
        return allowSuffix
             ? new CoordSpotter( nu ) {
                   public int getCoordType( ValueInfo info ) {
                       String ucd = info.getUCD();
                       if ( ucd != null ) {
                           String lucd = ucd.toLowerCase();
                           for ( int iu = 0; iu < nu; iu++ ) {
                               if ( lucd.startsWith( ucds[ iu ] ) ) {
                                   return iu;
                               }
                           }
                       }
                       return -1;
                   }
               }
             : new CoordSpotter( nu ) {
                   public int getCoordType( ValueInfo info ) {
                       String ucd = info.getUCD();
                       return ucd == null
                            ? -1
                            : ucdList.indexOf( ucd.toLowerCase() );
                   }
               };
    }

    /**
     * Returns a CoordSpotter that looks for a single 1-dimensional array.
     *
     * @param   leng  declared length of a 1-d array;
     *                if -1, only arrays of indeterminate length
     *                will be identified
     * @return  new spotter
     */
    public static CoordSpotter createVectorSpotter( int leng ) {
        return new CoordSpotter( 1 ) {
            public int getCoordType( ValueInfo info ) {
                int[] shape = info.getShape();
                final boolean isMatch;
                return shape != null && shape.length == 1 && shape[ 0 ] == leng
                     ? 0
                     : -1;
            }
        };
    }
}
