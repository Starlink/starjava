package uk.ac.starlink.vo;

import java.io.IOException;
import java.util.Map;
import uk.ac.starlink.util.TestCase;

public class VocabularyTest extends TestCase {

    public void testExample() throws IOException {
        Vocabulary vocab =
            Vocabulary.readVocabulary( VocabularyTest.class
                                      .getResource( "example.desise" ) );
        assertEquals( "http://www.ivoa.net/rdf/example", vocab.getUri() );
        assertEquals( "RDF Class", vocab.getFlavour() );
        Map<String,VocabTerm> terms = vocab.getTerms();
        assertEquals( 6, terms.size() );
        assertEquals( "Equatorial", terms.get( "EQUATORIAL" ).getLabel() );
        assertEquals( "As defined by 1998AJ....116..516M.",
                      terms.get( "ICRS" ).getDescription() );
        assertFalse( terms.get( "ICRS" ).isDeprecated() );
        assertFalse( terms.get( "ICRS" ).isPreliminary() );
        assertNull( terms.get( "ICRS" ).getUseInstead() );
        assertTrue( terms.get( "B1875.0" ).isDeprecated() );
        assertTrue( terms.get( "ICRS2" ).isPreliminary() );
        assertEquals( 0, terms.get( "B1875.0" ).getNarrower().length );
        assertEquals( 0, terms.get( "B1875.0" ).getWider().length );
        assertArrayEquals( new String[] { "ICRS", "ICRS2" },
                           terms.get( "EQUATORIAL" ).getNarrower() );
        assertArrayEquals( new String[] { "EQUATORIAL" },
                           terms.get( "ICRS" ).getWider() );
        VocabTerm eqfk5 = terms.get( "eq_FK5" );
        assertTrue( terms.get( "eq_FK5" ).isDeprecated() );
        assertEquals( "ICRS", terms.get( "eq_FK5" ).getUseInstead() );
    }
}
