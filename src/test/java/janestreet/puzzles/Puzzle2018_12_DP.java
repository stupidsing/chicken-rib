package janestreet.puzzles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import suite.inspect.Dump;
import suite.primitive.IntMutable;
import suite.primitive.adt.map.IntObjMap;
import suite.primitive.adt.set.IntSet;
import suite.streamlet.Read;
import suite.util.To;

/*
for all tile order, find the first score

  3, 24,  6,  4,  2, 15,  5,
 15,  5,  4,  2,  8,  3,  6,
  4, 12,  7, 14,  5,  2,  3,
  2,  3, 15,  5, 30,  6,  4,
  8,  4,  2,  3,  6,  5, 20,
  5,  6,  3,  1,  4, 24,  2,
 10,  2,  5, 15,  3,  4,  8,
SCORE = 230
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12_DP {

	private class Board {
		private short[][] g;
		private byte[][] p;
		private int score;

		private Board(short[][] g, byte[][] p, int score) {
			this.g = g;
			this.p = p;
			this.score = score;
		}
	}

	@Test
	public void test() {
		var nr = 9;
		var size = 7;
		var hallmark = 240;

		var tiles = new byte[][] { //
				{ 2, 5, 2, 6, 1, 6, }, //
				{ 5, 6, 6, 5, 6, 6, }, //
				{ 0, 4, 0, 3, 1, 4, }, //
				{ 4, 2, 5, 2, 5, 1, }, //
				{ 6, 4, 5, 3, 6, 2, 6, 3, }, //
				{ 6, 1, 5, 0, 6, 0, }, //
				{ 0, 2, 1, 2, 0, 1, }, //
				{ 0, 6, 1, 5, 0, 5, }, //
				{ 4, 5, 3, 6, 4, 6, }, //
				{ 2, 0, 3, 1, 2, 1, }, //
				{ 3, 5, 2, 4, 3, 4, }, //
				{ 1, 3, 2, 2, 2, 3, }, //
				{ 3, 3, 4, 3, 3, 2, }, //
				{ 0, 0, 1, 1, 1, 0, }, //
				{ 3, 0, 4, 1, 4, 0, }, //
				{ 4, 4, 5, 4, 5, 5, }, //
		};

		var board0 = new Board(new short[size][size], new byte[size + 2][size + 2], 0);
		var map = new IntObjMap<List<Board>>();
		map.put(0, List.of(board0));

		for (var n = 0; n < 6; n++) {
			// for (var n = 0; n < tiles.length; n++) {
			var map1 = new IntObjMap<List<Board>>();

			for (var e : map.streamlet()) {
				int v;

				for (var i = 0; i < tiles.length; i++)
					if (Integer.bitCount(v = e.t0 | 1 << i) == n + 1) {
						var tile = tiles[i];
						var list = map1.computeIfAbsent(v, v_ -> new ArrayList<>());
						var minScore = IntMutable.of(!list.isEmpty() ? list.get(0).score : Integer.MAX_VALUE);

						for (var board : e.t1) {
							var g = board.g;
							var p = board.p;

							var filler = new Object() {
								private int score = board.score;

								private void fill(byte[] tile, Runnable r) {
									int tl = tile.length;
									if (p[tile[tl - 2]][tile[tl - 1]] == 0)
										if (tl == 6)
											fill3(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], r);
										else
											fill4(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], tile[6], tile[7], r);
								}

								private void fill3(int x0, int y0, int x1, int y1, int x2, int y2, Runnable r) {
									var sa = findExcludeSet(x0, y0);
									var sb = findExcludeSet(x1, y1);
									var sc = findExcludeSet(x2, y2);
									var score0 = score;
									var inc = hallmark - score;

									var ax = Math.min(nr, inc / 2);
									for (var a = (short) 2; a < ax; a++) {
										var bx = !sa.contains(a) ? Math.min(nr, inc / a) : 0;
										for (var b = (short) 2; b < bx; b++) {
											var c = !sb.contains(b) && a != b ? (short) (a * b) : a;
											if (!sc.contains(c) && a != c && b != c) {
												if (c < inc) {
													g[x0][y0] = a;
													g[x1][y1] = b;
													g[x2][y2] = c;
													inc = c;
												}
											}
										}
									}

									if (inc < hallmark - score0) {
										score = score0 + inc;
										p[x2][y2] = 1;
										r.run();
										p[x2][y2] = 0;
										score = score0;
									}

									g[x0][y0] = g[x1][y1] = g[x2][y2] = 0;
								}

								private void fill4(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, Runnable r) {
									var sa = findExcludeSet(x0, y0);
									var sb = findExcludeSet(x1, y1);
									var sc = findExcludeSet(x2, y2);
									var sd = findExcludeSet(x3, y3);
									var score0 = score;
									var inc = hallmark - score;

									var ax = Math.min(nr, inc);
									for (var a = (short) 1; a < ax; a++) {
										var bx = !sa.contains(a) ? Math.min(nr, inc / a) : 0;
										for (var b = (short) 1; b < bx; b++) {
											var ab = a * b;
											var cx = !sb.contains(b) && a != b ? Math.min(nr, inc / ab) : 0;
											for (var c = (short) 1; c < cx; c++) {
												var d = !sc.contains(c) && a != c && b != c ? (short) (ab * c) : a;
												if (!sd.contains(d) && a != d && b != d && c != d) {
													if (d < inc) {
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

									if (inc < hallmark - score0) {
										score = score0 + inc;
										p[x3][y3] = 1;
										r.run();
										p[x3][y3] = 0;
										score = score0;
									}

									g[x0][y0] = g[x1][y1] = g[x2][y2] = g[x3][y3] = 0;
								}

								private IntSet findExcludeSet(int x, int y) {
									var s = new IntSet();
									for (short i = 0; i < size; i++) {
										s.add(g[i][y]);
										s.add(g[x][i]);
									}
									return s;
								}
							};

							filler.fill(tile, () -> {
								var score = filler.score;

								if (score < minScore.value()) {
									minScore.update(score);
									list.clear();
								}

								if (score <= minScore.value()) {
									var g1 = To.array(g.length, short[].class, j -> Arrays.copyOf(g[j], g[j].length));
									var p1 = To.array(p.length, byte[].class, j -> Arrays.copyOf(p[j], p[j].length));
									list.add(new Board(g1, p1, score));
								}
							});
						}
					}
			}

			map = map1;
		}

		var min = map.streamlet().values().concatMap(Read::from).min(Comparator.comparingInt(board -> board.score));

		System.out.println("SIZE = " + map.size());
		Dump.details(min);
	}

}
