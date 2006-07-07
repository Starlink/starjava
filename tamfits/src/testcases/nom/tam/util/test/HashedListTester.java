package nom.tam.util.test;

/* Copyright: Thomas McGlynn 1999.
 * This code may be used for any purpose, non-commercial
 * or commercial so long as this copyright notice is retained
 * in the source code or included in or referred to in any
 * derived software.
 */


import nom.tam.util.*;
import java.util.*;

/** This class tests and illustrates the use
 *  of the HashedList class.  Tests are in three
 *  parts.  
 *  <p>
 *  The first section tests the methods
 *  that are present in the Collection interface.
 *  All of the optional methods of that interface
 *  are supported.  This involves tests of the
 *  HashedClass interface directly.
 *  <p>
 *  The second set of tests uses the Iterator
 *  returned by the iterator() method and tests
 *  the standard Iterator methods to display
 *  and remove rows from the HashedList.
 *  <p>
 *  The third set of tests tests the extended
 *  capabilities of the HashedListIterator
 *  to add rows to the table, and to work
 *  as a cursor to move in a non-linear fashion
 *  through the list.
 *  <p>
 *  There is as yet no testing that the HashedList
 *  fails appropriately and gracefully.
 * 
 */
public class HashedListTester {
    
    public static void main(String[] args) {

	HashedList h1 = new HashedList();
	HashedList h2 = new HashedList();
	
	Cursor i = h1.iterator(0);
	Iterator j;
	
	// Add a few unkeyed rows.
	
	
	h1.add("Row 1");
	h1.add("Row 2");
	h1.add("Row 3");
	
	System.out.println("***** Collection methods *****\n");
	show("Three unkeyed elements",h1);
	h1.remove("Row 2");
	show("Did we remove Row 2?", h1);
	h1.clear();
	
	show("Cleared", h1);
	
	h1.add("key 1", "Row 1");
	h1.add("key 2", "Row 2");
	h1.add("key 3", "Row 3");
	
	show("Three keyed elements", h1);
	h1.removeKey("key 2");
	show("Did we remove Row 2 using a key?", h1);
	h1.clear();
	show("Cleared", h1);
	
	h1.add("key 1", "Row 1");
	h1.add("key 2", "Row 2");
	h1.add("key 3", "Row 3");
	show ("Three elements again!", h1);
	System.out.println("Check contains (true):"+h1.contains("Row 2"));
			   
	
	h2.add("key 4", "Row 4");
	h2.add("key 5", "Row 5");
	
	System.out.println("Check containsAll (false):"+h1.containsAll(h2));
	
	h1.addAll(h2);
	show("Should have 5 elements now", h1);
	System.out.println("Check containsAll (true):"+h1.containsAll(h2));
	System.out.println("Check contains (true):"+h1.contains("Row 4"));
	
	h1.remove("Row 4");
	show("Dropped Row 4:", h1);
	System.out.println("Check containsAll (false):"+h1.containsAll(h2));
	System.out.println("Check contains (false):"+h1.contains("Row 4"));
	
	System.out.println("Check isEmpty (false):"+h1.isEmpty());
	h1.remove("Row 1");
	h1.remove("Row 2");
	h1.remove("Row 3");
	h1.remove("Row 5");
	show("Removed all elements", h1);
	System.out.println("Check isEmpty (true):"+h1.isEmpty());
	h1.add("Row 1");
	h1.add("Row 2");
	h1.add("Row 3");
	h1.addAll(h2);
	show("Back to 5", h1);
	h1.removeAll(h2);
	show("Testing removeAll back to 3?", h1);
	h1.addAll(h2);
	h1.retainAll(h2);
	show("Testing retainAll now just 2?", h1);
	
	System.out.println("\n\n**** Test iterator **** \n");
	
	j = h1.iterator();
	while (j.hasNext()) {
	    System.out.println("Iterator got:"+j.next());
	}
	
	h1.clear();
	h1.add("key 1", "Row 1");
	h1.add("key 2", "Row 2");
	h1.add("Row 3");
	h1.add("key 4", "Row 4");
	h1.add("Row 5");
	j=h1.iterator();
	j.next();
	j.next();
	j.remove();  // Should get rid of second row
	show("Removed second row with iterator", h1);
	System.out.println("Iterator should still be OK:"+j.hasNext()+" "+j.next());
	System.out.println("Iterator should still be OK:"+j.hasNext()+" "+j.next());
	System.out.println("Iterator should still be OK:"+j.hasNext()+" "+j.next());
	System.out.println("Iterator should be done:"+j.hasNext());
	
	System.out.println("\n\n**** HashedListIterator ****\n");
	i = h1.iterator(0);
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should be done:"+i.hasNext());
	
	i.setKey("key 1");
	i.next();
	i.add("key 2", "Row 2");
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should be done:"+i.hasNext());
	
	i.setKey("key 4");
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should still be OK:"+i.hasNext()+" "+i.next());
	System.out.println("Iterator should be done:"+i.hasNext());
	
	i.setKey("key 2");
	i.next();
	i.next();
	i.add("Row 3.5");
	i.add("Row 3.6");
	show("Added some rows... should be 7", h1);
	
	i = h1.iterator("key 2");
	i.add("Row 1.5");
	i.add("key 1.7", "Row 1.7");
	i.add("Row 1.9");
	System.out.println("Iterator should point to 2:"+i.next());
	i.setKey("key 1.7");
	System.out.println("Iterator should point to 1.7:"+i.next());
				 
    }
    public static void show(String descrip, HashedList h) {
	
	System.out.println(descrip+" :  "+h.size());
	Object[] o = h.toArray();
	for (int i=0; i < o.length; i += 1) {
	    System.out.println("  "+o[i]);
	}
    }
}
	
	
	
	
