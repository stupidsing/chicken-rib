package chickenrib.btree.impl;

import java.nio.file.Path;
import java.util.Comparator;

import suite.serialize.Serialize.Serializer;

public class IbTreeConfiguration<Key> {

	private Path pathPrefix;
	private int pageSize;
	private int maxBranchFactor;
	private Comparator<Key> comparator;
	private Serializer<Key> serializer;
	private long capacity;

	public Path getPathPrefix() {
		return pathPrefix;
	}

	public void setPathPrefix(Path pathPrefix) {
		this.pathPrefix = pathPrefix;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getMaxBranchFactor() {
		return maxBranchFactor;
	}

	public void setMaxBranchFactor(int maxBranchFactor) {
		this.maxBranchFactor = maxBranchFactor;
	}

	public Comparator<Key> getComparator() {
		return comparator;
	}

	public void setComparator(Comparator<Key> comparator) {
		this.comparator = comparator;
	}

	public Serializer<Key> getSerializer() {
		return serializer;
	}

	public void setSerializer(Serializer<Key> serializer) {
		this.serializer = serializer;
	}

	public long getCapacity() {
		return capacity;
	}

	public void setCapacity(long capacity) {
		this.capacity = capacity;
	}

}
