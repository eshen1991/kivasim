/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

import java.util.*;

/**
 * @author Chris Hazard
 *
 */
public class DoubleAuction<ItemType, SellerType, BuyerType> {

	/**List that maintains all of the offers, always sorted descending by value.
	 */
	public List<Exchange<ItemType, SellerType, BuyerType>> offers = new ArrayList<Exchange<ItemType, SellerType, BuyerType>>();
	
	/**Counter of the number of items currently for sale 
	 */
	private int numItemsForSale = 0;

	/**Adds a new buy bid for the given buyer for the given value.
	 * @param buyer
	 * @param value
	 */
	public void addBid(BuyerType buyer, float value) {
		addBid(buyer, null, value);
	}
	
	/**Adds a new bid for the given buyer for the given value.  Maintains an item identifier
	 * for the buyer to use if the transaction succeeds.
	 * @param buyer
	 * @param item
	 * @param value
	 */
	public void addBid(BuyerType buyer, ItemType item, float value) {		
		Exchange<ItemType, SellerType, BuyerType> bid = new Exchange<ItemType, SellerType, BuyerType>(
				null, null, buyer, item, value); 
		int index = Collections.binarySearch(offers, bid);
		if(index < 0)
			index = -index-1;
		offers.add(index, bid);
	}
	
	/**Adds a new ask for the given seller to sell at the given value.
	 * @param buyer
	 * @param value
	 */
	public void addAsk(SellerType seller, float value) {
		addAsk(seller, null, value);
	}
	
	/**Adds a new ask for the given seller to sell at the given value.  Maintains an item identifier
	 * for the seller to use if the transaction succeeds.
	 * @param buyer
	 * @param value
	 */
	public void addAsk(SellerType seller, ItemType item, float value) {
		Exchange<ItemType, SellerType, BuyerType> ask = new Exchange<ItemType, SellerType, BuyerType>(
				seller, item, null, null, value);
		//insert the item into the appropriate location
		int index = Collections.binarySearch(offers, ask);
		if(index < 0)
			index = -index-1;
		offers.add(index, ask);	//binarySearch returns negative for where it should go if it doesn't exist
		
		//keep track of the number of items for sale for Mth and M+1st prices
		numItemsForSale++;
	}
	
	/**Removes all bids for the specified buyer.
	 * @param buyer
	 */
	public void removeBids(BuyerType buyer) {
		Iterator<Exchange<ItemType, SellerType, BuyerType>> iter = offers.iterator();
		while(iter.hasNext()) {
			Exchange<ItemType, SellerType, BuyerType> offer = iter.next();
			if(offer.buyer == buyer)
				iter.remove();
		}
	}
	
	/**Removes all asks for the specified seller
	 * @param seller
	 */
	public void removeAsks(SellerType seller) {
		Iterator<Exchange<ItemType, SellerType, BuyerType>> iter = offers.iterator();
		while(iter.hasNext()) {
			Exchange<ItemType, SellerType, BuyerType> offer = iter.next();
			if(offer.seller == seller) {
				iter.remove();

				//keep track of the number of items for sale for Mth and M+1st prices
				numItemsForSale--;
			}
		}
	}
	
	/**Returns the current bid price of the market: (M+1)st price.
	 * @return
	 */
	public float getBidPrice() {
		if(numItemsForSale == offers.size()) {
			if(numItemsForSale > 0)
				return 0.0f;
			else
				return Float.NEGATIVE_INFINITY;
		}
		
		//get m+1st
		return offers.get(numItemsForSale).value;
	}
	
	/**Returns the current ask price of the market: Mth price.
	 * @return
	 */
	public float getAskPrice() {
		if(numItemsForSale == 0)
			return Float.POSITIVE_INFINITY;
		
		//get mth
		return offers.get(numItemsForSale-1).value;
	}
	
	/**Returns true if the specified buyer will be allocated an item
	 * @param buyer
	 * @return
	 */
	public boolean isBuyerInTradingSet(BuyerType buyer) {
		//see if any offer down to and including the mth price
		// is from the specified buyer
		for(int i = 0; i < numItemsForSale; i++)
			if(offers.get(i).buyer == buyer)
				return true;
		return false;
	}
	
	/**Returns true if the specified seller will sell
	 * @param buyer
	 * @return
	 */
	public boolean isSellerInTradingSet(SellerType seller) {
		//see if any offer up to and including the m+1st price
		// is from the specified seller
		for(int i = offers.size()-1; i >= numItemsForSale; i--)
			if(offers.get(i).seller == seller)
				return true;
		return false;
	}
	
