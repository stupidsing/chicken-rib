package chickenrib.fs;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import chickenrib.btree.impl.PbTreeConfiguration;
import chickenrib.fs.impl.PbTreeFileSystemImpl;
import primal.Nouns.Utf8;
import suite.cfg.Defaults;
import suite.fs.FileSystem;
import suite.fs.FileSystemMutator;
import suite.os.FileUtil;
import suite.primitive.Bytes;
import suite.streamlet.As;
import suite.streamlet.Streamlet;
import suite.util.Copy;
import suite.util.To;

public class FileSystemTest {

	private int pageSize = 4096;

	private interface TestCase {
		public void test(FileSystem fs) throws IOException;
	}

	@Test
	public void testIbTreeFileSystem0() throws IOException {
		testIbTree(Defaults.tmp.resolve("pbTree-fs0"), true, this::testWriteOneFile);
	}

	// Writing too many files (testWriteFiles1) would fail this test case. Do
	// not know why.
	@Test
	public void testIbTreeFileSystem1() throws IOException {
		testIbTree(Defaults.tmp.resolve("pbTree-fs1"), true, this::testWriteFiles);
		testIbTree(Defaults.tmp.resolve("pbTree-fs1"), false, this::testReadFile);
	}

	private void testIbTree(Path path, boolean isNew, TestCase testCase) throws IOException {
		PbTreeConfiguration<Bytes> config = new PbTreeConfiguration<>();
		config.setPathPrefix(path);
		config.setPageSize(pageSize);
		config.setMaxBranchFactor(pageSize / 64);
		config.setCapacity(64 * 1024);

		try (FileSystem fs = new PbTreeFileSystemImpl(config, isNew)) {
			testCase.test(fs);
		}
	}

	private void testWriteOneFile(FileSystem fs) {
		Bytes filename = To.bytes("file");
		Bytes data = To.bytes("data");
		FileSystemMutator fsm = fs.mutate();

		fsm.replace(filename, data);
		assertEquals(1, fsm.list(filename, null).size());
		assertEquals(data, fsm.read(filename));

		fsm.replace(filename, null);
		assertEquals(0, fsm.list(filename, null).size());
	}

	private void testWriteFiles(FileSystem fs) throws IOException {
		Streamlet<Path> paths = FileUtil.findPaths(Paths.get("src/test/java/chickenrib/fs/"));
		FileSystemMutator fsm = fs.mutate();

		for (Path path : paths) {
			String filename = path.toString().replace(File.separatorChar, '/');
			Bytes name = Bytes.of(filename.getBytes(Utf8.charset));
			fsm.replace(name, Bytes.of(Files.readAllBytes(path)));
		}
	}

	private void testReadFile(FileSystem fs) throws IOException {
		String filename = "src/test/java/chickenrib/fs/FileSystemTest.java";
		FileSystemMutator fsm = fs.mutate();
		Bytes name = Bytes.of(filename.getBytes(Utf8.charset));
		Copy.stream(fsm.read(name).collect(As::inputStream), System.out);
	}

}
