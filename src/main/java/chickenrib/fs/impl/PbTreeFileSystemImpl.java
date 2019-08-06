package chickenrib.fs.impl;

import java.io.IOException;

import chickenrib.btree.impl.PbTreeConfiguration;
import chickenrib.btree.impl.PbTreeStack;
import primal.primitive.adt.Bytes;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.fs.impl.FileSystemKeyUtil;
import suite.fs.impl.FileSystemMutatorImpl;

public class PbTreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();

	private PbTreeStack<Bytes> pbTreeStack;
	private FileSystemMutator mutator;

	public PbTreeFileSystemImpl(PbTreeConfiguration<Bytes> config, boolean isNew) {
		config.setComparator(Bytes.comparator);
		config.setSerializer(keyUtil.serializer());
		pbTreeStack = new PbTreeStack<>(config);
		mutator = new FileSystemMutatorImpl(keyUtil, pbTreeStack.getIbTree()::begin);

		if (isNew)
			pbTreeStack.getIbTree().create().end(true);
	}

	@Override
	public void close() throws IOException {
		pbTreeStack.close();
	}

	@Override
	public FileSystemMutator mutate() {
		return mutator;
	}

}
