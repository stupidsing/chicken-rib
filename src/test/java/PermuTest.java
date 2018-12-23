import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import suite.streamlet.Read;

public class PermuTest {

	@Test
	public void test() {
		List<List<Integer>> lists = permute(7);
		for (List<Integer> list : lists)
			System.out.println(list);
		System.out.println(Read.from(lists).filter(this::isFullCycle).size());

	}

	private boolean isFullCycle(List<Integer> list) {
		int size = list.size();
		for (int i = 0; i < size; i++) {
			int f = i;
			for (int c = 1; c < size; c++) {
				f = list.get(f);
				if (f == i)
					return false;
			}
		}
		return true;
	}

	private List<List<Integer>> permute(int size) {
		List<Integer> list = new ArrayList<>();

		for (int i = 0; i < size; i++)
			list.add(null);

		List<List<Integer>> results = new ArrayList<>();
		permute(0, list, results);
		return results;
	}

	private void permute(int i, List<Integer> list, List<List<Integer>> results) {
		int size = list.size();
		if (i < size) {
			for (int p = 0; p < size; p++)
				if (list.get(p) == null) {
					list.set(p, i);
					permute(i + 1, list, results);
					list.set(p, null);
				}
		} else
			results.add(new ArrayList<>(list));
	}

}
