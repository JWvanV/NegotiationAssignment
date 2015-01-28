package negotiator.group11;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import misc.Range;
import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.DeadlineType;
import negotiator.Timeline;
import negotiator.actions.Accept;
import negotiator.actions.Action;
import negotiator.actions.EndNegotiation;
import negotiator.actions.Inform;
import negotiator.actions.Offer;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.group11.OpponentUtilityModel.InvalidBidException;
import negotiator.group11.OpponentUtilityModel.InvalidDomainException;
import negotiator.parties.AbstractNegotiationParty;
import negotiator.utility.UtilitySpace;

/**
 * This is your negotiation party.
 */
public class Group11 extends AbstractNegotiationParty {

	private SortedOutcomeSpace possibleBids;
	private ArrayList<BidDetailsWithNash> nashBids;
	private HashMap<Object, OpponentUtilityModel> opponents;
	private BidHistory allBids;
	private int round;
	private double lastUtility;
	private static final double startReservationUtility = 0.95;
	private double reservationUtility = startReservationUtility;

	private int lastAcceptCount;

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
		super(utilitySpace, deadlines, timeline, randomSeed);

		this.round = 0;
		this.lastUtility = 1;

		// create a list of bids
		possibleBids = new SortedOutcomeSpace(utilitySpace);
		allBids = new BidHistory();
		opponents = new HashMap<Object, OpponentUtilityModel>();

