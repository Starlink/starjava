package uk.ac.starlink.ttools.join;

import junit.framework.TestCase;
import uk.ac.starlink.table.join.HumanMatchEngine;
import uk.ac.starlink.table.join.MatchEngine;

public class MatchEngineTest extends TestCase {

    public void testEngines() throws Exception {
        MatchEngineParameter matcherParam = new MatchEngineParameter( "m");
        for ( String matcherTxt : MatchEngineParameter.getExampleValues() ) {
            MatchEngine matcher = matcherParam.createEngine( matcherTxt );
            checkEngine( matcher );
        }
    }

    private void checkEngine( MatchEngine engine ) {
        assertTrue( new HumanMatchEngine( engine ).isIdentity() );
    }
}
