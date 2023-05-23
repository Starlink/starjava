package uk.ac.starlink.ttools.plot2.geom;

import java.util.Iterator;
import java.util.NoSuchElementException;
import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines the shape of an ordered list of cells from a square matrix,
 * and a mapping between an index and the cells.
 * The list may include zero or more of diagonal, upper-triangle and
 * lower-triangle cells of the matrix.
 *
 * @author   Mark Taylor
 * @since    1 Jun 2023
 */
@Equality
public class MatrixShape implements Iterable<MatrixShape.Cell> {

    private final int nx_;
    private final boolean hasDiagonal_;
    private final boolean hasLower_;
    private final boolean hasUpper_;
    private final Mapper mapper_;

    /**
     * Constructs a MatrixShape containing selected cells.
     *
     * @param   nx   linear size of matrix
     * @param   hasDiagonal  true iff list includes cells on the diagonal
     *                       (x==y)
     * @param   hasLower    true iff list includes cells below the diagonal
     *                      (x&gt;y)
     * @param   hasUpper    true iff list includes cells above the diagonal
     *                      (x&lt;y)
     */
    public MatrixShape( int nx, boolean hasDiagonal,
                        boolean hasLower, boolean hasUpper ) {
        nx_ = nx;
        hasDiagonal_ = hasDiagonal;
        hasLower_ = hasLower;
        hasUpper_ = hasUpper;
        mapper_ = createMapper( this );
    }

    /**
     * Constructs a MatrixShape containing all cells in the matrix.
     *
     * @param  nx  linear size of matrix
     */
    public MatrixShape( int nx ) {
        this( nx, true, true, true );
    }

    /**
     * Returns the linear size of this matrix.
     *
     * @return  width (N) of NxN matrix
     */
    public int getWidth() {
        return nx_;
    }

    /**
     * Indicates whether this shape contains cells on the diagonal.
     *
     * @return  true iff diagonal cells are included (x==y)
     */
    public boolean hasDiagonal() {
        return hasDiagonal_;
    }

    /**
     * Indicates whether this shape contains cells below the diagonal.
     *
     * @return  true iff below-diagonal cells are included (x&gt;y)
     */
    public boolean hasLower() {
        return hasLower_;
    }

    /**
     * Indicates whether this shape contains cells above the diagonal.
     *
     * @return  true iff above-diagonal cells are included (x&lt;y)
     */
    public boolean hasUpper() {
        return hasUpper_;
    }

    /**
     * Returns the number of cells in this shape.
     *
     * @return  cell count
     */
    public int getCellCount() {
        return mapper_.ncell_;
    }

    /**
     * Returns the index of the cell at a given X,Y position.
     *
     * @param  ix  X index
     * @param  iy  Y index
     * @return   index of given cell into list, or -1
     */
    public int getIndex( int ix, int iy ) {
        return ix >= 0 && ix < nx_ && iy >= 0 && iy < nx_
             ? mapper_.getIndex( ix, iy )
             : -1;
    }

    /**
     * Returns the index of a given cell.
     *
     * @param  cell  cell
     * @return  index of given cell into list, or -1
     */
    public int getIndex( Cell cell ) {
        return getIndex( cell.getX(), cell.getY() );
    }

    /**
     * Returns the cell at a given position in this shapes list of cells.
     *
     * @param  icell  cell index
     * @return  cell, or null if out of range
     */
    public Cell getCell( int icell ) {
        return icell >= 0 && icell < mapper_.ncell_
             ? mapper_.getCell( icell )
             : null;
    }

