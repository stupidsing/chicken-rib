package chickenrib.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import chickenrib.btree.impl.PbTreeBuilder;
import chickenrib.btree.impl.PbTreeConfiguration;
import chickenrib.btree.impl.PbTreeImpl;
import chickenrib.btree.impl.PbTreeStack;
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

public class PbTreeTest {

	private Serialize serialize = Singleton.me.serialize;
	private int pageSize = 4096;

	@Test
	public void testSimple() throws IOException {
		PbTreeConfiguration<Integer> config = newIbTreeConfiguration("pbTree-stack", serialize.int_);
		config.setCapacity(65536);

		try (PbTreeStack<Integer> pbTreeStack = new PbTreeStack<>(config)) {
			PbTree<Integer> pbTree = pbTreeStack.getIbTree();
			pbTree.create().end(true);
			KeyDataStore<Integer> store = pbTree.begin();
			KeyValueMutator<Integer, Integer> mutator = store.mutate();

			for (int i = 0; i < 32; i++)
				mutator.put(i, i);

			// mutator.dump(System.out);

			System.out.println(To.list(mutator.keys(3, 10)));
		}
	}

	@Test
	public void testSingleLevel() throws IOException {
		PbTreeConfiguration<Integer> config = newIbTreeConfiguration("pbTree-single", serialize.int_);
		PbTreeBuilder builder = new PbTreeBuilder(config);

		try (PbTree<Integer> pbTree = builder.buildTree(Defaults.tmp.resolve("pbTree-single"), config, null)) {
			pbTree.create().end(true);

			KeyDataStore<Integer> store = pbTree.begin();
			KeyValueMutator<Integer, Integer> kvm = store.mutate();
			KeyDataMutator<Integer> kdm = store.mutateData();
			int size = pbTree.guaranteedCapacity();

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
		PbTreeConfiguration<String> config = newIbTreeConfiguration("pbTree-multi", serialize.string(16));
		PbTreeBuilder builder = new PbTreeBuilder(config);

		int i = 0;
		Path p0 = Defaults.tmp.resolve("pbTreeMulti" + i++);
		Path p1 = Defaults.tmp.resolve("pbTreeMulti" + i++);
		Path p2 = Defaults.tmp.resolve("pbTreeMulti" + i++);

		try (PbTreeImpl<Integer> pbTree0 = builder.buildAllocationIbTree(p0);
				PbTreeImpl<Integer> pbTree1 = builder.buildAllocationIbTree(p1, pbTree0);
				PbTree<String> pbTree2 = builder.buildTree(p2, config, pbTree1)) {
			test(pbTree2);
		}
	}

	@Test
	public void testStack() throws IOException {
		PbTreeConfiguration<String> config = newIbTreeConfiguration("pbTree-stack", serialize.string(16));
		config.setCapacity(65536);

		try (PbTreeStack<String> pbTreeStack = new PbTreeStack<>(config)) {
			test(pbTreeStack.getIbTree());
		}
	}

	private <Key extends Comparable<? super Key>> PbTreeConfiguration<Key> newIbTreeConfiguration( //
			String name, Serializer<Key> serializer) {
		PbTreeConfiguration<Key> config = new PbTreeConfiguration<>();
		config.setComparator(Object_::compare);
		config.setPathPrefix(Defaults.tmp.resolve(name));
		config.setPageSize(pageSize);
		config.setSerializer(serializer);
		config.setMaxBranchFactor(16);
		return config;
	}

	private void test(PbTree<String> pbTree) {
		pbTree.create().end(true);

		int size = pbTree.guaranteedCapacity();

		List<String> list = new ArrayList<>();
		for (int k = 0; k < size; k++)
			list.add("KEY-" + To.hex4(k));

		Collections.shuffle(list);

		// During each mutation, some new pages are required before old
		// pages can be discarded during commit. If we update too much data,
		// we would run out of allocatable pages. Here we limit ourself to
		// updating 25 keys each.

		for (Outlet<String> subset : Outlet.of(list).chunk(25)) {
			KeyDataStore<String> store = pbTree.begin();
			KeyDataMutator<String> mutator = store.mutateData();
			for (String s : subset)
				mutator.putTerminal(s);
			store.end(true);
		}

		assertEquals(size, dumpAndCount(pbTree.begin()));

		Collections.shuffle(list);

		for (List<String> subset : List_.chunk(list, 25)) {
			KeyDataStore<String> store = pbTree.begin();
			KeyValueMutator<String, Integer> mutator = store.mutate();
			for (String s : subset)
				mutator.remove(s);
			store.end(true);
		}

		assertEquals(0, dumpAndCount(pbTree.begin()));
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
