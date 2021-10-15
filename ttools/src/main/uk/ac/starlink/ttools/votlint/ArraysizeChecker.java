package uk.ac.starlink.ttools.votlint;

/**
 * Checks on arraysize attribute.
 * If arraysize="1" it issues a warning; this practice has been deprecated
 * since VOTable-1.3 Erratum #3.
 *
 * @author   Mark Taylor
 * @since    10 Jun 2019
 */
public class ArraysizeChecker implements AttributeChecker {

    public void check( String value, ElementHandler handler ) {
        if ( value != null && "1".equals( value.trim() ) ) {
            VotLintContext context = handler.getContext();
            context.warning( new VotLintCode( "AR1" ),
                             "arraysize=\"1\" deprecated"
                           + " since VOTable 1.3 Erratum #3" );
        }
    }
}
