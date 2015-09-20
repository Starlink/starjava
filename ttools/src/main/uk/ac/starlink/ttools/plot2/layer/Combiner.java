package uk.ac.starlink.ttools.plot2.layer;

import uk.ac.starlink.ttools.plot2.Equality;

/**
 * Defines the combination mode for accumulating values into a bin.
 *
 * <p>Note that the {@link #SUM} mode is usually sensible for unweighted values,
 * but if the values are weighted it may be more revealing to use
 * one of the others (like {@link #MEAN}).
 *
 * @author    Mark Taylor
 * @since     20 Sep 2015
 */
@Equality
public abstract class Combiner {

    private final String name_;
    private final String description_;

    /** Calculate the sum of all submitted values. */
    public static final ScalarCombiner SUM;

    /** Calculate the mean of all submitted values. */
    public static final Combiner MEAN;

    /** Calculate the minimum of all submitted values. */
    public static final ScalarCombiner MIN;

    /** Calculate the maximum of all submitted values. */
    public static final ScalarCombiner MAX;

    private static final Combiner[] COMBINERS = new Combiner[] {
        SUM = new SumCombiner(),
        MEAN = new MeanCombiner(),
        MIN = new MinCombiner(),
        MAX = new MaxCombiner(),
    };

    /**
     * Constructor.
     *
     * @param   name  name
     * @param   description  short textual description
     */
    protected Combiner( String name, String description ) {
        name_ = name;
        description_ = description;
    }

    /**
     * Creates an object which can be used to accumulate values.
     *
     * @return  new container
     */
    public abstract Container createContainer();

    /**
     * Returns this combiner's name.
     *
     * @return  name
     */
    public String getName() {
        return name_;
    }

    /**
     * Returns a short textual description of this combiner.
     *
     * @return  short description
     */
    public String getDescription() {
        return description_;
    }

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Returns a list of the known general purpose instances of this class.
     *
     * @return   combiner list
     */
    public static Combiner[] getKnownCombiners() {
        return COMBINERS.clone();
    }

    /**
     * Defines an object that can be used to accumulate values and
     * retrieve a result.
     */
    public interface Container {

        /**
         * Submits a new datum for accumulation to the result.
         *
         * @param  datum  new value to accumulate
         */
        void submit( double datum );

        /**
         * Returns the combined result of all the values accumulated so far.
         *
         * @return  combined value of all submitted data
         */
        double getResult();
    }

    /**
     * Subclass of Combiner which adds some new methods.
     * The new methods duplicate the basic functionality provided by the
     * parent class, but do it using a single externally held
     * primitive floating point value as the data store,
     * so it may be possible to use combiners subclassing
     * this class in a more efficient way than by creating Container objects.
     */
    public static abstract class ScalarCombiner extends Combiner {

        private final double initialState_;

        /**
         * Constructor.
         *
         * @param   name  name
         * @param   description  short textual description
         * @param   initialState  constant which must be used to initialise
         *                        the state value
         */
        protected ScalarCombiner( String name, String description,
                                  double initialState ) {
            super( name, description );
            initialState_ = initialState;
        }

        /**
         * Returns the constant which must be used to initialise the
         * state value.
         *
         * @return  initial state value
         */
        public double getInitialState() {
            return initialState_;
        }

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value.
         *
         * @param  oldState  previous state
         * @param  datum   newly submitted value
         * @return  new state
         */
        public abstract double getUpdatedState( double oldState, double datum );

        /**
         * Returns the actual result of the combination of accumulated data
         * given a state value.
         * This might just be the state value itself,
         * but it doesn't have to be.
         *
         * @param   state
         * @return   combination result
         */
        public abstract double extractResult( double state );
    }

    /**
     * Combiner implementation that calculates the mean.
     */
    private static class MeanCombiner extends Combiner {

        /**
         * Constructor.
         */
        public MeanCombiner() {
            super( "mean", "the mean of the combined values" );
        }

        public Container createContainer() {
            return new CountedSum();
        }

        /**
         * Container that holds a count and a sum.
         */
        private static class CountedSum implements Container {
            int count_;
            double sum_;
            public void submit( double datum ) {
                count_++;
                sum_ += datum;
            }
            public double getResult() {
                return sum_ / (double) count_;
            }
        }
    }

    /**
     * Partial Container implementation for use by ScalarCombiners.
     */
    private static abstract class ScalarContainer implements Container {

