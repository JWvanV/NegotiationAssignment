package negotiator.group11;

import negotiator.Bid;
import negotiator.bidding.BidDetails;

public class BidDetailsWithNash extends BidDetails {

	public BidDetailsWithNash(Bid bid, double nashProduct) {
		super(bid, nashProduct);
	}

	public BidDetailsWithNash(Bid bid, double nashProduct, double time) {
		super(bid, nashProduct, time);
	}
}
