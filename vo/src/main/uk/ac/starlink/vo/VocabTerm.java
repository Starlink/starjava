package uk.ac.starlink.vo;

/**
 * Represents a term in an IVOA Vocabulary and its associated properties.
 *
 * <p>This class is written with reference to Version 2.0 of the document
 * <em>Vocabularies in the VO</em>, and particularly the Desise
 * serialization described there.  Note that document is in
 * Working Draft status at time of writing.
 *
 * @see  <a href="http://www.ivoa.net/documents/Vocabularies/"
 *          >Vocabularies in the VO, Section 3</a>
 */
public interface VocabTerm {

    /**
     * Returns the term token itself.
     * This is the unqualified term name with no namespace or "#".
     *
     * @return  term
     */
    String getTerm();

    /**
     * Returns a human-readable string for display purposes.
     *
     * @return  label
     */
    String getLabel();

    /**
     * Returns a human-readable description of the underlying concept.
     *
     * @return  description
     */
    String getDescription();

    /**
     * Indicates whether this term is preliminary and hence may disappear
     * without warning from the vocabulary.
     *
     * @return  preliminary flag
     */
    boolean isPreliminary();

    /**
     * Indicates whether this term is deprecated, and hence should be avoided.
     *
     * @return  deprecation flag
     * @see   #getUseInstead
     */
    boolean isDeprecated();

    /**
     * Returns a list of terms related to this one by semantic widening. 
     *
     * @return  array of wider term strings
     */
    String[] getWider();

    /**
     * Returns a list of terms related to this one by semantic narrowing.
     *
     * @return  array of narrower term strings
     */
    String[] getNarrower();

    /**
     * Returns the term that should be used instead of this one.
     * This should only return a non-null value for deprecated terms.
     *
     * @return  term to use instead of this deprecated term, or null
     * @see   #isDeprecated
     */
    String getUseInstead();
}
