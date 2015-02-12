package negotiator.group11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import negotiator.Bid;
import negotiator.issue.Issue;
import negotiator.issue.Value;

/**
 * This class saves the history of bids done by a single opponent.
 */
public class OpponentBidHistory {

	private ArrayList<BidSequence> bids;

	public OpponentBidHistory() {
		bids = new ArrayList<OpponentBidHistory.BidSequence>();
	}

	/**
	 * Save a sequence of bids
	 * @param previousBid
	 * @param newBid
	 */
	public void add(Bid previousBid, Bid newBid) {
		bids.add(new BidSequence(previousBid, newBid));
	}

	/**
	 * @return the amount of bids in this history
	 */
	public int getSize() {
		return bids.size();
	}

	/**
	 * Try to determine what kind of strategy the opponent is using.
	 * 
	 * This is done by checking the difference in values between the 
	 * opponent's current and the opponent's own last offer, 
	 * and between the opponent's current and the overall last offer.
	 * 
	 * @return the strategy the opponent is most likely using.
	 */
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
					Integer c = counts.get(BidModificationStrategy.UNKNOWN);
					int currentCount = c == null ? 0 : c;
					counts.put(BidModificationStrategy.UNKNOWN, currentCount++);
				} else if (currentDistance < previousDistance) {
					Integer c = counts
							.get(BidModificationStrategy.MODIFY_PREVIOUS);
					int currentCount = c == null ? 0 : c;
					counts.put(BidModificationStrategy.MODIFY_PREVIOUS,
							currentCount++);
				} else {
					Integer c = counts.get(BidModificationStrategy.MODIFY_SELF);
					int currentCount = c == null ? 0 : c;
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

	/**
	 * Get the amount of different values between two bids
	 * 
	 * @param bid1 Bid
	 * @param bid2 Bid
	 * @return the amount of different values between bid1 and bid2
	 */
	private double getBidDifference(Bid bid1, Bid bid2) {
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

	/**
	 * There are three strategies defined, 
	 * UNKNOWN (everything unclassified), 
	 * MODIFY_SELF (using own bids) and
	 * MODIFY_PREVIOUS (modify opponent bids more to your liking)
	 */
	enum BidModificationStrategy {
		UNKNOWN, MODIFY_SELF, MODIFY_PREVIOUS
	}

	/**
	 * Wrapper for two following bids
	 */
	class BidSequence {
		Bid previous;
		Bid current;
		
		/**
		 * Wrapper for two following bids
		 */
		BidSequence(Bid previous, Bid current) {
			this.previous = previous;
			this.current = current;
		}
	}
}
