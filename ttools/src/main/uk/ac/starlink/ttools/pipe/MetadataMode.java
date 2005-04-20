package uk.ac.starlink.ttools.pipe;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;

/**
 * Processing mode which summarises table metadata.
 * 
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public class MetadataMode extends ProcessingMode {

    private PrintStream out_ = getOutputStream();

    public String getName() {
        return "meta";
    }

    /**
     * Writes metadata details to output stream.
     */
    public void process( StarTable table ) throws IOException {
        long nrow = table.getRowCount();
        int ncol = table.getColumnCount();
        String name = table.getName();
        out_.println();
        if ( name != null && name.trim().length() > 0 ) {
            out_.println( "Name:    " + name.trim() );
        }
        out_.println( "Columns: " + new Integer( ncol ) );
        out_.println( "Rows:    " +
                      ( nrow >= 0 ? Long.toString( nrow ) : "?" ) );

        List params = table.getParameters();
        if ( params.size() > 0 ) {
            out_.println();
            out_.println( "Parameters" );
            out_.println( "----------" );
            for ( Iterator it = table.getParameters().iterator();
                  it.hasNext(); ) {
                DescribedValue param = (DescribedValue) it.next();
                outMeta( param.getInfo().getName(), param.getValue() );
            }
        }

        out_.println();
        out_.println( "Columns" );
        out_.println( "-------" );
        ColumnInfo[] cinfos = Tables.getColumnInfos( table );
        for ( int icol = 0; icol < ncol; icol++ ) {
            String scol = Integer.toString( icol + 1 );
            for ( int i = scol.length(); i < 6; i++ ) {
                out_.print( ' ' );
            }
            out_.print( scol + ": " );
            ColumnInfo cinfo = cinfos[ icol ];
            out_.println( cinfo );
        }
    }

    /**
     * Outputs an item of metadata in a (somewhat) formatted way.
     *
     * @param  key  metadatum name
     * @param  value  metadatum value
     */
    private void outMeta( String key, Object val ) {
        String pad = "    ";
        if ( val != null ) {
            String value = val.toString().trim();
            if ( value.length() > 0 ) {
                if ( false && key.length() + value.length() < 78 ) {
                out_.println( key + ": " + value );
                }
                else {
                    out_.println( key + ":" );
                    value = pad + value.replaceAll( "\n", pad + "\n" );
                    out_.println( value );
                }
            }
        }
    }

}
