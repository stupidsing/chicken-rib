package janestreet.puzzles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import org.junit.Test;

import suite.primitive.adt.set.IntSet;
import suite.streamlet.FunUtil.Sink;

/*
for all tile order, find the first score

  6, 12,  3,  5,  4,  1,  7,
  1,  6,  4,  3, 20,  7,  2,
  2, 10, 21,  7, 14,  6,  3,
  3,  5,  6,  1,  7,  2,  4,
  5, 15,  7,  6,  2,  4,  1,
  4,  7,  1,  2,  6,  3,  5,
 12,  3,  2,  4,  1,  5, 25,
SCORE = 175
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12b {

	@Test
	public void test() {
		var nr = 8;
		var size = 7;
		var hallmark = 999;

		var tiles0 = new byte[][] { //
				{ 6, 4, 6, 2, 5, 3, 6, 3, }, //
				{ 5, 5, 4, 4, 5, 4, }, //
				{ 4, 6, 4, 5, 3, 6, }, //
				{ 3, 4, 3, 5, 2, 4, }, //
				{ 3, 0, 4, 0, 4, 1, }, //
				{ 5, 0, 6, 1, 6, 0, }, //
				{ 1, 3, 2, 3, 2, 2, }, //
				{ 0, 3, 0, 4, 1, 4, }, //
				{ 6, 5, 5, 6, 6, 6, }, //
				{ 1, 6, 2, 5, 2, 6, }, //
				{ 3, 1, 2, 0, 2, 1, }, //
				{ 3, 2, 3, 3, 4, 3, }, //
				{ 0, 6, 1, 5, 0, 5, }, //
				{ 0, 0, 1, 1, 1, 0, }, //
				{ 1, 2, 0, 2, 0, 1, }, //
				{ 4, 2, 5, 1, 5, 2, }, //
		};
		var tiles = new byte[16][];

		var random = new Random();
		var g = new short[7][7];

		var trialObject = new Object() {
			private int score;

			private void fill(Runnable r) {
				new Object() {
					private void f(int i) {
						if (hallmark <= score)
							;
						else if (i < tiles.length) {
							var tile = tiles[i];
							if (tile.length == 6)
								fill3(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], () -> f(i + 1));
							else
								fill4(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], tile[6], tile[7], () -> f(i + 1));
						} else
							r.run();
					}
				}.f(0);
			}

			private void fill3(int x0, int y0, int x1, int y1, int x2, int y2, Runnable r) {
				var max = Math.min(nr, hallmark - score - 1);
				var a0 = findMin(x0, y0, max);
				var b0 = findMin(x1, y1, max);
				var minScore = hallmark;
				short a1 = -1, b1 = -1;
				for (var a = a0; a < max; a++)
					for (var b = b0; b < max; b++) {
						var product = a * b;
						var score1 = score + product;
						if (score1 < minScore) {
							g[x0][y0] = a;
							g[x1][y1] = b;
							g[x2][y2] = (short) product;
							if (validateRowCols()) {
								minScore = score1;
								a1 = a;
								b1 = b;
							}
							g[x0][y0] = g[x1][y1] = g[x2][y2] = 0;
						}
					}

				if (minScore < hallmark) {
					var product = a1 * b1;
					g[x0][y0] = a1;
					g[x1][y1] = b1;
					g[x2][y2] = (short) product;
					score += product;
					r.run();
					score -= product;
					g[x0][y0] = g[x1][y1] = g[x2][y2] = 0;
				}
			}

			private void fill4(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, Runnable r) {
				var max = Math.min(nr, hallmark - score - 1);
				var a0 = findMin(x0, y0, max);
				var b0 = findMin(x1, y1, max);
				var c0 = findMin(x2, y2, max);
				var minScore = hallmark;
				short a1 = -1, b1 = -1, c1 = -1;
				for (var a = a0; a < max; a++)
					for (var b = b0; b < max; b++)
						for (var c = c0; c < max; c++) {
							var product = a * b * c;
							var score1 = score + product;
							if (score1 < minScore) {
								g[x0][y0] = a;
								g[x1][y1] = b;
								g[x2][y2] = c;
								g[x3][y3] = (short) product;
								if (validateRowCols()) {
									minScore = score1;
									a1 = a;
									b1 = b;
									c1 = c;
								}
								g[x0][y0] = g[x1][y1] = g[x2][y2] = g[x3][y3] = 0;
							}
						}

				if (minScore < hallmark) {
					var product = a1 * b1 * c1;
					g[x0][y0] = a1;
					g[x1][y1] = b1;
					g[x2][y2] = c1;
					g[x3][y3] = (short) product;
					score += product;
					r.run();
					score -= product;
					g[x0][y0] = g[x1][y1] = g[x2][y2] = g[x3][y3] = 0;
				}
			}

			private short findMin(int x, int y, int max) {
				var s = new IntSet();

				for (short i = 0; i < size; i++) {
					s.add(g[i][y]);
					s.add(g[x][i]);
				}

				for (short v = 1; v < max; v++)
					if (!s.contains(v))
						return v;

				return Short.MAX_VALUE;
			}

			private boolean validateRowCols() {
				var b = true;
				for (int x = 0; x < size; x++)
					b &= validateRow(x);
				for (int y = 0; y < size; y++)
					b &= validateCol(y);
				return b;
			}

			private boolean validateRow(int x) {
				var set = new IntSet();
				var b = true;
				for (int y = 0; y < size; y++) {
					var v = g[x][y];
					b &= v == 0 || set.add(v);
				}
				return b;
			}

			private boolean validateCol(int y) {
				var set = new IntSet();
				var b = true;
				for (int x = 0; x < size; x++) {
					var v = g[x][y];
					b &= v == 0 || set.add(v);
				}
				return b;
			}
		};

		var minScore = new int[] { Integer.MAX_VALUE, };

		Runnable tryOnce = () -> {
			trialObject.fill(() -> {
				if (trialObject.score < minScore[0]) {
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

					System.out.println("SCORE = " + (minScore[0] = trialObject.score));
				}
			});
		};

		var permuteTile = new Object() {
			private void permuteTile(byte[] tile0, Sink<byte[]> sink) {
				var tile1 = new byte[tile0.length];
				for (var i = 0; i < tile0.length; i += 2) {
					var j = 0;
					tile1[j + 0] = tile0[i + 0];
					tile1[j + 1] = tile0[i + 1];
					j += 2;
					while (j < i) {
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

		new Object() {
			private void permuteTiles(int t) {
				if (t < tiles0.length)
					for (var tile : new ArrayList<>(tilesSet)) {
						tilesSet.remove(tile);
						permuteTile.permuteTile(tile, tile_ -> {
							tiles[t] = tile_;
							permuteTiles(t + 1);
						});
						tilesSet.add(tile);
					}
				else
					tryOnce.run();
			}
		}.permuteTiles(0);
	}

}
