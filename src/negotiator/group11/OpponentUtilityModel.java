package negotiator.group11;

import java.util.HashMap;
import java.util.Map.Entry;

import negotiator.Bid;
import negotiator.Domain;
import negotiator.group11.OpponentBidHistory.BidModificationStrategy;
import negotiator.issue.ISSUETYPE;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;

/*
 * NOTE: Only supports (explicitly) Discrete Issue values
 */
public class OpponentUtilityModel {

	private OpponentBidHistory allBids;
	private OpponentBidHistory acceptedBids;

	private HashMap<IssueDiscrete, Double> issueWeights;
	private HashMap<IssueDiscrete, HashMap<ValueDiscrete, Integer>> valueCounts;

	public OpponentUtilityModel(Domain d) throws InvalidDomainException {

		allBids = new OpponentBidHistory();
		acceptedBids = new OpponentBidHistory();

		issueWeights = new HashMap<IssueDiscrete, Double>();
		valueCounts = new HashMap<IssueDiscrete, HashMap<ValueDiscrete, Integer>>();

		double defaultIssueWeight = 1.0 / d.getIssues().size();

		for (Issue i : d.getIssues()) {
			switch (i.getType()) {
			case DISCRETE:
				IssueDiscrete id = (IssueDiscrete) i;
				issueWeights.put(id, defaultIssueWeight);

				HashMap<ValueDiscrete, Integer> valueCount = new HashMap<ValueDiscrete, Integer>();
				for (int j = 0; j < id.getNumberOfValues(); j++)
					valueCount.put(id.getValue(j), 0);

				valueCounts.put(id, valueCount);
				break;
			default:
				throw new InvalidDomainException(i.getType());
			}
		}
	}

	public BidModificationStrategy getMostLikelyStrategy() {
		return allBids.getMostLikelyStrategy();
	}

	public void addAccept(Bid acceptBid) throws InvalidBidException {
		acceptedBids.add(acceptBid, acceptBid);
		updateCountersFromBid(acceptBid);
		// TODO argue if its smart or not to count an accept-bid double
		updateCountersFromBid(acceptBid);
	}

	public void addOffer(Bid previousBid, Bid offerBid)
			throws InvalidBidException {
		allBids.add(previousBid, offerBid);
		updateCountersFromBid(offerBid);
	}

	private void updateCountersFromBid(Bid b) throws InvalidBidException {
		for (Issue i : b.getIssues()) {
			switch (i.getType()) {
			case DISCRETE:
				try {
					HashMap<ValueDiscrete, Integer> valueCount = valueCounts
							.get((IssueDiscrete) i);
					ValueDiscrete v = (ValueDiscrete) b.getValue(i.getNumber());
					if (v == null) {
						throw new InvalidBidException(i.getType());
					} else {
						int currentCount = valueCount.get(v);
						currentCount++;
						valueCount.put(v, currentCount);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			default:
				throw new InvalidBidException(i.getType());
			}
		}
		updateWeightsFromCounters();
	}

	private void updateWeightsFromCounters() {
		double totalAmountOfMeasurementsPerIssue = allBids.getSize();

		HashMap<IssueDiscrete, Double> issueVariances = new HashMap<IssueDiscrete, Double>();

		for (Entry<IssueDiscrete, HashMap<ValueDiscrete, Integer>> e : valueCounts
				.entrySet()) {
			HashMap<ValueDiscrete, Integer> valueCount = e.getValue();

			double[] issueValueCounts = new double[valueCount.size()];
			int counter = 0;
			for (Entry<ValueDiscrete, Integer> entry : valueCount.entrySet()) {
				// dividing by the total to ensure that the result and total sum
				// < 1;
				issueValueCounts[counter] = entry.getValue()
						/ totalAmountOfMeasurementsPerIssue;
				counter++;
			}

			issueVariances.put(e.getKey(),
					Statistics.getVariance(issueValueCounts));

		}

		double totalVariance = 0;
		for (Entry<IssueDiscrete, Double> e : issueVariances.entrySet()) {
			totalVariance += e.getValue();
		}

		double amountOfRoomLeftToMakeVarancesSumUpToOne = 1 - totalVariance;
		double extraFreeVariancePointsPerIssue = amountOfRoomLeftToMakeVarancesSumUpToOne
				/ issueVariances.size();

		for (Entry<IssueDiscrete, Double> e : issueVariances.entrySet()) {
			double weight = e.getValue() + extraFreeVariancePointsPerIssue;
			issueWeights.put(e.getKey(), weight);
		}
	}

	public double getUtility(Bid b) throws InvalidBidException {
		double utility = 0;
		for (Issue i : b.getIssues()) {
			switch (i.getType()) {
			case DISCRETE:
				IssueDiscrete id = (IssueDiscrete) i;
				utility += issueWeights.get(id) * getIssueEvaluation(id, b);
				break;
			default:
				throw new InvalidBidException(i.getType());
			}
		}

		return utility;
	}

	private double getIssueEvaluation(IssueDiscrete i, Bid b)
			throws InvalidBidException {
		try {
			Value v = b.getValue(i.getNumber());
			switch (v.getType()) {
			case DISCRETE:
				ValueDiscrete vd = (ValueDiscrete) v;
				HashMap<ValueDiscrete, Integer> valueCount = valueCounts.get(i);
				double max = getMaxValue(valueCount);
				return valueCount.get(vd) / max;
			default:
				throw new InvalidBidException(v.getType());
			}
		} catch (Exception e) {
			if (e instanceof InvalidBidException)
				throw (InvalidBidException) e;
			else
				e.printStackTrace();
			return 0;
		}
	}

	private double getMaxValue(HashMap<ValueDiscrete, Integer> counts) {
		double max = 0;
		for (Entry<ValueDiscrete, Integer> e : counts.entrySet())
			max = Math.max(max, e.getValue());
		return max;
	}

	class InvalidDomainException extends Exception {
		private static final long serialVersionUID = -6947113453964713361L;

		public InvalidDomainException(ISSUETYPE issueType) {
			super("Domains with issues of type " + issueType
					+ " are not supported!");
		}
	}

	class InvalidBidException extends Exception {
		private static final long serialVersionUID = -801096984481420822L;

		public InvalidBidException(ISSUETYPE issueType) {
			super("Bids with issues of type " + issueType
					+ " are not supported!");
		}
	}
}
