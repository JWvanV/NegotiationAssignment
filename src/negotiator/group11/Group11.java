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
import negotiator.Domain;
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
		// TODO check if it might be fun if we give other data to the parent
		// i.e. randomseed+1
		super(utilitySpace, deadlines, timeline, randomSeed);

		this.round = 0;
		this.lastUtility = 1;

		// create a list of bids
		possibleBids = new SortedOutcomeSpace(utilitySpace);
		allBids = new BidHistory();
		opponents = new HashMap<Object, OpponentUtilityModel>();

		utilitySpace.setReservationValue(reservationUtility);
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
		double currentTime = getTime();
		// if we are the first party, make the best offer.
		if (!validActions.contains(Accept.class)) {
			return bid(possibleBids.getMaxBidPossible().getBid());
		}

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
			reservationUtility *= 0.6;
		}

		BidDetails lastBid = allBids.getLastBidDetails();

		if (currentTime > 0.95
				|| lastBid.getMyUndiscountedUtil() > reservationUtility) {
			return new Accept();
		} else {
			if (weTrustOurOpponentModel()) {
				sortOutcomeSpaceOnNashProduct();

				// TODO do something with the aprox opponent tactics
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

				// TODO maybe do other strategies
				if (unknownCounter >= modifyPreviousCounter
						&& unknownCounter >= modifySelfCounter)
					return getActionForTactic(Tactics.BESTNASH);
				else if (modifyPreviousCounter >= modifySelfCounter)
					return getActionForTactic(Tactics.NOSTALGIAN);
				else
					return getActionForTactic(Tactics.BESTNASH);
			} else {
				if (thereWillNeverBeATrustedOpponentModel()) {
					if (currentTime < 0.25) {
						return getActionForTactic(Tactics.NOSTALGIAN);
					} else if (currentTime < 0.5) {
						if (previousBidHasBeenAcceptedEnough())
							return getActionForTactic(Tactics.EDGEPUSHER);
						else
							return getActionForTactic(Tactics.HARDTOGET);
					} else {
						return getActionForTactic(Tactics.GIVEIN);
					}
				} else {
					return getActionForTactic(Tactics.RANDOM);
				}
			}

			// Afknaps
			// if (lastBid.getMyUndiscountedUtil() < 0.3 && getTime() > 0.8) {
			// return new EndNegotiation();
			// }

			// If you were made a decent offer once, reoffer it, unless it is
			// the
			// last bid, then accept...
			// if (bestBid != null
			// && bestBid.getMyUndiscountedUtil() > 1 - getTime()) {
			// if (Math.abs(bestBid.getMyUndiscountedUtil()
			// - lastBid.getMyUndiscountedUtil()) < 0.05) {
			// return new Accept();
			// } else {
			// return bid(bestBid.getBid());
			// }
			// }

			// return getActionForTactic(Tactics.GIVEIN);
		}
	}

	private static final int numberOfRoundForOpponentModel = 20;

	private boolean thereWillNeverBeATrustedOpponentModel() {
		return (round / getTime()) < numberOfRoundForOpponentModel;
	}

	private boolean weTrustOurOpponentModel() {
		return round > numberOfRoundForOpponentModel;
	}

	private boolean previousBidHasBeenAcceptedEnough() {
		// 0.7, because 2 otherParties, should result in 1 required accept
		int requiredAccepts = (int) ((getNumberOfParties() - 1) * 0.7);
		return lastAcceptCount != 0 && lastAcceptCount >= requiredAccepts;
	}

	private enum Tactics {
		RANDOM, BESTNASH, NOSTALGIAN, ASOCIAL, HARDTOGET, EDGEPUSHER, GIVEIN, THEFINGER
	}

	private Action getActionForTactic(Tactics t) {
		switch (t) {
		case RANDOM:
			return bid(possibleBids
					.getAllOutcomes()
					.get(possibleBids.getIndexOfBidNearUtility(new Random()
							.nextDouble())).getBid());
		case BESTNASH:
			// In the assumption that our opponent does not do this as well,
			// else this will keep on giving the same bid
			return bid(nashBids.get(nashBids.size() - 1).getBid());
		case NOSTALGIAN:
			return bid(allBids.getBestBidDetails().getBid());
		case ASOCIAL:
			return bid(possibleBids.getMaxBidPossible().getBid());
		case HARDTOGET:
			if (getTime() < 0.8)
				return getOfferFromPreviousUtil(0.9);
			else
				return new Accept();
		case EDGEPUSHER:
			// do a new bid that is a little better then last
			Bid lastBid = allBids.getLastBid();
			double lastUtil = getUtility(lastBid);
			List<BidDetails> allBetterBids = possibleBids
					.getBidsinRange(new Range(lastUtil, 1));
			for (BidDetails bd : allBetterBids) {
				if (bd.getMyUndiscountedUtil() > lastUtil)
					return bid(bd.getBid());
			}
			// No better bid to find, accept as well
			return new Accept();
		case GIVEIN:
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
				return new Accept();
		case THEFINGER:
			return new EndNegotiation();
		default:
			break;
		}

		// TODO fix this fallback value
		return getActionForTactic(Tactics.ASOCIAL);
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
				return (int) (1000000 * (lbdwn.getEstimatedNashValue() - rbdwn
						.getEstimatedNashValue()));
			}
		});

		nashBids = nashes;
	}

	private double getNashUtilityProduct(Bid b,
			ArrayList<OpponentUtilityModel> opponentModels) {
		// System.out.println("NashUtilProd for bid " + b.toString());

		double res = getUtility(b);

		// System.out.println("- My: " + res);

		for (OpponentUtilityModel m : opponentModels) {
			try {
				double util = m.getUtility(b);
				if (!Double.isNaN(util)) {
					res *= util;
				} else {

				}

				// System.out.println("- Other: " + util + " => " + res);

			} catch (InvalidBidException e) {
				e.printStackTrace();
			}
		}

		// System.out.println("-- Total: " + res);

		return res;
	}
}
