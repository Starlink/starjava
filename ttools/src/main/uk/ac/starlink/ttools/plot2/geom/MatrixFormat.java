package uk.ac.starlink.ttools.plot2.geom;

import uk.ac.starlink.ttools.plot2.data.CoordGroup;

/**
 * Defines which cells from a matrix are included.
 *
 * <p>The various parts are specified by Boolean wrapper types;
 * TRUE/FALSE value indicates unconditional in/exclusion,
 * and a null value will include those cells only if there is
 * something to plot in there.
 *
 * @author   Mark Taylor
 * @since    16 Aug 2023
 */
public enum MatrixFormat {

    /** Lower diagonal part of matrix is occupied. */
    LOWER( null, Boolean.TRUE, Boolean.FALSE,
           "only the lower diagonal part of the matrix is populated, " +
           "as well as the diagonal if diagonal elements are present" ),

    /** Upper diagonal part of matrix is occupied. */
    UPPER( null, Boolean.FALSE, Boolean.TRUE,
           "only the upper diagonal part of the matrix is populated, " +
           "as well as the diagonal if diagonal elements are present" ),

    /** Full matrix is occupied. */
    FULL( null, Boolean.TRUE, Boolean.TRUE,
          "all cells of the matrix are populated where present" );

    private final Boolean hasDiag_;
    private final Boolean hasLower_;
    private final Boolean hasUpper_;
    private final String description_;

    /**
     * Constructor.
     *
     * @param  hasDiag   whether diagonal elements are included
     * @param  hasLower  whether lower triangular elements are included
     * @param  hasUpper  whether upper triangular elements are included
     * @param  description  user-directed description of this format
     */
    MatrixFormat( Boolean hasDiag, Boolean hasLower, Boolean hasUpper,
                  String description ) {
        hasDiag_ = hasDiag;
        hasLower_ = hasLower;
        hasUpper_ = hasUpper;
        description_ = description;
    }

    /**
     * Indicates whether to include the diagonal cells.
     *
     * @return  diagonal cells included/excluded/automatic
     */
    public Boolean hasDiagonal() {
        return hasDiag_;
    }

    /**
     * Indicates whether to include the lower triangular cells.
     *
     * @return  lower triangular cells included/excluded/automatic
     */
    public Boolean hasLower() {
        return hasLower_;
    }

    /**
     * Indicates whether to include the upper triangular cells.
     *
     * @return  upper triangular cells included/excluded/automatic
     */
    public Boolean hasUpper() {
        return hasUpper_;
    }

    /**
     * Returns a user-directed description of this format.
     *
     * @return  description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns a MatrixShape for a matrix of a given size conforming to
     * this MatrixFormat.
     *
     * @param   nw   linear dimension of the matrix
     * @param   hasOnDiag  whether on-diagonal cells are present
     * @param   hasOffDiag  whether off-diagonal cells are present
     * @return   matrix shape
     * @see  #isOnDiagonal
     * @see  #isOffDiagonal
     */
    public MatrixShape getShape( int nw, boolean hasOnDiag,
                                 boolean hasOffDiag ) {
        boolean hasDiag = hasDiag_ == null ? hasOnDiag
                                           : hasDiag_.booleanValue();
        boolean hasLower = hasLower_ == null ? hasOffDiag
                                             : hasLower_.booleanValue();
        boolean hasUpper = hasUpper_ == null ? hasOffDiag
                                             : hasUpper_.booleanValue();

        /* Yes this looks funny.  There are various reversals going on,
         * related to the fact that graphics coordinates go down the screen,
         * and the cells of a splom plot typically go left to right for X
         * but top to bottom for Y.  This does the right thing, but it may
         * need to be disentangled at some point in the future if we allow
         * more freedom of how the plots are arranged on the page. */
        boolean shapeDiag = hasDiag;
        boolean shapeLower = hasUpper;
        boolean shapeUpper = hasLower;
        return new MatrixShape( nw, shapeDiag, shapeLower, shapeUpper );
    }

    /**
     * Indicates whether a given coordinate group represents a grid
     * element that would appear on the diagonal of a matrix plot.
     * This will return true if the coord group has one spatial
     * coordinate, that is if it's histogram-like.
     *
     * @param   cgrp   coord group
     * @return   true for on-diagonal matrix plot element
     */
    public static boolean isOnDiagonal( CoordGroup cgrp ) {
        return cgrp != null && cgrp.isSinglePartialPosition();
    }

    /**
     * Indicates whether a given coordinate group represents a grid
     * element that would appear off the diagonal of a matrix plot.
     * This will return true if the coord group has two spatial
     * coordinates, that is if it's scatter-plot-like.
     *
     * @param  cgrp  coord group, assumed to be the sort of coords
     *               that might show up in a matrix plot
     * @return   true for off-diagonal matrix plot element
     */
    public static boolean isOffDiagonal( CoordGroup cgrp ) {
        // on the assumption that all plotters that are going to show up here
        // have 2d positions if any.
        return cgrp != null && cgrp.getBasicPositionCount() > 0;
    }
}
