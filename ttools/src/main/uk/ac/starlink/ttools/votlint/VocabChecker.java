package uk.ac.starlink.ttools.votlint;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.xml.sax.SAXException;
import uk.ac.starlink.vo.Vocabulary;

/**
 * Checks an attribute that is defined by the content of an IVOA Vocabulary.
 *
 * @author   Mark Taylor
 * @since    25 Apr 2019
 */
public class VocabChecker implements AttributeChecker {

    private final URL vocabUrl_;
    private final Collection<String> fixedValues_;
    private Collection<String> retrievedValues_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.votlint" );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/timescale. */
    public static final VocabChecker TIMESCALE =
        new VocabChecker( "http://www.ivoa.net/rdf/timescale",
                          new String[] {
                              "TAI", "TT", "UT", "UTC", "GPS",
                              "TCG", "TCB", "TDB", "UNKNOWN", 
                          } );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/refposition. */
    public static final VocabChecker REFPOSITION =
        new VocabChecker( "http://www.ivoa.net/rdf/refposition",
                          new String[] {
                              "TOPOCENTER", "GEOCENTER", "BARYCENTER",
                              "HELIOCENTER", "EMBARYCENTER", "UNKNOWN",
                          } );

    /**
     * Constructor.
     *
     * @param   vocabUrl  URI/URL for vocabulary document
     * @param   values    hard-coded values known in vocabulary;
     *                    other values may be available by resolving
     *                    the vocabulary URL
     */
    private VocabChecker( String vocabUrl, String[] values ) {
        try {
            vocabUrl_ = new URL( vocabUrl );
        }
        catch ( MalformedURLException e ) {
            throw new IllegalArgumentException( "Not a URL: " + vocabUrl );
        }
        fixedValues_ = new LinkedHashSet<String>( Arrays.asList( values ) );
    }

    public void check( String nameValue, ElementHandler handler ) {
        VotLintContext context = handler.getContext();

        /* Note that short-circuit operator semantics means that terms
         * are only read from the online vocabulary document if the
         * test value cannot be found in the hard-coded list. */
        if ( ! fixedValues_.contains( nameValue ) &&
             ! getRetrievedValues().contains( nameValue ) ) {
            StringBuffer sbuf = new StringBuffer()
                .append( "\"" )
                .append( nameValue )
                .append( "\"" )
                .append( " not known in vocabulary " )
                .append( vocabUrl_ )
                .append( " (known:" );
            Set<String> values = new TreeSet<String>();
            values.addAll( fixedValues_ );
            values.addAll( getRetrievedValues() );
            for ( Iterator<String> it = values.iterator(); it.hasNext(); ) {
                sbuf.append( " " )
                    .append( it.next() );
                if ( it.hasNext() ) {
                    sbuf.append( "," );
                }
            }
            sbuf.append( ")" );
            context.warning( sbuf.toString() );
        }
    }

    /**
     * Lazily acquires vocabulary values by reading the resource at the
     * vocabulary URI.
     *
     * @return   values retrieved from online vocabulary;
     *           in case of a read error this may be empty, but not null
     */
    public Collection<String> getRetrievedValues() {
        if ( retrievedValues_ == null ) {
            String[] rvals;
            try {
                rvals = Vocabulary.readVocabulary( vocabUrl_ ).getTermNames();
                int nRead = rvals.length;
                if ( nRead > 0 ) {
                    Collection<String> set =
                        new HashSet<String>( Arrays.asList( rvals ) );
                    set.removeAll( fixedValues_ );
                    int nNew = set.size();
                    String msg = new StringBuffer()
                        .append( "Read vocabulary from " )
                        .append( vocabUrl_ )
                        .append( ": " )
                        .append( nRead )
                        .append( " terms, " )
                        .append( nNew )
                        .append( " unknown" )
                        .toString();
                    logger_.info( msg );
                }
                else {
                    logger_.warning( "No terms read from vocabulary at "
                                   + vocabUrl_ );
                }
            }
            catch ( IOException e ) {
                rvals = new String[ 0 ];
                logger_.log( Level.WARNING,
                             "Unable to read vocabulary from " + vocabUrl_, e );
            }
            catch ( SAXException e ) {
                rvals = new String[ 0 ];
                logger_.log( Level.WARNING,
                             "Unable to parse vocabulary from " + vocabUrl_, e);
            }
            retrievedValues_ =
                Collections
               .unmodifiableSet( new LinkedHashSet<String>( Arrays
                                                           .asList( rvals ) ) );
        }
        return retrievedValues_;
    }
}
