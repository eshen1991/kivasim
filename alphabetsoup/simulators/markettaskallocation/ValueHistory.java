/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**ValueHistory maintains a history of values for a particular entity
 * or a particular market, and allows for queries of the history.  The values may be prices, profits, etc.
 * Old values are weighted based on the time elapsed since they were recorded.  The formula for the 
 * weight of a value is discountFactor^(elapsedTime / discountPeriod).
 * @author Chris Hazard
 */
public class ValueHistory {
	
	public static class ValueAtTime {
		public double time;
		public float value;
		public ValueAtTime(float value, double time) {
			this.value = value;		this.time = time;
		}
	}
	
	/**Maintains the history of values, maximum of historyLength
	 */
	private Deque<ValueAtTime> valueHistory;
	
	private float currentValue;	//value based on exponentially weighted average
	
	private float discountFactor;
	private float discountPeriod;
	private int historyLength;
	
	/**LetterHistory constructor sets up initial values and data structures
	 * @param discountFactor exponent base for value weighting.
	 * 			A value after one discountPeriod has elapsed yields a discountFactor weight of the value 
	 * @param discountPeriod time for the weight to change from 1 to discountFactor
	 * @param historyLength number of historical values to keep (older ones are discarded)  
	 */
	public ValueHistory(float discountFactor, float discountPeriod, int historyLength) {
		this.discountFactor = discountFactor;	this.discountPeriod = discountPeriod;
		this.historyLength = historyLength;
		currentValue = 0.0f;

		valueHistory = new ArrayDeque<ValueAtTime>(historyLength);
	}
	
	/**Adds a new value for the specified time
	 * History is maintained in the order that new values are inserted.
	 * @param new_value
	 * @param time
	 */
	public void addNewValue(float new_value, double time) {
		ValueAtTime new_lvt = new ValueAtTime(new_value, time); 
		valueHistory.add(new_lvt);
		
		//see if need to purge some history
		while(valueHistory.size() > historyLength)
			valueHistory.remove();
		
		//compute the new exponentially weighted mean values
		double ave = 0.0;
		double weight_sum = 0.0;
		
		for(ValueAtTime vt : valueHistory) {
			double weight = Math.pow(discountFactor, (time - vt.time) / discountPeriod);
			double value = weight * vt.value;
			
			ave += value;
			weight_sum += weight;
		}
		
		//turn sum into average
		if(ave > 0.0 && weight_sum > 0.0)
			ave /= weight_sum;
		else
			ave = 0.0f;
		currentValue = (float)ave;
	}
	
	/**Computes the exponentially weighted average rate of values being entered based on the current time
	 * @param current_time
	 * @return
	 */
	public float getCurrentValueRate(double current_time) {
		//if only have 1 data point, then use the rate since the beginning
		if(valueHistory.size() <= 1) {
			if(valueHistory.size() == 0)
				return 0.0f;				//no rate if haven't recieved anything
			return (float)(1 / current_time);
		}
		
		//compute the new exponentially weighted mean value
		//each weight is computed by the earlier end of the period
		
		Iterator<ValueAtTime> i_vt = valueHistory.descendingIterator();
		double t = i_vt.next().time;
		//the weight of the first term is 1, because current_time - current time = 0,
		// so discountFactor ^ 0 = 1
		double first_value = 1 / (current_time - t);
		double prev_time = t;
		
		//compute the rest of the values and weights 
		double ave = 0.0;
		double weight_sum = 0.0;
		while(i_vt.hasNext()) {
			t = i_vt.next().time;
			double weight = Math.pow(discountFactor, (current_time - prev_time) / discountPeriod);
			//value is the weight multiplied by the rate (1/period) 
			double value = weight * 1 / (prev_time - t);
			
			prev_time = t;
			ave += value;
			weight_sum += weight;
		}
		
		//only allow the value to go down when no more events are arriving
		// because it can't go up since no events are arriving, and don't need to do additional calculations if equal
		if(first_value < ave / weight_sum) {
			ave += first_value;
			weight_sum += 1;	//weight of first term is 1, as described above 
		}
		
		//turn sum into average
		return (float)(ave / weight_sum);
	}

	/**
	 * @return the currentValue
	 */
	public float getCurrentValue() {
		return currentValue;
	}
}
