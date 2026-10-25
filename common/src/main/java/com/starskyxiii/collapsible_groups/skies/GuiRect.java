package com.starskyxiii.collapsible_groups.skies;

/** A rectangle in GUI pixels, with no game types, so layout maths can be tested on its own. */
public record GuiRect(int x, int y, int width, int height) {
	public int right() {
		return x + width;
	}

	public int bottom() {
		return y + height;
	}

	public boolean isEmpty() {
		return width <= 0 || height <= 0;
	}

	public boolean intersects(GuiRect other) {
		return !isEmpty() && !other.isEmpty()
			&& x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
	}

	public boolean contains(int pointX, int pointY) {
		return pointX >= x && pointX < right() && pointY >= y && pointY < bottom();
	}
}
