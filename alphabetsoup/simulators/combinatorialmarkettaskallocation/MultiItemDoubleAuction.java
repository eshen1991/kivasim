/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * @author Chris Hazard
 *
 */
public class MultiItemDoubleAuction<ItemType, ItemInstanceType, SellerType, BuyerType> {

	public HashMap<ItemType, DoubleAuction<ItemInstanceType, SellerType, BuyerType>> market = new HashMap<ItemType, DoubleAuction<ItemInstanceType, SellerType, BuyerType>>();

	
	public void addBid(BuyerType buyer, ItemType itemType, ItemInstanceType item, float value) {
		DoubleAuction<ItemInstanceType, SellerType, BuyerType> item_market = market.get(itemType);
		if(item_market == null)
			market.put(itemType, item_market = new DoubleAuction<ItemInstanceType, SellerType, BuyerType>());
		item_market.addBid(buyer, item, value);
	}
	
	public void addAsk(SellerType seller, ItemType itemType, ItemInstanceType item, float value) {
		DoubleAuction<ItemInstanceType, SellerType, BuyerType> item_market = market.get(itemType);
		if(item_market == null)
			market.put(itemType, item_market = new DoubleAuction<ItemInstanceType, SellerType, BuyerType>());
		item_market.addAsk(seller, item, value);
	}
	
	/**Removes all bids for the specified buyer.
	 * @param buyer
	 */
	public void removeBids(BuyerType buyer) {
		List<ItemType> keys = new ArrayList<ItemType>(market.keySet());
		for(ItemType it : keys) {
			DoubleAuction<ItemInstanceType, SellerType, BuyerType> c = market.get(it);
			c.removeBids(buyer);
			if(c.getNumAsks() + c.getNumBids() == 0)
				market.remove(it);
		}
	}
	
	/**Removes all asks for the specified seller
	 * @param seller
	 */
	public void removeAsks(SellerType seller) {
		List<ItemType> keys = new ArrayList<ItemType>(market.keySet());
		for(ItemType it : keys) {
			DoubleAuction<ItemInstanceType, SellerType, BuyerType> c = market.get(it);
			c.removeAsks(seller);
			if(c.getNumAsks() + c.getNumBids() == 0)
				market.remove(it);
		}
	}
	
	/**Returns a list of every ItemType that currently has a bid to be bought
	 * @return
	 */
	public List<ItemType> getItemTypesWithBids() {
		List<ItemType> l = new ArrayList<ItemType>();
		for(ItemType lt : market.keySet())
			if(market.get(lt).getNumBids() > 0)
				l.add(lt);
		return l;
	}
	
	/**Returns a list of every ItemType that is currently being sold
	 * @return
	 */
	public List<ItemType> getItemTypesWithAsks() {
		List<ItemType> l = new ArrayList<ItemType>();
		for(ItemType lt : market.keySet())
			if(market.get(lt).getNumAsks() > 0)
				l.add(lt);
		return l;
	}
	
	/**Returns the current bid price of the specified market: (M+1)st price.
	 * @return
	 */
	public float getBidPrice(ItemType t) {
		if(market.containsKey(t))
			return market.get(t).getBidPrice();
		return 0.0f;
	}
	
	/**Returns the current ask price of the market: Mth price.
	 * @return
	 */
	public float getAskPrice(ItemType t) {
		if(market.containsKey(t))
			return market.get(t).getAskPrice();
		return Float.POSITIVE_INFINITY;
	}
	
	/**Returns true if the specified buyer will be allocated an item
	 * @param buyer
	 * @return
	 */
	public boolean isBuyerInTradingSet(BuyerType buyer, ItemType t) {
		return market.containsKey(t) && market.get(t).isBuyerInTradingSet(buyer);
	}
	
	/**Returns true if the specified seller will sell the specified item
	 * @param buyer
	 * @return
	 */
	public boolean isSellerInTradingSet(SellerType seller, ItemType t) {
		return market.containsKey(t) && market.get(t).isSellerInTradingSet(seller);
	}
	
	/**Clears the markets at the specified price only for those buyers and sellers specified.
	 * Returns a list of the exchanges that take place.
	 * @param buyers
	 * @param sellers
	 * @return
	 */
	public List<Exchange<ItemInstanceType, SellerType, BuyerType>> clearMarkets(Set<BuyerType> buyers, Set<SellerType> sellers) {
		List<Exchange<ItemInstanceType, SellerType, BuyerType>> exchanges = new ArrayList<Exchange<ItemInstanceType, SellerType, BuyerType>>();
		for(DoubleAuction<ItemInstanceType, SellerType, BuyerType> auction : market.values())
			exchanges.addAll(auction.acceptAllExchangesFrom(buyers, sellers, auction.getBidPrice()));
		return exchanges;
	}

	public List<String> getAdditionalInfo() {
		DecimalFormat four_digits = new DecimalFormat("0.000");
		List<String> s = new ArrayList<String>();
		
		for(ItemType t : market.keySet()) {
			float ask_price = getAskPrice(t);
			String ask_price_string = "-----"; 
			if(!Float.isInfinite(ask_price))
				ask_price_string = four_digits.format(ask_price);
			s.add(t + ":  ask:(" + market.get(t).getNumAsks() + "): " + ask_price_string
					+ "   bid:(" + market.get(t).getNumBids() + "): " + four_digits.format(getBidPrice(t)));
		}
		return s;
	}
}
