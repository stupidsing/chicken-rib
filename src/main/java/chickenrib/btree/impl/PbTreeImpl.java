package chickenrib.btree.impl;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import chickenrib.btree.PbTree;
import primal.fp.Funs.Fun;
import suite.file.PageFile;
import suite.file.SerializedPageFile;
import suite.file.impl.FileFactory;
import suite.file.impl.SerializedFileFactory;
import suite.fs.KeyDataMutator;
import suite.fs.KeyDataStore;
import suite.fs.KeyValueMutator;
import suite.node.util.Singleton;
import suite.primitive.Bytes;
import suite.serialize.SerInput;
import suite.serialize.SerOutput;
import suite.serialize.Serialize;
import suite.serialize.Serialize.Serializer;
import suite.streamlet.Read;
import suite.streamlet.Streamlet;
import suite.util.List_;

/**
 * Immutable, on-disk B-tree implementation.
 *
 * To allow efficient page management, a large B-tree has one smaller B-tree for
 * storing unused pages, called allocation B-tree. That smaller one might
 * contain a even smaller allocation B-tree, until it becomes small enough to
 * fit in a single disk page.
 *
 * Mutator control is done by a "stamp" consisting of a chain of root page
 * numbers of all B-trees. The holder object persist the stamp into another
 * file.
 *
 * @author ywsing
 */
public class PbTreeImpl<Key> implements PbTree<Key> {

	private static Serialize serialize = Singleton.me.serialize;

	public static Serializer<Integer> pointerSerializer = serialize.nullable(serialize.int_);

	private Path path;
	private int pageSize;
	private Comparator<Key> comparator;
	private Serializer<Key> serializer;
	private Mutate mutate;

	private PageFile pageFile0;
	private SerializedPageFile<Page> pageFile;
	private SerializedPageFile<Bytes> payloadFile;
	private PbTreeImpl<Integer> allocationIbTree;

	private int maxBranchFactor; // Exclusive
	private int minBranchFactor; // Inclusive

	private class Page {
		private List<Slot> slots;

		private Page(List<Slot> slots) {
			this.slots = slots;
		}
	}

	private enum SlotType {
		BRANCH, DATA, TERMINAL,
	}

	/**
	 * In leaves, pointer would be null, and pivot stores the leaf value.
	 *
	 * Pivot would be null at the minimum side of a tree as the guarding key.
	 */
	private class Slot {
		private SlotType type;
		private Key pivot;
		private Integer pointer;

		private Slot(SlotType type, Key pivot, Integer pointer) {
			this.type = type;
			this.pivot = pivot;
			this.pointer = pointer;
		}

		private List<Slot> slots() {
			return type == SlotType.BRANCH ? read(pointer).slots : null;
		}
	}

	private class FindSlot {
		private Slot slot;
		private int i, c;

		private FindSlot(List<Slot> slots, Key key) {
			this(slots, key, true);
		}

		private FindSlot(List<Slot> slots, Key key, boolean isInclusive) {
			i = slots.size();
			while (0 < i)
				if ((c = comparator.compare((slot = slots.get(--i)).pivot, key)) <= 0)
					if (isInclusive || c < 0)
						break;
		}
	}

	private interface Allocator {
		public Integer allocate();

		public void discard(Integer pointer);

		public List<Integer> flush();
	}

	/**
	 * Protect discarded pages belonging to previous mutations, so that they are
	 * not being allocated immediately. This supports immutability (i.e.
	 * copy-on-write) and with this recovery can succeed.
	 *
	 * On the other hand, allocated and discarded pages are reused here, since
	 * they belong to current mutation.
	 */
	private class DelayedDiscardAllocator implements Allocator {
		private Allocator allocator;
		private Set<Integer> allocated = new HashSet<>();
		private Deque<Integer> discarded = new ArrayDeque<>(); // Non-reusable
		private Deque<Integer> allocateDiscarded = new ArrayDeque<>(); // Reusable

		private DelayedDiscardAllocator(Allocator allocator) {
			this.allocator = allocator;
		}

		public Integer allocate() {
			Integer pointer = allocateDiscarded.isEmpty() ? allocator.allocate() : allocateDiscarded.pop();
			allocated.add(pointer);
			return pointer;
		}

		public void discard(Integer pointer) {
			(allocated.remove(pointer) ? allocateDiscarded : discarded).push(pointer);
		}

