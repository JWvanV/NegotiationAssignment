package negotiator.group11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.Value;

public class OpponentBidHistory {

	private ArrayList<BidSequence> bids;

	public OpponentBidHistory() {
		bids = new ArrayList<OpponentBidHistory.BidSequence>();
	}

	public void add(Bid previousBid, Bid newBid) {
		bids.add(new BidSequence(previousBid, newBid));
	}
	
	public int getSize() {
		return bids.size();
	}

	public BidModificationStrategy getMostLikelyStrategy() {
		HashMap<BidModificationStrategy, Integer> counts = new HashMap<OpponentBidHistory.BidModificationStrategy, Integer>();

		if (bids.size() < 2) {
			return BidModificationStrategy.UNKNOWN;
		} else {
			for (int i = 1; i < bids.size(); i++) {
				BidSequence bsCurrent = bids.get(i);
				BidSequence bsPrevious = bids.get(i - 1);

				double currentDistance = getBidDifference(bsCurrent.previous,
						bsCurrent.current);
				double previousDistance = getBidDifference(bsPrevious.current,
						bsCurrent.current);

				if (currentDistance == previousDistance) {
					int currentCount = counts
							.get(BidModificationStrategy.UNKNOWN);
					counts.put(BidModificationStrategy.UNKNOWN, currentCount++);
				} else if (currentDistance < previousDistance) {
					int currentCount = counts
							.get(BidModificationStrategy.MODIFY_PREVIOUS);
					counts.put(BidModificationStrategy.MODIFY_PREVIOUS,
							currentCount++);
				} else {
					int currentCount = counts
							.get(BidModificationStrategy.MODIFY_SELF);
					counts.put(BidModificationStrategy.MODIFY_SELF,
							currentCount++);
				}
			}

			int maxCount = 0;
			BidModificationStrategy maxStrategy = BidModificationStrategy.UNKNOWN;

			for (Entry<BidModificationStrategy, Integer> e : counts.entrySet()) {
				if (e.getValue() > maxCount) {
					maxStrategy = e.getKey();
					maxCount = e.getValue();
				}
			}
			return maxStrategy;
		}
	}

	private double getBidDifference(Bid bid1, Bid bid2) {
		// TODO simplify counting if we decide not to make it more complicated
		HashMap<Integer, Boolean> counts = new HashMap<Integer, Boolean>();
		for (Issue i : bid1.getIssues()) {
			try {
				Value value1 = bid1.getValue(i.getNumber());
				Value value2 = bid2.getValue(i.getNumber());
				counts.put(i.getNumber(), value1 == value2);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		double differenceCount = 0;

		for (Entry<Integer, Boolean> e : counts.entrySet()) {
			if (!e.getValue())
				differenceCount += 1;
		}

		return differenceCount;
	}

	enum BidModificationStrategy {
		UNKNOWN, MODIFY_SELF, MODIFY_PREVIOUS
	}

	class BidSequence {
		Bid previous;
		Bid current;

		BidSequence(Bid previous, Bid current) {
			this.previous = previous;
			this.current = current;
		}
	}
}
