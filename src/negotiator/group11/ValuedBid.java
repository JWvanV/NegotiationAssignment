package negotiator.group11;

import negotiator.Bid;

/**
 * A bid combined with its utility.
 */
public class ValuedBid {

	private Bid bid;
	private double utility;

	public ValuedBid(Bid bid, double utility) {
		this.bid = bid;
		this.utility = utility;
	}

	/**
	 * @return the bid item
	 */
	public Bid getBid() {
		return this.bid;
	}

	/**
	 * @return the utility of this bid
	 */
	public double getUtility() {
		return this.utility;
	}

	public String toString() {
		return "Bid<" + this.getUtility() + ">";
	}
}
