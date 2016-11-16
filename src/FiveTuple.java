/*
 * Copyright Alex Chulkin (c) 2016
 */

/*
 * The helper FiveTuple class
 */
class FiveTuple<A, B, C, D, E> extends Pair<A, B> {

	private C third;
	private D fourth;
	private E fifth;

	FiveTuple(A first, B second, C third, D fourth, E fifth) {
		super(first,second);
		this.third = third;
		this.fourth = fourth;
		this.fifth = fifth;
	}

	C getThird() {
		return third;
	}

	public void setThird(C third) {
		this.third = third;
	}

	D getFourth() {
		return fourth;
	}

	public void setFourth(D fourth) {
		this.fourth = fourth;
	}

	E getFifth() {
		return fifth;
	}

	public void setFifth(E fifth) {
		this.fifth = fifth;
	}

}