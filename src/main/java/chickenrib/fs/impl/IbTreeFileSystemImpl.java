package chickenrib.fs.impl;

import java.io.IOException;

import chickenrib.btree.impl.IbTreeConfiguration;
import chickenrib.btree.impl.IbTreeStack;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.fs.impl.FileSystemKeyUtil;
import suite.fs.impl.FileSystemMutatorImpl;
import suite.primitive.Bytes;

public class IbTreeFileSystemImpl implements FileSystem {

	private FileSystemKeyUtil keyUtil = new FileSystemKeyUtil();

	private IbTreeStack<Bytes> ibTreeStack;
	private FileSystemMutator mutator;

	public IbTreeFileSystemImpl(IbTreeConfiguration<Bytes> config, boolean isNew) {
		config.setComparator(Bytes.comparator);
		config.setSerializer(keyUtil.serializer());
		ibTreeStack = new IbTreeStack<>(config);
		mutator = new FileSystemMutatorImpl(keyUtil, ibTreeStack.getIbTree()::begin);

		if (isNew)
			ibTreeStack.getIbTree().create().end(true);
	}

	@Override
	public void close() throws IOException {
		ibTreeStack.close();
	}

	@Override
	public FileSystemMutator mutate() {
		return mutator;
	}

}
