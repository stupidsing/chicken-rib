package chickenrib.btree.impl;

import java.nio.file.Path;

import suite.util.Util;

public class IbTreeBuilder {

	private IbTreeConfiguration<Integer> allocationIbTreeConfig;

	public IbTreeBuilder(IbTreeConfiguration<?> config) {
		allocationIbTreeConfig = new IbTreeConfiguration<Integer>();
		allocationIbTreeConfig.setPageSize(config.getPageSize());
		allocationIbTreeConfig.setMaxBranchFactor(config.getMaxBranchFactor());
		allocationIbTreeConfig.setComparator(Util.<Integer> comparator());
		allocationIbTreeConfig.setSerializer(IbTreeImpl.pointerSerializer);
	}

	/**
	 * Builds a small tree that would not span more than 1 page, i.e. no extra
	 * "page allocation tree" is required.
	 */
	public IbTreeImpl<Integer> buildAllocationIbTree(Path path) {
		return buildAllocationIbTree(path, null);
	}

	/**
	 * Builds an intermediate tree that is supported by a separate page
	 * allocation tree.
	 */
	public IbTreeImpl<Integer> buildAllocationIbTree(Path path, IbTreeImpl<Integer> allocationIbTree) {
		return buildTree(path, allocationIbTreeConfig, allocationIbTree);
	}

	public <Key> IbTreeImpl<Key> buildTree(Path path, IbTreeConfiguration<Key> config, IbTreeImpl<Integer> allocationIbTree) {
		return new IbTreeImpl<Key>(path, config, allocationIbTree);
	}

}
