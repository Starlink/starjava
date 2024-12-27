package uk.ac.starlink.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that prepares a mutator method for use and documentation
 * by the {@link BeanConfig} class.  This annotation is not required
 * for such configuration, but it can be used to improve documentation
 * and make usage easier.
 *
 * <p>This annotation will normally be applied to a bean-setting method
 * with a signature like <code>void setXxx(type)</code>.
 *
 * @author   Mark Taylor
 * @since    25 Sep 2020
 * @see      BeanConfig
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ConfigMethod {

    /**
     * Gives an alternative name for the property defined by this method.
     * If the annotated method is named <code>setXxx</code> and the property
     * value is <code>yyy</code>, this configuration defined by this method
     * can be addressed as either the property <code>xxx</code> or
     * <code>yyy</code>.
     *
     * @return   property name
     */
    String property();

    /**
     * User-directed documentation.  The format is not specified here,
     * but if the return value starts with a "&lt;" the content is
     * probably XML, and if it doesn't it's probably plain text.
     *
     * @return  user-directed documentation
     */
    String doc();

    /**
     * String representation of an example value for this property,
     * suitable for use in documentation.
     *
     * @return  example setting
     */
    String example() default "";

    /**
     * Usage string.  Only required if there is something more to say
     * than the data type.
     *
     * @return  short user-directed plain text usage string
     */
    String usage() default "";

    /**
     * If true, the setting should not be documented under normal circumstances.
     *
     * @return   true to hide
     */
    boolean hide() default false;

    /**
     * Gives a sequence index indicating the order in which the different
     * ConfigMethods in a given class should be listed.
     *
     * @return   sequence index for listing in documentation
     */
    int sequence() default 1000;
}
