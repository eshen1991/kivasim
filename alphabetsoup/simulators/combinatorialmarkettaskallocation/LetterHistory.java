/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;

import alphabetsoup.framework.Letter;

/**LetterHistory maintains the history of values for all letters for a particular entity
 * or a particular market, and allows for queries of the history.  The values may be prices, profits, times, etc.
 * Old values are weighted based on the time elapsed since they were recorded.  The formula for the 
 * weight of a value is discountFactor^(elapsedTime / discountPeriod).   
 * @author Chris Hazard
 */
public class LetterHistory {
	
	public class LetterValueAtTime {
		public double time;
		public Letter letter;
		public float value;
		public LetterValueAtTime(Letter letter, float value, double time) {
			this.letter = letter;	this.value = value;		this.time = time;
		}
	}

	public class ComputedValue {
		public LetterValueAtTime lvt;
		public float value;
		public ComputedValue(float value, LetterValueAtTime lvt) {
			this.value = value;	this.lvt = lvt;
		}
	}
	
	/**Maintains the history of values, maximum of historyLength
	 */
	private Deque<LetterValueAtTime> valueHistory;
	
	private HashMap<Character, ComputedValue> letterValue = new HashMap<Character, ComputedValue>();
	private HashMap<Integer, ComputedValue> colorValue = new HashMap<Integer, ComputedValue>();
	private HashMap<Character, HashMap<Integer, ComputedValue>> letterTileValue = new HashMap<Character, HashMap<Integer, ComputedValue>>(); 
	
	private float discountFactor;
	private float discountPeriod;
	private int historyLength;
	
	/**LetterHistory constructor sets up initial values and data structures
	 * @param discountFactor exponent base for value weighting.
	 * 			A value after one discountPeriod has elapsed yields a discountFactor weight of the value 
	 * @param discountPeriod time for the weight to change from 1 to discountFactor
	 * @param historyLength number of historical values to keep (older ones are discarded)  
	 */
	public LetterHistory(float discountFactor, float discountPeriod, int historyLength) {
		this.discountFactor = discountFactor;	this.discountPeriod = discountPeriod;
		this.historyLength = historyLength;
		
		valueHistory = new ArrayDeque<LetterValueAtTime>(historyLength);
	}
	
	/**Adds a new value for a Letter at the specified time
	 * History is maintained in the order that new values are inserted.
	 * @param letter
	 * @param new_value
	 * @param time
	 */
	public void addNewValue(Letter letter, float new_value, double time) {
		LetterValueAtTime new_lvt = new LetterValueAtTime(letter, new_value, time); 
		valueHistory.add(new_lvt);
		
		//see if need to purge some history
		while(valueHistory.size() > historyLength) {
			LetterValueAtTime lvt = valueHistory.remove();
			//if the last time a letter or color was updated was for this event entry
			// then we know that it's no longer used
			if(lvt == letterValue.get(lvt.letter.getLetter()).lvt)
				letterValue.remove(lvt.letter.getLetter());
			if(lvt == colorValue.get(lvt.letter.getColorID()).lvt)
				letterValue.remove(lvt.letter.getColorID());
			
			HashMap<Integer, ComputedValue> letter_values = letterTileValue.get(lvt.letter.getLetter());
			if(lvt == letter_values.get(lvt.letter.getColorID()).lvt) {
				letter_values.remove(lvt.letter.getColorID());
				//see if that was the last letter of that color, if so, remove that hash
				if(letter_values.size() == 0)
					letterTileValue.remove(lvt.letter.getLetter());
			}
		}
		
		//compute the new exponentially weighted mean values for this letter
		double color_ave = 0.0;
		double color_weight_sum = 0.0;
		double letter_ave = 0.0;
		double letter_weight_sum = 0.0;
		double letter_color_ave = 0.0;
		double letter_color_weight_sum = 0.0;
		
		for(LetterValueAtTime lvt : valueHistory) {
			//don't compute anything if it doesn't match
			if(lvt.letter.getLetter() != letter.getLetter()
					&& lvt.letter.getColorID() != letter.getColorID())
				continue;
			
			double weight = Math.pow(discountFactor, (time - lvt.time) / discountPeriod);
			double value = weight * lvt.value;
			
			//if matching color, accumulate color value
			if(lvt.letter.getColorID() == letter.getColorID()) {
				color_ave += value;
				color_weight_sum += weight;
				
				//if also matching letter, accumulate for the combo
				if(lvt.letter.getLetter() == letter.getLetter()) {
					letter_color_ave += value;
					letter_color_weight_sum += weight;
				}
			}
			
			//if matching letter, accumulate letter value
			if(lvt.letter.getLetter() == letter.getLetter()) {
				letter_ave += value;
				letter_weight_sum += weight;
			}
		}
		
		//turn sums into averages
		color_ave /= color_weight_sum;
		letter_ave /= letter_weight_sum;
		letter_color_ave /= letter_color_weight_sum;
		
		//if haven't dealt with this new letter before, then add it to letterTileValue
		HashMap<Integer, ComputedValue> letter_values = letterTileValue.get(letter.getLetter());
		if(letter_values == null) {
			letter_values = new HashMap<Integer, ComputedValue>();
			letterTileValue.put(letter.getLetter(), letter_values);
		}

		//insert new values
		letter_values.put(letter.getColorID(), new ComputedValue((float)letter_color_ave, new_lvt));
		letterValue.put(letter.getLetter(), new ComputedValue((float)letter_ave, new_lvt));
		colorValue.put(letter.getColorID(), new ComputedValue((float)color_ave, new_lvt));
	}
	
