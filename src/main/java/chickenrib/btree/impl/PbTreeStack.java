package chickenrib.btree.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import chickenrib.btree.PbTree;
import primal.Verbs.Last;
import primal.fp.Funs.Source;
import suite.os.FileUtil;

public class PbTreeStack<Key> implements Closeable {

	private List<PbTreeImpl<Integer>> allocationIbTrees = new ArrayList<>();
	private PbTree<Key> pbTree;

	public PbTreeStack(PbTreeConfiguration<Key> config) {
		Path pathPrefix = config.getPathPrefix();
		int pageSize = config.getPageSize();
		long capacity = config.getCapacity();
		long nPages = capacity / pageSize;

		PbTreeBuilder builder = new PbTreeBuilder(config);

		int i[] = new int[] { 0, };
		Source<Path> nextPath = () -> FileUtil.ext(pathPrefix, Integer.toString(i[0]++));

		PbTreeImpl<Integer> allocationIbTree;
		allocationIbTrees.add(builder.buildAllocationIbTree(nextPath.g()));

		while ((allocationIbTree = Last.of(allocationIbTrees)).guaranteedCapacity() < nPages)
			allocationIbTrees.add(builder.buildAllocationIbTree(nextPath.g(), allocationIbTree));

		pbTree = builder.buildTree(nextPath.g(), config, allocationIbTree);
	}

	@Override
	public void close() throws IOException {
		pbTree.close();
		ListIterator<PbTreeImpl<Integer>> li = allocationIbTrees.listIterator();
		while (li.hasPrevious())
			li.previous().close();
	}

	public PbTree<Key> getIbTree() {
		return pbTree;
	}

}
