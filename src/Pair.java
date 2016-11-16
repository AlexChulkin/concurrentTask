
/*
 * Copyright Alex Chulkin (c) 2016
 */

/*
 * The helper Pair class
 */
class Pair<F, S> {
	private F first;
	private S second;

	Pair(F first, S second) {
		this.first = first;
		this.second = second;
	}

	F getFirst() {
		return first;
	}

	public void setFirst(F first) {
		this.first = first;
	}

	S getSecond() {
		return second;
	}

	public void setSecond(S second) {
		this.second = second;
	}
}