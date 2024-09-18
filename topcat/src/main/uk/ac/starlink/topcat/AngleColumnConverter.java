package uk.ac.starlink.topcat;

/**
 * ColumnConverter that knows how to deal with angles.
 * It provides an additional method, {@link #angleExpression},
 * that's useful when assembling stilts command lines.
 *
 * @author   Mark Taylor
 * @since    18 Sep 2024
 */
public abstract class AngleColumnConverter extends ColumnConverter {

    private final String name_;
    private static final String F_R2D;
    private static final String F_D2R;
    static final String[] JEL_FUNCTIONS = new String[] {
        F_R2D = "radiansToDegrees",
        F_D2R = "degreesToRadians",
    };

    /**
     * Constructor.
     *
     * @param  name  converter name, will appear in GUI
     */
    protected AngleColumnConverter( String name ) {
        name_ = name;
    }

    /**
     * Returns a JEL expression that converts an an input value
     * suitable for input to this converter to a value in the
     * supplied angle unit.
     *
     * @param  inExpr   input expression, assumed JEL-friendly
     * @param  toUnit   output angular unit
     * @return   JEL expression evaluating to angular value in supplied unit
     */
    public abstract String angleExpression( String inExpr, Unit toUnit );

    @Override
    public String toString() {
        return name_;
    }

    /**
     * Creates a converter that converts to radians from a given angular unit.
     *
     * @param  fromUnit   input unit
     * @return  converter
     */
    public static AngleColumnConverter toRadianConverter( Unit fromUnit ) {
        return new AngleColumnConverter( fromUnit.toString().toLowerCase() ) {
            final double factor_ = Unit.RADIAN.perCircle_ / fromUnit.perCircle_;
            public Object convertValue( Object value ) {
                return value instanceof Number
                     ? ((Number) value).doubleValue() * factor_
                     : null;
            }
            public String convertExpression( String inExpr ) {
                return angleExpression( fromUnit, Unit.RADIAN, inExpr );
            }
            public String angleExpression( String inExpr, Unit toUnit ) {
                return angleExpression( fromUnit, toUnit, inExpr );
            }
        };
    }

    /**
     * Converts a JEL expression assumed in a given input unit to an
     * expression that will evaluate to the value in the given output unit.
     *
     * @param   fromUnit  input angular unit
     * @param   toUnit   output angular unit
     * @param   inExpr   input expression, assumed JEL-friendly
     * @return  compact JEL expression
     */
    public static String angleExpression( Unit fromUnit, Unit toUnit,
                                          String inExpr ) {
        if ( isZero( inExpr ) ) {
            return "0";
        }
        else if ( toUnit == fromUnit ) {
            return inExpr;
        }
        else if ( fromUnit == Unit.RADIAN ) {
            assert toUnit.perCircle_ == (int) toUnit.perCircle_;
            return angleExpression( Unit.DEGREE, toUnit,
                                    F_R2D + "(" + inExpr + ")" );
        }
        else if ( toUnit == Unit.RADIAN ) {
            assert fromUnit.perCircle_ == (int) fromUnit.perCircle_;
            return F_D2R + "("
                 + angleExpression( fromUnit, Unit.DEGREE, inExpr )
                 + ")";
        }
        else {
            if ( fromUnit.perCircle_ > toUnit.perCircle_ ) {
                double factor = fromUnit.perCircle_ / toUnit.perCircle_;
                assert factor == (int) factor;
                return TopcatJELUtils.groupForMultiply( inExpr )
                     + "/" + Integer.toString( (int) factor ) + ".";
            }
            else {
                double factor = toUnit.perCircle_ / fromUnit.perCircle_;
                assert factor == (int) factor;
                return TopcatJELUtils.groupForMultiply( inExpr )
                     + "*" + Integer.toString( (int) factor );
            }
        }
    }

    /**
     * Returns true iff the supplied expression is a numeric literal
     * equal to zero.
     *
     * @param  expr  expression
     * @return  true iff expr is zero
     */
    private static boolean isZero( String expr ) {
        try {
            return Double.parseDouble( expr ) == 0;
        }
        catch ( RuntimeException e ) {
            return false;
        }
    }

    /**
     * Angular units understood by this converter.
     */
    public enum Unit {

        /** Degree. */
        DEGREE( 360 ),

        /** Hour. */
        HOUR( 24 ),

        /** Arcminute. */
        ARCMIN( 60 * 360 ),

        /** Arcsecond. */
        ARCSEC( 3600 * 360 ),

        /** Radian. */
        RADIAN( 2 * Math.PI );

        final double perCircle_;

        /**
         * Constructor.
         *
         * @param  perCircle  number of this unit in a revolution
         */
        Unit( double perCircle ) {
            perCircle_ = perCircle;
        }
    }
}
