package negotiator.group11;

import java.util.HashMap;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.Domain;
import negotiator.bidding.BidDetails;
import negotiator.issue.Issue;
import negotiator.issue.Value;
import negotiator.utility.UtilitySpace;

public class Opponent {
	private BidHistory allBids;
	private BidHistory acceptedBids;
	private HashMap<Value, Integer> issueValueCounter;

	public Opponent() {
		allBids = new BidHistory();
		acceptedBids = new BidHistory();
		issueValueCounter = new HashMap<Value, Integer>();
	}

	public void addAccept(BidDetails acceptBid) {
		addOffer(acceptBid);
		acceptedBids.add(acceptBid);
	}

	public void addOffer(BidDetails offerBid) {
		allBids.add(offerBid);
		updateIssueValueCounter(offerBid.getBid());
	}

	private void updateIssueValueCounter(Bid b) {
		for (Issue i : b.getIssues()) {
			try {
				Value v = b.getValue(i.getNumber());
				int currentCount = issueValueCounter.get(v);
				issueValueCounter.put(v, ++currentCount);
			} catch (Exception e) {
				//System.out.println("THEFINGER: " + e.getLocalizedMessage());
			}
		}
	}
	
	public UtilitySpace calculateUtilitySpace(Domain d){
		UtilitySpace u = new UtilitySpace(d);
		for(Issue i : d.getIssues()){
			System.out.println("Issue " + i.toString());

			//TODO calculate issueWeigths / variance;
		}
		
		
//		u.setWeight(objective, weight)
		
		
		return u;
	}

	// TODO calculate per issue per value the amounts of bidded. the variance.
	// Higher priority is probably smaller variance; Variance = amount of times
	// picked
}
