package uk.ac.starlink.vo;

import java.util.Arrays;

/**
 * Represents foreign key information from a TableSet document.
 *
 * @author   Mark Taylor 
 * @since    21 Jan 2011
 * @see  <a href="http://www.ivoa.net/Documents/VODataService/"
 *          >IVOA VODataService Recommendation</a>
 */
public class ForeignMeta {

    String targetTable_;
    String description_;
    String utype_;
    String keyId_;
    Link[] links_;

    /**
     * Constructor.
     */
    protected ForeignMeta() {
    }

    /**
     * Returns the fully-qualified name of the target table for this
     * foreign key.
     *
     * @return   target table name
     */
    public String getTargetTable() {
        return targetTable_;
    }

    /**
     * Returns the description for this foreign key.
     *
     * @return   text description
     */
    public String getDescription() {
        return description_;
    }

    /**
     * Returns the utype associated with this foreign key.
     *
     * @return  utype
     */
    public String getUtype() {
        return utype_;
    }

    /**
     * Returns the key ID associated with this foreign key.
     *
     * @return  key ID
     */
    public String getKeyId() {
        return keyId_;
    }

    /**
     * Returns the column links associated with this foreign key.
     * If the result is null, nothing is known about the links,
     * and the list may need to be explicitly set.
     *
     * @return  array of links associated with this foreign key, or null
     */
    public Link[] getLinks() {
        return links_;
    }

    /**
     * Sets the links contained in this table.
     *
     * @param  links  link array
     */
    public void setLinks( Link[] links ) {
        links_ = links;
    }

    @Override
    public String toString() {
        return "->" + targetTable_ + Arrays.asList( links_ );
    }

    /**
     * Represents a linkage from a column in the source table to a column
     * in the target table.
     */
    public static class Link {

        String from_;
        String target_;

        protected Link() {
        }

        /**
         * Returns the name of the source column.
         *
         * @return  from column name
         */
        public String getFrom() {
            return from_;
        }

        /**
         * Returns the name of the destination column.
         *
         * @return  to column name
         */
        public String getTarget() {
            return target_;
        }

        @Override
        public String toString() {
            return from_ + "->" + target_;
        }
    }
}
