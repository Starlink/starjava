package uk.ac.starlink.gbin;

/**
 * Represents metadata easily recovered from a GBIN file.
 * This interface corresponds to (contains some of the same methods as)
 * the gaia.cu1.tools.dal.gbin.GbinMetaData class.
 *
 * <p>For convenience the methods here throw no exceptions, if something
 * goes wrong they should just return a null value.
 *
 * @author   Mark Taylor
 * @since    13 Aug 2014
 */
public interface GbinMeta {

    /**
     * Returns a description of the contents of the GBIN file.
     *
     * @param  showChunkBreakdown  if true, gives details of
     *         each individual chunk, otherwise just gives a
     *         summary of the gbin contents
     * @return  description, typically multi-line
     */
    String buildDescription( boolean showChunkBreakdown );

    /**
     * Returns the GBIN version number.
     *
     * @return  gbin version number
     */
    Integer getGbinVersionNumber();

    /**
     * Returns the unique list of solution id's present in this gbin
     * file in ascending order. The list can be empty if the objects do
     * not contain a solutionId field, or no objects were written. The
     * list can be null if no solutionId information was found for one
     * or more chunks (e.g. the data was written with an older version of
     * the writer class, or an older format (e.g. GbinV2).
     *
     * @return   array of solution IDs
     */
    long[] getSolutionIdList();

    /**
     * Returns the number of objects in the GBIN file.
     *
     * @return  element count
     */
    Long getTotalElementCount();
}
