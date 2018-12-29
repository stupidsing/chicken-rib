package janestreet.puzzles;

import org.junit.Test;

import suite.streamlet.FunUtil.Sink;

/*
for all tile order, find the first score

 20,  3,  6,  4,  2, 15,  5,
  5,  4, 18,  2,  8,  3,  6,
  4, 24,  5, 10,  6,  2,  3,
  2,  6, 15,  5,  3, 18,  4,
 10,  5,  2,  3,  4,  6, 24,
  3,  8,  4,  1, 20,  5,  2,
  6,  2,  3, 15,  5,  4,  8,
SCORE = 225
 */
// https://www.janestreet.com/puzzles/block-party-2/
public class Puzzle2018_12 {

	private int size = 7;
	private int size2 = c(size, size);

	private byte[][] combos = { //
			{ 2, 3, 6, }, //
			{ 2, 4, 8, }, //
			{ 2, 5, 10, }, //
			{ 2, 6, 12, }, //
			// { 2, 7, 14, }, //
			{ 3, 4, 12, }, //
			{ 3, 5, 15, }, //
			{ 3, 6, 18, }, //
			// { 3, 7, 21, }, //
			{ 4, 5, 20, }, //
			{ 4, 6, 24, }, //
			// { 4, 7, 28, }, //
			{ 5, 6, 30, }, //
			// { 5, 7, 35, }, //
	};

