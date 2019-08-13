package chickenrib.btree;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import chickenrib.btree.impl.PbTreeBuilder;
import chickenrib.btree.impl.PbTreeConfiguration;
import chickenrib.btree.impl.PbTreeStack;
import primal.MoreVerbs.Split;
import primal.Nouns.Tmp;
import primal.Verbs.Compare;
import primal.Verbs.Format;
import primal.puller.Puller;
import suite.fs.KeyDataStore;
import suite.node.util.Singleton;
import suite.serialize.Serialize;
import suite.serialize.Serialize.Serializer;

public class PbTreeTest {

	private Serialize serialize = Singleton.me.serialize;
	private int pageSize = 4096;

	@Test
	public void testSimple() throws IOException {
		var config = newIbTreeConfiguration("pbTree-stack", serialize.int_);
		config.setCapacity(65536);

		try (var pbTreeStack = new PbTreeStack<>(config)) {
			var pbTree = pbTreeStack.getIbTree();
			pbTree.create().end(true);
			var store = pbTree.begin();
			var mutator = store.mutate();

			for (int i = 0; i < 32; i++)
				mutator.put(i, i);

			// mutator.dump(System.out);

			System.out.println(mutator.keys(3, 10).toList());
		}
	}

	@Test
	public void testSingleLevel() throws IOException {
		var config = newIbTreeConfiguration("pbTree-single", serialize.int_);
		var builder = new PbTreeBuilder(config);

		try (var pbTree = builder.buildTree(Tmp.path("pbTree-single"), config, null)) {
			pbTree.create().end(true);

			var store = pbTree.begin();
			var kvm = store.mutate();
			var kdm = store.mutateData();
			int size = pbTree.guaranteedCapacity();

			for (var i = 0; i < size; i++)
				kdm.putTerminal(i);
			for (var i = size - 1; 0 <= i; i--)
				kvm.remove(i);
			for (var i = 0; i < size; i++)
				kdm.putTerminal(i);

			assertEquals(size, dumpAndCount(store));
		}
	}

	@Test
	public void testMultipleLevels() throws IOException {
		var config = newIbTreeConfiguration("pbTree-multi", serialize.string(16));
		var builder = new PbTreeBuilder(config);

		int i = 0;
		var p0 = Tmp.path("pbTreeMulti" + i++);
		var p1 = Tmp.path("pbTreeMulti" + i++);
		var p2 = Tmp.path("pbTreeMulti" + i++);

		try (var pbTree0 = builder.buildAllocationIbTree(p0);
				var pbTree1 = builder.buildAllocationIbTree(p1, pbTree0);
				PbTree<String> pbTree2 = builder.buildTree(p2, config, pbTree1)) {
			test(pbTree2);
		}
	}

	@Test
	public void testStack() throws IOException {
		var config = newIbTreeConfiguration("pbTree-stack", serialize.string(16));
		config.setCapacity(65536);

		try (var pbTreeStack = new PbTreeStack<String>(config)) {
			test(pbTreeStack.getIbTree());
		}
	}

	private <Key extends Comparable<? super Key>> PbTreeConfiguration<Key> newIbTreeConfiguration( //
			String name, Serializer<Key> serializer) {
		var config = new PbTreeConfiguration<Key>();
		config.setComparator(Compare::objects);
		config.setPathPrefix(Tmp.path(name));
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
			list.add("KEY-" + Format.hex4(k));

		Collections.shuffle(list);

		// During each mutation, some new pages are required before old
		// pages can be discarded during commit. If we update too much data,
		// we would run out of allocatable pages. Here we limit ourself to
		// updating 25 keys each.

		for (var subset : Puller.of(list).chunk(25)) {
			var store = pbTree.begin();
			var mutator = store.mutateData();
			for (var s : subset)
				mutator.putTerminal(s);
			store.end(true);
		}

		assertEquals(size, dumpAndCount(pbTree.begin()));

		Collections.shuffle(list);

		for (var subset : Split.chunk(list, 25)) {
			var store = pbTree.begin();
			var mutator = store.mutate();
			for (var s : subset)
				mutator.remove(s);
			store.end(true);
		}

		assertEquals(0, dumpAndCount(pbTree.begin()));
	}

	private int dumpAndCount(KeyDataStore<?> store) {
		var source = store.mutate().keys(null, null).source();
		var count = 0;
		Object object;

		while ((object = source.g()) != null) {
			System.out.println(object.toString());
			count++;
		}

		return count;
	}

}
