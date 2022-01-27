package uk.ac.starlink.ttools.jel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a public static method is to be evalated by JEL
 * at runtime not compile time, even if its arguments can be determined
 * at compile time.
 *
 * @author   Mark Taylor
 * @since    27 Jan 2022
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface StateDependent {
}
