package negotiator.group11;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.issue.Issue;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Group11 extends AbstractNegotiationParty {

	private SortedOutcomeSpace possibleBids;
	private HashMap<Object, Opponent> opponents;
	private BidHistory allBids;
	private int round;
	private double lastUtility;
	private double reservationUtility = 0.7;

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
		allBids = new BidHistory();
		opponents = new HashMap<Object, Opponent>();
	}

	private Offer bid(Bid bid) {
		allBids.add(new BidDetails(bid, getUtility(bid)));
		return new Offer(bid);
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
		this.round++;
		// if we are the first party, make the best offer.
		if (!validActions.contains(Accept.class)) {
			return bid(possibleBids.getMaxBidPossible().getBid());
		}

		BidDetails lastBid = allBids.getLastBidDetails();
		BidDetails bestBid = allBids.getBestBidDetails();

		// Afknaps
		// if (lastBid.getMyUndiscountedUtil() < 0.3 && getTime() > 0.5) {
		// return new EndNegotiation();
		// }

		// If you were made a decent offer once, reoffer it, unless it is the
		// last bid, then accept...
		if (bestBid != null && bestBid.getMyUndiscountedUtil() > 1 - getTime()) {
			if (Math.abs(bestBid.getMyUndiscountedUtil()
					- lastBid.getMyUndiscountedUtil()) < 0.05) {
				return new Accept();
			} else {
				return bid(bestBid.getBid());
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

	private HashMap<Integer, Boolean> getBidDifference(Bid bid1, Bid bid2) {
		HashMap<Integer, Boolean> res = new HashMap<Integer, Boolean>();
		for (Issue s : bid1.getIssues()) {
			try {
				Value value1 = bid1.getValue(s.getNumber());
				Value value2 = bid2.getValue(s.getNumber());
				res.put(s.getNumber(), value1 == value2);
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		return res;
	}

	private Offer getOfferFromPreviousUtil(double prevUtil) {
		BidDetails bid = possibleBids.getBidNearUtility(prevUtil * lastUtility);
		lastUtility = bid.getMyUndiscountedUtil();
		return bid(bid.getBid());
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

		// TODO also record what the Action was that this action is a response
		// to.

		Opponent opponent = opponents.get(sender);
		if (opponent == null) {
			opponent = new Opponent();
			opponents.put(sender, opponent);
		}

		Bid prevousBid = allBids.getLastBid();

		// Update opponent specific history
		if (action instanceof Offer) {
			// Update global history
			Bid bid = Action.getBidFromAction(action);
			BidDetails details = new BidDetails(bid, getUtility(bid));
			allBids.add(details);

			opponent.addOffer(details);
		} else if (action instanceof Accept) {
			// TODO check if the accept is actually from the lastBid
			BidDetails details = allBids.getLastBidDetails();

			opponent.addAccept(details);
		} else {
			System.out.println("WARNING :: UNKNOWN ACTION :: "
					+ action.getClass().getCanonicalName());
		}
	}

}
