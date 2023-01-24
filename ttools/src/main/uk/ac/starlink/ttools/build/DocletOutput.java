package uk.ac.starlink.ttools.build;

import java.io.IOException;

/**
 * Serializes text obtained from javadocs to some specific output format.
 *
 * <p>This interface is intended for working with the classes providing
 * functionality made available through the JEL expression language.
 * The methods have been introduced ad hoc to provide the functionality
 * required for that purpose, and do not intend to form a comprehensive
 * doclet output interface.
 *
 * <p>Since this interface is only intended for use at package build time,
 * and since the classes it works with have conservatively-written javadocs,
 * implementations do not need to be robust against all possible input.
 * In case of problems however they should warn or fail noisily rather than
 * produce sub-standard best-efforts output.
 *
 * <p>This interface makes no use of any javadoc API.
 * Its purpose is to act as a format-specific output handler
 * that is not tied to a particular javadoc parsing API.
 *
 * @author   Mark Taylor
 * @since    24 Jan 2023
 */
public interface DocletOutput {

    /**
     * Called at the start of operation.
     */
    void startOutput() throws IOException;

    /**
     * Called at the end of operation.
     */
    void endOutput() throws IOException;

    /**
     * Begin output of documentation for a given class.
     *
     * @param  className  fully qualified class name
     * @param  firstSentence  first sentence of class description, in HTML
     * @param  fullDescription   full text of class description, in HTML
     */
    void startClass( String className, String firstSentence,
                     String fullDescription ) throws IOException;

    /**
     * End output of documentation for the most recently started class.
     */
    void endClass() throws IOException;

    /**
     * Begin output of documentation for a given class member (field or method).
     *
     * @param  name    user-readable name of the member
     * @param  type   some user-readable (maybe avoiding technical terms)
     *                description of what kind of member it is
     * @param  memberId  string uniquely identifying this member within
     *                   its parent class
     * @param  description   full text of member description, in HTML
     */
    void startMember( String name, String type, String memberId,
                      String description )
            throws IOException;

    /**
     * End output of the most recently started member.
     */
    void endMember() throws IOException;

    /**
     * Writes a name-value pair presenting an attribute of a class member
     * (field or method).
     *
     * @param   key  item name
     * @param   value  item value, may be HTML
     */
    void outMemberItem( String key, String value ) throws IOException;

    /**
     * Writes a description of the parameters of a method.
     *
     * @param  params  parameter list
     */
    void outParameters( DocVariable[] params ) throws IOException;

    /**
     * Writes information about the return value of a method.
     *
     * @param  type   return type specification for presentation to the user
     * @param  commentText   description of returned value, may be HTML
     */
    void outReturn( String type, String commentText ) throws IOException;

    /**
     * Writes one or more example entries.
     *
     * @param  heading  heading for examples section
     * @param  examples   list of example lines, may be HTML
     */
    void outExamples( String heading, String[] examples ) throws IOException;

    /**
     * Writes one or more See Also entries.
     *
     * @param  heading  heading for See Also section
     * @param  sees   list of entries, may be HTML
     */
    void outSees( String heading, String[] sees ) throws IOException;

    /**
     * Characterises a method parameter.
     */
    public interface DocVariable {

        /**
         * Returns parameter name.
         *
         * @return  name
         */
        String getName();

        /**
         * Returns parameter type in user-friendly form.
         *
         * @return  type specification
         */
        String getType();

        /**
         * Returns parameter description.
         *
         * @return  description, may be HTML
         */
        String getCommentText();
    }
}
