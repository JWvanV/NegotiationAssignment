package negotiator.group11;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import negotiator.Bid;
import negotiator.BidHistory;
import negotiator.Domain;
import negotiator.bidding.BidDetails;
import negotiator.boaframework.SortedOutcomeSpace;
import negotiator.issue.Issue;
import negotiator.issue.IssueDiscrete;
import negotiator.issue.IssueInteger;
import negotiator.issue.IssueReal;
import negotiator.issue.Objective;
import negotiator.issue.Value;
import negotiator.issue.ValueDiscrete;
import negotiator.issue.ValueInteger;
import negotiator.issue.ValueReal;
import negotiator.utility.Evaluator;
import negotiator.utility.EvaluatorDiscrete;
import negotiator.utility.UtilitySpace;

public class Opponent {
	private OpponentBidHistory allBids;
	private OpponentBidHistory acceptedBids;
	private HashMap<Value, Integer> issueValueCounter;

	public Opponent() {
		allBids = new OpponentBidHistory();
		acceptedBids = new OpponentBidHistory();
		issueValueCounter = new HashMap<Value, Integer>();
	}

	public void addAccept(Bid acceptBid) {
		acceptedBids.add(acceptBid, acceptBid);
		updateIssueValueCounter(acceptBid);
		// TODO argue if its smart or not to count an accept-bid double
		updateIssueValueCounter(acceptBid);
	}

	public void addOffer(Bid previousBid, Bid offerBid) {
		allBids.add(previousBid, offerBid);
		updateIssueValueCounter(offerBid);
	}

	private void updateIssueValueCounter(Bid b) {
		for (Issue i : b.getIssues()) {
			try {
				Value v = b.getValue(i.getNumber());
				int currentCount = issueValueCounter.get(v);
				currentCount++;
				issueValueCounter.put(v, currentCount);
			} catch (Exception e) {
				// System.out.println("THEFINGER: " + e.getLocalizedMessage());
			}
		}
	}

	public SortedOutcomeSpace getSortedOutcomeSpace(Domain d) {
		return new SortedOutcomeSpace(calculateUtilitySpace(d));
	}

	public UtilitySpace calculateUtilitySpace(Domain d) {
		UtilitySpace u = new UtilitySpace(d);

		HashMap<Issue, Double> issueVariances = new HashMap<Issue, Double>();
		int totalAmountOfMeasurementsPerIssue = allBids.getSize();

		for (Issue i : d.getIssues()) {
			System.out.println("Issue " + i.toString());

			double[] issueValueCounts = null;

			switch (i.getType()) {
			case DISCRETE:
				IssueDiscrete lIssueDiscrete = (IssueDiscrete) i;
				issueValueCounts = new double[lIssueDiscrete
						.getNumberOfValues()];
				for (int j = 0; j < lIssueDiscrete.getNumberOfValues(); j++) {
					// divide by totalAmountOfMeasurementsPerIssue, so it
					// becomes < 1, and will result in only positive weights
					ValueDiscrete vd = lIssueDiscrete.getValue(j);
					System.out.println("- Value: "
							+ ((vd == null) ? "null" : vd.getValue()));
					Integer valueCountSomething = issueValueCounter.get(vd);
					int valueCount = valueCountSomething == null ? 0
							: valueCountSomething;
					issueValueCounts[j] = ((double) valueCount)
							/ totalAmountOfMeasurementsPerIssue;

					try {
						EvaluatorDiscrete ed = (EvaluatorDiscrete) u
								.getEvaluator(i.getNumber());
						ed.setEvaluationDouble(vd, valueCount);
					} catch (Exception e1) {
						System.out.println("setEvaluationDouble EXCEPTION: "
								+ (e1 == null ? "null" : e1
										.getLocalizedMessage()));
						e1.printStackTrace();
					}
				}
				break;
			default:
				System.out.println("WARNING: Issue type " + i.getType()
						+ " is not supported!");
				// throw new Exception("issue type " + i.getType()
				// + " not supported");
				break;
			}

			if (issueValueCounts != null) {
				Statistics s = new Statistics(issueValueCounts);
				issueVariances.put(i, s.getVariance());
			}
		}

		double totalVariance = 0;
		for (Entry<Issue, Double> e : issueVariances.entrySet()) {
			totalVariance += e.getValue();
		}

		double amountOfRoomLeftToMakeVarancesSumUpToOne = 1 - totalVariance;
		double extraFreeVariancePointsPerIssue = amountOfRoomLeftToMakeVarancesSumUpToOne
				/ issueVariances.size();

		for (Entry<Issue, Double> e : issueVariances.entrySet()) {
			double weight = e.getValue() + extraFreeVariancePointsPerIssue;
			Issue i = e.getKey();
			Evaluator ev = getEvaluator(u, i);
			if (ev == null) {
				System.out.println("ERROR: NULL EVALUATOR");
			} else {
				ev.setWeight(weight);
			}
//			u.setWeight(i, weight);
			// TODO Stop this from throwing a nullpointer
//			u.lock(i);
		}
		u.normalizeChildren(d.getIssue(0).getParent());

		return u;
	}

	private Evaluator getEvaluator(UtilitySpace u, Issue i) {
		for (Entry<Objective, Evaluator> e : u.getEvaluators()) {
			if (e.getKey() == i)
				return e.getValue();
		}
		return null;
	}
}