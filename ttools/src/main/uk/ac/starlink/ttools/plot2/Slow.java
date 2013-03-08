package uk.ac.starlink.ttools.plot2;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;

/**
 * Indicates that a method may take a non-negligable amount of time.
 * This is usually only the case in fact if large amounts of data
 * are involved in the plot.
 * Such methods ought not to be invoked on the AWT Event Dispatch Thread.
 */
@Inherited
@Documented
@Target({ElementType.METHOD, ElementType.CONSTRUCTOR})
public @interface Slow {
}
