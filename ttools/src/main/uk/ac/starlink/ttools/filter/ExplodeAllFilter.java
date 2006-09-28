package uk.ac.starlink.ttools.filter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.logging.Logger;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ExplodedStarTable;

/**
 * Table filter for replacing every N-element array valued column in 
 * a table with N scalar-valued columns.
 *
 * @author   Mark Taylor
 * @since    5 Aug 2005
 */
public class ExplodeAllFilter extends BasicFilter {

    private static Logger logger_ = 
        Logger.getLogger( "uk.ac.starlink.ttools.filter" );

    public ExplodeAllFilter() {
        super( "explodeall", "[-ifndim <ndim>] [-ifshape <dims>]" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Replaces any columns which is an N-element arrays with",
            "N scalar columns.",
            "Only columns with fixed array sizes are affected.",
            "The action can be restricted to only columns of a certain",
            "shape using the flags.",
            "</p>",
            "<p>If the <code>-ifndim</code> flag is used, then only columns",
            "of dimensionality <code>&lt;ndim&gt;</code> will be exploded.",
            "<code>&lt;ndim&gt;</code> may be 1, 2, ....",
            "</p>",
            "<p>If the <code>-ifshape</code> flag is used, then only columns",
            "with a specific shape will be exploded;",
            "<code>&lt;dims&gt;</code> is a space- or comma-separated list",
            "of dimension extents, with the most rapidly-varying first,",
            "e.g. '<code>2 5</code>' to explode all 2 x 5 element array",
            "columns.",
            "</p>",
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {

        /* Collect flags. */
        int ndim = -1;
        int[] shape = null;
        while ( argIt.hasNext() ) {
            String arg = (String) argIt.next();
            if ( "-ifndim".equals( arg ) && ndim < 0 && argIt.hasNext() ) {
                argIt.remove();
                String txt = (String) argIt.next();
                argIt.remove();
                try {
                    ndim = Integer.parseInt( txt );
                    if ( ndim < 1 ) {
                        throw new ArgException( txt + " not positive" );
                    }
                }
                catch ( NumberFormatException e ) {
                    throw new ArgException( txt + " not an integer", e );
                }
            }
            else if ( "-ifshape".equals( arg ) && shape == null 
                      && argIt.hasNext() ) {
                argIt.remove();
                String txt = (String) argIt.next();
                argIt.remove();
                try {
                    String[] txts = txt.split( "( +|,)" );
                    shape = new int[ txts.length ];
                    for ( int i = 0; i < txts.length; i++ ) {
                        shape[ i ] = Integer.parseInt( txts[ i ] );
                        if ( shape[ i ] < 1 ) {
                            throw new IllegalArgumentException(
                               txts[ i ] + " is negative" );
                        }
                    }
                }
                catch ( RuntimeException e ) {
                    throw new ArgException(
                        "\"" + txt + "\" not a space- or comma-separated list "
                      + "of positive integers", e );
                }
            }
        }
        final int reqNdim = ndim;
        final int[] reqShape = shape;

        /* Create and return the step. */
        return new ProcessingStep() {
            public StarTable wrap( StarTable base ) {

                /* Work out which columns to explode, if there is a 
                 * restriction. */
                int ncol = base.getColumnCount();
                boolean[] explodes = new boolean[ ncol ];
                int nexplode = 0;
                for ( int icol = 0; icol < ncol; icol++ ) {
                    ColumnInfo info = base.getColumnInfo( icol );
                    boolean explode;
                    if ( info.isArray() ) {
                        int[] shape = info.getShape();
                        explode = shape != null;
                        if ( shape != null ) {
                            if ( shape.length == 0 ||
                                 shape[ shape.length - 1 ] < 0 ) {
                                explode = false;
                            }
                            if ( reqNdim >= 0 ) {
                                explode = explode
                                       && shape.length == reqNdim;
                            }
                            if ( reqShape != null ) {
                                explode = explode
                                   && Arrays.equals( shape, reqShape );
                            }
                        }
                    }
                    else {
                        explode = false;
                    }
                    explodes[ icol ] = explode;
                    if ( explodes[ icol ] ) {
                        nexplode++;
                    }
                }
                logger_.info( "Exploding " + nexplode + 
                              "/" + ncol + " columns" );

                /* Return the wrapped table. */
                return new ExplodedStarTable( base, explodes );
            }
        };
    }
}