	/**Returns a list of all the exchanges that occur given the current market from the specified subset
	 * of buyers and sellers (those that are not able to currently participate are 
	 * @param buyers
	 * @param sellers
	 * @param price
	 * @return
	 */
	public List<Exchange<ItemType, SellerType, BuyerType>> acceptAllExchangesFrom(Set<BuyerType> buyers, Set<SellerType> sellers, float price) {
		List<Exchange<ItemType, SellerType, BuyerType>> exchanges = new ArrayList<Exchange<ItemType, SellerType, BuyerType>>();

		while(true) {
			
			//get buyer in the top m prices
			Exchange<ItemType, SellerType, BuyerType> b = null;
			for(int i = 0; i < numItemsForSale; i++) {
				if(buyers.contains(offers.get(i).buyer)) {
					b = offers.remove(i);
					break;
				}
			}
			//no offer in the top m, so nothing to report
			if(b == null)
				break;
			
			//found one in the top m, now get cheapest seller
			Exchange<ItemType, SellerType, BuyerType> s = null;
			for(int i = offers.size()-1; i >= numItemsForSale-1; i--) {
				if(sellers.contains(offers.get(i).seller)) {
					s = offers.remove(i);

					//keep track of the number of items for sale for Mth and M+1st prices
					numItemsForSale--;
					break;
				}
			}
			
			//no seller to match... put buyer back in
			if(s == null) {
				addBid(b.buyer, b.buyerItem, b.value);
				break;
			}
			
			//an exchange occured
			exchanges.add(new Exchange<ItemType, SellerType, BuyerType>(s.seller, s.sellerItem, b.buyer, b.buyerItem, price));			
		}
			
		return exchanges;
	}
	
	/**Returns the next exchange for the specified buyer.  Returns a populated Exchange
	 * class if a transaction takes place, including the seller information.
	 * The seller with the lowest price is chosen (since the earliest successful/capable bidder is calling).
	 * If no transaction is winning for the given buyer, this function returns null.
	 * The clearing price used by this function is the bid price.
	 * @param buyer
	 * @return
	 */
	public Exchange<ItemType, SellerType, BuyerType> acceptNextBuyerExchange(BuyerType buyer) {

		float exchange_price = getBidPrice();
		
		//see if any offer down to and including the mth price
		// is from the specified buyer
		Exchange<ItemType, SellerType, BuyerType> b = null;
		for(int i = 0; i < numItemsForSale; i++) {
			if(offers.get(i).buyer == buyer) {
				b = offers.remove(i);
				break;
			}
		}
		//no offer in the top m, so nothing to report
		if(b == null)
			return null;
		
		//found one in the top m, now get cheapest seller
		Exchange<ItemType, SellerType, BuyerType> s = null;
		for(int i = offers.size()-1; i >= numItemsForSale-1; i--) {
			if(offers.get(i).seller != null) {
				s = offers.remove(i);

				//keep track of the number of items for sale for Mth and M+1st prices
				numItemsForSale--;
				break;
			}
		}
		
		//no seller to match... put buyer back in
		if(s == null) {
			addBid(b.buyer, b.buyerItem, b.value);
			return null;
		}
		
		//copy over the information for the exchange and return it
		b.seller = s.seller;
		b.sellerItem = s.sellerItem;
		b.value = exchange_price;
		return b;
	}
	
	/**Returns the next exchange for the specified seller.  Returns a populated Exchange
	 * class if a transaction takes place, including the buyer information.
	 * The buyer with the highest price is chosen (since the earliest successful/capable asker is calling).
	 * If no transaction is winning for the given seller, this function returns null.
	 * The clearing price used by this function is the ask price.
	 * @param buyer
	 * @return
	 */
	public Exchange<ItemType, SellerType, BuyerType> acceptNextSellerExchange(SellerType seller) {
		float exchange_price = getAskPrice();
		
		//see if any offer up to and including the m+1st price
		// is from the specified seller
		Exchange<ItemType, SellerType, BuyerType> s = null;
		for(int i = offers.size()-1; i >= numItemsForSale-1; i--) {
			if(offers.get(i).seller == seller) {
				s = offers.remove(i);

				//keep track of the number of items for sale for Mth and M+1st prices
				numItemsForSale--;
				break;
			}
		}
		//no offer in offers at or below m+1, so nothing to report
		if(s == null)
			return null;
		
		//found one at or below m+1, now get highest buyer
		Exchange<ItemType, SellerType, BuyerType> b = null;
		for(int i = 0; i < numItemsForSale; i++) {
			if(offers.get(i).buyer != null) {
				b = offers.remove(i);
				break;
			}
		}
		
		//no buyer to match... put seller back in
		if(b == null) {
			addAsk(s.seller, s.sellerItem, s.value);
			return null;
		}
		
		//copy over the information for the exchange and return it
		s.buyer = b.buyer;
		s.buyerItem = b.buyerItem;
		s.value = exchange_price;
		return s;
	}

	/**Returns number of asks
	 * @return
	 */
	public int getNumAsks() {
		return numItemsForSale;
	}
	
	/**Returns number of buy bids
	 * @return
	 */
	public int getNumBids() {
		return offers.size() - numItemsForSale;
	}
}
