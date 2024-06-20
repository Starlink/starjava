package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.HealpixTableInfo;
import uk.ac.starlink.table.MetaCopyStarTable;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.ValueInfo;
import uk.ac.starlink.ttools.cone.HealpixTiling;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;

/**
 * Filter to manipulate HEALPix-specific metadata of a table
 * supposed to contain HEALPix pixel data.
 *
 * @author   Mark Taylor
 * @since    3 Jan 2019
 */
public class HealpixMetadataFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public HealpixMetadataFilter() {
        super( "healpixmeta",
               "[-level <n>] " +
               "[-implicit|-column <col-id>] " +
               "[-csys C|G|E] " +
               "[-nested|-ring] " );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Adjusts the table metadata items that describe how",
            "HEALPix pixel data is encoded in the table.",
            "</p>",
            "<p>Zero or more of the following flags may be supplied:",
            "<ul>",
            "<li><code>-level &lt;n&gt;</code>:",
                "Defines the HEALPix level;",
                "the sky is split into 12*4^n pixels.",
                "This quantity is equal to logarithm base 2 of NSIDE.</li>",
            "<li><code>-implicit</code>:",
                "Declares that pixel indices are implicit, so that row",
                "<m>i</m> represents HEALPix pixel index <m>i</m>.",
                "The table should have 12*4^level rows in this case.",
                "Not to be used with <code>-column</code>.</li>",
            "<li><code>-column &lt;col-id&gt;</code>:",
                "Declares that the column identified",
                "contains the (0-based) HEALPix pixel index.",
                "Not to be used with <code>-implicit</code>.</li>",
            "<li><code>-csys C|G|E</code>:",
                "Declares the sky coordinate system to which the HEALPix",
                "pixels apply:",
                "<strong>C</strong>elestial(=equatorial),",
                "<strong>G</strong>alactic or",
                "<strong>E</strong>cliptic.",
                "Some applications assume Galactic",
                "if this is not specified.</li>",
            "<li><code>-nested</code>:",
                "Declares that the NESTED ordering scheme is in use.",
                "Not to be used with <code>-ring</code>.</li>",
            "<li><code>-ring</code>:",
                "Declares that the RING ordering scheme is in use.",
                "Not to be used with <code>-nested</code>.</li>",
            "</ul>",
            "</p>",
            "<p>The effect of this filter is to write, or overwrite,",
            "certain special table parameters (per-table metadata)",
            "that STIL uses to describe how HEALPix pixel information",
            "is encoded in a table, specifically the HEALPix level,",
            "the column containing pixel index, the ordering scheme,",
            "and the sky coordinate system.",
            "Adding these parameters doesn't do anything on its own,",
            "but some of the STIL I/O handlers recognise these parameters,",
            "and they affect how the table will be formatted for output.",
            "In particular, if you set these parameters and then output",
            "to FITS format,",
            "the output table will contain headers defined by the",
            "<webref url='"
                   + "https://healpix.sourceforge.io/data/examples/"
                   + "healpix_fits_specs.pdf"
                   + "'>HEALPix-FITS</webref>",
            "serialization format which is understood by several",
            "other applications to describe HEALPix maps.",
            "If you write to VOTable format, the metadata will only",
            "be recognised by other STIL-based applications",
            "but it means that if you, e.g., load the table into TOPCAT",
            "and then write it out again as FITS, the HEALPix information",
            "should be preserved.",
            "</p>",
            "<p>When writing tables marked up like this to FITS,",
            "you have two options.",
            "If you write to one of the \"normal\" FITS formats",
            "(e.g. <code>fits</code>, <code>fits-basic</code>)",
            "then suitable headers will be added;",
            "in this case if an explicit pixel index column is used it must be",
            "the first column, and should be named \"PIXEL\".",
            "This may be enough for other applications to recognise",
            "the HEALPix metadata.",
            "However, if you use the special <code>fits-healpix</code> format",
            "more efforts will be made to conform to the HEALPix-FITS",
            "convention, for instance moving and renaming",
            "the explicit pixel index column if required.",
            "</p>",
            "<p>The table parameters affected by this filter are:",
            "<code>" + HealpixTableInfo.HPX_LEVEL_INFO.getName() + "</code>,",
            "<code>" + HealpixTableInfo.HPX_ISNEST_INFO.getName() + "</code>,",
            "<code>" + HealpixTableInfo.HPX_COLNAME_INFO.getName() + "</code>,",
            "<code>" + HealpixTableInfo.HPX_CSYS_INFO.getName() + "</code>.",
            "Note these are not defined by any standard,",
            "they are defined and used only by STILTS and related",
            "applications (TOPCAT).",
            "</p>",
            explainSyntax( new String[] { "col-id", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        int level = -1;
        String colid = null;
        HealpixTableInfo.HpxCoordSys csys = null;
        Boolean isNest = null;
        while ( argIt.hasNext() ) {
            String arg = argIt.next();
            if ( arg.equals( "-level" ) && argIt.hasNext() ) {
                argIt.remove();
                String sLevel = argIt.next();
                argIt.remove();
                try {
                    level = Integer.parseInt( sLevel );
                }
                catch ( NumberFormatException e ) {
                    throw new ArgException( "-level argument not numeric: "
                                          + sLevel );
                }
                if ( level < 0 || level > HealpixTiling.MAX_LEVEL ) {
                    throw new ArgException( "-level argument out of range: "
                                          + sLevel );
                }
            }
            else if ( arg.equals( "-column" ) && argIt.hasNext() ) {
                argIt.remove();
                colid = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-implicit" ) ) {
                argIt.remove();
                colid = "";
            }
            else if ( arg.equals( "-csys" ) && argIt.hasNext() ) {
                argIt.remove();
                String sSys = argIt.next();
                argIt.remove();
                csys = sSys.length() == 1
                     ? HealpixTableInfo
                      .HpxCoordSys
                      .fromCharacter( Character
                                     .toUpperCase( sSys.charAt( 0 ) ) )
                     : null;
                if ( csys == null ) {
                    throw new ArgException( "-csys argument not valid char: "
                                          + sSys );
                }
            }
            else if ( arg.equals( "-nest" ) | arg.equals( "-nested" ) ) {
                argIt.remove();
                isNest = Boolean.TRUE;
            }
            else if ( arg.equals( "-ring" ) ) {
                argIt.remove();
                isNest = Boolean.FALSE;
            }
            else {
                throw new ArgException( "Unknown argument " + arg );
            }
        }
        return new HealpixStep( level, colid, csys, isNest );
    }

    /**
     * Processing step implementation for this class.
     */
    private static class HealpixStep implements ProcessingStep {

        private final int level_;
        private final String colid_;
        private final HealpixTableInfo.HpxCoordSys csys_;
        private final Boolean isNest_;

        /**
         * Constructor.  Arguments indicate HEALPix-specific parameters
         * that should be (over)written.
         *
         * @param  level  healpix level, or negative for no write
         * @param  colid  identifier for explicit pixel index column,
         *                or empty string for implicit pixel index,
         *                or null for no write
         * @param  csys   coord sys, or null for no write
         * @param  isNest true for nested, false for ring, or null for no write
         */
        public HealpixStep( int level, String colid,
                            HealpixTableInfo.HpxCoordSys csys,
                            Boolean isNest ) {
            level_ = level;
            colid_ = colid;
            csys_ = csys;
            isNest_ = isNest;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            MetaCopyStarTable table = new MetaCopyStarTable( base );
            List<DescribedValue> params =
                new ArrayList<DescribedValue>( table.getParameters() );
            table.setParameters( params );
            if ( level_ >= 0 ) {
                replaceMeta( params, HealpixTableInfo.HPX_LEVEL_INFO,
                             Integer.valueOf( level_ ) );
            }
            if ( colid_ != null ) {
                final String colname;
                if ( colid_.trim().length() == 0 ) {
                    colname = null;
                }
                else {
                    int icol = new ColumnIdentifier( table )
                              .getColumnIndex( colid_ );
                    colname = table.getColumnInfo( icol ).getName();
                }
                replaceMeta( params, HealpixTableInfo.HPX_COLNAME_INFO,
                             colname );
            }
            if ( csys_ != null ) {
                replaceMeta( params, HealpixTableInfo.HPX_CSYS_INFO,
                             csys_.getCharString() );
            }
            if ( isNest_ != null ) {
                replaceMeta( params, HealpixTableInfo.HPX_ISNEST_INFO,
                             isNest_ );
            }
            HealpixTableInfo hpxInfo = HealpixTableInfo.fromParams( params );
            return table;
        }

        /**
         * Writes a given parameter value into a parameter list,
         * or overwrites it if it is present.  Any similarly named values
         * are removed.
         *
         * @param  params  writable list of parameters to adjust
         * @param  key     metadata key
         * @param  value   metadata value
         */
        private static void replaceMeta( List<DescribedValue> params,
                                         ValueInfo key, Object value ) {
            DescribedValue toAdd = new DescribedValue( key, value );
            for ( ListIterator<DescribedValue> it = params.listIterator();
                  it.hasNext(); ) {
                DescribedValue dval = it.next();
                if ( dval.getInfo().getName().equals( key.getName() ) ) {
                    if ( toAdd != null ) {
                        it.set( toAdd );
                        toAdd = null;
                    }
                    else {
                        it.remove();
                    }
                }
            }
            if ( toAdd != null ) {
                params.add( toAdd );
            }
        }
    }
}
