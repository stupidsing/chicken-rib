import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import primal.MoreVerbs.Read;

public class PermuTest {

	@Test
	public void test() {
		var lists = permute(7);
		for (var list : lists)
			System.out.println(list);
		System.out.println(Read.from(lists).filter(this::isFullCycle).size());

	}

	private boolean isFullCycle(List<Integer> list) {
		var size = list.size();
		for (var i = 0; i < size; i++) {
			var f = i;
			for (var c = 1; c < size; c++) {
				f = list.get(f);
				if (f == i)
					return false;
			}
		}
		return true;
	}

	private List<List<Integer>> permute(int size) {
		var list = new ArrayList<Integer>();

		for (var i = 0; i < size; i++)
			list.add(null);

		var results = new ArrayList<List<Integer>>();
		permute(0, list, results);
		return results;
	}

	private void permute(int i, List<Integer> list, List<List<Integer>> results) {
		var size = list.size();
		if (i < size) {
			for (var p = 0; p < size; p++)
				if (list.get(p) == null) {
					list.set(p, i);
					permute(i + 1, list, results);
					list.set(p, null);
				}
		} else
			results.add(new ArrayList<>(list));
	}

}
