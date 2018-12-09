package janestreet.puzzles;

import java.util.Random;

import org.junit.Test;

import suite.primitive.adt.set.IntSet;

/*
random a tile order, find the minimal score

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
public class Puzzle2018_12a {

	@Test
	public void test() {
		var nr = 8;
		var size = 7;
		var hallmark = 175;

		var tiles = new byte[][] { //
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

		var random = new Random();
		var g = new short[7][7];

		for (var tile : tiles)
			for (var i = 0; i < 9; i++) {
				var i0 = random.nextInt(tile.length / 2) * 2;
				var i1 = random.nextInt(tile.length / 2) * 2;
				var old0 = tile[i0 + 0];
				var old1 = tile[i0 + 1];
				tile[i0 + 0] = tile[i1 + 0];
				tile[i0 + 1] = tile[i1 + 1];
				tile[i1 + 0] = old0;
				tile[i1 + 1] = old1;
			}

		for (var i = 0; i < 99; i++) {
			var i0 = random.nextInt(tiles.length - 1) + 1;
			var i1 = random.nextInt(tiles.length - 1) + 1;
			var old = tiles[i0];
			tiles[i0] = tiles[i1];
			tiles[i1] = old;
		}

		System.out.println("TILES");
		for (var tile : tiles) {
			System.out.print("{ ");
			for (var v : tile)
				System.out.print(v + ", ");
			System.out.println("}, //");
		}
		System.out.println("TILES");

		var object = new Object() {
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
				var s0 = new IntSet();
				var s1 = new IntSet();
				var s2 = new IntSet();

				for (short x = 0; x < size; x++) {
					s0.add(g[x][y0]);
					s1.add(g[x][y1]);
					s2.add(g[x][y2]);
				}

				for (short y = 0; y < size; y++) {
					s0.add(g[x0][y]);
					s1.add(g[x1][y]);
					s2.add(g[x2][y]);
				}

				for (short a = 1; a < max; a++)
					if (!s0.contains(a)) {
						g[x0][y0] = a;
						for (short b = 1; b < max; b++)
							if (!s1.contains(b)) {
								g[x1][y1] = b;
								fillIn(a, b, x2, y2, () -> {
									if (validateRowCols())
										r.run();
								});
								g[x1][y1] = 0;
							}
						g[x0][y0] = 0;
					}
			}

			private void fill4(int x0, int y0, int x1, int y1, int x2, int y2, int x3, int y3, Runnable r) {
				var max = Math.min(nr, hallmark - score - 1);
				var s0 = new IntSet();
				var s1 = new IntSet();
				var s2 = new IntSet();
				var s3 = new IntSet();

				for (short x = 0; x < size; x++) {
					s0.add(g[x][y0]);
					s1.add(g[x][y1]);
					s2.add(g[x][y2]);
					s3.add(g[x][y3]);
				}

				for (short y = 0; y < size; y++) {
					s0.add(g[x0][y]);
					s1.add(g[x1][y]);
					s2.add(g[x2][y]);
					s3.add(g[x3][y]);
				}

				for (short a = 1; a < max; a++)
					if (!s0.contains(a)) {
						g[x0][y0] = a;
						for (short b = 1; b < max; b++)
							if (!s1.contains(b)) {
								g[x1][y1] = b;
								for (short c = 1; c < max; c++)
									if (!s2.contains(c)) {
										g[x2][y2] = c;
										fillIn(a, b, c, x3, y3, () -> {
											if (validateRowCols())
												r.run();
										});
										g[x2][y2] = 0;
									}
								g[x1][y1] = 0;
							}
						g[x0][y0] = 0;
					}
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

			private void fillIn(int a, int b, int x, int y, Runnable r) {
				if (a <= b && b % a == 0) {
					g[x][y] = (short) (b / a);
					score += b;
					r.run();
					score -= b;
				}
				if (b <= a && a % b == 0) {
					g[x][y] = (short) (a / b);
					score += a;
					r.run();
					score -= a;
				}
				g[x][y] = (short) (a * b);
				score += g[x][y];
				r.run();
				score -= g[x][y];
				g[x][y] = 0;
			}

			private void fillIn(int a, int b, int c, int x, int y, Runnable r) {
				var bc = b * c;
				var ca = c * a;
				var ab = a * b;
				if (bc <= a && a % bc == 0) {
					g[x][y] = (short) (a / bc);
					score += a;
					r.run();
					score -= a;
				}
				if (ca <= b && b % ca == 0) {
					g[x][y] = (short) (b / ca);
					score += b;
					r.run();
					score -= b;
				}
				if (ab <= c && c % ab == 0) {
					g[x][y] = (short) (c / ab);
					score += c;
					r.run();
					score -= c;
				}
				var product = a * bc;
				g[x][y] = (short) product;
				score += product;
				r.run();
				score -= product;
				g[x][y] = 0;
			}
		};

		var minScore = new int[] { Integer.MAX_VALUE, };

		object.fill(() -> {
			if (object.score < minScore[0]) {
				for (short x = 0; x < size; x++) {
					for (short y = 0; y < size; y++) {
						var s = "   " + g[x][y] + ",";
						var length = s.length();
						System.out.print(s.substring(length - 4, length));
					}
					System.out.println();
				}

				System.out.println("SCORE = " + (minScore[0] = object.score));
			}
		});
	}

}
