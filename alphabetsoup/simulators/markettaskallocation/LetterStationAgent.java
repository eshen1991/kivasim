/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import alphabetsoup.base.LetterStationBase;
import alphabetsoup.framework.Letter;
import alphabetsoup.framework.LetterType;

/**
 * @author Chris Hazard
 *
 */
public class LetterStationAgent extends LetterStationBase {
	
	private MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, ?> situatedMarket = null;
	
	public LetterStationAgent(float station_radius, float letter_to_bucket_time, int bundle_size, int station_capacity) {
		super(station_radius, letter_to_bucket_time, bundle_size, station_capacity);
	}
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	private double lastBidUpdateTime = Double.NEGATIVE_INFINITY;

	private double profit = 0.0;
	
	List<Letter> outstandingLetterBundles = new ArrayList<Letter>();
	
	public void letterBundleSold(Exchange<Letter, LetterStationAgent, ?> e) {
		outstandingLetterBundles.remove(e.sellerItem);
		profit += e.value;
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return Math.min(lastBidUpdateTime + bidUpdateInterval, super.getNextEventTime(cur_time));
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {
		curTime = cur_time;
		SimulationWorldMarketTaskAllocation sw = SimulationWorldMarketTaskAllocation.getSimulationWorld(); 
		
		LetterManager lm = sw.letterManager;
		
		//update valuations on letters in queue
		if(curTime >= lastBidUpdateTime + bidUpdateInterval) {
			lastBidUpdateTime = curTime;
			
			situatedMarket.removeAsks(this);
			//place bids for all of the letter bundles that the letter station currently has
			for(Letter l : outstandingLetterBundles) {
				//TODO come up with better letter valuations
				situatedMarket.addAsk(this, l.getType(), l, 0.0f);
			}
			
			Economy economy = sw.economy;
			//float price_range = (getBundleSize() * economy.getWordCompletionLetterMarginalRevenue()) - lm.getLetterBundleCost();
			for(LetterType lt : lm.letterMarket.keySet()) {
				//TODO come up with real valuations here based on markets
				lm.letterMarket.get(lt).removeBids(this);
				
				//determine price to ask for a letter,
				// based on the prices of the letters at each WordStation, weighted
				// inversely to the distance
				// prices are also scaled after taking off the base bundle cost,
				// and clamped to the bundle cost
				float ask_price = 0.0f;
				float distance_weight = 0.0f;
				for(MultiItemDoubleAuction<LetterType, Letter, BucketAgent, WordStationAgent> m : economy.letterToWordMarkets) {
					float price = m.getBidPrice(lt);
					float dist = 1 / economy.getMarketLocation(m).getDistance(this);
					price -= lm.getLetterBundleCost();
					price /= 3;
					price += lm.getLetterBundleCost();
					ask_price += dist * Math.max(price, lm.getLetterBundleCost());
					distance_weight += dist;
				}
				//ask_price /= economy.letterToWordMarkets.size();
				ask_price /= distance_weight;
				lm.letterMarket.get(lt).addBid(this, ask_price);
			}
		}
		
		if(cur_time < blockedUntilTime)
			return;
		idleTime += cur_time - last_time;
		
		giveLetterBundleToBucket(cur_time);
	}
	
	public List<String> getAdditionalInfo() {
		DecimalFormat four_digits = new DecimalFormat("0.000");
		List<String> s = new ArrayList<String>();
		s.add("Current profit: " + four_digits.format(profit));
		return s;
	}
	
	public void addBundle(Letter l) {
		//actually assign the bundle
		super.addBundle(l);
		outstandingLetterBundles.add(l);
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
	public MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, ?> getSituatedMarket() {
		return situatedMarket;
	}

	/**
	 * @param situatedMarket the situatedMarket to set
	 */
	public void setSituatedMarket(
			MultiItemDoubleAuction<LetterType, Letter, LetterStationAgent, ?> situatedMarket) {
		this.situatedMarket = situatedMarket;
	}
}
