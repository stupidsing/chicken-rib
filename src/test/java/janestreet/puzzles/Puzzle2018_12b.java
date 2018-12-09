package janestreet.puzzles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import org.junit.Test;

import suite.primitive.adt.set.IntSet;
import suite.streamlet.FunUtil.Sink;

/*
for all tile order, find the first score

 20, 21,  3,  5, 15,  2,  6,
  4,  5,  7,  8,  3, 12,  2,
 18,  3,  2,  4, 20, 10,  5,
  3,  6, 14,  2,  5,  4, 15,
  6,  2,  4,  7, 24,  5,  3,
  2, 20,  5,  3,  4,  6, 12,
  5, 10,  6,  1,  2,  3,  4,
SCORE = 231
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12b {

	@Test
	public void test() {
		var nr = 8;
		var size = 7;
		var hallmark = 231;

		var tiles0 = new byte[][] { //
				{ 6, 3, 6, 4, 5, 3, 6, 2, }, //
				{ 2, 2, 2, 3, 1, 3, }, //
				{ 4, 1, 3, 0, 4, 0, }, //
				{ 4, 6, 4, 5, 3, 6, }, //
				{ 3, 4, 3, 5, 2, 4, }, //
				{ 2, 6, 1, 6, 2, 5, }, //
				{ 1, 2, 0, 2, 0, 1, }, //
				{ 0, 3, 1, 4, 0, 4, }, //
				{ 0, 5, 0, 6, 1, 5, }, //
				{ 6, 5, 6, 6, 5, 6, }, //
				{ 1, 0, 1, 1, 0, 0, }, //
				{ 4, 2, 5, 2, 5, 1, }, //
				{ 5, 5, 5, 4, 4, 4, }, //
				{ 5, 0, 6, 0, 6, 1, }, //
				{ 3, 1, 2, 1, 2, 0, }, //
				{ 3, 3, 4, 3, 3, 2, }, //
		};
		var tiles = new byte[16][];
		var g = new short[7][7];

		var filler = new Object() {
			private int score;

			private void fill(byte[] tile, Runnable r) {
				if (tile.length == 6)
					fill3(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], r);
				else
					fill4(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], tile[6], tile[7], r);
			}

			private void fill3(int x0, int y0, int x1, int y1, int x2, int y2, Runnable r) {
				var max = Math.min(nr, hallmark - score - 1);
				var sa = findExcludeSet(x0, y0);
				var sb = findExcludeSet(x1, y1);
				var sc = findExcludeSet(x2, y2);
				var minScore = hallmark;
				short a1 = -1, b1 = -1;
				for (short a = 2; a < max; a++)
					if (!sa.contains(a)) {
						var bx = (minScore - score) / a;
						for (short b = 2; b < bx; b++)
							if (!sb.contains(b) && a != b) {
								var c = a * b;
								var score1 = score + c;
								if (!sc.contains(c) && score1 < minScore && a != c && b != c && score1 < minScore) {
									g[x0][y0] = a;
									g[x1][y1] = b;
									g[x2][y2] = (short) c;
									minScore = score1;
									a1 = a;
									b1 = b;
									g[x0][y0] = g[x1][y1] = g[x2][y2] = 0;
								}
							}
					}

				if (minScore < hallmark) {
					var c1 = a1 * b1;
					g[x0][y0] = a1;
					g[x1][y1] = b1;
					g[x2][y2] = (short) c1;
					score += c1;
					r.run();
					score -= c1;
					g[x0][y0] = g[x1][y1] = g[x2][y2] = 0;
				}
			}

			private void fill4(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, Runnable r) {
				var max = Math.min(nr, hallmark - score - 1);
				var sa = findExcludeSet(x0, y0);
				var sb = findExcludeSet(x1, y1);
				var sc = findExcludeSet(x2, y2);
				var sd = findExcludeSet(x3, y3);
				var minScore = hallmark;
				short a1 = -1, b1 = -1, c1 = -1;
				for (short a = 1; a < max; a++)
					if (!sa.contains(a))
						for (short b = 1; b < max; b++)
							if (!sb.contains(b) && a != b) {
								var cx = (minScore - score) / a * b;
								for (short c = 1; c < cx; c++)
									if (!sc.contains(c) && a != c && b != c) {
										var d = a * b * c;
										var score1 = score + d;
										if (!sd.contains(d) && a != d && b != d && c != d && score1 < minScore) {
											g[x0][y0] = a;
											g[x1][y1] = b;
											g[x2][y2] = c;
											g[x3][y3] = (short) d;
											minScore = score1;
											a1 = a;
											b1 = b;
											c1 = c;
											g[x0][y0] = g[x1][y1] = g[x2][y2] = g[x3][y3] = 0;
										}
									}
							}

				if (minScore < hallmark) {
					var d1 = a1 * b1 * c1;
					g[x0][y0] = a1;
					g[x1][y1] = b1;
					g[x2][y2] = c1;
					g[x3][y3] = (short) d1;
					score += d1;
					r.run();
					score -= d1;
					g[x0][y0] = g[x1][y1] = g[x2][y2] = g[x3][y3] = 0;
				}
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
				for (var tile : tiles) {
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
					var j = 0;
					tile1[j + 0] = tile0[i + 0];
					tile1[j + 1] = tile0[i + 1];
					j += 2;
					while (j <= i) {
						tile1[j + 0] = tile0[j - 2];
						tile1[j + 1] = tile0[j - 1];
						j += 2;
					}
					while (j < tile1.length) {
						tile1[j + 0] = tile0[j + 0];
						tile1[j + 1] = tile0[j + 1];
						j += 2;
					}
					sink.f(tile1);
				}
			}
		};

		var tilesSet = new HashSet<>(Arrays.asList(tiles0));

		var permuteTiles = new Object() {
			private void p(int t) {
				if (t < tiles0.length)
					for (var tile : new ArrayList<>(tilesSet)) {
						tilesSet.remove(tile);
						permuteTile.p(tile, tile_ -> {
							tiles[t] = tile_;
							filler.fill(tile_, () -> p(t + 1));
						});
						tilesSet.add(tile);
					}
				else
					tryOnce.run();
			}
		};

		permuteTiles.p(0);
	}

}
