package negotiator.group11;

import java.util.Arrays;

/**
 * Class to use for basic statistics.
 * 
 * Source:
 * http://stackoverflow.com/questions/7988486/how-do-you-calculate-the-variance-median-and-standard-deviation-in-c-or-java
 */
public class Statistics {

	/**
	 * Get the mean of dataset
	 * @param data
	 * @return
	 */
	public static double getMean(double[] data) {
		double sum = 0.0;
		for (double a : data)
			sum += a;
		return sum / data.length;
	}

	/**
	 * Get the variance of dataset
	 * @param data
	 * @return
	 */
	public static double getVariance(double[] data) {
		double mean = getMean(data);
		double temp = 0;
		for (double a : data)
			temp += (mean - a) * (mean - a);
		return temp / data.length;
	}

	/**
	 * Get the standard deviation of dataset
	 * @param data
	 * @return
	 */
	public static double getStdDev(double[] data) {
		return Math.sqrt(getVariance(data));
	}

	/**
	 * Get the median of dataset
	 * @param data
	 * @return
	 */
	public static double median(double[] data) {
		double[] b = new double[data.length];
		System.arraycopy(data, 0, b, 0, b.length);
		Arrays.sort(b);

		if (data.length % 2 == 0) {
			return (b[(b.length / 2) - 1] + b[b.length / 2]) / 2.0;
		} else {
			return b[b.length / 2];
		}
	}
}