		utilitySpace.setReservationValue(reservationUtility);
	}

	/**
	 * Convenience method to make a new offer and save the relevant information
	 * 
	 * @param bid
	 * @return
	 */
	private Offer bid(Bid bid) {
		BidDetails bd = new BidDetails(bid, getUtility(bid));
		lastUtility = bd.getMyUndiscountedUtil();
		allBids.add(bd);
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
		double currentTime = getTime();
		// if we are the first party, make the best offer.
		if (!validActions.contains(Accept.class))
			return getActionForTactic(Tactics.ASOCIAL);

		// Give in to our reservation value in the endgame;
		double timeTostartGiveInReservationValue = 0.85;
		if (currentTime > timeTostartGiveInReservationValue) {
			double giveInProgress = (currentTime - timeTostartGiveInReservationValue)
					/ (1 - timeTostartGiveInReservationValue);
			reservationUtility = startReservationUtility
					- (0.4 * giveInProgress);
			utilitySpace.setReservationValue(reservationUtility);
		}

		if (previousBidHasBeenAcceptedEnough()) {
			reservationUtility *= 0.8;
		}

		BidDetails lastBid = allBids.getLastBidDetails();

		if (currentTime > 0.95
				|| (lastBid != null && lastBid.getMyUndiscountedUtil() > reservationUtility)) {
			return new Accept();
		} else {
			// Short negotiation
			if (thereWillNeverBeATrustedOpponentModel()) {
				if (previousBidHasBeenAcceptedEnough())
					return getActionForTactic(Tactics.EDGEPUSHER);
				else {
					// No consensus yet
					if (currentTime < 0.25) {
						// First quarter:
						return getActionForTactic(Tactics.HARDTOGET);
					} else if (currentTime < 0.5) {
						// Second quarter:
						return getActionForTactic(Tactics.NOSTALGIAN);
					} else {
						// Last half:
						return getActionForTactic(Tactics.GIVEIN);
					}
				}
			} else {
				// Long negotiation
				if (weTrustOurOpponentModel()) {
					// Enough rounds have passed
					sortOutcomeSpaceOnNashProduct();

					int unknownCounter = 0;
					int modifyPreviousCounter = 0;
					int modifySelfCounter = 0;
					for (Entry<Object, OpponentUtilityModel> e : opponents
							.entrySet()) {
						switch (e.getValue().getMostLikelyStrategy()) {
						case UNKNOWN:
							unknownCounter++;
							break;
						case MODIFY_PREVIOUS:
							modifyPreviousCounter++;
							break;
						case MODIFY_SELF:
							modifySelfCounter++;
							break;
						}
					}

					if (unknownCounter >= modifyPreviousCounter
							&& unknownCounter >= modifySelfCounter)
						return getActionForTactic(Tactics.BESTNASH);
					else if (modifyPreviousCounter >= modifySelfCounter)
						return getActionForTactic(Tactics.EDGEPUSHER);
					else
						return getActionForTactic(Tactics.BESTNASH);
				} else {
					// Build opponent model
					return getActionForTactic(Tactics.RANDOM);
				}
			}
		}
	}

	/**
	 * The minimum required rounds needed to make an opponent model
	 */
	private static final int numberOfRoundForOpponentModel = 10;

	/**
	 * Determine whether we will ever have an opponent model significant enough
	 * 
	 * @return true iff there will never be a trusted opponent model
	 */
	private boolean thereWillNeverBeATrustedOpponentModel() {
		return (round / getTime()) < numberOfRoundForOpponentModel;
	}

	private boolean first = true;

	/**
	 * Determine whether the current opponent model is trustworthy based on the
	 * amount of rounds passed
	 * 
	 * @return true iff the opponent model is trusted
	 */
	private boolean weTrustOurOpponentModel() {
		return round > numberOfRoundForOpponentModel;
	}

	/**
	 * Determine whether the previous bid has been accepted many times by other
	 * parties
	 * 
	 * @return true iff enough parties have accepted the previous bid
	 */
	private boolean previousBidHasBeenAcceptedEnough() {
		// 0.7, because 2 otherParties, should result in 1 required accept
		int requiredAccepts = (int) ((getNumberOfParties() - 1) * 0.7);
		return lastAcceptCount != 0 && lastAcceptCount >= requiredAccepts;
	}

	/**
	 * Definition of the available tactics for this agent.
	 * 
	 * RANDOM - Offer a random bid above your reservation value BESTNASH - Offer
	 * the best Nash bid according to opponent models NOSTALGIAN - Offer the
	 * best bid that has ever been done by any agent ASOCIAL - Offer the best
	 * bid possible for you HARDTOGET - Offer a bid of 0.99 * the previous
	 * utility EDGEPUSHER - Offer a bid slightly better than the one before
	 * GIVEIN - Offer a bid near your reservation value THEFINGER - Leave the
	 * negotiation
	 */
	private enum Tactics {
		RANDOM, BESTNASH, NOSTALGIAN, ASOCIAL, HARDTOGET, EDGEPUSHER, GIVEIN, THEFINGER
	}

	/**
	 * Based on a specific tactic and the internal parameters, this will give an
	 * action to perform.
	 * 
	 * @param t
	 * @return
	 */
	private Action getActionForTactic(Tactics t) {
		System.out.println("Round " + round + " | Tactic: " + t);
		switch (t) {
		case RANDOM:
			// We don't want to bid under our reservation value
			List<BidDetails> randomBids = possibleBids
					.getBidsinRange(new Range(getUtilitySpace()
							.getReservationValue(), 1));
			if (randomBids.size() == 0) {
				return getActionForTactic(Tactics.GIVEIN);
			} else {
				return bid(randomBids.get(
						new Random().nextInt(randomBids.size())).getBid());
			}
		case BESTNASH:
			// In the assumption that our opponent does not do this as well,
			// else this will keep on giving the same bid
			return bid(nashBids.get(nashBids.size() - 1).getBid());
		case NOSTALGIAN:
			return bid(allBids.getBestBidDetails().getBid());
		case ASOCIAL:
			return bid(possibleBids.getMaxBidPossible().getBid());
		case HARDTOGET:
			return getOfferFromPreviousUtil(0.99);
		case EDGEPUSHER:
			// do a new bid that is a little better then last
			Bid lastBid = allBids.getLastBid();
			double lastUtil = getUtility(lastBid);
			List<BidDetails> allBetterBids = possibleBids
					.getBidsinRange(new Range(lastUtil, 1));
			// Get first that is better, since i don't know how getBidsInRange
			// is
			// sorted. Also I want to avoid picking the lastBid;
			for (BidDetails bd : allBetterBids) {
				if (bd.getMyUndiscountedUtil() > lastUtil)
					return bid(bd.getBid());
			}
			// No better bid to find, accept as well
			return new Accept();
		case GIVEIN:
			if (getTime() > 0.95)
				return new Accept();
			else {
				// double currentTime = getTime();
				// double discount = Math.max(1,
				// (-1.9531 * Math.pow(currentTime, 2))
				// + (2.2251 * currentTime) + 0.3626);
				// discount = 1.3 - (0.6 * currentTime);
				return getOfferFromPreviousUtil(reservationUtility);
			}
		case THEFINGER:
			return new EndNegotiation();
		default:
			break;
		}

		return getActionForTactic(Tactics.ASOCIAL);
	}

	/**
	 * Get an offer with discount times the utility of your last utility
	 * 
	 * @param discount
	 *            multiplication factor
	 * @return
	 */
	private Offer getOfferFromPreviousUtil(double discount) {
		BidDetails bid = possibleBids.getBidNearUtility(discount * lastUtility);
		return bid(bid.getBid());
	}

	/**
	 * 
	 * @return the partial of rounds done, or 0 when there is no deadline
	 */
	private double getTime() {
		if (this.deadlines != null) {
			Object d = this.deadlines.get(DeadlineType.ROUND);

			if (d != null && (int) d != 0) {
				return (double) this.round / (int) d;
			}
		}
		return getTimeLine().getTime();
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
		super.receiveMessage(sender, action);

		// Here you can listen to other parties' messages

		try {
			OpponentUtilityModel opponent = opponents.get(sender);
			if (opponent == null) {
				opponent = new OpponentUtilityModel(getUtilitySpace()
						.getDomain());
				opponents.put(sender, opponent);
			}
			Bid prevousBid = allBids.getLastBid();

			// Update opponent specific history
			if (action instanceof Offer) {
				// Update global history
				Bid bid = Action.getBidFromAction(action);
				BidDetails details = new BidDetails(bid, getUtility(bid));
				allBids.add(details);

				opponent.addOffer(prevousBid, bid);

				lastAcceptCount = 0;
			} else if (action instanceof Accept) {
				opponent.addAccept(prevousBid);

				lastAcceptCount++;
			} else if (action instanceof Inform) {
				// TODO handle info
				// Inform inform = (Inform) action;
				// if (inform.getName().equals("numParties")) {
				// int numParties = ((Integer) inform.getValue()).intValue();
				// for (int i = 0; i < numParties; i++) {
				// System.out
				// .println("Simon says: \"Welcome to the negotiation, party "
				// + (i + 1) + "!\"");
				// }
				// }
			} else {
				System.out.println("WARNING :: UNKNOWN ACTION :: "
						+ action.getClass().getCanonicalName());
			}
		} catch (InvalidDomainException | InvalidBidException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Method to set up some of the internal parameters.
	 * 
	 * Creates from the list of possible bids a list of bids sorted on Nash
	 * product, determined by the opponent models available.
	 */
	private void sortOutcomeSpaceOnNashProduct() {
		ArrayList<OpponentUtilityModel> opponentModels = new ArrayList<OpponentUtilityModel>();
		for (Entry<Object, OpponentUtilityModel> e : opponents.entrySet()) {
			opponentModels.add(e.getValue());
		}

		List<BidDetails> bids = possibleBids.getAllOutcomes();
		ArrayList<BidDetailsWithNash> nashes = new ArrayList<BidDetailsWithNash>();

		for (BidDetails bd : bids) {
			nashes.add(new BidDetailsWithNash(bd.getBid(),
					getNashUtilityProduct(bd.getBid(), opponentModels)));
		}

		Collections.sort(nashes, new Comparator<BidDetailsWithNash>() {
			@Override
			public int compare(BidDetailsWithNash lbdwn,
					BidDetailsWithNash rbdwn) {
				// Big value (1000000000) is because else the values would be so
				// small that Java would throw a 'MisuseOfContractException'
				return (int) (2000000000 * (lbdwn.getEstimatedNashValue() - rbdwn
						.getEstimatedNashValue()));
			}
		});

		nashBids = nashes;
	}

	/**
	 * Based on a list of opponent models, this function determines the Nash
	 * product for a certain bid.
	 * 
	 * @param b
	 *            Bid to evaluate
	 * @param opponentModels
	 *            list of opponents
	 * @return Nash product
	 */
	private double getNashUtilityProduct(Bid b,
			ArrayList<OpponentUtilityModel> opponentModels) {

		double res = getUtility(b);
		for (OpponentUtilityModel m : opponentModels) {
			try {
				double util = m.getUtility(b);
				if (!Double.isNaN(util))
					res *= util;
			} catch (InvalidBidException e) {
				e.printStackTrace();
			}
		}

		return res;
	}
}
