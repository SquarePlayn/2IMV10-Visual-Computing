/*
 * Copyright 2020 Kaj Wortel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.tue.visualcomputingproject.group9a.project.common.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * This abstract class can be used to easily create a new iterator.
 * The implementer only has to provide a single function which will request the next element,
 * or call the {@link #done()} function when there are no more elements.
 * No elements will be returned from the iterator after the {@link #done()} function has been invoked,
 * even if it is currently executing the {@link #generateNext()} function.
 * 
 * @author Kaj Wortel
 */
public abstract class GeneratorIterator<E>
        implements Iterator<E> {
    
    /* ------------------------------------------------------------------------
     * Variables.
     * ------------------------------------------------------------------------
     */
    /** The element to be generated next. */
    private E nextElem;
    /** Whether the next element has been generated. */
    private boolean generated = false;
    /** Denotes whether there still are elements remaining. */
    private boolean done = false;
    
    
    /* ------------------------------------------------------------------------
     * Functions.
     * ------------------------------------------------------------------------
     */
    @Override
    public boolean hasNext() {
        if (done) return false;
        if (!generated) {
            nextElem = generateNext();
            generated = true;
        }
        return !done;
    }
    
    @Override
    public E next() {
        if (!hasNext()) throw new NoSuchElementException();
        generated = false;
        return nextElem;
    }
    
    /**
     * This function should be called when there are no more elements available.
     * The {@link #generateNext()} function won't be called after calling this function.
     */
    protected final void done() {
        done = true;
    }
    
    /**
     * Generates the next element. If no more elements exist, then the function
     * {@link #done()} should be called.
     * 
     * @return The next element to be returned.
     */
    protected abstract E generateNext();
    
    
}
