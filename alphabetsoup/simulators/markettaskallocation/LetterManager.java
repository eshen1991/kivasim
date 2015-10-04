/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.util.*;

import alphabetsoup.framework.Letter;
import alphabetsoup.framework.LetterStation;
import alphabetsoup.framework.LetterType;
import alphabetsoup.framework.SimulationWorld;
import alphabetsoup.framework.Updateable;
import alphabetsoup.framework.Word;
import alphabetsoup.framework.WordStation;
/**Basic simple implementation of a Letter Manager, which dispenses incoming letters to Letter
 * stations as they have space. The Letter Manager notified when a new Word is assigned to a WordStation.
 * @author Chris Hazard
 */
public class LetterManager implements Updateable {
	
	//letters that have been requested but haven't been dispensed
	protected List<Letter> requiredLetters = new ArrayList<Letter>();
	
	//letters that have been dispensed that haven't yet been requested
	protected List<Letter> surplusLetters = new ArrayList<Letter>();
	
	private float letterBundleCost;
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	
	private double profit = 0.0;
	
	public LetterManager(float letter_bundle_cost) {
		letterBundleCost = letter_bundle_cost;
	}
	
	public HashMap<LetterType, DoubleAuction<LetterType, LetterManager, LetterStationAgent>> letterMarket = new HashMap<LetterType, DoubleAuction<LetterType, LetterManager, LetterStationAgent>>();

	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return curTime + bidUpdateInterval;
	}
	
	/**Creates a new market for the specified LetterType.
	 * Places a single sell offer at the price of 0.0, since it just goes to the highest bidder when
	 * the station is free.
	 * @param w
	 */
	public void addNewLetterToLetterMarket(LetterType lt) {
		DoubleAuction<LetterType, LetterManager, LetterStationAgent> market = letterMarket.get(lt);
		if(market == null) {
			market = new DoubleAuction<LetterType, LetterManager, LetterStationAgent>();
			letterMarket.put(lt, market);
		}
		
		//add the new word at a free price... only give it to the highest paying wordstation when it's free
		market.addAsk(this, lt, letterBundleCost);
	}
	
	/**Called by the system when a new word has been assigned to a Wordstation.
	 * It is useful as it indicates what new letters need to be filled in the system (based on the word).
	 * @param s WordStation the Word was assigned to
	 * @param w Word assigned
	 */
	public void newWordAssignedToStation(WordStation s, Word w) {
		//for every letter in the word
		for(Letter l : w.getOriginalLetters()) {
			
			//see if the letter has already been put into the system as surplus
			boolean has_been_added = false;
			for(Letter m : surplusLetters) {
				//if the letter has already been dispensed, then it's not surplus anymore
				if(l.doesMatch(m)) {
					surplusLetters.remove(m);
					has_been_added = true;
					break;
				}
			}
			
			//don't add a letter that's already been added
			if(has_been_added)
				continue;
			
			//add a new market for this letter
			addNewLetterToLetterMarket(l.getType());
			setProfit(getProfit() - letterBundleCost);
			
			//add this letter to the required list
			requiredLetters.add(l.clone());
			int bundle_size = SimulationWorld.getSimulationWorld().letterStations[0].getBundleSize();
			for(int i = 1; i < bundle_size; i++)
				surplusLetters.add(l.clone());
		}
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {
		curTime = cur_time;
		
		//nothing for sale, so can't do anything
		if(letterMarket.size() == 0)
			return;
		
		//check every letter station and see if it needs a new letter
		for(LetterStation ls : SimulationWorldMarketTaskAllocation.getSimulationWorld().getLetterStations()) {
			//give the station letters if it needs them
			if(ls.getAssignedLetters().size() < ls.getCapacity()) {
				
				//of all words, see if this word station is currently winning any auctions
				// and if so, choose the one that provides the greatest profit
				// over the final revenue of the word
				int best_letter_index = -1;
				float highest_profit = Float.NEGATIVE_INFINITY;
				LetterStationAgent lsa = (LetterStationAgent)ls;
				for(int letter_index = 0; letter_index < requiredLetters.size(); letter_index++) {
					Letter l = requiredLetters.get(letter_index);
					LetterType lt = l.getType();
					DoubleAuction<LetterType, LetterManager, LetterStationAgent> market = letterMarket.get(lt);
					
					//only look at winning results
					if(!market.isBuyerInTradingSet(lsa))
						continue;
					
					//if the profit for this letter is best, then use it
					float profit = market.getBidPrice();
					if(profit > highest_profit) {
						highest_profit = profit;
						best_letter_index = letter_index;
					}
				}
				
				//this letter station wasn't winning anything
				if(best_letter_index == -1)
					continue;
				
				//a word exchange is taking place!
				Letter l = requiredLetters.remove(best_letter_index);
				
				//get exchange, and get rid of the market for that letter if no more sales exist
				Exchange e = letterMarket.get(l.getType()).acceptNextBuyerExchange(lsa);
				if(letterMarket.get(l.getType()).getNumAsks() == 0)
					letterMarket.remove(l.getType());
				
				//assign the letter
				lsa.addBundle(l);
				lsa.setProfit(lsa.getProfit() - e.value);
				setProfit(getProfit() + e.value);
			}
		}
	}

	/**
	 * @return the letterBundleCost
	 */
	public float getLetterBundleCost() {
		return letterBundleCost;
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
