/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/licenses/publicdomain
 */

// Last update Mon Nov  1 07:23:15 2004  Doug Lea  (dl at gee)

package uk.ac.starlink.vo;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * An unbounded LIFO BlockingQueue. Implemented as a simple
 * singly-linked list protected by a ReentrantLock, with a Condition
 * to manage waiting for elements in take().
 */
public class LinkedBlockingStack<E> extends AbstractQueue<E> 
    implements BlockingQueue<E> {

    /** Simple linked list nodes */
    static class Node<E> {
        E item;
        Node next;
        Node(E x, Node n) { item = x; next = n; }
    }

    private Node<E> head;
    private int count;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition cond = lock.newCondition();

    public LinkedBlockingStack() {
    }

    public LinkedBlockingStack(Collection c) {
        addAll(c);
    }

    /** Returns the lock object used for synchronizing this object's state. */
    protected ReentrantLock getLock() {
        return lock;
    }

    /** Insert node at front of list */
    private void insert(E o) {
        head = new Node<E>(o, head);
        ++count;
        cond.signal();
    }
    
    /** Remove node at front of list. Call only when nonempty */
    private E extract() {
        E x = head.item;
        head = head.next;
        --count;
        return x;
    }

    public int size() {
        lock.lock();
        try { 
            return count;
        } finally { lock.unlock(); }
    }        

    public boolean offer(E o) {
        if (o == null) throw new NullPointerException();
        lock.lock();
        try { 
            insert(o);
            return true;
        } finally { lock.unlock(); }
    }

    public void put(E o) {
        offer(o);
    }

    public boolean offer(E o, long t, TimeUnit unit) {
        return offer(o);
    }

    public E peek() {
        lock.lock();
        try { 
            if (count == 0)
                return null;
            return head.item;
        } finally { lock.unlock(); }
    }

    public E take() throws InterruptedException {
        lock.lock();
        try { 
            while (count == 0)
                cond.await();
            return extract();
        } finally { lock.unlock(); }
    }

    public E poll() {
        lock.lock();
        try { 
            if (count == 0)
                return null;
            return extract();
        } finally { lock.unlock(); }
    }

    public E poll(long t, TimeUnit unit) throws InterruptedException {
        long ns = unit.toNanos(t);
        lock.lock();
        try { 
            for (;;) {
                if (count != 0) 
                    return extract();
                if (ns <= 0)
                    return null;
                ns = cond.awaitNanos(ns);
            }
        } finally { lock.unlock(); }
    }

    public int remainingCapacity() { 
        return Integer.MAX_VALUE; 
    }

    public boolean contains(Object o) {
        lock.lock();
        try { 
            for (Node<E> p = head; p != null; p = p.next) 
                if (o.equals(p.item))
                    return true;
            return false;
        } finally { lock.unlock(); }
    }        

    public boolean remove(Object o) {
        lock.lock();
        try { 
            Node<E> trail = null;
            Node<E> p = head;
            while (p != null) {
                Node<E> next = p.next;
                if (o.equals(p.item)) {
                    if (trail == null) 
                        head = next;
                    else
                        trail.next = next;
                    --count;
                    return true;
                }
                trail = p;
                p = next;
            }
            return false;
        } finally { lock.unlock(); }
    }        


    public void clear() {
        lock.lock();
        try { 
            head = null;
            count = 0;
        } finally { lock.unlock(); }
    }        
    

    public int drainTo(Collection c) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        Node<E> p;
        lock.lock();
        try { 
            p = head;
            head = null;
            count = 0;
        } finally { lock.unlock(); }
        int n = 0;
        while (p != null) {
            E x = p.item;
            c.add(x);
            ++n;
            p = p.next;
        }
        return n;
    }

    public int drainTo(Collection c, int max) {
        if (c == null)
            throw new NullPointerException();
        if (c == this)
            throw new IllegalArgumentException();
        int n = 0;
        while (n < max) {
            E x = poll();
            if (x == null)
                break;
            c.add(x);
            ++n;
        }
        return n;
    }

    public Iterator<E> iterator() {
        return new Itr();
    }

    // Utilities needed by iterators

    /** Get next under lock. Needed by iterator */
    Node<E> getNext(Node<E> p) {
        lock.lock();
        try { 
            return p.next;
        } finally { lock.unlock(); }
    }        

    /** Get head of list under lock. Needed by iterator */
    Node<E> getHead() {
        lock.lock();
        try { 
            return head;
        } finally { lock.unlock(); }
    }        

    /** Variant of remove needed by iterator */
    boolean removeNode(Node<E> x) {
        lock.lock();
        try { 
            Node<E> trail = null;
            Node<E> p = head;
            while (p != null) {
                Node<E> next = p.next;
                if (p == x) {
                    if (trail == null) 
                        head = next;
                    else
                        trail.next = next;
                    --count;
                    return true;
                }
                trail = p;
                p = next;
            }
            return false;
        } finally { lock.unlock(); }
    }        

    /** Iterator for LinkedBlockingStack */
    class Itr implements Iterator<E> {
        Node<E> last;
        Node<E> current;
        Node<E> next = getHead();

        public boolean hasNext() {
            if (current != null) 
                return true;
            if ((current = next) == null) 
                return false;
            next = getNext(next);
            return true;
        }

        public E next() {
            if (current == null && !hasNext()) 
                throw new NoSuchElementException();
            last = current;
            current = null;
            return last.item;
        }

        public void remove() {
            if (last == null)
                throw new IllegalStateException();
            removeNode(last);
        }
    }
}
