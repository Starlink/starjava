package uk.ac.starlink.ttools.build;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a class, method or field is to be excluded from the
 * auto-documentation made available to application users.
 *
 * <p>This may be used, for instance, for methods that should be available
 * for use, but which might clutter up the auto-documentation in an
 * unhelpful way for some reason.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD,ElementType.FIELD})
public @interface HideDoc {
}
