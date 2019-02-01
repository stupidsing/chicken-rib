package chickenrib.btree.impl;

import java.nio.file.Path;

import suite.object.Object_;

public class PbTreeBuilder {

	private PbTreeConfiguration<Integer> allocationIbTreeConfig;

	public PbTreeBuilder(PbTreeConfiguration<?> config) {
		allocationIbTreeConfig = new PbTreeConfiguration<Integer>();
		allocationIbTreeConfig.setPageSize(config.getPageSize());
		allocationIbTreeConfig.setMaxBranchFactor(config.getMaxBranchFactor());
		allocationIbTreeConfig.setComparator(Object_::compare);
		allocationIbTreeConfig.setSerializer(PbTreeImpl.pointerSerializer);
	}

	/**
	 * Builds a small tree that would not span more than 1 page, i.e. no extra
	 * "page allocation tree" is required.
	 */
	public PbTreeImpl<Integer> buildAllocationIbTree(Path path) {
		return buildAllocationIbTree(path, null);
	}

	/**
	 * Builds an intermediate tree that is supported by a separate page
	 * allocation tree.
	 */
	public PbTreeImpl<Integer> buildAllocationIbTree(Path path, PbTreeImpl<Integer> allocationIbTree) {
		return buildTree(path, allocationIbTreeConfig, allocationIbTree);
	}

	public <Key> PbTreeImpl<Key> buildTree(Path path, PbTreeConfiguration<Key> config, PbTreeImpl<Integer> allocationIbTree) {
		return new PbTreeImpl<Key>(path, config, allocationIbTree);
	}

}
