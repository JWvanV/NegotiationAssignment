package negotiator.group11;

import java.util.List;
import java.util.Map;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Group11 extends AbstractNegotiationParty {

	private SortedOutcomeSpace possibleBids;
	private BidHistory opponentBids;
	private int round;
	private double lastUtility;

	/**
	 * Please keep this constructor. This is called by genius.
	 *
	 * @param utilitySpace
	 *            Your utility space.
	 * @param deadlines
	 *            The deadlines set for this negotiation.
	 * @param timeline
	 *            Value counting from 0 (start) to 1 (end).
	 * @param randomSeed
	 *            If you use any randomization, use this seed for it.
	 */
	public Group11(UtilitySpace utilitySpace,
			Map<DeadlineType, Object> deadlines, Timeline timeline,
			long randomSeed) {
		// TODO check if it might be fun if we give other data to the parent
		// i.e. randomseed+1
		super(utilitySpace, deadlines, timeline, randomSeed);

		this.round = 0;
		this.lastUtility = 1;

		// create a list of bids
		possibleBids = new SortedOutcomeSpace(utilitySpace);
		opponentBids = new BidHistory();
	}

	/**
	 * Each round this method gets called and ask you to accept or offer. The
	 * first party in the first round is a bit different, it can only propose an
	 * offer.
	 *
	 * @param validActions
	 *            Either a list containing both accept and offer or only offer.
	 * @return The chosen action.
	 */
	@Override
	public Action chooseAction(List<Class> validActions) {
		System.out.println("chooseAction:");
		for(Class c : validActions){
			System.out.println("- " + c.getCanonicalName());
		}
		
		this.round++;
		// if we are the first party, make the best offer.
		if (!validActions.contains(Accept.class)) {
			return new Offer(possibleBids.getMaxBidPossible().getBid());
		}

		BidDetails lastBid = opponentBids.getLastBidDetails();
		BidDetails bestBid = opponentBids.getBestBidDetails();

		// Afknaps
		if (lastBid.getMyUndiscountedUtil() < 0.3 && getTime() > 0.5) {
			return new EndNegotiation();
		}

		// If you were made a decent offer once, reoffer it, unless it is the
		// last bid, then accept...
		if (bestBid != null && bestBid.getMyUndiscountedUtil() > 1 - getTime()) {
			if (Math.abs(bestBid.getMyUndiscountedUtil()
					- lastBid.getMyUndiscountedUtil()) < 0.05) {
				return new Accept();
			} else {
				return new Offer(bestBid.getBid());
			}
		}

		// Stupid bidding, just go lower to see if it works
		if (getTime() < 0.5)
			return getOfferFromPreviousUtil(0.99);
		else if (getTime() < 0.7)
			return getOfferFromPreviousUtil(0.95);
		else if (getTime() < 0.8)
			return getOfferFromPreviousUtil(0.9);
		else if (getTime() < 0.9)
			return getOfferFromPreviousUtil(0.8);
		else if (getTime() < 0.95)
			return getOfferFromPreviousUtil(0.7);
		else
			// Eventually, accept
			return new Accept();
	}

	private Offer getOfferFromPreviousUtil(double prevUtil) {
		BidDetails bid = possibleBids.getBidNearUtility(prevUtil * lastUtility);
		lastUtility = bid.getMyUndiscountedUtil();
		return new Offer(bid.getBid());
	}

	/**
	 * 
	 * @return the partial of rounds done, or 0 when there is no deadline
	 */
	private double getTime() {
		int deadline = (int) this.deadlines.get(DeadlineType.ROUND);

		if (deadline != 0) {
			return (double) this.round / deadline;
		} else {
			return getTimeLine().getTime();
		}
	}

	/**
	 * All offers proposed by the other parties will be received as a message.
	 * You can use this information to your advantage, for example to predict
	 * their utility.
	 *
	 * @param sender
	 *            The party that did the action.
	 * @param action
	 *            The action that party did.
	 */
	@Override
	public void receiveMessage(Object sender, Action action) {
		// Here you can listen to other parties' messages
		if (action instanceof Offer) {
			Bid bid = ((Offer) action).getBid();
			opponentBids.add(new BidDetails(bid, this.getUtility(bid)));
		}
	}

}
