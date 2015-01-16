package negotiator.group11;

import negotiator.Bid;
import negotiator.bidding.BidDetails;

public class BidDetailsWithNash extends BidDetails {
	private static final long serialVersionUID = 5739523536737751055L;

	public BidDetailsWithNash(Bid bid, double nashProduct) {
		super(bid, nashProduct);
	}

	public BidDetailsWithNash(Bid bid, double nashProduct, double time) {
		super(bid, nashProduct, time);
	}

	public double getEstimatedNashValue() {
		return getMyUndiscountedUtil();
	}
}