    /**
     * Returns an iterator over this shape's cells.
     *
     * @return  cell iterator
     */
    public Iterator<Cell> iterator() {
        return new Iterator<Cell>() {
            int ic = 0;
            public boolean hasNext() {
                return ic < mapper_.ncell_;
            }
            public Cell next() {
                if ( ic < mapper_.ncell_ ) {
                    return mapper_.getCell( ic++ );
                }
                else {
                    throw new NoSuchElementException();
                }
            }
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String toString() {
        return new StringBuffer( 6 )
             .append( nx_ )
             .append( '(' )
             .append( hasDiagonal_ ? 'D' : '.' )
             .append( hasLower_ ? 'L' : '.' )
             .append( hasUpper_ ? 'U' : '.' )
             .append( ')' )
             .toString();
    }

    @Override
    public int hashCode() {
        int code = 221512;
        code = 23 * code + nx_;
        code = 23 * code + ( hasDiagonal_ ? 0 : 2 );
        code = 23 * code + ( hasUpper_ ? 0 : 4 );
        code = 23 * code + ( hasLower_ ? 0 : 8 );
        return code;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o instanceof MatrixShape ) {
            MatrixShape other = (MatrixShape) o;
            return this.nx_ == other.nx_
                && this.hasDiagonal_ == other.hasDiagonal_
                && this.hasUpper_ == other.hasUpper_
                && this.hasLower_ == other.hasLower_;
        }
        else {
            return false;
        }
    }

    /**
     * Returns an object that can map between cells and indices for a
     * given matrix shape.
     */
    private static Mapper createMapper( MatrixShape shape ) {
        int nx = shape.nx_;
        boolean hasDiagonal = shape.hasDiagonal_;
        boolean hasLower = shape.hasLower_;
        boolean hasUpper = shape.hasUpper_;
        if ( hasUpper && hasLower ) {
            if ( hasDiagonal ) {
                return new Mapper( nx * nx ) {
                    int getIndex( int ix, int iy ) {
                        return ix + nx * iy;
                    }
                    Cell getCell( int icell ) {
                        return new Cell( icell % nx, icell / nx );
                    }
                };
            }
            else {
                int nx1 = nx - 1;
                return new Mapper( nx * nx1 ) {
                    int getIndex( int ix, int iy ) {
                        return ix != iy
                             ? ix + ( ix > iy ? -1 : 0 ) + nx1 * iy
                             : -1;
                    }
                    Cell getCell( int icell ) {
                        int jx = icell % nx1;
                        int iy = icell / nx1;
                        return new Cell( jx + ( jx >= iy ? 1 : 0 ), iy );
                    }
                };
            }
        }
        else if ( !hasUpper && !hasLower ) {
            if ( hasDiagonal ) {
                return new Mapper( nx ) {
                    int getIndex( int ix, int iy ) {
                        return ix == iy ? ix : -1;
                    }
                    Cell getCell( int icell ) {
                        return new Cell( icell, icell );
                    }
                };
            }
            else {
                return new Mapper( 0 ) {
                    int getIndex( int ix, int iy ) {
                        return -1;
                    }
                    Cell getCell( int icell ) {
                        return null;
                    }
                };
            }
        }
        else {
            assert hasUpper != hasLower;
            int nx1 = nx - 1;
            int nt = nx * nx1 / 2;
            return new Mapper( nt + ( hasDiagonal ? nx : 0 ) ) {
                int getIndex( int ix, int iy ) {
                    int ip = hasLower ? ix : iy;
                    int iq = hasLower ? iy : ix;
                    if ( ip > iq ) {
                        return ip * ( ip - 1 ) / 2 + iq;
                    }
                    else if ( hasDiagonal && ix == iy ) {
                        return nt + ix;
                    }
                    else {
                        return -1;
                    }
                }
                Cell getCell( int icell ) {
                    if ( icell < nt ) {
                        // Magic cribbed from
                        // https://stackoverflow.com/questions/4803180/.
                        int iq = (int)
                                 ( ( -1 + Math.sqrt( 8 * icell + 1 ) ) / 2 );
                        int ip = icell - iq * ( iq + 1 ) / 2;
                        return hasLower ? new Cell( iq + 1, ip )
                                        : new Cell( ip, iq + 1 );
                    }
                    else if ( hasDiagonal ) {
                        int id = icell - nt;
                        return id >= 0 && id < nx
                             ? new Cell( id, id )
                             : null;
                    }
                    else {
                        return null;
                    }
                }
            };
        }
    }

    /**
     * Represents one cell in a 2x2 matrix.
     * Just encapsulates non-negative (X,Y) coordinates.
     */
    @Equality
    public static class Cell {

        private final int ix_;
        private final int iy_;

        /**
         * Constructor.
         *
         * @param  ix  X index
         * @param  iy  Y index
         */
        public Cell( int ix, int iy ) {
            ix_ = ix;
            iy_ = iy;
        }

        /**
         * Returns X index.
         *
         * @return  index in X direction
         */
        public int getX() {
            return ix_;
        }

        /**
         * Returns Y index.
         *
         * @return  index in Y direction
         */
        public int getY() {
            return iy_;
        }

        @Override
        public String toString() {
            return ix_ + "," + iy_;
        }

        @Override
        public int hashCode() {
            return ix_ | ( iy_ << 16 );
        }

        @Override
        public boolean equals( Object o ) {
            if ( o instanceof Cell ) {
                Cell other = (Cell) o;
                return this.ix_ == other.ix_
                    && this.iy_ == other.iy_;
            }
            else {
                return false;
            }
        }
    }

    /**
     * Defines an object that can map between cells and indices for a given
     * matrix shape.
     */
    private static abstract class Mapper {
        final int ncell_;

        /**
         * Constructor.
         *
         * @param  ncell  number of cells
         */
        Mapper( int ncell ) {
            ncell_ = ncell;
        }

        /**
         * Maps from legal X,Y position to list index.
         *
         * @param  ix  X index in range
         * @param  iy  Y index in range
         * @return    index into list, or undefined if inputs are not in range
         */
        abstract int getIndex( int ix, int iy );

        /**
         * Maps from legal cell index to X,Y position.
         *
         * @param  icell  index into list
         * @return  cell position, or undefined if input is not in range
         */
        abstract Cell getCell( int icell );
    }
}
