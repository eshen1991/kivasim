/**
 * 
 */
package alphabetsoup.simulators.markettaskallocation;

import java.util.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Disk;

import alphabetsoup.framework.*;
import alphabetsoup.userinterface.Renderable;
import alphabetsoup.waypointgraph.Waypoint;

/**BucketStorageAgent implements a basic Bucketbot manager that uses basic queues of tasks
 * to dispense jobs to bucketbots.
 * @author Chris Hazard
 */
public class BucketStorageAgent implements Updateable, Renderable {

	HashSet<Waypoint> unusedBucketStorageLocations = new HashSet<Waypoint>();
	HashMap<Bucket,Waypoint> usedBucketStorageLocations = new HashMap<Bucket,Waypoint>();
	
	private double curTime = 0.0;
	private double bidUpdateInterval = 0.5;
	
	private double profit = 0.0;
	
	/**Adds a new valid currently used location to store buckets on the map
	 */
	public void addNewUsedBucketStorageLocation(Bucket b, Waypoint w) {
		usedBucketStorageLocations.put(b, w);
		((BucketAgent)b).setAssignedStorage(w);
	}
	
	/**Adds a new valid unused location to store buckets on the map
	 */
	public void addNewValidBucketStorageLocation(Waypoint w) {
		unusedBucketStorageLocations.add(w);
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return curTime + bidUpdateInterval;
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {
		curTime = cur_time;
		Economy economy = SimulationWorldMarketTaskAllocation.getSimulationWorld().economy;
		
		//reset storage markets
		for(MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> m : economy.storageMarkets)
			m.removeAsks(this);
		
		//put a single ask in each one that isn't currently occupied
		for(Waypoint w : unusedBucketStorageLocations) {
			MultiItemDoubleAuction<Waypoint, Waypoint, BucketStorageAgent, BucketAgent> m = economy.getClosestStorageMarket(w);
			m.addAsk(this, w, w, 0.0f);
		}
	}	
	
	//routines which may be used to render the current used storage locations
	private Disk disk = new Disk();
	public void render() {
		GL11.glLineWidth(1.0f);
		GL11.glColor4ub((byte)0xFF, (byte)0x0, (byte)0x0, (byte)0xFF);
		for(Bucket b : usedBucketStorageLocations.keySet()) {
			Waypoint w = usedBucketStorageLocations.get(b);
			
			//draw the center
			GL11.glPushMatrix();
			GL11.glTranslatef(w.getX(), w.getY(), 0.0f);
			disk.draw(0.0f, SimulationWorldMarketTaskAllocation.getSimulationWorld().map.getTolerance(), 10, 1);
			GL11.glPopMatrix();
			
			//draw the paths
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex2f(w.getX(), w.getY());
			GL11.glVertex2f(b.getX(), b.getY());			
			GL11.glEnd();
		}
		
		GL11.glColor4ub((byte)0xFF, (byte)0xFF, (byte)0x0, (byte)0xFF);
		for(Waypoint w : unusedBucketStorageLocations) {
			
			//draw the center
			GL11.glPushMatrix();
			GL11.glTranslatef(w.getX(), w.getY(), 0.0f);
			disk.draw(0.0f, SimulationWorldMarketTaskAllocation.getSimulationWorld().map.getTolerance(), 10, 1);
			GL11.glPopMatrix();
		}
	}

	public void renderOverlayDetails() {
		
	}
	
	public void renderDetails() {
		
	}

	public boolean isMouseOver(float mouse_x, float mouse_y) {
		return false;
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
