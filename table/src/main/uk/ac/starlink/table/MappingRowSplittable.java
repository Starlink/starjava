package uk.ac.starlink.table;

import java.io.IOException;
import java.util.function.Function;
import java.util.function.LongSupplier;

/**
 * RowSplittable which wraps another RowSplittable but presents
 * different column contents as controlled by a supplied column data
 * mapping function.  The methods other than the data access are
 * delegated to a supplied base RowSplittable, so that the row
 * structure must be the same as for the base.
 *
 * <p><strong>Note:</strong> this cannot in general be extended
 * by an anonymous class to change aspects of the behaviour
 * other than the column mapping, since the {@link #split} method
 * has to return an instance of the same class rather than of its superclass.
 *
 * @author   Mark Taylor
 * @since    12 Oct 2020
 */
public class MappingRowSplittable extends WrapperRowSequence
                                  implements RowSplittable {

    private final RowSplittable baseSplit_;
    private final Function<RowSplittable,RowData> dataMapper_;
    private final RowData data_;

    /**
     * Constructor.
     *
     * @param  baseSplit  base splittable
     * @param  dataMapper   used to acquire data access behaviour from
     *                      newly split base instances
     */
    public MappingRowSplittable( RowSplittable baseSplit,
                                 Function<RowSplittable,RowData> dataMapper ) {
        super( baseSplit );
        baseSplit_ = baseSplit;
        dataMapper_ = dataMapper;
        data_ = dataMapper.apply( baseSplit );
    }

    @Override
    public Object getCell( int icol ) throws IOException {
        return data_.getCell( icol );
    }

    @Override
    public Object[] getRow() throws IOException {
        return data_.getRow();
    }

    public long splittableSize() {
        return baseSplit_.splittableSize();
    }

    public LongSupplier rowIndex() {
        return baseSplit_.rowIndex();
    }

    public RowSplittable split() {
        RowSplittable spl = baseSplit_.split();
        return spl == null ? null
                           : new MappingRowSplittable( spl, dataMapper_ );
    }
}
