package chickenrib.btree.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import chickenrib.btree.IbTree;
import suite.os.FileUtil;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class IbTreeStack<Key> implements Closeable {

	private List<IbTreeImpl<Integer>> allocationIbTrees = new ArrayList<>();
	private IbTree<Key> ibTree;

	public IbTreeStack(IbTreeConfiguration<Key> config) {
		Path pathPrefix = config.getPathPrefix();
		int pageSize = config.getPageSize();
		long capacity = config.getCapacity();
		long nPages = capacity / pageSize;

		IbTreeBuilder builder = new IbTreeBuilder(config);

		int i[] = new int[] { 0, };
		Source<Path> nextPath = () -> FileUtil.ext(pathPrefix, Integer.toString(i[0]++));

		IbTreeImpl<Integer> allocationIbTree;
		allocationIbTrees.add(builder.buildAllocationIbTree(nextPath.source()));

		while ((allocationIbTree = Util.last(allocationIbTrees)).guaranteedCapacity() < nPages)
			allocationIbTrees.add(builder.buildAllocationIbTree(nextPath.source(), allocationIbTree));

		ibTree = builder.buildTree(nextPath.source(), config, allocationIbTree);
	}

	@Override
	public void close() throws IOException {
		ibTree.close();
		ListIterator<IbTreeImpl<Integer>> li = allocationIbTrees.listIterator();
		while (li.hasPrevious())
			li.previous().close();
	}

	public IbTree<Key> getIbTree() {
		return ibTree;
	}

}
