package uk.ac.starlink.ttools.mode;

import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import uk.ac.starlink.table.ColumnInfo;
import uk.ac.starlink.table.DescribedValue;
import uk.ac.starlink.table.StarTable;
import uk.ac.starlink.table.Tables;
import uk.ac.starlink.task.Environment;
import uk.ac.starlink.task.Parameter;
import uk.ac.starlink.ttools.DocUtils;
import uk.ac.starlink.ttools.TableConsumer;
import uk.ac.starlink.ttools.filter.MetadataFilter;

/**
 * Processing mode which summarises table metadata.
 *
 * @author   Mark Taylor (Starlink)
 * @since    11 Feb 2005
 */
public class MetadataMode implements ProcessingMode {

    public Parameter[] getAssociatedParameters() {
        return new Parameter[ 0 ];
    }

    public String getDescription() {
        return DocUtils.join( new String[] {
           "<p>Prints the table metadata to standard output.",
           "The name and type etc of each column is tabulated,",
           "and table parameters are also shown.",
           "</p>",
           "<p>See the " + DocUtils.filterRef( new MetadataFilter() ),
           "filter for more flexible output of table metadata.",
           "</p>",
        } );
    }

    public TableConsumer createConsumer( Environment env ) {
        final PrintStream out = env.getOutputStream();
        return new TableConsumer() {
            public void consume( StarTable table ) {
                reportMetadata( out, table );
            }
        };
    }

    /**
     * Gathers metadata from a table and writes it to a given output stream.
     *
     * @param   out  output stream
     * @param   table  table
     */
    private void reportMetadata( PrintStream out, StarTable table ) {
        long nrow = table.getRowCount();
        int ncol = table.getColumnCount();
        String name = table.getName();
        out.println();
        if ( name != null && name.trim().length() > 0 ) {
            out.println( "Name:    " + name.trim() );
        }
        out.println( "Columns: " + new Integer( ncol ) );
        out.println( "Rows:    " +
                     ( nrow >= 0 ? Long.toString( nrow ) : "?" ) );

        List params = table.getParameters();
        if ( params.size() > 0 ) {
            out.println();
            out.println( "Parameters" );
            out.println( "----------" );
            for ( Iterator it = table.getParameters().iterator();
                  it.hasNext(); ) {
                DescribedValue param = (DescribedValue) it.next();
                outMeta( out, param.getInfo().getName(), param.getValue() );
            }
        }

        out.println();
        out.println( "Columns" );
        out.println( "-------" );
        ColumnInfo[] cinfos = Tables.getColumnInfos( table );
        for ( int icol = 0; icol < ncol; icol++ ) {
            String scol = Integer.toString( icol + 1 );
            for ( int i = scol.length(); i < 6; i++ ) {
                out.print( ' ' );
            }
            out.print( scol + ": " );
            ColumnInfo cinfo = cinfos[ icol ];
            String text = cinfo.toString();
            String desc = cinfo.getDescription();
            if ( desc != null ) {
               text = text + " - " + desc;
            }
            out.println( text );
        }
    }

    /**
     * Outputs an item of metadata in a (somewhat) formatted way.
     *
     * @param  out  output stream
     * @param  key  metadatum name
     * @param  value  metadatum value
     */
    private void outMeta( PrintStream out, String key, Object val ) {
        String pad = "    ";
        if ( val != null ) {
            String value = val.toString().trim();
            if ( value.length() > 0 ) {
                out.println( key + ":" );
                value = pad + value.replaceAll( "\n", "\n" + pad );
                out.println( value );
            }
        }
    }
}
