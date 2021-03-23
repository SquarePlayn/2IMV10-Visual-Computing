package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;


import nl.tue.visualcomputingproject.group9a.project.common.util.FunctionIterator;

import java.util.*;
import java.util.function.Function;

/**
 * Data class storing a point with it's index in the vertex buffer.
 */
@SuppressWarnings("unchecked")
public class StoreElement<Data extends PointIndexData>
		implements Iterable<Data> {
	private Object data;
	
	public class SingleNonNullIterator
			implements Iterator<Data> {
		boolean hasNext = true;
		
		public SingleNonNullIterator() {
			if (data == null) {
				hasNext = false;
			}
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Data next() {
			if (!hasNext()) throw new NoSuchElementException();
			hasNext = false;
			return (Data) data;
		}
		
		@Override
		public void remove() {
			data = null;
		}
		
	}
	
	public StoreElement() {
		data = null;
	}
	
	public StoreElement(Data data) {
		this.data = data;
	}
	
	public void add(Data newData) {
		if (newData == null) return;
		if (data == null) {
			this.data = newData;
			return;
		}
		
		if (data instanceof PointIndexData) {
			List<Data> list = new ArrayList<>(2);
			list.add((Data) data);
			data = list;
		}
		((List<Data>) data).add(newData);
	}
	
	public int size() {
		if (data == null) {
			return 0;
		} else if (data instanceof PointIndexData) {
			return 1;
		} else if (data instanceof List) {
			return ((List<Data>) data).size();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Data get(int index) {
		if (data == null) {
			throw new IndexOutOfBoundsException("Has no element, but got: " + index);
		} else if (data instanceof PointIndexData) {
			if (index != 0) throw new IndexOutOfBoundsException("Expected 0, but got: " + index);
			return (Data) data;
		} else if (data instanceof List) {
			return ((List<Data>) data).get(index);
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Data back() {
		if (data == null) {
			throw new IndexOutOfBoundsException("Has no element!");
		} else if (data instanceof PointIndexData) {
			return (Data) data;
		} else if (data instanceof List) {
			List<Data> list = (List<Data>) data;
			return list.get(list.size() - 1);
		} else {
			throw new IllegalStateException();
		}
	}
	
	public void sort(Comparator<? super Data> cmp) {
		if (data instanceof List) {
			((List<Data>) data).sort(cmp);
		}
	}

	public String toString() {
		return "[" + data + "]";
	}
	
	@Override
	public Iterator<Data> iterator() {
		if (data == null || data instanceof PointIndexData) {
			return new SingleNonNullIterator();
		} else if (data instanceof List) {
			return ((List<Data>) data).iterator();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public <T> Iterator<T> iterator(Function<Data, T> function) {
		return new FunctionIterator<>(iterator(), function);
	}
	
	
}
