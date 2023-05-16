package uk.ac.starlink.ttools.votlint;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.ac.starlink.vo.VocabTerm;
import uk.ac.starlink.vo.Vocabulary;

/**
 * Checks values that are defined by the content of an IVOA Vocabulary.
 *
 * @author   Mark Taylor
 * @since    25 Apr 2019
 */
public class VocabChecker {

    private final URL vocabUrl_;
    private final String vocabUri_;
    private final Collection<String> fixedTerms_;
    private Map<String,VocabTerm> retrievedTerms_;
    private static final Logger logger_ =
        Logger.getLogger( "uk.ac.starlink.ttools.votlint" );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/timescale. */
    /* Term list is from https://www.ivoa.net/rdf/timescale/2019-03-15/. */
    public static final VocabChecker TIMESCALE =
        new VocabChecker( "http://www.ivoa.net/rdf/timescale",
                          new String[] {
                              "TAI", "TT", "UT", "UTC", "GPS",
                              "TCG", "TCB", "TDB", "UNKNOWN", 
                          } );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/refposition. */
    /* Term list is from https://www.ivoa.net/rdf/refposition/2019-03-15/. */
    public static final VocabChecker REFPOSITION =
        new VocabChecker( "http://www.ivoa.net/rdf/refposition",
                          new String[] {
                              "TOPOCENTER", "GEOCENTER", "BARYCENTER",
                              "HELIOCENTER", "EMBARYCENTER", "UNKNOWN",
                          } );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/datalink/core. */
    /* Term list is from https://www.ivoa.net/rdf/datalink/core/2022-01-27/. */
    public static final VocabChecker DATALINK_CORE =
        new VocabChecker( "http://www.ivoa.net/rdf/datalink/core",
                          new String[] {
                              "auxiliary", "bias", "calibration", "coderived",
                              "counterpart", "cutout", "dark", "derivation",
                              "detached-header", "documentation", "error",
                              "flat", "noise", "package", "preview",
                              "preview-image", "preview-plot", "proc",
                              "progenitor", "this", "thumbnail", "weight",
                          } );

    /** Instance for vocabulary at http://www.ivoa.net/rdf/product-type. */
    /* Term list is from https://www.ivoa.net/rdf/product-type/2021-11-18/;
     * at time of writing all terms are marked Preliminary. */
    public static final VocabChecker PRODUCT_TYPE =
        new VocabChecker( "http://www.ivoa.net/rdf/product-type",
                          new String[] {
                              "cube", "dynamic-spectrum", "event", "image",
                              "measurements", "sed", "spectrum", "timeseries",
                              "visibility",
                          } );

    /** Instance for vocabulary at http://www.ivoa.net/examples. */
    /* Term list is from https://www.ivoa.net/rdf/examples/2023-01-19/;
     * at time of writing all terms are marked Preliminary, but are scheduled
     * to be non-preliminary following TCG endorsement at Bologna interop
     * 07-05-2023. */
    public static final VocabChecker EXAMPLES =
        new VocabChecker( "http://www.ivoa.net/rdf/examples",
                          new String[] {
                              "capability", "continuation", "generic-parameter",
                              "key", "name", "query", "table", "value",
                          } ); 

    /** Static instances of this class. */
    private static final VocabChecker[] INSTANCES = {
        TIMESCALE, REFPOSITION, DATALINK_CORE, PRODUCT_TYPE, EXAMPLES,
    };

    /**
     * Constructor.
     *
     * @param   vocabUrl  URI/URL for vocabulary document
     * @param   fixedTerms    hard-coded non-preliminary, non-deprecated terms
     *                        known in the vocabulary;
     *                        other terms may be available by resolving
     *                        the vocabulary URL
     */
    public VocabChecker( String vocabUrl, String[] fixedTerms ) {
        try {
            vocabUrl_ = new URL( vocabUrl );
        }
        catch ( MalformedURLException e ) {
            throw new IllegalArgumentException( "Not a URL: " + vocabUrl );
        }
        vocabUri_ = vocabUrl;
        fixedTerms_ = Collections.unmodifiableSet(
                          new LinkedHashSet<String>(
                              Arrays.asList( fixedTerms ) ) );
    }

