package nl.tue.visualcomputingproject.group9a.project.common.util;

import lombok.AllArgsConstructor;

import java.util.Iterator;
import java.util.function.Function;

@AllArgsConstructor
public class FunctionIterator<S, T>
		implements Iterator<T> {
	
	private final Iterator<S> it;
	private final Function<S, T> function;

	@Override
	public boolean hasNext() {
		return it.hasNext();
	}

	@Override
	public T next() {
		return function.apply(it.next());
	}

	@Override
	public void remove() {
		it.remove();
	}
	
}