		public List<Integer> flush() {
			while (!discarded.isEmpty())
				allocator.discard(discarded.pop());
			while (!allocateDiscarded.isEmpty())
				allocator.discard(allocateDiscarded.pop());
			return allocator.flush();
		}
	}

	private class SwappingAllocator implements Allocator {
		private int active;
		private Deque<Integer> deque;

		private SwappingAllocator(int active) {
			reset(active);
		}

		public Integer allocate() {
			return deque.pop();
		}

		public void discard(Integer pointer) {
		}

		public List<Integer> flush() {
			List<Integer> stamp = Arrays.asList(active);
			reset(1 - active);
			return stamp;
		}

		private void reset(int active) {
			this.active = active;
			deque = new ArrayDeque<>(Arrays.asList(1 - active));
		}
	}

	private class SubIbTreeAllocator implements Allocator {
		private PbTreeImpl<Integer>.Store store;

		private SubIbTreeAllocator(PbTreeImpl<Integer>.Store mutator) {
			this.store = mutator;
		}

		public Integer allocate() {
			KeyValueMutator<Integer, Integer> mutator = store.mutate();
			Integer pointer = mutator.keys(0, Integer.MAX_VALUE).first();
			if (pointer != null) {
				mutator.remove(pointer);
				return pointer;
			} else
				throw new RuntimeException("Pages exhausted");
		}

		public void discard(Integer pointer) {
			store.mutateData().putTerminal(pointer);
		}

		public List<Integer> flush() {
			return store.flush();
		}
	}

	public class Store implements KeyDataStore<Key> {
		private Allocator allocator;
		private Integer root;

		private Store(Allocator allocator) {
			this.allocator = allocator;
			root = persist(Arrays.asList(new Slot(SlotType.TERMINAL, null, null)));
		}

		private Store(Allocator allocator, Integer root) {
			this.allocator = allocator;
			this.root = root;
		}

		@Override
		public void end(boolean isComplete) {
			if (isComplete)
				mutate.commit(this);
		}

		@Override
		public KeyValueMutator<Key, Integer> mutate() {
			return new KeyValueMutator<Key, Integer>() {
				public Streamlet<Key> keys(Key start, Key end) {
					return PbTreeImpl.this.keys0(root, start, end);
				}

				public Integer get(Key key) {
					return get0(root, key, SlotType.TERMINAL);
				}

				public void put(Key key, Integer value) {
					update(key, new Slot(SlotType.TERMINAL, key, value));
				}

				public void remove(Key key) {
					delete(key);
				}
			};
		}

		@Override
		public KeyDataMutator<Key> mutateData() {
			return new KeyDataMutator<Key>() {
				public Streamlet<Key> keys(Key start, Key end) {
					return keys0(root, start, end);
				}

				public Bytes getPayload(Key key) {
					Integer pointer = get0(root, key, SlotType.DATA);
					return pointer != null ? payloadFile.load(pointer) : null;
				}

				public boolean getTerminal(Key key) {
					return stream(root, key, null).first() != null;
				}

				public void putPayload(Key key, Bytes payload) {
					Integer pointer = allocator.allocate();
					payloadFile.save(pointer, payload);
					update(key, new Slot(SlotType.DATA, key, pointer));
				}

				public void putTerminal(Key key) {
					update(key, new Slot(SlotType.TERMINAL, key, null));
				}

				public void removePayload(Key key) {
					delete(key);
				}

				public void removeTerminal(Key key) {
					delete(key);
				}
			};
		}

		private void update(Key key, Slot slot1) {
			update(key, slot -> slot1);
		}

		private void update(Key key, Fun<Slot, Slot> fun) {
			allocator.discard(root);
			root = newRootPage(update(read(root).slots, key, fun));
		}

		private List<Slot> update(List<Slot> slots0, Key key, Fun<Slot, Slot> fun) {
			FindSlot fs = new FindSlot(slots0, key);
			int s0 = fs.i;
			int s1 = fs.i + 1;
			List<Slot> replaceSlots;

			// Adds the node into it
			if (fs.slot.type == SlotType.BRANCH)
				replaceSlots = update(discard(fs.slot).slots(), key, fun);
			else if (fs.c != 0)
				replaceSlots = Arrays.asList(fs.slot, fun.apply(null));
			else
				replaceSlots = Arrays.asList(fun.apply(discard(fs.slot)));

			List<Slot> slots1 = List_.concat(List_.left(slots0, s0), replaceSlots, List_.right(slots0, s1));
			List<Slot> slots2;

			// Checks if need to split
			if (slots1.size() < maxBranchFactor)
				slots2 = Arrays.asList(slot(slots1));
			else { // Splits into two if reached maximum number of nodes
				List<Slot> leftSlots = List_.left(slots1, minBranchFactor);
				List<Slot> rightSlots = List_.right(slots1, minBranchFactor);
				slots2 = Arrays.asList(slot(leftSlots), slot(rightSlots));
			}

			return slots2;
		}