        /* Value that contains the whole state of this container. */
        private double state_;

        /**
         * Constructor.
         *
         * @param  initialState  initial state value
         */
        ScalarContainer( double initialState ) {
            state_ = initialState;
        }

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value.
         *
         * @param  oldState  previous state
         * @param  datum   newly submitted value
         * @return  new state
         */
        abstract double getUpdatedState( double oldState, double datum );

        /**
         * Returns the actual result of the combination of accumulated data
         * given a state value.
         * This might just be the state value itself,
         * but it doesn't have to be.
         *
         * @param   state  state value
         * @return   combination result
         */
        abstract double extractResult( double state );

        public void submit( double datum ) {
            state_ = getUpdatedState( state_, datum );
        }

        public double getResult() {
            return extractResult( state_ );
        }
    }

    /**
     * Combiner implementation that calculates the sum.
     */
    private static class SumCombiner extends ScalarCombiner {

        /**
         * Constructor.
         */
        SumCombiner() {
            super( "sum", "the sum of all the combined values", 0 );
        }

        public double getUpdatedState( double oldState, double datum ) {
            return oldState + datum;
        }

        public double extractResult( double state ) {
            return state;
        }

        public Container createContainer() {
            return new SumContainer();
        }

        /**
         * Container implementation for use with SumCombiner.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class SumContainer extends ScalarContainer {
            SumContainer() {
                super( 0 );
            }
            double getUpdatedState( double oldState, double datum ) {
                return oldState + datum;
            }
            public double extractResult( double state ) {
                return state;
            }
        }
    }

    /**
     * Combiner implementation that calculates the minimum submitted value.
     */
    private static class MinCombiner extends ScalarCombiner {

        /**
         * Constructor.
         */
        MinCombiner() {
            super( "min", "the minimum of all the combined values",
                   Double.NaN );
        }

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (minimum).
         *
         * @param  oldState  previous state
         * @param  datum   newly submitted value
         * @return  new state
         */
        static double combine( double oldState, double datum ) {
            return Double.isNaN( oldState ) ? datum
                                            : Math.min( oldState, datum );
        }

        /**
         * Returns the result value corresponding to a state primitive.
         *
         * @param  state  state value
         * @return  minimum result
         */
        static double result( double state ) {
            return Double.isNaN( state ) ? 0 : state;
        }

        public double getUpdatedState( double oldState, double datum ) {
            return combine( oldState, datum );
        }

        public double extractResult( double state ) {
            return result( state );
        }

        public Container createContainer() {
            return new MinContainer();
        }

        /**
         * Container implementation for use with MinCombiner.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MinContainer extends ScalarContainer {
            MinContainer() {
                super( Double.NaN );
            }
            double getUpdatedState( double oldState, double datum ) {
                return combine( oldState, datum );
            }
            public double extractResult( double state ) {
                return result( state );
            }
        }
    }

    /**
     * Combiner implementation that calculates the maximum submitted value.
     */
    private static class MaxCombiner extends ScalarCombiner {

        /**
         * Constructor.
         */
        MaxCombiner() {
            super( "max", "the maximum of all the combined values",
                   Double.NaN );
        }

        /**
         * Combines the existing state value with a supplied datum
         * to give a new state value (maximum).
         *
         * @param  oldState  previous state
         * @param  datum   newly submitted value
         * @return  new state
         */
        static double combine( double oldState, double datum ) {
            return Double.isNaN( oldState ) ? datum
                                            : Math.max( oldState, datum );
        }

        /**
         * Returns the result value corresponding to a state primitive.
         *
         * @param  state  state value
         * @return  maximum result
         */
        static double result( double state ) {
            return Double.isNaN( state ) ? 0 : state;
        }

        public double getUpdatedState( double oldState, double datum ) {
            return combine( oldState, datum );
        }

        public double extractResult( double state ) {
            return result( state );
        }

        public Container createContainer() {
            return new MaxContainer();
        }

        /**
         * Container implementation for use with MaxCombiner.
         * Note this is a static class to keep memory usage down
         * if there are many instances.
         */
        private static class MaxContainer extends ScalarContainer {
            MaxContainer() {
                super( Double.NaN );
            }
            double getUpdatedState( double oldState, double datum ) {
                return combine( oldState, datum );
            }
            public double extractResult( double state ) {
                return result( state );
            }
        }
    }
}
