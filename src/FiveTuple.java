/*
 * The helper FiveTuple class
 */
public class FiveTuple<A, B, C, D, E> extends Pair<A,B> {

	private C third;
	private D fourth;
	private E fifth;

	FiveTuple(A first, B second, C third, D fourth, E fifth) {
		super(first,second);
		this.third = third;
		this.fourth = fourth;
		this.fifth = fifth;
	}

	public void setThird(C third) {
		this.third = third;
	}

	public void setFourth(D fourth) {
		this.fourth = fourth;
	}

	public void setFifth(E fifth){
		this.fifth = fifth;
	}
	public C getThird() {
		return third;
	}
	
	public D getFourth() {
		return fourth;
	}
	
	public E getFifth(){
		return fifth;
	}

}