		private void delete(Key key) {
			allocator.discard(root);
			root = newRootPage(delete(read(root).slots, key));
		}

		private List<Slot> delete(List<Slot> slots0, Key key) {
			FindSlot fs = new FindSlot(slots0, key);
			int size = slots0.size();
			int s0 = fs.i, s1 = fs.i + 1;
			List<Slot> replaceSlots;

			// Removes the node from it
			if (fs.slot.type == SlotType.BRANCH) {
				List<Slot> slots1 = delete(discard(fs.slot).slots(), key);

				// Merges with a neighbor if reached minimum number of nodes
				if (slots1.size() < minBranchFactor)
					if (0 < s0)
						replaceSlots = merge(discard(slots0.get(--s0)).slots(), slots1);
					else if (s1 < size)
						replaceSlots = merge(slots1, discard(slots0.get(s1++)).slots());
					else
						replaceSlots = Arrays.asList(slot(slots1));
				else
					replaceSlots = Arrays.asList(slot(slots1));
			} else if (fs.c == 0)
				replaceSlots = Collections.emptyList();
			else
				throw new RuntimeException("Node not found " + key);

			return List_.concat(List_.left(slots0, s0), replaceSlots, List_.right(slots0, s1));
		}

		private List<Slot> merge(List<Slot> slots0, List<Slot> slots1) {
			List<Slot> merged;

			if (maxBranchFactor <= slots0.size() + slots1.size()) {
				List<Slot> leftSlots, rightSlots;

				if (minBranchFactor < slots0.size()) {
					leftSlots = List_.left(slots0, -1);
					rightSlots = List_.concat(Arrays.asList(List_.last(slots0)), slots1);
				} else if (minBranchFactor < slots1.size()) {
					leftSlots = List_.concat(slots0, Arrays.asList(List_.first(slots1)));
					rightSlots = List_.right(slots1, 1);
				} else {
					leftSlots = slots0;
					rightSlots = slots1;
				}

				merged = Arrays.asList(slot(leftSlots), slot(rightSlots));
			} else
				merged = Arrays.asList(slot(List_.concat(slots0, slots1)));

			return merged;
		}

		private List<Integer> flush() {
			return List_.concat(Arrays.asList(root), allocator.flush());
		}

		private Integer newRootPage(List<Slot> slots) {
			Slot slot;
			Integer pointer;
			if (slots.size() == 1 && (slot = slots.get(0)).type == SlotType.BRANCH)
				pointer = slot.pointer;
			else
				pointer = persist(slots);
			return pointer;
		}

		private Slot slot(List<Slot> slots) {
			return new Slot(SlotType.BRANCH, List_.first(slots).pivot, persist(slots));
		}

		private Slot discard(Slot slot) {
			if (slot != null && slot.type != SlotType.TERMINAL)
				allocator.discard(slot.pointer);
			return slot;
		}

		private Integer persist(List<Slot> slots) {
			Integer pointer = allocator.allocate();
			write(pointer, new Page(slots));
			return pointer;
		}
	}

	private class Mutate implements Closeable {
		private SerializedPageFile<List<Integer>> stampFile;

		private Mutate() {
			PageFile stampPageFile = FileFactory.pageFile(path.resolveSibling(path.getFileName() + ".stamp"), false, pageSize);
			stampFile = SerializedFileFactory.serialized(stampPageFile, serialize.list(serialize.int_));
		}

		private Store begin() {
			return store(stampFile.load(0));
		}

		private void commit(Store mutator) {
			List<Integer> stamp = mutator.flush();
			sync();
			stampFile.save(0, stamp);
		}

		public void close() throws IOException {
			stampFile.close();
		}
	}

