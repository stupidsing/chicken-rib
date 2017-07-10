package chickenrib.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import chickenrib.btree.impl.IbTreeBuilder;
import chickenrib.btree.impl.IbTreeConfiguration;
import chickenrib.btree.impl.IbTreeImpl;
import chickenrib.btree.impl.IbTreeStack;
import suite.Constants;
import suite.fs.KeyDataMutator;
import suite.fs.KeyDataStore;
import suite.fs.KeyValueMutator;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Source;
import suite.util.List_;
import suite.util.Object_;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.To;

public class IbTreeTest {

	private int pageSize = 4096;

	@Test
	public void testSimple() throws IOException {
		IbTreeConfiguration<Integer> config = newIbTreeConfiguration("ibTree-stack", Serialize.int_);
		config.setCapacity(65536);

		try (IbTreeStack<Integer> ibTreeStack = new IbTreeStack<>(config)) {
			IbTree<Integer> ibTree = ibTreeStack.getIbTree();
			ibTree.create().end(true);
			KeyDataStore<Integer> store = ibTree.begin();
			KeyValueMutator<Integer, Integer> mutator = store.mutate();

			for (int i = 0; i < 32; i++)
				mutator.put(i, i);

			// mutator.dump(System.out);

			System.out.println(To.list(mutator.keys(3, 10)));
		}
	}

	@Test
	public void testSingleLevel() throws IOException {
		IbTreeConfiguration<Integer> config = newIbTreeConfiguration("ibTree-single", Serialize.int_);
		IbTreeBuilder builder = new IbTreeBuilder(config);

		try (IbTree<Integer> ibTree = builder.buildTree(Constants.tmp.resolve("ibTree-single"), config, null)) {
			ibTree.create().end(true);

			KeyDataStore<Integer> store = ibTree.begin();
			KeyValueMutator<Integer, Integer> kvm = store.mutate();
			KeyDataMutator<Integer> kdm = store.mutateData();
			int size = ibTree.guaranteedCapacity();

			for (int i = 0; i < size; i++)
				kdm.putTerminal(i);
			for (int i = size - 1; 0 <= i; i--)
				kvm.remove(i);
			for (int i = 0; i < size; i++)
				kdm.putTerminal(i);

			assertEquals(size, dumpAndCount(store));
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		IbTreeConfiguration<String> config = newIbTreeConfiguration("ibTree-multi", Serialize.string(16));
		IbTreeBuilder builder = new IbTreeBuilder(config);

		int i = 0;
		Path p0 = Constants.tmp.resolve("ibTreeMulti" + i++);
		Path p1 = Constants.tmp.resolve("ibTreeMulti" + i++);
		Path p2 = Constants.tmp.resolve("ibTreeMulti" + i++);

		try (IbTreeImpl<Integer> ibTree0 = builder.buildAllocationIbTree(p0);
				IbTreeImpl<Integer> ibTree1 = builder.buildAllocationIbTree(p1, ibTree0);
				IbTree<String> ibTree2 = builder.buildTree(p2, config, ibTree1)) {
			test(ibTree2);
		}
	}

	@Test
	public void testStack() throws IOException {
		IbTreeConfiguration<String> config = newIbTreeConfiguration("ibTree-stack", Serialize.string(16));
		config.setCapacity(65536);

		try (IbTreeStack<String> ibTreeStack = new IbTreeStack<>(config)) {
			test(ibTreeStack.getIbTree());
		}
	}

	private <Key extends Comparable<? super Key>> IbTreeConfiguration<Key> newIbTreeConfiguration( //
			String name, Serializer<Key> serializer) {
		IbTreeConfiguration<Key> config = new IbTreeConfiguration<>();
		config.setComparator(Object_.<Key> comparator());
		config.setPathPrefix(Constants.tmp.resolve(name));
		config.setPageSize(pageSize);
		config.setSerializer(serializer);
		config.setMaxBranchFactor(16);
		return config;
	}

	private void test(IbTree<String> ibTree) {
		ibTree.create().end(true);

		int size = ibTree.guaranteedCapacity();

		List<String> list = new ArrayList<>();
		for (int k = 0; k < size; k++)
			list.add("KEY-" + To.hex4(k));

		Collections.shuffle(list);

		// During each mutation, some new pages are required before old
		// pages can be discarded during commit. If we update too much data,
		// we would run out of allocatable pages. Here we limit ourself to
		// updating 25 keys each.

		for (Outlet<String> subset : Outlet.of(list).chunk(25)) {
			KeyDataStore<String> store = ibTree.begin();
			KeyDataMutator<String> mutator = store.mutateData();
			for (String s : subset)
				mutator.putTerminal(s);
			store.end(true);
		}

		assertEquals(size, dumpAndCount(ibTree.begin()));

		Collections.shuffle(list);

		for (List<String> subset : List_.chunk(list, 25)) {
			KeyDataStore<String> store = ibTree.begin();
			KeyValueMutator<String, Integer> mutator = store.mutate();
			for (String s : subset)
				mutator.remove(s);
			store.end(true);
		}

		assertEquals(0, dumpAndCount(ibTree.begin()));
	}

	private int dumpAndCount(KeyDataStore<?> store) {
		Source<?> source = store.mutate().keys(null, null).source();
		Object object;
		int count = 0;

		while ((object = source.source()) != null) {
			System.out.println(object.toString());
			count++;
		}

		return count;
	}

}
