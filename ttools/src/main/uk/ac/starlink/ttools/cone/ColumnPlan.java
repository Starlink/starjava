package uk.ac.starlink.ttools.cone;

/**
 * Describes the arrangement of columns in the output table based on
 * the columns in the upload and raw result tables.
 *
 * @author   Mark Taylor
 * @since    5 Jun 2014
 */
public interface ColumnPlan {

    /**
     * Returns the number of columns in the output table.
     *
     * @return  output column count
     */
    int getOutputColumnCount();

    /**
     * Returns a coded value indicating where to find the column
     * corresponding to a given output column.
     * If the result is positive, then return_value is
     * a column index in the raw result table.
     * If the result is negative, then (-return_value-1) is
     * column index in the upload table
     *
     * @param  icolOutput  column index in output table
     * @return   coded location for column source
     */
    int getOutputColumnLocation( int icolOutput );

    /**
     * Returns the index of the row identifier column in the result table.
     *
     * @return   identifer column index
     */
    int getResultIdColumnIndex();

    /**
     * Returns the index of the match score column in the result table.
     * Must point to an actual column.
     *
     * @return  score column index
     */
    int getResultScoreColumnIndex();
}
