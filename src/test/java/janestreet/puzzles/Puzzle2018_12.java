package janestreet.puzzles;

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
SCORE = 230
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12 {

	@Test
	public void test() {
		var nr = 9;
		var size = 7;
		var hallmark = 230;

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
					r.run();
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
					r.run();
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
			private void p(byte[] tile, Sink<byte[]> sink) {
				for (var i = tile.length - 2; 0 <= i; i -= 2) {
					var xp = (byte) (1 + tile[i + 0]);
					var yp = (byte) (1 + tile[i + 1]);
					if (p[xp - 1][yp] == 0 && p[xp + 1][yp] == 0 && p[xp][yp - 1] == 0 && p[xp][yp + 1] == 0) {
						swap(tile, i, tile.length - 2);
						p[xp][yp] = 1;
						sink.f(tile);
						p[xp][yp] = 0;
						swap(tile, i, tile.length - 2);
					}
				}
			}

			private void swap(byte[] tile, int i, int j) {
				var old0 = tile[i + 0];
				var old1 = tile[i + 1];
				tile[i + 0] = tile[j + 0];
				tile[i + 1] = tile[j + 1];
				tile[j + 0] = old0;
				tile[j + 1] = old1;
			}
		};

		var permuteTiles = new Object() {
			private void p(int t) {
				if (t < tiles.length)
					for (var i = t; i < tiles.length; i++) {
						swap(i, t);
						permuteTile.p(tiles[t], tile_ -> filler.fill(tile_, () -> p(t + 1)));
						swap(i, t);
					}
				else
					tryOnce.run();
			}

			private void swap(int x, int y) {
				var t = tiles[x];
				tiles[x] = tiles[y];
				tiles[y] = t;
			}
		};

		permuteTiles.p(0);
	}

}
