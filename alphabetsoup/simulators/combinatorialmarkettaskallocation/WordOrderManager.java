/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

import java.util.HashMap;

import alphabetsoup.framework.Updateable;
import alphabetsoup.framework.Word;
import alphabetsoup.framework.WordList;
import alphabetsoup.framework.WordStation;

/**Basic simple implementation of a WordOrderManager, which dispenses incoming words to Word
 * stations as they have space.
 * @author Chris Hazard
 */
public class WordOrderManager implements Updateable {

	public HashMap<Word, DoubleAuction<Word, WordStationAgent, WordOrderManager>> wordMarket = new HashMap<Word, DoubleAuction<Word, WordStationAgent, WordOrderManager>>(); 
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	
	private double profit = 0.0;
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return curTime + bidUpdateInterval;
	}
	
	/**Creates a new market for the specified word.
	 * Places a single sell offer at the price of 0.0, since it just goes to the highest bidder when
	 * the station is free.
	 * @param w
	 */
	private void addNewWordToWordMarket(Word w) {
		DoubleAuction<Word, WordStationAgent, WordOrderManager> new_market = new DoubleAuction<Word, WordStationAgent, WordOrderManager>();
		//add the new word at a free price... only give it to the highest paying wordstation when it's free
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		float revenue_for_word = economy.getRevenueForCompletingWord(w);
		new_market.addBid(this, w, revenue_for_word);
		setProfit(getProfit() + revenue_for_word);
		wordMarket.put(w, new_market);
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {
		curTime = cur_time;
		
		WordList wl = SimulationWorldMarketTaskAllocation.getSimulationWorld().getWordList();
		LetterManager lm = ((LetterManager)((SimulationWorldMarketTaskAllocation)SimulationWorldMarketTaskAllocation.getSimulationWorld()).letterManager);
		
		//see if all done
		if(wl.getAvailableWords().size() == 0)
			return;
		
		//see if have all the words on the market,
		// if not, add them
		if(wordMarket.size() < wl.getAvailableWords().size()) {
			for(Word w : wl.getAvailableWords())
				if(!wordMarket.containsKey(w))
					addNewWordToWordMarket(w);
		}
		
		//check every word station to see if it's in need of a new word
		for(WordStation ws : SimulationWorldMarketTaskAllocation.getSimulationWorld().getWordStations()) {
			//give the station words if it needs them and is currently winning it
			// if a word station is winning more than one, then choose the one 
			// that has the largest bid-ask spread, that is, the one that it can do most efficiently
			if(ws.getAssignedWords().size() < ws.getCapacity()) {
				
				//of all words, see if this word station is currently winning any auctions
				// and if so, choose the one that takes the least cost
				int best_word_index = -1;
				float largest_spread = 0.0f;
				WordStationAgent wsa = (WordStationAgent)ws;
				for(int word_index = 0; word_index < wl.getAvailableWords().size(); word_index++) {
					Word w = wl.getAvailableWords().get(word_index);
					DoubleAuction<Word, WordStationAgent, WordOrderManager> market = wordMarket.get(w);
					
					//only look at winning results
					if(!market.isSellerInTradingSet(wsa))
						continue;
					
					//if the profit for this word is best, then use it
					float spread = market.getAskPrice() - market.getBidPrice();
					if(spread > largest_spread) {
						largest_spread = spread;
						best_word_index = word_index;
					}
				}
				
				//this word station wasn't winning anything
				if(best_word_index == -1)
					continue;
				
				//a word exchange is taking place!
				Word w = wl.takeAvailableWord(best_word_index);
				//make a market for the new word that came in by taking a word
				if(wl.getAvailableWords().size() > 0)
					addNewWordToWordMarket(wl.getAvailableWords().get(wl.getAvailableWords().size()-1));
				
				//get exchange, and then get rid of the market for that word
				Exchange e = wordMarket.get(w).acceptNextSellerExchange(wsa);
				wordMarket.remove(w);
				
				//assign the word
				wsa.assignWord(w);
				wsa.setProfit(wsa.getProfit() + e.value);
				
				setProfit(getProfit() - e.value);
				
				//let other things know that the word has been assigned
				lm.newWordAssignedToStation(ws, w);
			}
			
			//can't continue if out of words
			if(wl.getAvailableWords().size() == 0)
				return;
		}
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
}
