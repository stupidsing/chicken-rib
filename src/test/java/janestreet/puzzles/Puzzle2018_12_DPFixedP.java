package janestreet.puzzles;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import suite.inspect.Dump;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.set.IntSet;
import suite.streamlet.Read;

/*
fixes the product cells,
use DP to build up each tiles one-by-one (and generate a list of boards with minimum score per each tile setting),
assume greedy approach would work (that every smallest feasible new tile would lead to a final global minimum),
and find it

 5,  2,  8,  4,  3,  6, 18
 2, 10,  4,  6, 12,  3,  5
18,  3,  2, 12,  5,  4, 20
 3,  6, 10,  5,  4, 20,  2
12,  4,  3,  2,  7,  5, 10
 4, 15,  5,  1, 14,  2,  3
20,  5,  6,  3,  2, 12,  4
.score = 217 [Integer]
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12_DPFixedP {

	private int size = 7;
	private int size2 = c(size, size);
	private int hallmark = 230;

	private byte[][] combos = { //
			{ 2, 3, 6, }, //
			{ 2, 4, 8, }, //
			{ 2, 5, 10, }, //
			{ 2, 6, 12, }, //
			{ 2, 7, 14, }, //
			{ 3, 4, 12, }, //
			{ 3, 5, 15, }, //
			{ 3, 6, 18, }, //
			{ 3, 7, 21, }, //
			{ 4, 5, 20, }, //
			{ 4, 6, 24, }, //
			{ 4, 7, 28, }, //
			// { 5, 6, 30, }, //
			// { 5, 7, 35, }, //
			// { 6, 7, 42, }, //
	};

	private class Board {
		private final byte[] g;
		private final int score, hashCode;

		private Board(byte[] g, int score) {
			this.g = g;
			this.score = score;
			hashCode = Arrays.hashCode(g);
		}

		public int hashCode() {
			return hashCode;
		}

		public boolean equals(Object object) {
			return object.getClass() == Board.class ? Arrays.equals(g, ((Board) object).g) : false;
		}
	}

	@Test
	public void test() {
		byte[][] tiles = { //
				{ c(3, 5), c(2, 4), c(3, 4), }, //
				{ c(4, 6), c(4, 5), c(3, 6), }, //
				{ c(2, 2), c(1, 3), c(2, 3), }, //
				{ c(4, 3), c(3, 3), c(3, 2), }, //
				{ c(1, 0), c(0, 0), c(1, 1), }, //
				{ c(4, 0), c(3, 0), c(4, 1), }, //
				{ c(5, 4), c(4, 4), c(5, 5), }, //
				{ c(6, 5), c(6, 6), c(5, 6), }, //
				{ c(0, 6), c(1, 5), c(0, 5), }, //
				{ c(0, 1), c(0, 2), c(1, 2), }, //
				{ c(2, 6), c(2, 5), c(1, 6), }, //
				{ c(3, 1), c(2, 0), c(2, 1), }, //
				{ c(0, 4), c(0, 3), c(1, 4), }, //
				{ c(5, 2), c(4, 2), c(5, 1), }, //
				{ c(6, 1), c(6, 0), c(5, 0), }, //
				{ c(6, 3), c(6, 4), c(5, 3), c(6, 2), }, //
		};

		var random = new Random();

		for (var i = 0; i < 19; i++) {
			var tile = tiles[random.nextInt(tiles.length)];
			var j = random.nextInt(3);
			var t = tile[j];
			tile[j] = tile[0];
			tile[0] = t;
		}

		var set = new IntSet();

		var permute = new Object() {
			private Board minBoard;

			private void p(int i) {
				if (i < tiles.length) {
					var tile = tiles[i];
					for (var j = 0; j < tile.length; j++) {
						swap(tile, j);
						var xy = tile[0];
						if (!set.contains(xy - 1) && !set.contains(xy + 1) && !set.contains(xy - 8) && !set.contains(xy + 8)) {
							set.add(xy);
							p(i + 1);
							set.remove(xy);
						}
						swap(tile, j);
					}
				} else {
					var board = find(tiles);
					if (board != null)
						if (minBoard == null || board.score < minBoard.score) {
							minBoard = board;
							hallmark = minBoard.score;
						}
					Dump.details(minBoard);
				}
			}

			private void swap(byte[] tile, int j) {
				var t = tile[j];
				tile[j] = tile[0];
				tile[0] = t;
			}
		};

		permute.p(0);

		Dump.details(permute.minBoard);
	}

	private Board find(byte[][] tiles) {
		var board0 = new Board(new byte[size2], 0);

		var map = new IntObjMap<Set<Board>>();
		map.put(0, Set.of(board0));

		for (var n = 0; n < tiles.length; n++) {
			var est = (tiles.length - 1 - n) * 6;
			var map1 = new IntObjMap<Set<Board>>();

			for (var e : map.streamlet()) {
				int v;

				for (var i = 0; i < tiles.length; i++)
					if (Integer.bitCount(v = e.t0 | 1 << i) == n + 1) {
						var tile = tiles[i];
						var set = map1.computeIfAbsent(v, v_ -> new HashSet<>());
						var minScore = IntMutable.of(!set.isEmpty() ? set.iterator().next().score : Integer.MAX_VALUE);

						for (var board : e.t1) {
							var g = board.g;
							var xbitmasks = new long[size];
							var ybitmasks = new long[size];

							for (var x = 0; x < size; x++)
								for (var y = 0; y < size; y++) {
									var m = 1l << g[c(x, y)];
									xbitmasks[x] |= m;
									ybitmasks[y] |= m;
								}

							IntSink updateMin = score -> {
								if (score < minScore.value()) {
									minScore.update(score);
									set.clear();
								}

								if (score <= minScore.value())
									set.add(new Board(Arrays.copyOf(g, size2), score));
							};

							var score = board.score;

							new Object() {
								private void fill(byte[] tile) {
									if (tile.length == 3)
										fill3(tile[1], tile[2], tile[0]);
									else if (tile.length == 4) {
										g[tile[1]] = 1;
										fill3(tile[2], tile[3], tile[0]);
										g[tile[2]] = 1;
										fill3(tile[1], tile[3], tile[0]);
										g[tile[3]] = 1;
										fill3(tile[1], tile[2], tile[0]);
									}
								}

								private void fill3(byte xy0, byte xy1, byte xy2) {
									var bmka = xbitmasks[xy0 / 8] | ybitmasks[xy0 % 8];
									var bmkb = xbitmasks[xy1 / 8] | ybitmasks[xy1 % 8];
									var bmkc = xbitmasks[xy2 / 8] | ybitmasks[xy2 % 8];
									var inc = hallmark - score - est;
									byte c;

									var go = new Object() {
										private void g(byte a, byte b, byte c) {
											if ((bmka & 1l << a) == 0 && (bmkb & 1l << b) == 0) {
												g[xy0] = a;
												g[xy1] = b;
												g[xy2] = c;
												updateMin.f(score + c);
												g[xy0] = g[xy1] = g[xy2] = 0;
											}
										}
									};

									for (var combo : combos)
										if ((bmkc & 1l << (c = combo[2])) == 0 && c < inc) {
											go.g(combo[0], combo[1], c);
											go.g(combo[1], combo[0], c);
										}
								}
							}.fill(tile);
						}
					}
			}

			map = map1;
			System.out.println("SIZE[" + n + "] = " + map.size());
		}

		return map.streamlet().values().concatMap(Read::from).minOrNull(Comparator.comparingInt(board -> board.score));
	}

	private byte c(int x, int y) {
		return (byte) (x * 8 + y);
	}

}
