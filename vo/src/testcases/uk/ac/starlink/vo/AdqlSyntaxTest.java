package uk.ac.starlink.vo;

import junit.framework.TestCase;

public class AdqlSyntaxTest extends TestCase {

    public void testSyntax() {
        AdqlSyntax syntax = AdqlSyntax.getInstance();
        for ( String word : syntax.getReservedWords() ) {
            assertTrue( syntax.isReserved( word ) );
            assertTrue( syntax.isReserved( word.toUpperCase() ) );
            assertTrue( syntax.isReserved( word.toLowerCase() ) );
        }
        assertEquals( "\"abc\"", syntax.quote( "abc" ) );
        assertEquals( "abc", syntax.quoteIfNecessary( "abc" ) );
        assertEquals( "\"SELECT\"", syntax.quoteIfNecessary( "SELECT" ) );
        assertEquals( "DEC", syntax.quoteIfNecessary( "DEC" ) );
        assertEquals( "\"a b c\"", syntax.quoteIfNecessary( "a b c" ) );
        assertEquals( "\"xx\"\"yy\"", syntax.quote( "xx\"yy" ) );
        assertEquals( "\"xx\"\"yy\"", syntax.quoteIfNecessary( "xx\"yy" ) );
        assertTrue( syntax.isReserved( "select" ) );
        assertTrue( syntax.isReserved( "SELECT" ) );
        assertTrue( ! syntax.isReserved( "sandana" ) );
        assertTrue( syntax.isIdentifier( "SELECT" ) );
        assertTrue( syntax.isIdentifier( "ABC" ) );
        assertTrue( ! syntax.isIdentifier( "_ABC" ) );
    }
}
