package uk.ac.starlink.hds;

import java.io.IOException;
import uk.ac.starlink.ast.Channel;

/**
 * An AST Channel for reading an one-dimensional _CHAR HDSObject that
 * contains an AST object.  The details of the format (continuation
 * lines and so on) are taken from the behaviour of the NDF library.
 *
 * @author   Mark Taylor (Starlink)
 */
class ARYReadChannel extends Channel {

    private long[] pos;
    private final int nel;
    private final HDSObject hobj;

    /**
     * Creates a Channel from a one-dimensional character HDSObject.
     *
     * @param  hobj  a 1-d _CHAR HDS object
     */
    ARYReadChannel( HDSObject hobj ) throws HDSException {
        if ( hobj.datType().startsWith( "_CHAR" ) &&
             hobj.datShape().length == 1 ) {
            this.hobj = hobj;
            pos = new long[] { 1L };
            nel = (int) hobj.datShape()[ 0 ];
        }
        else {
            throw new IllegalArgumentException( 
                "Supplied HDS object is not 1-d _CHAR array" );
        }
    }

    protected String source() throws IOException {
        String line;
        try {
            line = hobj.datCell( pos ).datGet0c();
            pos[ 0 ]++;

            String next;
            while ( pos[ 0 ] <= nel ) {
                next = hobj.datCell( pos ).datGet0c();
                if ( next.length() > 0 && next.charAt( 0 ) == '+' ) {
                    line += next.substring( 1 );
                    pos[ 0 ]++;
                }
                else {
                    break;
                }
            }
        }
        catch ( HDSException e ) {
            throw new IOException( e.getMessage() );
        }
        return line;
    }

    protected void sink( String line ) {
        throw new UnsupportedOperationException( "sink() not provided" );
    }

}
