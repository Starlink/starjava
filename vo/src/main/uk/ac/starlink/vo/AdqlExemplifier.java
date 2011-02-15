package uk.ac.starlink.vo;

/**
 * Can generate example ADQL queries.
 *
 * @author   Mark Taylor
 * @since    15 Feb 2011
 */
public class AdqlExemplifier {

    private final String indent_;
    private final String newline_;

    /**
     * Constructor.
     *
     * @param   lineBreaks  whether output ADQL should include multiline
     *                      formatting
     */
    public AdqlExemplifier( boolean lineBreaks ) {
        if ( lineBreaks ) {
            indent_ = "   ";
            newline_ = "\n";
        }
        else {
            indent_ = "";
            newline_ = "";
        }
    }

    /**
     * Simple single-table example query.
     *
     * @param  table  table to query
     */
    public String createSimpleExample( TableMeta table ) {
        ColumnMeta[] cols = table.getColumns();
        final String colSelection;
        if ( cols != null && cols.length > 0 ) {
            StringBuffer sbuf = new StringBuffer();
            for ( int i = 0; i < Math.min( cols.length, 3 ); i++ ) {
                if ( i > 0 ) {
                    sbuf.append( ", " );
                }
                sbuf.append( cols[ i ].getName() );
            }
            colSelection = sbuf.toString();
        }
        else {
            colSelection = "*";
        }
        return new StringBuffer()
            .append( "SELECT" )
            .append( breakLevel( 1 ) )
            .append( "TOP 1000" )
            .append( breakLevel( 1 ) )
            .append( colSelection )
            .append( breakLevel( 1 ) )
            .append( "FROM" )
            .append( ' ' )
            .append( table.getName() )
            .toString();
    }

    /**
     * Returns a string which can be used to separate parts of an output
     * example string.
     *
     * @param   level  notional indentation level
     * @return   whitespace string
     */
    private String breakLevel( int level ) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append( newline_ );
        for ( int i = 0; i < level; i++ ) {
            sbuf.append( indent_ );
        }
        return sbuf.toString();
    }
}
