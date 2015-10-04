/**
 * 
 */
package alphabetsoup.simulators.combinatorialmarkettaskallocation;

public class Exchange<ItemType, SellerType, BuyerType>
		implements Comparable<Exchange<ItemType, SellerType, BuyerType>> {
	
	public SellerType seller;
	public ItemType sellerItem;
	public BuyerType buyer;
	public ItemType buyerItem;		
	public float value;
	
	public Exchange(SellerType seller, ItemType sellerItem,
					BuyerType buyer, ItemType buyerItem, float value) {
		this.seller = seller;	this.sellerItem = sellerItem;
		this.buyer = buyer;		this.buyerItem = buyerItem;
		this.value = value;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Exchange<ItemType, SellerType, BuyerType> o) {
    	if(value < o.value) return +1;
    	if(value > o.value)	return -1;
    	return 0;
    }
}