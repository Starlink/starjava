package uk.ac.starlink.topcat;

import java.util.BitSet;

/**
 * A RowSubset which maintains the inclusion status of each row as
 * a separate flag.
 *
 * @author    Mark Taylor (Starlink)
 */
public class BitsRowSubset implements RowSubset {

     private BitSet bits;
     private String name;

     /**
      * Constructs a new row subset with a given BitSet and name.
      *
      * @param   name  subset name
      * @param   bits  flag vector
      */
     public BitsRowSubset( String name, BitSet bits ) {
         this.name = name;
         this.bits = bits;
     }

     /**
      * Returns the <tt>BitSet</tt> object used to store the inclusion
      * status flags.
      *
      * @return  flag vector
      */
     public BitSet getBitSet() {
         return bits;
     }

     public String getName() {
         return name;
     }

     public boolean isIncluded( long lrow ) {
         return bits.get( (int) lrow );
     }

     public String toString() {
         return name;
     }

}
