package uk.ac.starlink.datanode.nodes;

import java.io.*;
import uk.ac.starlink.ast.*;
import uk.ac.starlink.hds.*;

class WCSChannel extends Channel {
    private long[] pos;
    private int nel;
    private HDSObject hobj;

    /**
     * Creates a Channel from a one-dimensional character HDSObject.
     *
     * @param  hobj  a 1-d _CHAR HDS object
     * @throws NoSuchDataException  if hobj is not 1-d or not _CHAR
     */
    WCSChannel( HDSObject hobj ) throws NoSuchDataException {
        try {
            if ( hobj.datType().startsWith( "_CHAR" ) &&
                 hobj.datShape().length == 1 ) {
                this.hobj = hobj;
                pos = new long[] { 1L }; 
                nel = (int) hobj.datShape()[ 0 ];
            }
            else {
                throw new NoSuchDataException( "Not a character array" );
            }
        }
        catch ( HDSException e ) {
            throw new NoSuchDataException( e.getMessage() );
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

    /**
     * Not implemented.
     *
     * @throws  UnsupportedOperationException
     */
    protected void sink( String line ) {
        throw new UnsupportedOperationException( "sink() not provided" );
    }
}
