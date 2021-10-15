package uk.ac.starlink.ttools.votlint;

/**
 * Checks an attribute whose content is defined by the content of
 * an IVOA Vocabulary.
 *
 * @author   Mark Taylor
 * @since    25 Jun 2021
 */
public class VocabAttributeChecker implements AttributeChecker {

    private final VocabChecker vocabChecker_;

    public VocabAttributeChecker( VocabChecker vocabChecker ) {
        vocabChecker_ = vocabChecker;
    }

    public void check( String nameValue, ElementHandler handler ) {
        final VotLintContext context = handler.getContext();
        vocabChecker_.checkTerm( nameValue, new VocabChecker.TermReporter() {
            public void termFound() {
            }
            public void termUnknown( String msg ) {
                context.warning( new VotLintCode( "VCU" ), msg );
            }
            public void termDeprecated( String msg ) {
                context.warning( new VotLintCode( "VCD" ), msg );
            }
            public void termPreliminary( String msg ) {
                context.info( new VotLintCode( "VCP" ), msg );
            }
        } );
    }
}
