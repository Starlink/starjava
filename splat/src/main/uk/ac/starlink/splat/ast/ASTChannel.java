// Copyright (C) 2002 Central Laboratory of the Research Councils

// History:
//    30-MAY-2002 (Peter W. Draper):
//       Original version.

package uk.ac.starlink.splat.ast;

import java.io.*;
import uk.ac.starlink.ast.*;

/**
 * AST Channel for reading and writing native encodings using
 * String arrays.
 *
 * @author Peter W. Draper
 * @version $Id$
 */
public class ASTChannel
    extends Channel
{
    /**
     * The String array.
     */
    private String[] astArray = null;

    /**
     * The current index. This increments for each call to source.
     */
    private int position = 0;

    /**
     * Creates a Channel from a String array
     *
     * @param astArray array holding an AST native encoding. Note that
     *                 a reference to this is kept so any
     *                 modifications will be seen.
     */
    public ASTChannel( String[] astArray )
    {
        setArray( astArray );
    }

    /**
     * Read the next line from the array. If past end return null.
     */
    protected String source()
        throws IOException
    {
        if ( position >= astArray.length ) {
            return null;
        }
        return astArray[position++];
    }

    /**
     * Sink function. Note that a String is added to the input array
     * at the current position. If this exceeds the initial storage
     * then the line is quietly thrown away, however the array index
     * is still incremented, so a later query can determine how much
     * storage was required.
     */
    protected void sink( String line )
    {
        if ( position < astArray.length  ) {
            astArray[position] = line;
        }
        position++;
    }

    /**
     * Set the array position index.
     */
    public void setIndex( int position )
    {
        this.position = position;
    }

    /**
     * Get the array position index.
     */
    public int getIndex()
    {
        return position;
    }

    /**
     * Get the size of the backing String array.
     */
    public int getSize()
    {
        return astArray.length;
    }

    /**
     * Replace the String array.
     */
    public void setArray( String[] astArray )
    {
        this.astArray = astArray;
        position = 0;
    }


    // Utility function for writing an AstObject to System.out.
    public static void astWrite( AstObject object )
    {
        final Channel chan = new Channel() {
                public void sink( String line ) {
                    System.out.println( line );
                }
            };
        try {
            chan.write( object );
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
