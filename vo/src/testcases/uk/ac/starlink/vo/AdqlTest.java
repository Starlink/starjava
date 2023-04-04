package uk.ac.starlink.vo;

import junit.framework.TestCase;
import adql.parser.feature.LanguageFeature;

public class AdqlTest extends TestCase {

    public void testAdqlGeoIds() {
        assertEquals( LanguageFeature.TYPE_ADQL_GEO,
                      AdqlValidator.ADQLGEO_FEATURE_TYPE_VOLLT );
        assertEquals( AdqlValidator.ADQLGEO_FEATURE_TYPE_VOLLT,
                      TapCapability.ADQLGEO_FEATURE_TYPE );
        assertFalse( AdqlValidator.ADQLGEO_FEATURE_TYPE_VOLLT
                    .equals( AdqlValidator.ADQLGEO_FEATURE_TYPE_VARIANT ) );
    }

    public void testDefaultAdqls() {
        assertTrue( TapCapabilityPanel.testAdqls() );
    }

    public void testVersions() {
        assertTrue( AdqlSyntax.INSTANCE20.isReserved( "SELECT" ) );
        assertTrue( AdqlSyntax.INSTANCE21.isReserved( "SELECT" ) );
        assertFalse( AdqlSyntax.INSTANCE20.isReserved( "ILIKE" ) );
        assertTrue( AdqlSyntax.INSTANCE21.isReserved( "ILIKE" ) );
        assertTrue( AdqlSyntax.getInstance().isReserved( "ILIKE" ) );
    }
}
