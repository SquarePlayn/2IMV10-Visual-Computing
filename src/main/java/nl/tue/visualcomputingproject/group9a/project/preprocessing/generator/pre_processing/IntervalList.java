package nl.tue.visualcomputingproject.group9a.project.preprocessing.generator.pre_processing;

import lombok.AllArgsConstructor;
import lombok.Getter;
import nl.tue.visualcomputingproject.group9a.project.common.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class IntervalList {
	private final List<Interval> intervals = new ArrayList<>();
	private boolean sorted = true;
	
	@Getter
	@AllArgsConstructor
	private static class Interval {
		final private double value;
		final private boolean begin;
	}
	
	public void addInterval(double v1, double v2) {
		double min, max;
		if (v1 < v2) {
			min = v1;
			max = v2;
		} else {
			min = v2;
			max = v1;
		}
		intervals.add(new Interval(min, true));
		intervals.add(new Interval(max, false));
		sorted = false;
	}
	
	public int size() {
		return intervals.size() / 2;
	}
	
	private void sort() {
		if (sorted) return;
		// Sort on value, then beginning intervals.
		intervals.sort((i1, i2) -> {
			int cmp = Double.compare(i1.value, i2.value);
			if (cmp != 0) return cmp;
			return Boolean.compare(i1.isBegin(), i2.isBegin());
		});
		sorted = true;
	}
	
	public int countGaps() {
		if (intervals.isEmpty()) return 0;
		sort();
		
		int gaps = -1; // To ignore last infinite gap.
		int open = 0;
		for (Interval i : intervals) {
			if (i.isBegin()) {
				open++;
			} else {
				if (--open == 0) {
					gaps++;
				} else if (open < 0) {
					throw new IllegalStateException();
				}
			}
		}
		if (open != 0) {
			throw new IllegalStateException();
		}
		
		return gaps;
	}
	
	public Pair<Double, Double> getTopInterval() {
		if (intervals.isEmpty()) return null;
		sort();
		
		int open = 1;
		for (int i = intervals.size() - 2; i >= 0; i--) {
			if (intervals.get(i).isBegin()) {
				if (--open == 0) {
					return new Pair<>(
							intervals.get(i).getValue(),
							intervals.get(intervals.size() - 1).getValue()
					);
				} else if (open < 0) {
					throw new IllegalStateException();
				}
			} else {
				open++;
			}
		}
		throw new IllegalStateException();
	}
	
}
