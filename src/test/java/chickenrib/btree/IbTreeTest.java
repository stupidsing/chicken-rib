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
import suite.cfg.Defaults;
import suite.fs.KeyDataMutator;
import suite.fs.KeyDataStore;
import suite.fs.KeyValueMutator;
import suite.node.util.Singleton;
import suite.object.Object_;
import suite.serialize.Serialize;
import suite.serialize.Serialize.Serializer;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.Outlet;
import suite.util.List_;
import suite.util.To;

public class IbTreeTest {

	private Serialize serialize = Singleton.me.serialize;
	private int pageSize = 4096;

	@Test
	public void testSimple() throws IOException {
		IbTreeConfiguration<Integer> config = newIbTreeConfiguration("ibTree-stack", serialize.int_);
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
		IbTreeConfiguration<Integer> config = newIbTreeConfiguration("ibTree-single", serialize.int_);
		IbTreeBuilder builder = new IbTreeBuilder(config);

		try (IbTree<Integer> ibTree = builder.buildTree(Defaults.tmp.resolve("ibTree-single"), config, null)) {
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
		IbTreeConfiguration<String> config = newIbTreeConfiguration("ibTree-multi", serialize.string(16));
		IbTreeBuilder builder = new IbTreeBuilder(config);

		int i = 0;
		Path p0 = Defaults.tmp.resolve("ibTreeMulti" + i++);
		Path p1 = Defaults.tmp.resolve("ibTreeMulti" + i++);
		Path p2 = Defaults.tmp.resolve("ibTreeMulti" + i++);

		try (IbTreeImpl<Integer> ibTree0 = builder.buildAllocationIbTree(p0);
				IbTreeImpl<Integer> ibTree1 = builder.buildAllocationIbTree(p1, ibTree0);
				IbTree<String> ibTree2 = builder.buildTree(p2, config, ibTree1)) {
			test(ibTree2);
		}
	}

	@Test
	public void testStack() throws IOException {
		IbTreeConfiguration<String> config = newIbTreeConfiguration("ibTree-stack", serialize.string(16));
		config.setCapacity(65536);

		try (IbTreeStack<String> ibTreeStack = new IbTreeStack<>(config)) {
			test(ibTreeStack.getIbTree());
		}
	}

	private <Key extends Comparable<? super Key>> IbTreeConfiguration<Key> newIbTreeConfiguration( //
			String name, Serializer<Key> serializer) {
		IbTreeConfiguration<Key> config = new IbTreeConfiguration<>();
		config.setComparator(Object_::compare);
		config.setPathPrefix(Defaults.tmp.resolve(name));
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

		while ((object = source.g()) != null) {
			System.out.println(object.toString());
			count++;
		}

		return count;
	}

}