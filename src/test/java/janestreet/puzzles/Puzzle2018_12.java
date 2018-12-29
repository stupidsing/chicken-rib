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
				{ c(2, 5), c(2, 6), c(1, 6), }, //
				{ c(5, 6), c(6, 5), c(6, 6), }, //
				{ c(0, 4), c(0, 3), c(1, 4), }, //
				{ c(6, 1), c(5, 0), c(6, 0), }, //
				{ c(4, 2), c(5, 2), c(5, 1), }, //
				{ c(0, 6), c(1, 5), c(0, 5), }, //
				{ c(1, 3), c(2, 2), c(2, 3), }, //
				{ c(4, 4), c(5, 5), c(5, 4), }, //
				{ c(3, 4), c(2, 4), c(3, 5), }, //
				{ c(4, 5), c(3, 6), c(4, 6), }, //
				{ c(6, 4), c(5, 3), c(6, 2), c(6, 3), }, //
				{ c(3, 3), c(4, 3), c(3, 2), }, //
				{ c(2, 0), c(3, 1), c(2, 1), }, //
				{ c(1, 0), c(1, 1), c(0, 0), }, //
				{ c(3, 0), c(4, 1), c(4, 0), }, //
				{ c(0, 2), c(0, 1), c(1, 2), }, //
		};

		var g = new byte[size2];
		var p = new byte[c(size + 1, size + 1)];
		var xbitmasks = new long[size];
		var ybitmasks = new long[size];

		var filler = new Object() {
			private int score;

			private void fill(byte[] tile, Runnable r) {
				if (tile.length == 3)
					fill3(tile[0], tile[1], tile[2], r);
				else
					fill4(tile[0], tile[1], tile[2], tile[3], r);
			}

			private void fill3(byte xy0, byte xy1, byte xy2, Runnable r) {
				var bmka = xbitmasks[xy0 / 8] | ybitmasks[xy0 % 8];
				var bmkb = xbitmasks[xy1 / 8] | ybitmasks[xy1 % 8];
				var bmkc = xbitmasks[xy2 / 8] | ybitmasks[xy2 % 8];
				var score0 = score;
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
					var b0 = 1l << g[xy0];
					var b1 = 1l << g[xy1];
					var b2 = 1l << g[xy2];
					xbitmasks[xy0 / 8] |= b0;
					xbitmasks[xy1 / 8] |= b1;
					xbitmasks[xy2 / 8] |= b2;
					ybitmasks[xy0 % 8] |= b0;
					ybitmasks[xy1 % 8] |= b1;
					ybitmasks[xy2 % 8] |= b2;
					p[cm(xy2 / 8 + 1, xy2 % 8 + 1)] = 1;
					score = score0 + inc;
					r.run();
					score = score0;
					p[cm(xy2 / 8 + 1, xy2 % 8 + 1)] = 0;
					ybitmasks[xy2 % 8] &= ~b2;
					ybitmasks[xy1 % 8] &= ~b1;
					ybitmasks[xy0 % 8] &= ~b0;
					xbitmasks[xy2 / 8] &= ~b2;
					xbitmasks[xy1 / 8] &= ~b1;
					xbitmasks[xy0 / 8] &= ~b0;
					g[xy0] = g[xy1] = g[xy2] = 0;
				}
			}

			private void fill4(byte xy0, byte xy1, byte xy2, byte xy3, Runnable r) {
				var bmka = xbitmasks[xy0 / 8] | ybitmasks[xy0 % 8];
				var bmkb = xbitmasks[xy1 / 8] | ybitmasks[xy1 % 8];
				var bmkc = xbitmasks[xy2 / 8] | ybitmasks[xy2 % 8];
				var bmkd = xbitmasks[xy3 / 8] | ybitmasks[xy3 % 8];
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
					var b0 = 1l << g[xy0];
					var b1 = 1l << g[xy1];
					var b2 = 1l << g[xy2];
					var b3 = 1l << g[xy3];
					xbitmasks[xy0 / 8] |= b0;
					xbitmasks[xy1 / 8] |= b1;
					xbitmasks[xy2 / 8] |= b2;
					xbitmasks[xy3 / 8] |= b3;
					ybitmasks[xy0 % 8] |= b0;
					ybitmasks[xy1 % 8] |= b1;
					ybitmasks[xy2 % 8] |= b2;
					ybitmasks[xy3 % 8] |= b3;
					p[cm(xy3 / 8 + 1, xy3 % 8 + 1)] = 1;
					score = score0 + inc;
					r.run();
					score = score0;
					p[cm(xy3 / 8 + 1, xy3 % 8 + 1)] = 0;
					ybitmasks[xy3 % 8] &= ~b3;
					ybitmasks[xy2 % 8] &= ~b2;
					ybitmasks[xy1 % 8] &= ~b1;
					ybitmasks[xy0 % 8] &= ~b0;
					xbitmasks[xy3 / 8] &= ~b3;
					xbitmasks[xy2 / 8] &= ~b2;
					xbitmasks[xy1 / 8] &= ~b1;
					xbitmasks[xy0 / 8] &= ~b0;
					g[xy0] = g[xy1] = g[xy2] = g[xy3] = 0;
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
				for (var i = tile.length - 1; 0 <= i; i--) {
					var xyp = tile[i];
					var xp = xyp / 8 + 1;
					var yp = xyp % 8 + 11;
					if (p[cm(xp - 1, yp)] == 0 && p[cm(xp + 1, yp)] == 0 && p[cm(xp, yp - 1)] == 0 && p[cm(xp, yp + 1)] == 0) {
						swap(tile, i, tile.length - 1);
						p[cm(xp, yp)] = 1;
						sink.f(tile);
						p[cm(xp, yp)] = 0;
						swap(tile, i, tile.length - 1);
					}
				}
			}

			private void swap(byte[] tile, int i, int j) {
				var old = tile[i];
				tile[i] = tile[j];
				tile[j] = old;
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
