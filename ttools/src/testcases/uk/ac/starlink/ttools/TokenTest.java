package uk.ac.starlink.ttools;

import uk.ac.starlink.task.UsageException;
import uk.ac.starlink.util.TestCase;

public class TokenTest extends TestCase {

    public TokenTest( String name ) {
        super( name );
    }

    public void testWords() throws UsageException {
        assertArrayEquals( new String[] { "x" }, words( "x" ) );
        assertArrayEquals( new String[] { "abc", "def", "ghi" },
                           words( "  abc  def \t   ghi\n" ) );
        assertArrayEquals( new String[] { "1", "a b c", "2" },
                           words( " 1 'a b c' 2 " ) );
        assertArrayEquals( new String[] { "1", " a b  c ", "2" },
                           words( " 1 \" a b  c \" '2' " ) );
        assertArrayEquals( new String[] { "first=1", "second=22", "3rd= 333 " },
                           words( " first=1  second=\"22\" 3rd=' 333 ' " ) );

        assertArrayEquals( new String[] { "run", "to", "the", },
                           words( " run to the # hills " ) );
        assertArrayEquals( new String[] { "run", "to", "the", " # hills " },
                           words( " run to the ' # hills ' " ) );
        try {
            words( "incomplete ' a b c" );
            fail();
        }
        catch ( UsageException e ) {
        }
    }

    public void testLines() throws UsageException {
        assertArrayEquals( new String[] { "head 3", " tail 1" },
                           lines( "head 3; tail 1" ) );
        assertArrayEquals( new String[] { "head 1", " tail '2;'", "nose 4 " },
                           lines( "head 1; tail '2;'\nnose 4 " ) );
    }

    private static String[] words( String line ) throws UsageException {
        return Tokenizer.tokenizeWords( line );
    }

    private static String[] lines( String text ) throws UsageException {
        return Tokenizer.tokenizeLines( text );
    }
}
