package negotiator.group11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import negotiator.Bid;
import negotiator.BidIterator;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Group11 extends AbstractNegotiationParty {

	private ArrayList<ValuedBid> possibleBids;
	private ArrayList<Bid> opponentBids;
	private int round;

	/**
	 * Please keep this constructor. This is called by genius.
	 *
	 * @param utilitySpace Your utility space.
	 * @param deadlines The deadlines set for this negotiation.
	 * @param timeline Value counting from 0 (start) to 1 (end).
	 * @param randomSeed If you use any randomization, use this seed for it.
	 */
	public Group11(UtilitySpace utilitySpace,
				  Map<DeadlineType, Object> deadlines,
				  Timeline timeline,
				  long randomSeed) {
		// Make sure that this constructor calls it's parent.
		super(utilitySpace, deadlines, timeline, randomSeed);

		this.round = 0;
		
		// create a list of bids
		possibleBids = new ArrayList<ValuedBid>();
		opponentBids = new ArrayList<Bid>();
		// fill the list with all possible bids
		BidIterator iterator = new BidIterator(utilitySpace.getDomain());
		while (iterator.hasNext()) {
			Bid bid = iterator.next();
			possibleBids.add(new ValuedBid(bid, this.getUtility(bid)));
		}
		// Sort the list of bids, highest utility first
		Collections.sort(possibleBids, new Comparator<ValuedBid>() {
		    public int compare(ValuedBid bid1, ValuedBid bid2) {
		    	if (bid1.getUtility() > bid2.getUtility()) {
		    		return -1;
		    	} else if (bid1.getUtility() < bid2.getUtility()) {
		    		return 1;
		    	} else {
		    		return 0;
		    	}
		    }
		});
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The first party in
	 * the first round is a bit different, it can only propose an offer.
	 *
	 * @param validActions Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class> validActions) {
		this.round++;
		
		// If you are made an offer you can't refuse...
		if(opponentBids.size() > 0 && this.getUtility(opponentBids.get(opponentBids.size() - 1)) > 1 - getTime()) {
			return new Accept();
		}
		
		// When the deadline is not near yet, make an offer
		// if we are the first party, also offer.
		if (!validActions.contains(Accept.class) || getTime() < 0.9) {
			return new Offer(possibleBids.remove(0).getBid());
		}
		else {
			return new Accept();
		}
	}
	
	/**
	 * 
	 * @return the partial of rounds done, or 0 when there is no deadline
	 */
	private double getTime() {
		int deadline = (int) this.deadlines.get(DeadlineType.ROUND);
		
		if(deadline != 0) {
			return (double) this.round/deadline;
		} else {
			return 0;
		}
	}


	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict their utility.
	 *
	 * @param sender The party that did the action.
	 * @param action The action that party did.
	 */
	@Override
	public void receiveMessage(Object sender, Action action) {
		// Here you can listen to other parties' messages		
		if(action instanceof Offer) {
			opponentBids.add(((Offer) action).getBid());
		}
	}

}