	/**Returns the value of the given letter (for any color) based on historical data 
	 * @param l
	 * @return
	 */
	float getValueOfLetter(char l) {
		ComputedValue cv = letterValue.get(l);
		if(cv == null)
			return 0.0f;
		return cv.value;
	}
	
	/**Returns the value of the given color (for any letter) based on historical data
	 * @param c
	 * @return
	 */
	float getValueOfColor(int c) {
		ComputedValue cv = colorValue.get(c);
		if(cv == null)
			return 0.0f;
		return cv.value;
	}
	
	/**Returns the value of the given letter tile (letter and color) based on historical data
	 * @param l
	 * @return
	 */
	float getValueOfLetterTile(Letter l) {
		HashMap<Integer, ComputedValue> letter_values = letterTileValue.get(l.getLetter());
		if(letter_values == null)
			return 0.0f;
		ComputedValue cv = letter_values.get(l.getColorID());
		if(cv == null)
			return 0.0f;
		return cv.value;
	}
	
	public class LetterValuePair implements Comparable<LetterValuePair> {
		public char letter;
		public float value;
		public LetterValuePair(char letter, float value) {
			this.letter = letter;	this.value = value;
		}
	    public int compareTo(LetterValuePair o) {
	    	if(value < o.value)	return +1;
	    	if(value > o.value)	return -1;
	    	return 0;
	    }
	}
	/**Returns a List of all letters sorted descending by value 
	 * @return
	 */
	List<LetterValuePair> getHighestLetterValues() {
		//get all the possible values
		ArrayList<LetterValuePair> letters = new ArrayList<LetterValuePair>();
		for(ComputedValue cv : letterValue.values())
			letters.add(new LetterValuePair(cv.lvt.letter.getLetter(), cv.value));
		Collections.sort(letters);
		return letters;
	}
	
	public class ColorValuePair implements Comparable<ColorValuePair> {
		public int color;
		public float value;
		public ColorValuePair(int color, float value) {
			this.color = color;	this.value = value;
		}
	    public int compareTo(ColorValuePair o) {
	    	if(value < o.value)	return +1;
	    	if(value > o.value)	return -1;
	    	return 0;
	    }
	}
	/**Returns a List of all letters sorted descending by value 
	 * @return
	 */
	List<ColorValuePair> getHighestColorValues() {
		//get all the possible values
		ArrayList<ColorValuePair> colors = new ArrayList<ColorValuePair>();
		for(ComputedValue cv : colorValue.values())
			colors.add(new ColorValuePair(cv.lvt.letter.getColorID(), cv.value));
		Collections.sort(colors);
		return colors;
	}
	
	public class LetterTileValuePair implements Comparable<LetterTileValuePair> {
		public Letter letter;
		public float value;
		public LetterTileValuePair(Letter letter, float value) {
			this.letter = letter;	this.value = value;
		}
	    public int compareTo(LetterTileValuePair o) {
	    	if(value < o.value) return +1;
	    	if(value > o.value)	return -1;
	    	return 0;
	    }
	}
	/**Returns a List of all letters sorted descending by value 
	 * @return
	 */
	List<LetterTileValuePair> getHighestLetterTileValues() {
		//get all the possible values
		ArrayList<LetterTileValuePair> letters = new ArrayList<LetterTileValuePair>();
		for(HashMap<Integer, ComputedValue> letter_values : letterTileValue.values())
			for(ComputedValue cv : letter_values.values())
				letters.add(new LetterTileValuePair(cv.lvt.letter, cv.value));
		Collections.sort(letters);
		return letters;
	}
}
