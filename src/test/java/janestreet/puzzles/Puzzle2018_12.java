package janestreet.puzzles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import suite.primitive.adt.set.IntSet;
import suite.streamlet.FunUtil.Sink;

/*
for all tile order, find the first score

  3, 24,  6,  4,  2, 15,  5,
 15,  5,  4,  2,  8,  3,  6,
  4, 12,  7, 14,  5,  2,  3,
  2,  3, 15,  5, 30,  6,  4,
  8,  4,  2,  3,  6,  5, 20,
  5,  6,  3,  1,  4, 24,  2,
 10,  2,  5, 15,  3,  4,  8,
SCORE = 215
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12 {

	@Test
	public void test() {
		var nr = 8;
		var size = 7;
		var hallmark = 999;

		var tiles0 = new byte[][] { //
				{ 2, 5, 2, 6, 1, 6, }, //
				{ 5, 6, 6, 5, 6, 6, }, //
				{ 0, 4, 0, 3, 1, 4, }, //
				{ 5, 1, 4, 2, 5, 2, }, //
				{ 6, 4, 5, 3, 6, 2, 6, 3, }, //
				{ 6, 1, 6, 0, 5, 0, }, //
				{ 0, 2, 0, 1, 1, 2, }, //
				{ 0, 6, 1, 5, 0, 5, }, //
				{ 4, 5, 4, 6, 3, 6, }, //
				{ 2, 0, 3, 1, 2, 1, }, //
				{ 3, 5, 2, 4, 3, 4, }, //
				{ 1, 3, 2, 2, 2, 3, }, //
				{ 3, 3, 3, 2, 4, 3, }, //
				{ 0, 0, 1, 1, 1, 0, }, //
				{ 3, 0, 4, 0, 4, 1, }, //
				{ 4, 4, 5, 4, 5, 5, }, //
		};

		var tiles1 = new byte[tiles0.length][];
		var g = new short[size][size];
		var p = new byte[size + 2][size + 2];

		var filler = new Object() {
			private int score;

			private void fill(byte[] tile, Runnable r) {
				if (tile.length == 6)
					fill3(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], r);
				else
					fill4(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], tile[6], tile[7], r);
			}

			private void fill3(int x0, int y0, int x1, int y1, int x2, int y2, Runnable r) {
				var score0 = score;
				score = hallmark;
				var sa = findExcludeSet(x0, y0);
				var sb = findExcludeSet(x1, y1);
				var sc = findExcludeSet(x2, y2);
				int score1;

				var ax = Math.min(nr, (hallmark - score0) / 2);
				for (short a = 2; a < ax; a++)
					if (!sa.contains(a)) {
						var bx = Math.min(nr, (hallmark - score0) / a);
						for (short b = 2; b < bx; b++)
							if (!sb.contains(b) && a != b) {
								var c = (short) (a * b);
								if (!sc.contains(c) && a != c && b != c)
									if ((score1 = score0 + c) < score) {
										g[x0][y0] = a;
										g[x1][y1] = b;
										g[x2][y2] = c;
										score = score1;
									}
							}
					}

				if (score < hallmark)
					r.run();

				g[x0][y0] = g[x1][y1] = g[x2][y2] = 0;
				score = score0;
			}

			private void fill4(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, Runnable r) {
				var score0 = score;
				var score = hallmark;
				var sa = findExcludeSet(x0, y0);
				var sb = findExcludeSet(x1, y1);
				var sc = findExcludeSet(x2, y2);
				var sd = findExcludeSet(x3, y3);
				int score1;

				var ax = Math.min(nr, hallmark - score0);
				for (short a = 1; a < ax; a++)
					if (!sa.contains(a)) {
						var bx = Math.min(nr, (hallmark - score0) / a);
						for (short b = 1; b < bx; b++)
							if (!sb.contains(b) && a != b) {
								var ab = a * b;
								var cx = Math.min(nr, (hallmark - score0) / ab);
								for (short c = 1; c < cx; c++)
									if (!sc.contains(c) && a != c && b != c) {
										var d = (short) (ab * c);
										if (!sd.contains(d) && a != d && b != d && c != d)
											if ((score1 = score0 + d) < score) {
												g[x0][y0] = a;
												g[x1][y1] = b;
												g[x2][y2] = c;
												g[x3][y3] = d;
												score = score1;
											}
									}
							}
					}

				if (score < hallmark)
					r.run();

				g[x0][y0] = g[x1][y1] = g[x2][y2] = g[x3][y3] = 0;
				score = score0;
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

		var minScore = new int[] { Integer.MAX_VALUE, };

		Runnable tryOnce = () -> {
			if (filler.score < minScore[0]) {
				System.out.println("TILES");

				for (var tile : tiles1) {
					System.out.print("{ ");
					for (var v : tile)
						System.out.print(v + ", ");
					System.out.println("}, //");
				}

				System.out.println("TILES");

				for (short x = 0; x < size; x++) {
					for (short y = 0; y < size; y++) {
						var s = "   " + g[x][y] + ",";
						var length = s.length();
						System.out.print(s.substring(length - 4, length));
					}
					System.out.println();
				}

				System.out.println("SCORE = " + (minScore[0] = filler.score));
			}
		};

		var permuteTile = new Object() {
			private void p(byte[] tile0, Sink<byte[]> sink) {
				var tile1 = new byte[tile0.length];
				for (var i = 0; i < tile0.length; i += 2) {
					var xs = tile0[i + 0];
					var ys = tile0[i + 1];
					var xp = (byte) (1 + xs);
					var yp = (byte) (1 + ys);

					if (p[xp - 1][yp] == 0 && p[xp + 1][yp] == 0 && p[xp][yp - 1] == 0 && p[xp][yp + 1] == 0) {
						var j = 0;
						while (j < i) {
							tile1[j + 0] = tile0[j + 0];
							tile1[j + 1] = tile0[j + 1];
							j += 2;
						}
						while (j < tile1.length - 2) {
							tile1[j + 0] = tile0[j + 2];
							tile1[j + 1] = tile0[j + 3];
							j += 2;
						}
						tile1[j + 0] = xs;
						tile1[j + 1] = ys;
						j += 2;

						p[xp][yp] = 1;
						sink.f(tile1);
						p[xp][yp] = 0;
					}
				}
			}
		};

		var tilesSet = new HashSet<>(Arrays.asList(tiles0));

		var permuteTiles = new Object() {
			private void p(int t) {
				if (t < tiles0.length)
					for (var tile : new ArrayList<>(tilesSet)) {
						tilesSet.remove(tile);
						permuteTile.p(tile, tile_ -> filler.fill(tiles1[t] = tile_, () -> p(t + 1)));
						tilesSet.add(tile);
					}
				else
					tryOnce.run();
			}
		};

		permuteTiles.p(0);
	}

}