	/**
	 * Constructor for larger trees that require another tree for page
	 * allocation management.
	 */
	public PbTreeImpl(Path path, PbTreeConfiguration<Key> config, PbTreeImpl<Integer> allocationIbTree) {
		this.path = path;
		pageSize = config.getPageSize();
		comparator = Comparator.nullsFirst(config.getComparator());
		serializer = serialize.nullable(config.getSerializer());
		maxBranchFactor = config.getMaxBranchFactor();
		this.allocationIbTree = allocationIbTree;

		int pageSize = config.getPageSize();

		mutate = new Mutate();
		minBranchFactor = maxBranchFactor / 2;
		pageFile0 = FileFactory.pageFile(path, false, pageSize);
		pageFile = SerializedFileFactory.serialized(pageFile0, newPageSerializer());
		payloadFile = SerializedFileFactory.serialized(pageFile0, serialize.bytes(pageSize));
	}

	@Override
	public void close() throws IOException {
		mutate.close();
		pageFile.close();
	}

	@Override
	public Store begin() {
		return mutate.begin();
	}

	@Override
	public int guaranteedCapacity() {
		if (allocationIbTree != null)
			// Refers the long pile above
			return allocationIbTree.guaranteedCapacity() * 9 / 10 * (minBranchFactor - 1) + 1;
		else
			// There are at most maxBranchFactor - 1 nodes, and need to keep 1
			// for the guard node too
			return maxBranchFactor - 2;
	}

	@Override
	public Store create() {
		List<Integer> stamp0;

		if (allocationIbTree != null) {
			PbTreeImpl<Integer>.Store store = allocationIbTree.create();
			KeyDataMutator<Integer> mutator = store.mutateData();
			int nPages = allocationIbTree.guaranteedCapacity();
			for (int p = 0; p < nPages; p++)
				mutator.putTerminal(p);
			stamp0 = store.flush();
		} else
			stamp0 = Arrays.asList(0);

		return new Store(allocator(stamp0));
	}

	private Streamlet<Key> keys0(Integer pointer, Key start, Key end) {
		return stream(pointer, start, end).map(slot -> slot.pivot);
	}

	private Integer get0(Integer root, Key key, SlotType slotType) {
		Slot slot = stream(root, key, null).first();
		if (slot != null && slot.type == slotType && comparator.compare(slot.pivot, key) == 0)
			return slot.pointer;
		else
			return null;
	}

	private Streamlet<Slot> stream(Integer pointer, Key start, Key end) {
		return stream0(pointer, start, end).drop(1);
	}

	private Streamlet<Slot> stream0(Integer pointer, Key start, Key end) {
		List<Slot> node = read(pointer).slots;
		int i0 = start != null ? new FindSlot(node, start, false).i : 0;
		int i1 = end != null ? new FindSlot(node, end, false).i + 1 : node.size();

		if (i0 < i1)
			return Read.from(node.subList(Math.max(0, i0), i1)).concatMap(slot -> {
				if (slot.type == SlotType.BRANCH)
					return stream0(slot.pointer, start, end);
				else
					return Read.each(slot);
			});
		else
			return Read.empty();
	}

	private Store store(List<Integer> stamp) {
		return new Store(allocator(List_.right(stamp, 1)), stamp.get(0));
	}

	private Allocator allocator(List<Integer> stamp0) {
		Allocator allocator;
		if (allocationIbTree != null)
			allocator = new SubIbTreeAllocator(allocationIbTree.store(stamp0));
		else
			allocator = new SwappingAllocator(stamp0.get(0));
		return new DelayedDiscardAllocator(allocator);
	}

	private Page read(Integer pointer) {
		return pageFile.load(pointer);
	}

	private void write(Integer pointer, Page page) {
		pageFile.save(pointer, page);
	}

	private void sync() {
		pageFile0.sync();
	}

	private Serializer<Page> newPageSerializer() {
		Serializer<List<Slot>> slotsSerializer = serialize.list(new Serializer<Slot>() {
			public Slot read(SerInput si) throws IOException {
				SlotType type = SlotType.values()[si.readByte()];
				Key pivot = serializer.read(si);
				Integer pointer = pointerSerializer.read(si);
				return new Slot(type, pivot, pointer);
			}

			public void write(SerOutput so, Slot slot) throws IOException {
				so.writeByte(slot.type.ordinal());
				serializer.write(so, slot.pivot);
				pointerSerializer.write(so, slot.pointer);
			}
		});

		return new Serializer<Page>() {
			public Page read(SerInput si) throws IOException {
				return new Page(slotsSerializer.read(si));
			}

			public void write(SerOutput so, Page page) throws IOException {
				slotsSerializer.write(so, page.slots);
			}
		};
	}

}
