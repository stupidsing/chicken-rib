package janestreet.puzzles;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import suite.inspect.Dump;
import suite.node.util.TreeUtil.IntInt_Bool;
import suite.primitive.IntMutable;
import suite.primitive.IntPrimitives.IntSink;
import suite.primitive.adt.map.IntObjMap;
import suite.streamlet.Read;
import suite.util.To;

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
		private final byte[][] g;
		private final byte[][] p;
		private final int score, hashCode;

		private Board(byte[][] g, byte[][] p, int score) {
			this.g = g;
			this.p = p;
			this.score = score;

			int h = 7;
			for (var r : g)
				for (var b : r)
					h = h * 31 + b;

			hashCode = h;
		}

		public int hashCode() {
			return hashCode;
		}

		public boolean equals(Object object) {
			if (object.getClass() == Board.class) {
				Board board = (Board) object;
				var b = true;

				for (var x = 0; x < g.length; x++)
					for (var y = 0; y < g[x].length; y++)
						b &= g[x][y] == board.g[x][y];

				return b;
			} else
				return false;
		}
	}

	@Test
	public void test() {
		var nr = 8;
		var pr = 36;
		var size = 7;
		var hallmark = 230;

		var tiles = new byte[][] { //
				{ 2, 5, 2, 6, 1, 6, }, //
				{ 5, 6, 6, 5, 6, 6, }, //
				{ 0, 4, 0, 3, 1, 4, }, //
				{ 4, 2, 5, 2, 5, 1, }, //
				{ 6, 1, 5, 0, 6, 0, }, //
				{ 6, 4, 5, 3, 6, 3, 6, 2, }, //
				{ 0, 6, 1, 5, 0, 5, }, //
				{ 0, 2, 1, 2, 0, 1, }, //
				{ 2, 0, 3, 1, 2, 1, }, //
				{ 3, 5, 2, 4, 3, 4, }, //
				{ 4, 5, 3, 6, 4, 6, }, //
				{ 1, 3, 2, 2, 2, 3, }, //
				{ 3, 3, 4, 3, 3, 2, }, //
				{ 0, 0, 1, 1, 1, 0, }, //
				{ 3, 0, 4, 1, 4, 0, }, //
				{ 4, 4, 5, 4, 5, 5, }, //
		};

		var board0 = new Board(new byte[size][size], new byte[size + 2][size + 2], 0);
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
									var m = 1l << g[x][y];
									xbitmasks[x] |= m;
									ybitmasks[y] |= m;
								}

							IntInt_Bool vp = (xs, ys) -> {
								var xp = 1 + xs;
								var yp = 1 + ys;
								return p[xp - 1][yp] + p[xp + 1][yp] + p[xp][yp - 1] + p[xp][yp + 1] == 0;
							};

							IntSink r = score -> {
								if (score < minScore.value()) {
									minScore.update(score);
									set.clear();
								}

								if (score <= minScore.value()) {
									var g1 = To.array(g.length, byte[].class, j -> Arrays.copyOf(g[j], g[j].length));
									var p1 = To.array(p.length, byte[].class, j -> Arrays.copyOf(p[j], p[j].length));
									set.add(new Board(g1, p1, score));
								}
							};

							var score = board.score;

							var filler = new Object() {
								private void fill(byte[] tile) {
									byte xs, ys;

									if (tile.length == 6) {
										if (vp.apply(xs = tile[0], ys = tile[1]))
											fill3(tile[2], tile[3], tile[4], tile[5], xs, ys);
										if (vp.apply(xs = tile[2], ys = tile[3]))
											fill3(tile[0], tile[1], tile[4], tile[5], xs, ys);
										if (vp.apply(xs = tile[4], ys = tile[5]))
											fill3(tile[0], tile[1], tile[2], tile[3], xs, ys);
									} else {
										if (vp.apply(xs = tile[0], ys = tile[1]))
											fill4(tile[2], tile[3], tile[4], tile[5], tile[6], tile[7], xs, ys);
										if (vp.apply(xs = tile[2], ys = tile[3]))
											fill4(tile[0], tile[1], tile[4], tile[5], tile[6], tile[7], xs, ys);
										if (vp.apply(xs = tile[4], ys = tile[5]))
											fill4(tile[0], tile[1], tile[2], tile[3], tile[6], tile[7], xs, ys);
										if (vp.apply(xs = tile[6], ys = tile[7]))
											fill4(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], xs, ys);
									}
								}

								private void fill4_(byte x0, byte y0, byte x1, byte y1, byte x2, byte y2, byte x3, byte y3) {
									g[x0][y0] = 1;
									fill3(x1, y1, x2, y2, x3, y3);
									g[x1][y1] = 1;
									fill3(x0, y0, x2, y2, x3, y3);
									g[x2][y2] = 1;
									fill3(x0, y0, x1, y1, x3, y3);
								}

								private void fill3(byte x0, byte y0, byte x1, byte y1, byte x2, byte y2) {
									var bmka = xbitmasks[x0] | ybitmasks[y0];
									var bmkb = xbitmasks[x1] | ybitmasks[y1];
									var bmkc = xbitmasks[x2] | ybitmasks[y2];
									var inc = Math.min(pr, hallmark - score);
									byte a = 0, b = 0, c = 0;

									for (var combo : combos)
										if (true //
												&& (bmka & 1l << (a = combo[0])) == 0 //
												&& (bmkb & 1l << (b = combo[1])) == 0 //
												&& (bmkc & 1l << (c = combo[2])) == 0 //
												&& c < inc) {
											g[x0][y0] = a;
											g[x1][y1] = b;
											g[x2][y2] = c;
											inc = c;
										}

									for (var combo : combos)
										if (true //
												&& (bmka & 1l << (a = combo[1])) == 0 //
												&& (bmkb & 1l << (b = combo[0])) == 0 //
												&& (bmkc & 1l << (c = combo[2])) == 0 //
												&& c < inc) {
											g[x0][y0] = a;
											g[x1][y1] = b;
											g[x2][y2] = c;
											inc = c;
										}

									if (g[x0][y0] != 0) {
										p[x2 + 1][y2 + 1] = 1;
										r.f(score + inc);
										p[x2 + 1][y2 + 1] = 0;
										g[x0][y0] = g[x1][y1] = g[x2][y2] = 0;
									}
								}

								private void fill4(byte x0, byte y0, byte x1, byte y1, byte x2, byte y2, byte x3, byte y3) {
									var bmka = xbitmasks[x0] | ybitmasks[y0];
									var bmkb = xbitmasks[x1] | ybitmasks[y1];
									var bmkc = xbitmasks[x2] | ybitmasks[y2];
									var bmkd = xbitmasks[x3] | ybitmasks[y3];
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
														g[x0][y0] = a;
														g[x1][y1] = b;
														g[x2][y2] = c;
														g[x3][y3] = d;
														inc = d;
													}
												}
											}
										}
									}

									if (g[x0][y0] != 0) {
										p[x3 + 1][y3 + 1] = 1;
										r.f(score + inc);
										p[x3 + 1][y3 + 1] = 0;
										g[x0][y0] = g[x1][y1] = g[x2][y2] = g[x3][y3] = 0;
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

}