    /**
     * Checks whether a term is present in this vocabulary,
     * and reports to a callback interface. 
     *
     * @param  value  vocabulary name item to test
     * @param  termReporter  destination for reports;
     *                       exactly one of its methods will be invoked
     */
    public void checkTerm( String value, TermReporter termReporter ) {

        /* Note that the online vocabulary document is only consulted
         * if encountered vocabulary terms are not present in the
         * hard-coded list. */
        if ( fixedTerms_.contains( value ) ) {
            termReporter.termFound();
        }
        else {
            VocabTerm term = getRetrievedTerms().get( value );
            if ( term == null ) {
                StringBuffer sbuf = new StringBuffer()
                    .append( "\"" )
                    .append( value )
                    .append( "\"" )
                    .append( " not known in vocabulary " )
                    .append( vocabUrl_ )
                    .append( " (known:" );
                Set<String> terms = new TreeSet<String>();
                terms.addAll( fixedTerms_ );
                terms.addAll( getRetrievedTerms().keySet() );
                for ( Iterator<String> it = terms.iterator(); it.hasNext(); ) {
                    sbuf.append( " " )
                        .append( it.next() );
                    if ( it.hasNext() ) {
                        sbuf.append( "," );
                    }
                }
                sbuf.append( ")" );
                termReporter.termUnknown( sbuf.toString() );
            }
            else if ( term.isDeprecated() ) {
                String msg = new StringBuffer()
                   .append( "\"" )
                   .append( value )
                   .append( "\"" )
                   .append( " is marked *deprecated* in vocabulary " )
                   .append( vocabUrl_ )
                   .toString();
                termReporter.termDeprecated( msg );
            }
            else if ( term.isPreliminary() ) {
                String msg = new StringBuffer()
                   .append( "\"" )
                   .append( value )
                   .append( "\"" )
                   .append( " is marked *preliminary* in vocabulary " )
                   .append( vocabUrl_ )
                   .toString();
                termReporter.termPreliminary( msg );
            }
            else {
                termReporter.termFound();
            }
        }
    }

    /**
     * Returns the URI/URL of this object's vocabulary.
     *
     * @return  vocabulary URL
     */
    public URL getVocabularyUrl() {
        return vocabUrl_;
    }

    /**
     * Returns the URI of this object's vocabulary in string form.
     *
     * @return  vocabulary URL
     */
    public String getVocabularyUri() {
        return vocabUri_;
    }

    /**
     * Returns the hard-coded list of terms known by this checker.
     * It may not be complete if this class is out of date with respect to
     * the vocabulary itself.
     *
     * @return   unmodifiable list of known terms
     */
    public Collection<String> getFixedTerms() {
        return fixedTerms_;
    }

    /**
     * Lazily acquires vocabulary values by reading the resource at the
     * vocabulary URI.
     *
     * @return   term map retrieved from online vocabulary;
     *           in case of a read error this may be empty, but not null
     */
    public Map<String,VocabTerm> getRetrievedTerms() {
        if ( retrievedTerms_ == null ) {
            Map<String,VocabTerm> terms;
            try {
                terms = Vocabulary.readVocabulary( vocabUrl_ ).getTerms();
                int nRead = terms.size();
                if ( nRead > 0 ) {
                    terms = new LinkedHashMap<>( terms );
                    terms.keySet().removeAll( fixedTerms_ );
                    int nNew = terms.size();
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
                terms = Collections.emptyMap();
                logger_.log( Level.WARNING,
                             "Unable to read vocabulary from " + vocabUrl_, e );
            }
            retrievedTerms_ = Collections.unmodifiableMap( terms );
        }
        return retrievedTerms_;
    }

    @Override
    public String toString() {
        return getVocabularyUri();
    }

    /**
     * Run to check hard-coded term lists against online versions.
     * This can be done periodically, and the hard-coded lists updated
     * accordingly.
     */
    public static void main( String[] args ) throws IOException {
        for ( VocabChecker checker : INSTANCES ) {
            System.out.println( checkFixedTerms( checker ) );
            System.out.println();
        }
    }

    /**
     * Checks the hard-coded terms known for a given checker,
     * and returns a report summarising the result.
     *
     * @param  checker  checker to test
     * @return   multi-line string reporting status
     */
    private static String checkFixedTerms( VocabChecker checker )
            throws IOException {
        Set<String> fixed = new TreeSet<String>( checker.getFixedTerms() );
        Set<String> retrieved =
            new TreeSet<String>( Vocabulary
                                .readVocabulary( checker.getVocabularyUrl() )
                                .getTerms().keySet() );
        StringBuffer sbuf = new StringBuffer()
            .append( checker )
            .append( ":" )
            .append( "\n            fixed: " )
            .append( fixed )
            .append( "\n        retrieved: " )
            .append( retrieved );
        if ( retrieved.equals( fixed ) ) {
            sbuf.append( "\n    Up to date." );
        }
        else {
            Set<String> fixedOnly = new TreeSet<>( fixed );
            Set<String> retrievedOnly = new TreeSet<>( retrieved );
            fixedOnly.removeAll( retrieved );
            retrievedOnly.removeAll( fixed );
            sbuf.append( "\n       fixed only: " )
                .append( fixedOnly )
                .append( "\n   retrieved only: " )
                .append( retrievedOnly );
        }
        return sbuf.toString();
    }

    /**
     * Callback interface for reporting vocabulary interrogation results.
     */
    public interface TermReporter {

        /**
         * Invoked if the presented term was found as a normal entry
         * in the vocabulary.
         */
        void termFound();

        /**
         * Invoked if no such term was found in the vocabulary.
         *
         * @param  msg  user-directed message giving details
         */
        void termUnknown( String msg );

        /**
         * Invoked if the presented term was found in the vocabulary
         * but flagged as "deprecated".
         *
         * @param  msg  user-directed message giving details
         */
        void termDeprecated( String msg );

        /**
         * Invoked if the presented term was found in the vocabulary
         * but flagged as "preliminary".
         *
         * @param  msg  user-directed message giving details
         */
        void termPreliminary( String msg );
    }
}