	@Test
	public void test() {
		var nr = 8;
		var pr = 36;
		var size = 7;
		var hallmark = 240;

		var tiles = new byte[][] { //
				{ 2, 5, 2, 6, 1, 6, }, //
				{ 5, 6, 6, 5, 6, 6, }, //
				{ 0, 4, 0, 3, 1, 4, }, //
				{ 6, 1, 5, 0, 6, 0, }, //
				{ 4, 2, 5, 2, 5, 1, }, //
				{ 0, 6, 1, 5, 0, 5, }, //
				{ 1, 3, 2, 2, 2, 3, }, //
				{ 4, 4, 5, 5, 5, 4, }, //
				{ 3, 4, 2, 4, 3, 5, }, //
				{ 4, 5, 3, 6, 4, 6, }, //
				{ 6, 4, 5, 3, 6, 2, 6, 3, }, //
				{ 3, 3, 4, 3, 3, 2, }, //
				{ 2, 0, 3, 1, 2, 1, }, //
				{ 1, 0, 1, 1, 0, 0, }, //
				{ 3, 0, 4, 1, 4, 0, }, //
				{ 0, 2, 0, 1, 1, 2, }, //
		};

		var g = new byte[size2];
		var p = new byte[c(size + 1, size + 1)];
		var xbitmasks = new long[size];
		var ybitmasks = new long[size];

		var filler = new Object() {
			private int score;

			private void fill(byte[] tile, Runnable r) {
				if (tile.length == 6)
					fill3(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], r);
				else
					fill4(tile[0], tile[1], tile[2], tile[3], tile[4], tile[5], tile[6], tile[7], r);
			}

			private void fill3(byte x0, byte y0, byte x1, byte y1, byte x2, byte y2, Runnable r) {
				var bmka = xbitmasks[x0] | ybitmasks[y0];
				var bmkb = xbitmasks[x1] | ybitmasks[y1];
				var bmkc = xbitmasks[x2] | ybitmasks[y2];
				var score0 = score;
				var inc = Math.min(pr, hallmark - score);
				byte a = 0, b = 0, c = 0;

				for (var combo : combos)
					if (true //
							&& (bmka & 1l << (a = combo[0])) == 0 //
							&& (bmkb & 1l << (b = combo[1])) == 0 //
							&& (bmkc & 1l << (c = combo[2])) == 0 //
							&& c < inc) {
						g[c(x0, y0)] = a;
						g[c(x1, y1)] = b;
						g[c(x2, y2)] = c;
						inc = c;
					}

				for (var combo : combos)
					if (true //
							&& (bmka & 1l << (a = combo[1])) == 0 //
							&& (bmkb & 1l << (b = combo[0])) == 0 //
							&& (bmkc & 1l << (c = combo[2])) == 0 //
							&& c < inc) {
						g[c(x0, y0)] = a;
						g[c(x1, y1)] = b;
						g[c(x2, y2)] = c;
						inc = c;
					}

				if (g[c(x0, y0)] != 0) {
					var b0 = 1l << g[c(x0, y0)];
					var b1 = 1l << g[c(x1, y1)];
					var b2 = 1l << g[c(x2, y2)];
					xbitmasks[x0] |= b0;
					xbitmasks[x1] |= b1;
					xbitmasks[x2] |= b2;
					ybitmasks[y0] |= b0;
					ybitmasks[y1] |= b1;
					ybitmasks[y2] |= b2;
					p[cm(x2 + 1, y2 + 1)] = 1;
					score = score0 + inc;
					r.run();
					score = score0;
					p[cm(x2 + 1, y2 + 1)] = 0;
					ybitmasks[y2] &= ~b2;
					ybitmasks[y1] &= ~b1;
					ybitmasks[y0] &= ~b0;
					xbitmasks[x2] &= ~b2;
					xbitmasks[x1] &= ~b1;
					xbitmasks[x0] &= ~b0;
					g[c(x0, y0)] = g[c(x1, y1)] = g[c(x2, y2)] = 0;
				}
			}

			private void fill4(byte x0, byte y0, byte x1, byte y1, byte x2, byte y2, byte x3, byte y3, Runnable r) {
				var bmka = xbitmasks[x0] | ybitmasks[y0];
				var bmkb = xbitmasks[x1] | ybitmasks[y1];
				var bmkc = xbitmasks[x2] | ybitmasks[y2];
				var bmkd = xbitmasks[x3] | ybitmasks[y3];
				var score0 = score;
				var inc = Math.min(pr, hallmark - score);
				var ab = Integer.MAX_VALUE;

				var ax = Math.min(nr, inc);
				for (var a = (byte) 1; a < ax; a++) {
					var bx = (bmka & 1l << a) == 0 ? Math.min(nr, inc / a) : 0;
					for (var b = (byte) 1; b < bx; b++) {
						var cx = (bmkb & 1l << b) == 0 && a != b ? Math.min(nr, inc / (ab = a * b)) : 0;
						for (var c = (byte) 1; c < cx; c++) {
							var d = (bmkc & 1l << c) == 0 && a != c && b != c ? (byte) (ab * c) : a;
							if (d < inc) {
								var e = (bmkd & 1l << d) == 0 && a != d && b != d && c != d;
								if (e) {
									g[c(x0, y0)] = a;
									g[c(x1, y1)] = b;
									g[c(x2, y2)] = c;
									g[c(x3, y3)] = d;
									inc = d;
								}
							}
						}
					}
				}

				if (g[c(x0, y0)] != 0) {
					var b0 = 1l << g[c(x0, y0)];
					var b1 = 1l << g[c(x1, y1)];
					var b2 = 1l << g[c(x2, y2)];
					var b3 = 1l << g[c(x3, y3)];
					xbitmasks[x0] |= b0;
					xbitmasks[x1] |= b1;
					xbitmasks[x2] |= b2;
					xbitmasks[x3] |= b3;
					ybitmasks[y0] |= b0;
					ybitmasks[y1] |= b1;
					ybitmasks[y2] |= b2;
					ybitmasks[y3] |= b3;
					p[cm(x3 + 1, y3 + 1)] = 1;
					score = score0 + inc;
					r.run();
					score = score0;
					p[cm(x3 + 1, y3 + 1)] = 0;
					ybitmasks[y3] &= ~b3;
					ybitmasks[y2] &= ~b2;
					ybitmasks[y1] &= ~b1;
					ybitmasks[y0] &= ~b0;
					xbitmasks[x3] &= ~b3;
					xbitmasks[x2] &= ~b2;
					xbitmasks[x1] &= ~b1;
					xbitmasks[x0] &= ~b0;
					g[c(x0, y0)] = g[c(x1, y1)] = g[c(x2, y2)] = g[c(x3, y3)] = 0;
				}
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

				for (byte x = 0; x < size; x++) {
					for (byte y = 0; y < size; y++) {
						var s = "   " + g[c(x, y)] + ",";
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
					if (p[cm(xp - 1, yp)] == 0 && p[cm(xp + 1, yp)] == 0 && p[cm(xp, yp - 1)] == 0 && p[cm(xp, yp + 1)] == 0) {
						swap(tile, i, tile.length - 2);
						p[cm(xp, yp)] = 1;
						sink.f(tile);
						p[cm(xp, yp)] = 0;
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

	private byte cm(int x, int y) {
		return c(x & 7, y & 7);
	}

	private byte c(int x, int y) {
		return (byte) (x * 8 + y);
	}

}
