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
    Link[] links_;

    public String getTargetTable() {
        return targetTable_;
    }

    public String getDescription() {
        return description_;
    }

    public String getUtype() {
        return utype_;
    }

    public Link[] getLinks() {
        return links_;
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

        public String getFrom() {
            return from_;
        }

        public String getTarget() {
            return target_;
        }

        @Override
        public String toString() {
            return from_ + "->" + target_;
        }
    }
}
