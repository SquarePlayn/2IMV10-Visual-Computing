package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.point_store;


import lombok.RequiredArgsConstructor;
import lombok.Setter;
import nl.tue.visualcomputingproject.group9a.project.common.util.FunctionIterator;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;
import org.joml.Vector3d;

import java.util.*;

/**
 * Data class storing a point with it's index in the vertex buffer.
 */
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PointIndex
		implements Iterable<Pair<Vector3d, Integer>> {
	private Object data;
	
	public class SingleNonNullIterator
			implements Iterator<Pair<Vector3d, Integer>> {
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
		public Pair<Vector3d, Integer> next() {
			if (!hasNext()) throw new NoSuchElementException();
			hasNext = false;
			return (Pair<Vector3d, Integer>) data;
		}
		
		@Override
		public void remove() {
			data = null;
		}
		
	}
	
	
	public PointIndex(Vector3d vec) {
		data = new Pair<Vector3d, Integer>(vec, null);
	}
	
	public void add(Vector3d vec, Integer index) {
		if (data == null) {
			data = new Pair<>(vec, index);
			return;
		}
		
		if (data instanceof Pair) {
			List<Pair<Vector3d, Integer>> list = new ArrayList<>(2);
			list.add((Pair<Vector3d, Integer>) data);
			data = list;
		}
		((List<Pair<Vector3d, Integer>>) data).add(new Pair<>(vec, index));
	}
	
	public void addVector(Vector3d vec) {
		add(vec, null);
	}
	
	public int size() {
		if (data == null) {
			return 0;
		} else if (data instanceof Pair) {
			return 1;
		} else if (data instanceof List) {
			return ((List<Pair<Vector3d, Integer>>) data).size();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Pair<Vector3d, Integer> get(int index) {
		if (data == null) {
			throw new IndexOutOfBoundsException("Has no element, but got: " + index);
		} else if (data instanceof Pair) {
			if (index != 0) throw new IndexOutOfBoundsException("Expected 0, but got: " + index);
			return (Pair<Vector3d, Integer>) data;
		} else if (data instanceof List) {
			return ((List<Pair<Vector3d, Integer>>) data).get(index);
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Pair<Vector3d, Integer> back() {
		if (data == null) {
			throw new IndexOutOfBoundsException("Has no element!");
		} else if (data instanceof Pair) {
			return (Pair<Vector3d, Integer>) data;
		} else if (data instanceof List) {
			List<Pair<Vector3d, Integer>> list = (List<Pair<Vector3d, Integer>>) data;
			return list.get(list.size() - 1);
		} else {
			throw new IllegalStateException();
		}
	}
	
	public void sort(Comparator<? super Pair<Vector3d, Integer>> cmp) {
		if (data instanceof List) {
			((List<Pair<Vector3d, Integer>>) data).sort(cmp);
		}
	}

	public String toString() {
		return "[" + data + "]";
	}
	
	@Override
	public Iterator<Pair<Vector3d, Integer>> iterator() {
		if (data == null || data instanceof Pair) {
			return new SingleNonNullIterator();
		} else if (data instanceof List) {
			return ((List<Pair<Vector3d, Integer>>) data).iterator();
		} else {
			throw new IllegalStateException();
		}
	}
	
	public Iterator<Vector3d> vertexIterator() {
		return new FunctionIterator<>(iterator(), Pair::getFirst);
	}
	
	public Iterator<Integer> indexIterator() {
		return new FunctionIterator<>(iterator(), Pair::getSecond);
	}
	
	
}
