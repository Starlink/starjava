package uk.ac.starlink.ttools.filter;

import java.io.IOException;
import java.util.Iterator;
import uk.ac.starlink.ttools.jel.ColumnIdentifier;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DefaultValueInfo;
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
               "[-name <name>] [-units <units>] [-ucd <ucd>]\n" +
               "[-utype <utype>] [-xtype <xtype>] [-desc <descrip>]\n" +
               "[-shape <n>[,<n>...][,*]] [-elsize <n>]\n" +
               "<colid-list>" );
    }

    protected String[] getDescriptionLines() {
        return new String[] {
            "<p>Modifies the metadata of one or more columns.",
            "Some or all of the name, units, ucd, utype, xtype, description,",
            "shape and elementsize of",
            "the column(s), identified by <code>&lt;colid-list&gt;</code>",
            "can be set by using some or all of the listed flags.",
            "Typically, <code>&lt;colid-list&gt;</code> will simply be",
            "the name of a single column.",
            "</p>",
            "<p>The <code>-name</code>, <code>-units</code>,",
            "<code>-ucd</code>, <code>-utype</code>, <code>-xtype</code>",
            "and <code>-desc</code> flags just take textual arguments.",
            "The <code>-shape</code> flag can also be used,",
            "but is intended only for array-valued columns,",
            "e.g. <code>-shape 3,3</code> to declare a 3x3 array.",
            "The final entry only in the shape list",
            "may be a \"<code>*</code>\" character",
            "to indicate unknown extent.",
            "Array values with no specified shape effectively have a",
            "shape of \"<code>*</code>\".",
            "The <code>-elsize</code> flag may be used to specify the length",
            "of fixed length strings; use with non-string columns",
            "is not recommended.",
            "</p>",
            explainSyntax( new String[] { "colid-list", } ),
        };
    }

    public ProcessingStep createStep( Iterator<String> argIt )
            throws ArgException {
        String colidList = null;
        String rename = null;
        String units = null;
        String ucd = null;
        String utype = null;
        String xtype = null;
        String desc = null;
        int[] shape = null;
        int elsize = -1;
        while ( argIt.hasNext() && colidList == null ) {
            String arg = argIt.next();
            if ( arg.equals( "-name" ) && argIt.hasNext() ) {
                argIt.remove();
                rename = argIt.next();
                argIt.remove();
            }
            else if ( arg.startsWith( "-unit" ) && argIt.hasNext() ) {
                argIt.remove();
                units = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-ucd" ) && argIt.hasNext() ) {
                argIt.remove();
                ucd = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-utype" ) && argIt.hasNext() ) {
                argIt.remove();
                utype = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-xtype" ) && argIt.hasNext() ) {
                argIt.remove();
                xtype = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-desc" ) && argIt.hasNext() ) {
                argIt.remove();
                desc = argIt.next();
                argIt.remove();
            }
            else if ( arg.equals( "-shape" ) && argIt.hasNext() ) {
                argIt.remove();
                String shapeTxt = argIt.next();
                argIt.remove();
                try {
                    shape = DefaultValueInfo.unformatShape( shapeTxt );
                }
                catch ( Exception e ) {
                    throw new ArgException( "Bad -shape specification \""
                                          + shapeTxt + "\"" );
                }
            }
            else if ( arg.equals( "-elsize" ) && argIt.hasNext() ) {
                argIt.remove();
                String elsizeTxt = argIt.next();
                argIt.remove();
                try {
                    elsize = Integer.parseInt( elsizeTxt );
                }
                catch ( NumberFormatException e ) {
                    throw new ArgException( "Bad -elsize specification \""
                                          + elsizeTxt + "\"" );
                }
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
        return new ColMetaStep( colidList, rename, units, ucd, utype, xtype,
                                desc, shape, elsize );
    }

    /**
     * ProcessingStep implementation for altering column metadata.
     */
    private static class ColMetaStep implements ProcessingStep {

        final String colidList_;
        final String name_;
        final String units_;
        final String ucd_;
        final String utype_;
        final String xtype_;
        final String desc_;
        final int[] shape_;
        final int elsize_;

        /**
         * Constructor.
         *
         * @param   colidList  list of column ids to affect
         * @param   name    new column name
         * @param   units   new column units
         * @param   ucd     new column ucd
         * @param   utype   new column utype
         * @param   xtype   new column xtype
         * @param   desc    new column description
         * @param   shape   new shape array
         * @param   elsize  new element size
         */
        public ColMetaStep( String colidList, String name, String units,
                            String ucd, String utype, String xtype,
                            String desc, int[] shape, int elsize ) {
            colidList_ = colidList;
            name_ = name;
            units_ = units;
            ucd_ = ucd;
            utype_ = utype;
            xtype_ = xtype;
            desc_ = desc;
            shape_ = shape;
            elsize_ = elsize;
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
                    if ( utype_ != null ) {
                        info.setUtype( utype_ );
                    }
                    if ( xtype_ != null ) {
                        info.setXtype( xtype_ );
                    }
                    if ( desc_ != null ) {
                        info.setDescription( desc_ );
                    }
                    if ( shape_ != null ) {
                        info.setShape( shape_ );
                    }
                    if ( elsize_ >= 0 ) {
                        info.setElementSize( elsize_ );
                    }
                }
                colInfos[ icol ] = info;
            }
            StarTable out = new WrapperStarTable( base ) {
                public ColumnInfo getColumnInfo( int icol ) {
                    return colInfos[ icol ];
                }
            };
            for ( int icol = 0; icol < ncol; icol++ ) {
                if ( colFlags[ icol ] ) {
                    AddColumnFilter.checkDuplicatedName( out, icol );
                }
            }
            return out;
        }
    }
}
