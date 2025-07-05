/*
 * This file is part of Invision Archive Tools (IAT).
 *
 * Copyright (C) 2025 Mark Fisher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.urbanelf.iat.util;

import java.util.ArrayList;

/** A pool of objects that can be reused to avoid allocation.
 * @author Nathan Sweet */
abstract public class Pool<T> {
    /** The maximum number of objects that will be pooled. */
    public final int max;
    /** The highest number of free objects. Can be reset any time. */
    public int peak;

    private final ArrayList<T> freeObjects;

    /** Creates a pool with an initial capacity of 16 and no maximum. */
    public Pool () {
        this(16, Integer.MAX_VALUE);
    }

    /** Creates a pool with the specified initial capacity and no maximum. */
    public Pool (int initialCapacity) {
        this(initialCapacity, Integer.MAX_VALUE);
    }

    /** @param initialCapacity The initial size of the array supporting the pool. No objects are created/pre-allocated. Use
     *           {@link #fill(int)} after instantiation if needed.
     * @param max The maximum number of free objects to store in this pool. */
    public Pool (int initialCapacity, int max) {
        freeObjects = new ArrayList<>(initialCapacity);
        this.max = max;
    }

    abstract protected T newObject ();

    /** Returns an object from this pool. The object may be new (from {@link #newObject()}) or reused (previously
     * {@link #free(Object) freed}). */
    public T obtain () {
        return freeObjects.isEmpty() ? newObject() : freeObjects.removeLast();
    }

    /** Puts the specified object in the pool, making it eligible to be returned by {@link #obtain()}. If the pool already contains
     * {@link #max} free objects, the specified object is {@link #discard(Object) discarded}, it is not reset and not added to the
     * pool.
     * <p>
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times. */
    public void free (T object) {
        if (object == null) throw new IllegalArgumentException("object cannot be null.");
        if (freeObjects.size() < max) {
            freeObjects.add(object);
            peak = Math.max(peak, freeObjects.size());
            reset(object);
        } else
            discard(object);
    }

    /** Adds the specified number of new free objects to the pool. Usually called early on as a pre-allocation mechanism but can be
     * used at any time.
     *
     * @param size the number of objects to be added */
    public void fill (int size) {
        for (int i = 0; i < size; i++)
            if (freeObjects.size() < max) freeObjects.add(newObject());
        peak = Math.max(peak, freeObjects.size());
    }

    /** Called when an object is freed to clear the state of the object for possible later reuse. The default implementation calls
     * {@link Poolable#reset()} if the object is {@link Poolable}. */
    protected void reset (T object) {
        if (object instanceof Poolable) ((Poolable)object).reset();
    }

    /** Called when an object is discarded. This is the case when an object is freed, but the maximum capacity of the pool is
     * reached, and when the pool is {@link #clear() cleared} */
    protected void discard (T object) {
        reset(object);
    }

    /** Puts the specified objects in the pool. Null objects within the array are silently ignored.
     * <p>
     * The pool does not check if an object is already freed, so the same object must not be freed multiple times.
     * @see #free(Object) */
    public void freeAll (ArrayList<T> objects) {
        if (objects == null) throw new IllegalArgumentException("objects cannot be null.");
        ArrayList<T> freeObjects = this.freeObjects;
        int max = this.max;
        for (int i = 0, n = objects.size(); i < n; i++) {
            T object = objects.get(i);
            if (object == null) continue;
            if (freeObjects.size() < max) {
                freeObjects.add(object);
                reset(object);
            } else {
                discard(object);
            }
        }
        peak = Math.max(peak, freeObjects.size());
    }

    /** Removes and discards all free objects from this pool. */
    public void clear () {
        ArrayList<T> freeObjects = this.freeObjects;
        for (int i = 0, n = freeObjects.size(); i < n; i++)
            discard(freeObjects.get(i));
        freeObjects.clear();
    }

    /** The number of objects available to be obtained. */
    public int getFree () {
        return freeObjects.size();
    }

    /** Objects implementing this interface will have {@link #reset()} called when passed to {@link Pool#free(Object)}. */
    static public interface Poolable {
        /** Resets the object for reuse. Object references should be nulled and fields may be set to default values. */
        public void reset ();
    }
}