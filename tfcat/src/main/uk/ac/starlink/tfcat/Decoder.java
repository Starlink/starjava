package uk.ac.starlink.tfcat;

/**
 * Turns a JSON object into a typed java object.
 *
 * @param  <T>  output type
 */
@FunctionalInterface
public interface Decoder<T> {

    /**
     * Takes a parsed JSON object (may be an array or something else)
     * and attempts to decode it into an object of this decoder's
     * parameterised type.  In case of failure, null is returned.
     * Any fatal or recoverable errors encountered during decoding
     * should be reported through the supplied reporter.
     *
     * @param   reporter  destination for error messages
     * @param   json   input JSON object
     * @return   decoded object, or null
     */
    T decode( Reporter reporter, Object json );
}
