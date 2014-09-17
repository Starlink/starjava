package uk.ac.starlink.splat.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * This class wraps the standard java.util.List and allows to
 * add constraints (defined by ConstraintType enum) for adding new items to it.
 * 
 * @author David Andresic
 * @version $Id$
 *
 */
public class ConstrainedList<T> implements List<T>{
	
	/**
	 * Available constraint types
	 *
	 */
	public static enum ConstraintType {
		DENY_NULL_VALUES
	};
	
	private List<ConstraintType> constraintTypes;
	private Class listClass;
	private boolean throwException = false;
	List<T> wrappedList;
	
	/**
	 * Creates an ArrayList instance with constraint defined by constraintType
	 * @param constraintType constraint type to apply
	 */
	public ConstrainedList(ConstraintType constraintType) {
		this(Arrays.asList(new ConstraintType[]{constraintType}));
	}
	
	/**
	 * Creates an ArrayList instance with constraints defined by constraintTypes
	 * @param constraintTypes constraint types to apply
	 */
	public ConstrainedList(List<ConstraintType> constraintTypes) {
		this(constraintTypes, ArrayList.class);
	}
	
	/**
	 * Creates a List instance of listClass with constraint defined by constraintType
	 * @param constraintType constraint type to apply
	 * @param listClass desired List to wrap
	 */
	public ConstrainedList(ConstraintType constraintType, Class listClass) {
		this(Arrays.asList(new ConstraintType[]{constraintType}), listClass);
	}
	
	/**
	 * Creates a List instance of listClass with constraints defined by constraintTypes
	 * @param constraintTypes constraint types to apply
	 * @param listClass desired List to wrap
	 */
	public ConstrainedList(List<ConstraintType> constraintTypes, Class<? extends List> listClass) {
		if (constraintTypes == null)
			throw new IllegalArgumentException("At least one constraint type has to be set.");
		
		if (listClass == null)
			throw new IllegalArgumentException("List class cannot be null.");

		this.constraintTypes = new ArrayList<ConstraintType>(constraintTypes); // who knows what the user passes
		this.listClass = listClass;
		
		instantiateList();
		
	}
	
	/**
	 * Enables or disables throwing the IllegalArgumentException
	 * on constraint violation.
	 * 
	 * @param throwException
	 */
	public void setThrowException(boolean throwException) {
		this.throwException = throwException;
	}
	
	/**
	 * @return TRUE if the exception will be thrown on constraint violation, FALSE otherwise
	 */
	public boolean isThrowException() {
		return throwException;
	}
	
	protected void handleConstraintViolation(String exceptionMessage) {
		if (throwException)
			throw new IllegalArgumentException(exceptionMessage);
	}
	
	@SuppressWarnings("unchecked")
	private void instantiateList() {
		Constructor<?>[] ctors = listClass.getDeclaredConstructors();
		Constructor<?> ctor = null;
		
		for (int i = 0; i < ctors.length; i++) {
		    ctor = ctors[i];
		    if (ctor.getGenericParameterTypes().length == 0)
			break;
		}
		
		try {
		    ctor.setAccessible(true);
	 	    wrappedList = (List<T>)ctor.newInstance();
		}
		catch (Exception e) {
			throw new IllegalStateException("Unable to instantiate wrapped List by class '" 
					+ listClass.getName() + "'. Nested exception is: " + e.getMessage());
		}
	}

	protected boolean checkConstraintViolation(T e) {
		boolean passed = true;
		String exceptionMessage = "";
			
			for (ConstraintType ct : constraintTypes) {
				switch(ct) {
					case DENY_NULL_VALUES:
						if (e == null)
							passed = false;
						break;
				}
			}
		
		if (!passed)
			handleConstraintViolation(exceptionMessage);
			
		return passed;
	}
	
	/* List's methods */
	
	//@Override
	public boolean add(T e) {
		if (checkConstraintViolation(e))
			return wrappedList.add(e);
		else
			return false;
	}

	//@Override
	public void add(int index, T element) {
		if (checkConstraintViolation(element))
			wrappedList.add(index, element);
	}

	//@Override
	public boolean addAll(Collection<? extends T> c) {
		boolean passed = true;
		for (T e : c) {
			if (!checkConstraintViolation(e))
				passed = false;
		}
		return !passed ? false : wrappedList.addAll(c);
	}

	//@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		boolean passed = true;
		for (T e : c) {
			if (!checkConstraintViolation(e))
				passed = false;
		}
		return !passed ? false : wrappedList.addAll(index, c);
	}

	//@Override
	public void clear() {
		wrappedList.clear();
	}

	//@Override
	public boolean contains(Object o) {
		return wrappedList.contains(o);
	}

	//@Override
	public boolean containsAll(Collection<?> c) {
		return wrappedList.containsAll(c);
	}

	//@Override
	public T get(int index) {
		return wrappedList.get(index);
	}

	//@Override
	public int indexOf(Object o) {
		return wrappedList.indexOf(o);
	}

	//@Override
	public boolean isEmpty() {
		return wrappedList.isEmpty();
	}

	//@Override
	public Iterator<T> iterator() {
		return wrappedList.iterator();
	}

	//@Override
	public int lastIndexOf(Object o) {
		return wrappedList.lastIndexOf(o);
	}

	//@Override
	public ListIterator<T> listIterator() {
		
		// TODO: and146: there is a set(e) method on ListIterator 
		// that allows to pass the item to the list - treat it 
		// and remove throwing the exception
		
		throw new java.lang.UnsupportedOperationException("This method is currently not supported.");

	}

	//@Override
	public ListIterator<T> listIterator(int index) {
		// TODO: and146: there is a set(e) method on ListIterator 
		// that allows to pass the item to the list - treat it 
		// and remove throwing the exception
				
		throw new java.lang.UnsupportedOperationException("This method is currently not supported.");
	}

	//@Override
	public boolean remove(Object o) {
		return wrappedList.remove(o);
	}

	//@Override
	public T remove(int index) {
		return wrappedList.remove(index);
	}

	//@Override
	public boolean removeAll(Collection<?> c) {
		return wrappedList.removeAll(c);
	}

	//@Override
	public boolean retainAll(Collection<?> c) {
		return wrappedList.retainAll(c);
	}

	//@Override
	public T set(int index, T element) {
		if (!checkConstraintViolation(element))
			return null;
		else
			return wrappedList.set(index, element);
	}

	//@Override
	public int size() {
		return wrappedList.size();
	}

	//@Override
	public List<T> subList(int fromIndex, int toIndex) {
		return wrappedList.subList(fromIndex, toIndex);
	}

	//@Override
	public Object[] toArray() {
		return wrappedList.toArray();
	}

	//@Override
	public <T> T[] toArray(T[] a) {
		return wrappedList.toArray(a);
	}
}
