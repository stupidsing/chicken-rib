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
import suite.fs.KeyDataStore;
import suite.fs.KeyDataStoreMutator;
import suite.fs.KeyValueStore;
import suite.os.FileUtil;
import suite.streamlet.Outlet;
import suite.util.FunUtil.Source;
import suite.util.Serialize;
import suite.util.Serialize.Serializer;
import suite.util.To;
import suite.util.Util;

public class IbTreeTest {

	private int pageSize = 4096;

	@Test
	public void testSimple() throws IOException {
		IbTreeConfiguration<Integer> config = createIbTreeConfiguration("ibTree-stack", Serialize.int_);
		config.setCapacity(65536);

		try (IbTreeStack<Integer> ibTreeStack = new IbTreeStack<>(config)) {
			IbTree<Integer> ibTree = ibTreeStack.getIbTree();
			ibTree.create().end(true);
			KeyDataStoreMutator<Integer> mutator = ibTree.begin();
			KeyValueStore<Integer, Integer> store = mutator.store();

			for (int i = 0; i < 32; i++)
				store.put(i, i);

			// mutator.dump(System.out);

			System.out.println(To.list(store.keys(3, 10)));
		}
	}

	@Test
	public void testSingleLevel() throws IOException {
		IbTreeConfiguration<Integer> config = createIbTreeConfiguration("ibTree-single", Serialize.int_);
		IbTreeBuilder builder = new IbTreeBuilder(config);

		try (IbTree<Integer> ibTree = builder.buildTree(FileUtil.tmp.resolve("ibTree-single"), config, null)) {
			ibTree.create().end(true);

			KeyDataStoreMutator<Integer> mutator = ibTree.begin();
			KeyValueStore<Integer, Integer> store = mutator.store();
			KeyDataStore<Integer> dataStore = mutator.dataStore();
			int size = ibTree.guaranteedCapacity();

			for (int i = 0; i < size; i++)
				dataStore.putTerminal(i);
			for (int i = size - 1; 0 <= i; i--)
				store.remove(i);
			for (int i = 0; i < size; i++)
				dataStore.putTerminal(i);

			assertEquals(size, dumpAndCount(mutator));
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		IbTreeConfiguration<String> config = createIbTreeConfiguration("ibTree-multi", Serialize.string(16));
		IbTreeBuilder builder = new IbTreeBuilder(config);

		int i = 0;
		Path p0 = FileUtil.tmp.resolve("ibTreeMulti" + i++);
		Path p1 = FileUtil.tmp.resolve("ibTreeMulti" + i++);
		Path p2 = FileUtil.tmp.resolve("ibTreeMulti" + i++);

		try (IbTreeImpl<Integer> ibTree0 = builder.buildAllocationIbTree(p0);
				IbTreeImpl<Integer> ibTree1 = builder.buildAllocationIbTree(p1, ibTree0);
				IbTree<String> ibTree2 = builder.buildTree(p2, config, ibTree1)) {
			test(ibTree2);
		}
	}

	@Test
	public void testStack() throws IOException {
		IbTreeConfiguration<String> config = createIbTreeConfiguration("ibTree-stack", Serialize.string(16));
		config.setCapacity(65536);

		try (IbTreeStack<String> ibTreeStack = new IbTreeStack<>(config)) {
			test(ibTreeStack.getIbTree());
		}
	}

	private int dumpAndCount(KeyDataStoreMutator<?> mutator) {
		Source<?> source = mutator.store().keys(null, null).source();
		Object object;
		int count = 0;

		while ((object = source.source()) != null) {
			System.out.println(object.toString());
			count++;
		}

		return count;
	}

	private <Key extends Comparable<? super Key>> IbTreeConfiguration<Key> createIbTreeConfiguration( //
			String name, Serializer<Key> serializer) {
		IbTreeConfiguration<Key> config = new IbTreeConfiguration<>();
		config.setComparator(Util.<Key> comparator());
		config.setPathPrefix(FileUtil.tmp.resolve(name));
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

		for (Outlet<String> subset : Outlet.from(list).chunk(25)) {
			KeyDataStoreMutator<String> mutator0 = ibTree.begin();
			KeyDataStore<String> dataStore0 = mutator0.dataStore();
			for (String s : subset)
				dataStore0.putTerminal(s);
			mutator0.end(true);
		}

		assertEquals(size, dumpAndCount(ibTree.begin()));

		Collections.shuffle(list);

		for (List<String> subset : Util.splitn(list, 25)) {
			KeyDataStoreMutator<String> mutator1 = ibTree.begin();
			KeyValueStore<String, Integer> store1 = mutator1.store();
			for (String s : subset)
				store1.remove(s);
			mutator1.end(true);
		}

		assertEquals(0, dumpAndCount(ibTree.begin()));
	}

}
