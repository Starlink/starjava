package uk.ac.starlink.ttools.plot2;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an object must have the {@link java.lang.Object#equals} and
 * {@link java.lang.Object#hashCode}
 * methods implemented intelligently.
 * These methods must be implemented such that two instances of the
 * class constructed from equivalent inputs evaluate as equal to each other,
 * and must yield the same <code>hashCode</code>.
 * This will normally mean that the Object implementation must be overridden,
 * so that different instances can under appropriate circumstances
 * evaluate equal.
 *
 * <p>Within the plotting system, instances of classes annotated with 
 * this annotation may be used either effectively or literally as
 * keys in some map storing values which are expensive to calculate.
 * These keys, which are themselves cheap to obtain, can be compared with
 * cached values (typically produced during an earlier plotting cycle)
 * to determine whether the values need to be recalculated.
 * There is rather a lot of this that goes on during TOPCAT plotting,
 * and it is important for performance.
 *
 * <p>In most cases this annotation is applied to a class or interface,
 * indicating that all subtypes must have equality semantics.
 * Applied to a method, it means that the method return value must have
 * equality semantics (or be null).
 *
 * <p>Deviations from the behaviour required by this annotation can be
 * difficult to spot.  Some assertions are scattered around in the code
 * to try to catch such deviations.
 *
 * @author   Mark Taylor
 * @since    7 Feb 2013
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD,ElementType.FIELD})
public @interface Equality {
}
