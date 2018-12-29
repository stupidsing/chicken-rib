package janestreet.puzzles;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.function.IntPredicate;

import org.junit.Test;

import suite.inspect.Dump;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.adt.map.IntObjMap;
import suite.streamlet.Read;

/*
for all tile order, find the first score

 35,  6,  3,  5,  4,  8,  2,
  5,  7, 18,  2, 20,  4,  3,
  3,  4,  5, 10,  2, 18,  6,
  2, 12,  4,  3, 10,  5,  8,
  6,  3,  7, 12,  5,  2,  4,
  4,  2, 14,  1,  3, 15,  5,
 20,  5,  2,  4,  8,  3, 15,
SCORE = 229
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12_DP {

	private int size = 7;
	private int size2 = c(size, size);

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
			{ 5, 6, 30, }, //
			{ 5, 7, 35, }, //
	};

	private class Board {
		private final byte[] g;
		private final byte[] p;
		private final int score, hashCode;

		private Board(byte[] g, byte[] p, int score) {
			this.g = g;
			this.p = p;
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
		var nr = 8;
		var pr = 36;
		var hallmark = 230;

		var tiles = new byte[][] { //
				{ c(2, 5), c(2, 6), c(1, 6), }, //
				{ c(5, 6), c(6, 5), c(6, 6), }, //
				{ c(0, 4), c(0, 3), c(1, 4), }, //
				{ c(4, 2), c(5, 2), c(5, 1), }, //
				{ c(6, 1), c(5, 0), c(6, 0), }, //
				{ c(6, 4), c(5, 3), c(6, 3), c(6, 2), }, //
				{ c(0, 6), c(1, 5), c(0, 5), }, //
				{ c(0, 2), c(1, 2), c(0, 1), }, //
				{ c(2, 0), c(3, 1), c(2, 1), }, //
				{ c(3, 5), c(2, 4), c(3, 4), }, //
				{ c(4, 5), c(3, 6), c(4, 6), }, //
				{ c(1, 3), c(2, 2), c(2, 3), }, //
				{ c(3, 3), c(4, 3), c(3, 2), }, //
				{ c(0, 0), c(1, 1), c(1, 0), }, //
				{ c(3, 0), c(4, 1), c(4, 0), }, //
				{ c(4, 4), c(5, 4), c(5, 5), }, //
		};

		var board0 = new Board(new byte[size2], new byte[c(size + 1, size + 1)], 0);
		var map = new IntObjMap<Set<Board>>();
		map.put(0, Set.of(board0));

		// for (var n = 0; n < 9; n++) {
		for (var n = 0; n < tiles.length; n++) {
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
							var p = board.p;
							var xbitmasks = new long[size];
							var ybitmasks = new long[size];

							for (var x = 0; x < size; x++)
								for (var y = 0; y < size; y++) {
									var m = 1l << g[c(x, y)];
									xbitmasks[x] |= m;
									ybitmasks[y] |= m;
								}

							IntPredicate vp = xys -> {
								var xp = 1 + xys / 8;
								var yp = 1 + xys % 8;
								return p[cm(xp - 1, yp)] + p[cm(xp + 1, yp)] + p[cm(xp, yp - 1)] + p[cm(xp, yp + 1)] == 0;
							};

							IntSink r = score -> {
								if (score < minScore.value()) {
									minScore.update(score);
									set.clear();
								}

								if (score <= minScore.value())
									set.add(new Board(Arrays.copyOf(g, size2), Arrays.copyOf(p, c(size + 2, size + 2)), score));
							};

							var score = board.score;

							var filler = new Object() {
								private void fill(byte[] tile) {
									byte xys;

									if (tile.length == 3) {
										if (vp.test(xys = tile[0]))
											fill3(tile[1], tile[2], xys);
										if (vp.test(xys = tile[1]))
											fill3(tile[0], tile[2], xys);
										if (vp.test(xys = tile[2]))
											fill3(tile[0], tile[1], xys);
									} else {
										if (vp.test(xys = tile[0]))
											fill4(tile[1], tile[2], tile[3], xys);
										if (vp.test(xys = tile[1]))
											fill4(tile[0], tile[2], tile[3], xys);
										if (vp.test(xys = tile[2]))
											fill4(tile[0], tile[1], tile[3], xys);
										if (vp.test(xys = tile[3]))
											fill4(tile[0], tile[1], tile[2], xys);
									}
								}

								private void fill4_(byte xy0, byte xy1, byte xy2, byte xy3) {
									g[xy0] = 1;
									fill3(xy1, xy2, xy3);
									g[xy1] = 1;
									fill3(xy0, xy2, xy3);
									g[xy2] = 1;
									fill3(xy0, xy1, xy3);
								}

								private void fill3(byte xy0, byte xy1, byte xy2) {
									var bmka = xbitmasks[xy0 / 8] | ybitmasks[xy0 % 8];
									var bmkb = xbitmasks[xy1 / 8] | ybitmasks[xy1 % 8];
									var bmkc = xbitmasks[xy2 / 8] | ybitmasks[xy2 % 8];
									var inc = Math.min(pr, hallmark - score);
									byte a = 0, b = 0, c = 0;

									for (var combo : combos)
										if (true //
												&& (bmka & 1l << (a = combo[0])) == 0 //
												&& (bmkb & 1l << (b = combo[1])) == 0 //
												&& (bmkc & 1l << (c = combo[2])) == 0 //
												&& c < inc) {
											g[xy0] = a;
											g[xy1] = b;
											g[xy2] = c;
											inc = c;
										}

									for (var combo : combos)
										if (true //
												&& (bmka & 1l << (a = combo[1])) == 0 //
												&& (bmkb & 1l << (b = combo[0])) == 0 //
												&& (bmkc & 1l << (c = combo[2])) == 0 //
												&& c < inc) {
											g[xy0] = a;
											g[xy1] = b;
											g[xy2] = c;
											inc = c;
										}

									if (g[xy0] != 0) {
										p[cm(xy2 / 8 + 1, xy2 % 8 + 1)] = 1;
										r.f(score + inc);
										p[cm(xy2 / 8 + 1, xy2 % 8 + 1)] = 0;
										g[xy0] = g[xy1] = g[xy2] = 0;
									}
								}

								private void fill4(byte xy0, byte xy1, byte xy2, byte xy3) {
									var bmka = xbitmasks[xy0 / 8] | ybitmasks[xy0 % 8];
									var bmkb = xbitmasks[xy1 / 8] | ybitmasks[xy1 % 8];
									var bmkc = xbitmasks[xy2 / 8] | ybitmasks[xy2 % 8];
									var bmkd = xbitmasks[xy3 / 8] | ybitmasks[xy3 % 8];
									var inc = Math.min(pr, hallmark - score);
									var ab = Integer.MAX_VALUE;

									var ax = Math.min(nr, inc);
									for (var a = (byte) 1; a < ax; a++) {
										var bx = (bmka & 1 << a) == 0 ? Math.min(nr, inc / a) : 0;
										for (var b = (byte) 1; b < bx; b++) {
											var cx = (bmkb & 1 << b) == 0 && a != b ? Math.min(nr, inc / (ab = a * b)) : 0;
											for (var c = (byte) 1; c < cx; c++) {
												var d = (bmkc & 1 << c) == 0 && a != c && b != c ? (byte) (ab * c) : a;
												if (d < inc) {
													var e = (bmkd & 1l << d) == 0 && a != d && b != d && c != d;
													if (e) {
														g[xy0] = a;
														g[xy1] = b;
														g[xy2] = c;
														g[xy3] = d;
														inc = d;
													}
												}
											}
										}
									}

									if (g[xy0] != 0) {
										p[cm(xy3 / 8 + 1, xy3 % 8 + 1)] = 1;
										r.f(score + inc);
										p[cm(xy3 / 8 + 1, xy3 % 8 + 1)] = 0;
										g[xy0] = g[xy1] = g[xy2] = g[xy3] = 0;
									}
								}
							};

							filler.fill(tile);
						}
					}
			}

			map = map1;
			System.out.println("SIZE[" + n + "] = " + map.size());
		}

		var min = map.streamlet().values().concatMap(Read::from).min(Comparator.comparingInt(board -> board.score));

		Dump.details(min);
	}

	private byte cm(int x, int y) {
		return c(x & 7, y & 7);
	}

	private byte c(int x, int y) {
		return (byte) (x * 8 + y);
	}

}
