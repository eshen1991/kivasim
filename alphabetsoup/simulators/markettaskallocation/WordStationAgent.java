/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import alphabetsoup.base.WordStationBase;
import alphabetsoup.framework.Letter;
import alphabetsoup.framework.LetterType;
import alphabetsoup.framework.SimulationWorld;
import alphabetsoup.framework.Word;

/**
 * @author Chris Hazard
 *
 */
public class WordStationAgent extends WordStationBase {
	
	private MultiItemDoubleAuction<LetterType, Letter, ?, WordStationAgent> situatedMarket = null;

	public WordStationAgent(float station_radius, float bucket_to_letter_time, float word_completion_time, int station_capacity) {
		super(station_radius, bucket_to_letter_time, word_completion_time, station_capacity);
		
//		revenueFromWords = new ValueHistory(.9f, 10.0f, 100);
	}
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	private double lastBidUpdateTime = Double.NEGATIVE_INFINITY;

	private double profit = 0.0;
	
//	private ValueHistory revenueFromWords;
	
	/**letters of words that have not yet been allocated to a Bucket for completion
	 */
	private Map<Letter, Word> outstandingLetters = new HashMap<Letter, Word>();
	
	public void letterBought(Exchange<Letter, ?, WordStationAgent> e) {
		outstandingLetters.remove(e.buyerItem);
		profit -= e.value;
	}
	
	public float getMarketValueOfLetter(LetterType lt) {
//		TODO come up with other market-based valuations
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		float market_price = situatedMarket.getAskPrice(lt);
		if(market_price < economy.getWordCompletionLetterMarginalRevenue())
			return market_price;
		//shave a little off to get some stochasticness going (so not one word station gets them all)
		return economy.getWordCompletionLetterMarginalRevenue() * (1.0f - .5f * SimulationWorld.rand.nextFloat());
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return Math.min(lastBidUpdateTime + bidUpdateInterval, super.getNextEventTime(cur_time));
	}
	
	public void update(double last_time, double cur_time) {
		curTime = cur_time;
		
		WordOrderManager wom = SimulationWorldMarketTaskAllocation.getSimulationWorld().wordManager;
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;

		//update valuations on words in queue
		if(curTime >= lastBidUpdateTime + bidUpdateInterval) {
			lastBidUpdateTime = curTime;

			situatedMarket.removeBids(this);
			//place bids for all of the letters that are not being serviced
			for(Letter l : outstandingLetters.keySet()) {
//				TODO come up with better letter valuations
				situatedMarket.addBid(this, l.getType(), l, economy.getWordCompletionLetterMarginalRevenue());
			}

			
			for(Word w : wom.wordMarket.keySet()) {
				wom.wordMarket.get(w).removeAsks(this);
				float bid = 0.0f;
				for(Letter l : w.getOriginalLetters())
					bid += getMarketValueOfLetter(l.getType());
				wom.wordMarket.get(w).addAsk(this, bid);
			}
		}
		
		if(cur_time < blockedUntilTime)
			return;
		idleTime += cur_time - last_time;
		
		Word completed_word = removeAnyCompletedWord(cur_time); 
		if(completed_word != null) {
//TODO example of how to use value history
//			float word_value = economy.getRevenueForCompletingWord(completed_word);
//			revenueFromWords.addNewValue(word_value, cur_time);
			return;
		}
		
		takeLetterFromBucket(cur_time);
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.WordStation#assignWord(alphabetsoup.framework.Word)
	 */
	public void assignWord(Word w) {
		//actually assign the word
		super.assignWord(w);
		//record all the letters that will need to be obtained from the markets
		for(Letter l : w.getOriginalLetters())
			outstandingLetters.put(l, w);
	}
	
	public List<String> getAdditionalInfo() {
		DecimalFormat four_digits = new DecimalFormat("0.000");
		List<String> s = new ArrayList<String>();
/*		float value = revenueFromWords.getCurrentValue();
		float rate = revenueFromWords.getCurrentValueRate(curTime);
		s.add("Expected revenue per word: " + four_digits.format(value));
		s.add("Word completion rate (words/sec): " + four_digits.format(rate));
		s.add("Expected revenue rate: " + four_digits.format(value * rate));
*/		s.add("Current profit: " + four_digits.format(profit));
		return s;
	}

	/**
	 * @return the profit
	 */
	public double getProfit() {
		return profit;
	}

	/**
	 * @param profit the profit to set
	 */
	public void setProfit(double profit) {
		this.profit = profit;
	}

	/**
	 * @return the situatedMarket
	 */
	public MultiItemDoubleAuction<LetterType, Letter, ?, WordStationAgent> getSituatedMarket() {
		return situatedMarket;
	}

	/**
	 * @param situatedMarket the situatedMarket to set
	 */
	public void setSituatedMarket(
			MultiItemDoubleAuction<LetterType, Letter, ?, WordStationAgent> situatedMarket) {
		this.situatedMarket = situatedMarket;
	}
}
