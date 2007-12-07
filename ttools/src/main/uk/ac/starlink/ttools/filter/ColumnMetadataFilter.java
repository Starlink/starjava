package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.WrapperStarTable;

/**
 * Filter to alter metadata of one or more columns.
 *
 * @author   Mark Taylor
 * @since    26 Apr 2006
 */
public class ColumnMetadataFilter extends BasicFilter {

    /**
     * Constructor.
     */
    public ColumnMetadataFilter() {
        super( "colmeta",
               "[-name <name>] [-units <units>] [-ucd <ucd>] " +
               "[-desc <descrip>]\n" +
               "<colid-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Modifies the metadata of one or more columns.",
            "Some or all of the name, units, ucd and description of ",
            "the column(s), identified by <code>&lt;colid-list&gt;</code>",
            "can be set by using some or all of the listed flags.",
            "Typically, <code>&lt;colid-list&gt;</code> will simply be",
            "the name of a single column.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator argIt ) throws ArgException {
        String colidList = null;
        String rename = null;
        String units = null;
        String ucd = null;
        String desc = null;
        while ( argIt.hasNext() && colidList == null ) {
            String arg = (String) argIt.next();
            if ( arg.equals( "-name" ) && argIt.hasNext() ) {
                argIt.remove();
                rename = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.startsWith( "-unit" ) && argIt.hasNext() ) {
                argIt.remove();
                units = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-ucd" ) && argIt.hasNext() ) {
                argIt.remove();
                ucd = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-desc" ) && argIt.hasNext() ) {
                argIt.remove();
                desc = (String) argIt.next();
                argIt.remove();
            }
            else if ( arg.startsWith( "-" ) ) {
                argIt.remove();
                throw new ArgException( "No such flag " + arg );
            }
            else if ( colidList == null ) {
                colidList = arg;
                argIt.remove();
            }
        }
        if ( colidList == null ) {
            throw new ArgException( "No columns specified" );
        }
        return new ColMetaStep( colidList, rename, units, ucd, desc );
    }

    /**
     * ProcessingStep implementation for altering column metadata.
     */
    private static class ColMetaStep implements ProcessingStep {

        final String colidList_;
        final String name_;
        final String units_;
        final String ucd_;
        final String desc_;

        /**
         * Constructor.
         *
         * @param   colidList  list of column ids to affect
         * @param   name    new column name
         * @param   units   new column units
         * @param   ucd     new column ucd
         * @param   desc    new column description
         */
        public ColMetaStep( String colidList, String name, String units,
                            String ucd, String desc ) {
            colidList_ = colidList;
            name_ = name;
            units_ = units;
            ucd_ = ucd;
            desc_ = desc;
        }

        public StarTable wrap( StarTable base ) throws IOException {
            int ncol = base.getColumnCount();
            boolean[] colFlags = new ColumnIdentifier( base )
                                .getColumnFlags( colidList_ );
            assert colFlags.length == ncol;
            final ColumnInfo[] colInfos = new ColumnInfo[ ncol ];
            for ( int icol = 0; icol < ncol; icol++ ) {
                ColumnInfo info = new ColumnInfo( base.getColumnInfo( icol ) );
                if ( colFlags[ icol ] ) {
                    if ( name_ != null ) {
                        info.setName( name_ );
                    }
                    if ( units_ != null ) {
                        info.setUnitString( units_ );
                    }
                    if ( ucd_ != null ) {
                        info.setUCD( ucd_ );
                    }
                    if ( desc_ != null ) {
                        info.setDescription( desc_ );
                    }
                }
                colInfos[ icol ] = info;
            }

            return new WrapperStarTable( base ) {
                public ColumnInfo getColumnInfo( int icol ) {
                    return colInfos[ icol ];
                }
            };
        }
    }
}
