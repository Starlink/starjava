package uk.ac.starlink.table.formats;

import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.table.ValueInfo;

/**
 * A StarTableWriter which writes to the IPAC text format.
 * The data format is defined at
 * <a href="http://irsa.ipac.caltech.edu/applications/DDGEN/Doc/ipac_tbl.html"
 *    >http://irsa.ipac.caltech.edu/applications/DDGEN/Doc/ipac_tbl.html</a>.
 *
 * @author   Mark Taylor
 * @since    20 Sep 2012
 */
public class IpacTableWriter extends AbstractTextTableWriter {

    /** String representation for null values. */
    public static String NULL = "null";

    /**
     * Constructor.
     */
    @SuppressWarnings("this-escape")
    public IpacTableWriter() {
        super( new String[] { "tbl", "ipac" }, true );
        setMaxWidth( 1000 );
        // The comments parameter may be many lines long
        setMaximumParameterLength( 100_000 );
        setEncoding( StandardCharsets.US_ASCII );
    }

    /**
     * Returns "IPAC".
     */
    @Override
    public String getFormatName() {
        return "IPAC";
    }

    public boolean docIncludesExample() {
        return true;
    }

    public String getXmlDescription() {
        return readText( "IpacTableWriter.xml" );
    }

    /**
     * Returns "text/plain".
     */
    @Override
    public String getMimeType() {
        return "text/plain";
    }

    @Override
    public int getMinNameWidth( ColumnInfo info ) {
        return new IpacHead( info ).name_.length();
    }

    public String formatValue( Object val, ValueInfo info, int width ) {
        return Tables.isBlank( val )
             ? NULL
             : info.formatValue( val, width );
    }

    public String formatColumnHeads( int[] colwidths, ColumnInfo[] cinfos ) {
        int ncol = cinfos.length;
        IpacHead[] heads = new IpacHead[ ncol ];
        String[] names = new String[ ncol ];
        String[] types = new String[ ncol ];
        String[] units = new String[ ncol ];
        String[] nulls = new String[ ncol ];
        for ( int ic = 0; ic < ncol; ic++ ) {
            IpacHead head = new IpacHead( cinfos[ ic ] );
            names[ ic ] = head.name_;
            types[ ic ] = head.type_;
            units[ ic ] = head.unit_;
            nulls[ ic ] = NULL;
        }
        return new StringBuilder()
              .append( formatIpacLine( colwidths, names, '|' ) )
              .append( formatIpacLine( colwidths, types, '|' ) )
              .append( formatIpacLine( colwidths, units, '|' ) )
              .append( formatIpacLine( colwidths, nulls, '|' ) )
              .toString();
    }

    protected String formatLine( int[] colwidths, String[] data ) {
        return formatIpacLine( colwidths, data, ' ' );
    }

    protected String formatSeparator( int[] colwidths ) {
        return "";
    }

    /**
     * Formats a header or data line.
     *
     * @param  colwidths  array of column content width values
     * @param  data   array of column values
     * @param  sepChar  character separating fields
     * @return   row formatted for inclusion in an IPAC table,
     *           including line-end character
     */
    private String formatIpacLine( int[] colwidths, String[] data,
                                   char sepChar ) {
        StringBuilder sbuf = new StringBuilder();
        for ( int ic = 0; ic < data.length; ic++ ) {
            sbuf.append( sepChar );
            sbuf.append( ' ' );
            String datum = ( data[ ic ] == null ) ? "" : data[ ic ];
            if ( datum.length() > colwidths[ ic ] ) {
                datum = datum.substring( 0, colwidths[ ic ] );
            }
            int padding = colwidths[ ic ] - datum.length();
            sbuf.append( datum );
            if ( padding > 0 ) {
                for ( int j = 0; j < padding; j++ ) {
                    sbuf.append( ' ' );
                }
            }
            sbuf.append( ' ' );
        }
        sbuf.append( sepChar );
        sbuf.append( '\n' );
        return sbuf.toString();
    }

    protected String formatParam( String name, String value, Class<?> clazz ) {
        String[] lines = value.split( "[\\n\\r]+" );
        int maxl = 320;
        if ( IpacTableBuilder.COMMENT_INFO.getName().equals( name ) ) {
            StringBuffer sbuf = new StringBuffer();
            for ( int il = 0; il < lines.length; il++ ) {
                sbuf.append( "\\ " )
                    .append( truncateLine( lines[ il ], maxl ) )
                    .append( '\n' );
            }
            return sbuf.toString();
        }
        else {
            return new StringBuffer()
               .append( '\\' )
               .append( name.trim() )
               .append( " = " )
               .append( clazz.equals( String.class )
                            ? quoteString( truncateLine( lines[ 0 ], maxl ) )
                            : value )
               .append( '\n' )
               .toString();
        }
    }

    /**
     * Truncate a string to avoid it being very long.
     *
     * @param   txt   string
     * @param   maxLeng  maximum length
     * @return  txt or a version of it truncated to maxLeng
     */
    private String truncateLine( String txt, int maxLeng ) {
        return txt.length() > maxLeng ? txt.substring( 0, maxLeng ) : txt;
    }

    /**
     * Quotes a string value using single or double quotes as appropriate.
     * Internal double quote characters are replaced by single ones if
     * both types are present in the input string.
     *
     * @param  raw  unquoted string
     * @return  quoted string
     */
    private static String quoteString( String raw ) {
        boolean hasSingle = raw.indexOf( '\'' ) >= 0;
        boolean hasDouble = raw.indexOf( '"' ) >= 0;
        if ( ! hasDouble ) {
            return "\"" + raw + "\"";
        }
        else if ( ! hasSingle ) {
            return "'" + raw + "'";
        }
        else {  // has both
            return "\"" + raw.replaceAll( "\"", "'" ) + "\"";
        }
    }

    /**
     * Encapsulates IPAC header information for a column.
     */
    private static class IpacHead {
        final String name_;
        final String type_;
        final String unit_;

        /**
         * Constructor.
         *
         * @param  info  column metadata
         */
        IpacHead( ColumnInfo info ) {
            String name = info.getName().trim()
                                        .replaceAll( "[^a-zA-Z0-9_]+", "_" );
            if ( name.length() > 40 ) {
                name = name.substring( 0, 40 );
            }
            String unit = info.getUnitString();
            if ( unit == null ) {
                unit = "";
            }
            final String type;
            Class<?> clazz = info.getContentClass();
            if ( clazz.equals( Integer.class ) ||
                 clazz.equals( Short.class ) ||
                 clazz.equals( Byte.class ) ) {
                type = "int";
            }
            else if ( clazz.equals( Long.class ) ) {
                type = "long";
            }
            else if ( clazz.equals( Double.class ) ) {
                type = "double";
            }
            else if ( clazz.equals( Float.class ) ) {
                type = "float";
            }
            else if ( clazz.equals( String.class ) ||
                      clazz.equals( Character.class ) ) {
                type = "char";
            }
            else {
                type = "char";
            }
            int width = 0;
            width = Math.max( width, type.length() );
            width = Math.max( width, NULL.length() );
            width = Math.max( width, unit.length() );
            int npad = width - name.length();
            for ( int i = 0; i < npad; i++ ) {
                name += " ";
            }
            name_ = name;
            type_ = type;
            unit_ = unit;
        }
    }
}
