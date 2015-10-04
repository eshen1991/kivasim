/**
 * 
 */
package alphabetsoup.simulators.simpleexample;

import java.util.*;

import alphabetsoup.base.BucketbotBase;
import alphabetsoup.base.BucketbotTask;
import alphabetsoup.framework.*;

/**BucketbotDriver implements a functioning Bucketbot that can complete tasks,
 * but only evades other buckets and bucketbots by trying to go directly around.
 * @author Chris Hazard
 */
public class BucketbotExample extends BucketbotBase {

	float frustration = 0.0f;	//0.0->1.0 for maximal frustration
	int stuckCount = 0;			//number of updates not able to move at all
	static final double sqrt2 = Math.sqrt(2.0);
	
	public BucketbotExample(float bucketbot_radius, float bucket_pickup_setdown_time,
			float bucketbot_max_acceleration, float bucketbot_max_velocity, float collision_penalty_time) {
		super(bucketbot_radius, bucket_pickup_setdown_time, bucketbot_max_acceleration, bucketbot_max_velocity, collision_penalty_time);
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Bucketbot#assignTask(alphabetsoup.base.BucketbotTask)
	 */
	//public void assignTask(Object tt) {
	public <T> void assignTask(T tt) {
		BucketbotTask t = (BucketbotTask)tt;
		setCurrentTask(t);
		stateQueue.clear();
		if(t == null)
			return;
		switch(t.getTaskType()) {
		case NONE:	break;
		case CANCEL:	break;
		case MOVE:
			stateQueue.add(new BucketbotMove(t.getDestinationX(), t.getDestinationY()));
			break;
		case STORE_BUCKET:
			if(t.getBucket() != getBucket()) {
				SimulationWorldSimpleExample.getSimulationWorld().bucketbotManager.taskAborted(this, t);
				return;
			}
			
			//if don't have bucket requested to store, then go get it 
			if(getBucket() == null) {
				stateQueue.add(new BucketbotMove(t.getBucket().getX(), t.getBucket().getY()));
				stateQueue.add(new BucketbotPickupBucket(t.getBucket()));
			}
			stateQueue.add(new BucketbotMove(t.getDestinationX(), t.getDestinationY()));
			stateQueue.add(new BucketbotSetdownBucket());
			break;
		case TAKE_BUCKET_TO_LETTER_STATION:
			if(t.getBucket() != getBucket()) {
				stateQueue.add(new BucketbotMove(t.getBucket().getX(), t.getBucket().getY()));
				stateQueue.add(new BucketbotPickupBucket(t.getBucket()));
			}
			stateQueue.add(new BucketbotMove(t.getLetterStation().getX(),
					t.getLetterStation().getY() + t.getLetterStation().getRadius() + getRadius() ));
			stateQueue.add(new BucketbotMove(t.getLetterStation().getX(), t.getLetterStation().getY()));
			stateQueue.add(new BucketbotGetLetter(t.getLetter(), t.getLetterStation()));
			stateQueue.add(new BucketbotMove(t.getLetterStation().getX(),
					t.getLetterStation().getY() - t.getLetterStation().getRadius() - getRadius() ));
			
			/*
			stateQueue.add(new BucketbotMove(t.getLetterStation().getX() + t.getLetterStation().getRadius() + 2*getRadius(),
					t.getLetterStation().getY() + t.getLetterStation().getRadius() + 2*getRadius() ));
			stateQueue.add(new BucketbotMove(t.getLetterStation().getX(),
					t.getLetterStation().getY() + t.getLetterStation().getRadius() + 2*getRadius() ));
			stateQueue.add(new BucketbotMove(t.getLetterStation().getX(), t.getLetterStation().getY()));
			stateQueue.add(new BucketbotGetLetter(t.getLetter(), t.getLetterStation()));
			*/
			break;
		case TAKE_BUCKET_TO_WORD_STATION:
			if(t.getBucket() != getBucket()) {
				stateQueue.add(new BucketbotMove(t.getBucket().getX(), t.getBucket().getY()));
				stateQueue.add(new BucketbotPickupBucket(t.getBucket()));
			}
			stateQueue.add(new BucketbotMove(t.getWordStation().getX(),
					t.getWordStation().getY() + t.getWordStation().getRadius() + getRadius() ));
			stateQueue.add(new BucketbotMove(t.getWordStation().getX(), t.getWordStation().getY()));
			stateQueue.add(new BucketbotPutLetter(t.getLetter(), t.getWordStation()));
			stateQueue.add(new BucketbotMove(t.getWordStation().getX(),
					t.getWordStation().getY() - t.getWordStation().getRadius() - getRadius() ));

			/*
			stateQueue.add(new BucketbotMove(t.getWordStation().getX() - t.getWordStation().getRadius() - 2*getRadius(),
					t.getWordStation().getY() + t.getWordStation().getRadius() + 2*getRadius() ));
			stateQueue.add(new BucketbotMove(t.getWordStation().getX(),
					t.getWordStation().getY() + t.getWordStation().getRadius() + 2*getRadius() ));
			stateQueue.add(new BucketbotMove(t.getWordStation().getX(), t.getWordStation().getY()));
			stateQueue.add(new BucketbotPutLetter(t.getLetter(), t.getWordStation()));
			*/
			break;
		}
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Bucketbot#idle()
	 */
	public void idle() {
		SimulationWorldSimpleExample.getSimulationWorld().bucketbotManager.taskComplete(this, getCurrentTask());
		SimulationWorldSimpleExample.getSimulationWorld().bucketbotManager.requestNewTask(this);
		if(stateQueue.size() > 0)
			stateQueue.get(0).act(this);
	}

	public class BucketbotPickupBucket implements BucketbotState {
		public String getStateName() { return "PickupBucket"; }
		Bucket bucket;
		public BucketbotPickupBucket(Bucket b) {
			bucket = b;
		}
		public void act(BucketbotBase self) {
			//act based on whether bucket was picked up
			if(pickupBucket(bucket)) {
				stateQueue.remove(0);
			}
			else { //failed to pick up bucket
				SimulationWorldSimpleExample.getSimulationWorld().bucketbotManager.taskAborted(self, getCurrentTask());
				stateQueue.clear();
			}
		}
	}
	
	public class BucketbotSetdownBucket implements BucketbotState {
		public String getStateName() { return "SetdownBucket"; }
		public BucketbotSetdownBucket() { }
		public void act(BucketbotBase self) {
			if(setdownBucket()) {
				stateQueue.remove(0);
			}
			else { //failed to set down bucket
				SimulationWorldSimpleExample.getSimulationWorld().bucketbotManager.taskAborted(self, getCurrentTask());
				stateQueue.clear();
			}
		}
	}
	
	public class BucketbotGetLetter implements BucketbotState {
		public String getStateName() { return "GetLetter"; }
		Letter letter;
		LetterStation station;
		boolean requested_letters = false;
		public BucketbotGetLetter(Letter l, LetterStation s) {
			letter = l;	station = s;
		}
		public void act(BucketbotBase self) {
	
			//make sure bucketbot is in vacinity of station, otherwise go there
			if(self.getDistance(station.getX(), station.getY()) > SimulationWorldSimpleExample.getSimulationWorld().getMap().getTolerance()) {
				stateQueue.add(0, new BucketbotMove(station.getX(), station.getY()));
				return;
			}

			//if it's the first time, request the letters
			if(!requested_letters) {
				station.requestLetter(self, letter);
				requested_letters = true;
			}

			if(self.getBucket() == null) {
				//something wrong happened... don't have a bucket!
				SimulationWorldSimpleExample.getSimulationWorld().bucketbotManager.taskAborted(self, getCurrentTask());
				stateQueue.remove(0);
				if(stateQueue.size() > 0)
					stateQueue.get(0).act(self);
				return;
			}
			
			//see if all letter has been deposited in the bucket
			if(self.getBucket().containsLetter(letter)) {
				stateQueue.remove(0);
				if(stateQueue.size() > 0)
					stateQueue.get(0).act(self);
				return;
			}
		}
	}
	
	public class BucketbotPutLetter implements BucketbotState {
		public String getStateName() { return "PutLetter"; }
		Letter letter;
		WordStation station;
		boolean requested_letters_take = false;
		public BucketbotPutLetter(Letter l, WordStation s) {
			letter = l;	station = s;
		}
		public void act(BucketbotBase self) {
			
			//make sure bucketbot is in vacinity of station, otherwise go there
			if(self.getDistance(station.getX(), station.getY()) > SimulationWorldSimpleExample.getSimulationWorld().getMap().getTolerance()) {
				stateQueue.add(0, new BucketbotMove(station.getX(), station.getY()));
				return;
			}
			
			//if it's the first time, request the letters be taken
			if(!requested_letters_take) {
					station.requestLetterTake(self, letter, null);
				requested_letters_take= true;
			}
			
			if(self.getBucket() == null) {
				//something wrong happened... don't have a bucket!
				SimulationWorldSimpleExample.getSimulationWorld().bucketbotManager.taskAborted(self, getCurrentTask());
				stateQueue.remove(0);
				if(stateQueue.size() > 0)
					stateQueue.get(0).act(self);
				return;
			}
			
			//see if letter has been taken from the bucket
			if(!self.getBucket().containsLetter(letter)) {
				stateQueue.remove(0);
				if(stateQueue.size() > 0)
					stateQueue.get(0).act(self);
				return;
			}
		}
	}
	
	/**CollideableObject is used for sorting collideable objects
	 * in order of direction (so they are put around in a circle)
	 * @author Chris Hazard
	 */
	private static class CollideableObject implements Comparator<CollideableObject> {
		public CollideableObject(float s, float dist, float dir) {
			size = s; distance = dist; direction = dir;
		}
		float size, distance, direction;

		public int compare(CollideableObject o1, CollideableObject o2) {		
			float dir1 = ((CollideableObject)o1).direction;
			float dir2 = ((CollideableObject)o2).direction;
			if(dir1 > dir2) return 1;
			if(dir2 > dir1)	return -1;
			return 0;
		}
	}

	/**getBestEvadeDirection returns the largest approximate gap to escape.
	 * This gap is approximated by finding the gaps between all potential
	 * collideable objects around the Bucketbot, and weighting them positively 
	 * by size and weighting them inversely by both distance and gapsize.
	 * The direction bisecting the largest of these weighted gaps is returned. 
	 * @param visible_distance how far the Bucketbot can see
	 * @return best direction to evade
	 */
	public float getBestEvadeDirection(float visible_distance) {
		alphabetsoup.framework.Map map = SimulationWorldSimpleExample.getSimulationWorld().getMap();
		//get visible objects within the distance of the next planned update
		Collection<Circle> visible_objects = map.getBucketbotsWithinDistance(getX(), getY(), visible_distance);
		if(getBucket() != null)
			visible_objects.addAll(map.getBucketsWithinDistance(getX(), getY(), visible_distance));

		//if nothing other than bucketbot (and bucket if applicable), keep going in same direction
		if( (getBucket() == null && visible_objects.size() <= 1) 
				|| (getBucket() != null && visible_objects.size() <= 2) )
			return getDirection();

	
		List<CollideableObject> objects = new ArrayList<CollideableObject>();
		//get object distances and directions
		for(Circle c : visible_objects) {
			if(c == this || c == getBucket())
				continue;

			float object_direction = (float)Math.atan2(c.getY() - getY(), c.getX() - getX());
			objects.add(new CollideableObject(2*c.getRadius(), getDistance(c.getX(), c.getY()),  object_direction) );
		}
		
		//add 4 walls
		if(getX() < visible_distance)
			objects.add(new CollideableObject((float)(2*Math.sqrt(visible_distance*visible_distance - getX()*getX()) ), 
												getX(), (float)Math.PI));
		if(map.getWidth() - getX() < visible_distance)
			objects.add(new CollideableObject((float)(2*Math.sqrt(visible_distance*visible_distance - (map.getWidth()-getX())*(map.getWidth()-getX()) ) ), 
												map.getWidth() - getX(), 0.0f));
		
		if(getY() < visible_distance)
			objects.add(new CollideableObject((float)(2*Math.sqrt(visible_distance*visible_distance - getY()*getY()) ), 
												getY(), (float)(3*Math.PI/2)));
		if(map.getHeight() - getY() < visible_distance)
			objects.add(new CollideableObject((float)(2*Math.sqrt(visible_distance*visible_distance - (map.getHeight()-getY())*(map.getHeight()-getY()) ) ), 
												map.getHeight() - getY(), (float)Math.PI/2));

		//if no objects, then nothing to collide with
		if(objects.size() == 0)
			return getDirection();

		//sort objects by direction, so between each two objects is the smallest real gaps
		Collections.sort(objects, objects.get(0));

		//add beginning one again
		objects.add(new CollideableObject( objects.get(0).size, objects.get(0).distance, (float)(objects.get(0).direction + 2*Math.PI)) );
		
		//find maximal gap
		//start with first gap
		float evade_direction = (objects.get(0).direction + objects.get(1).direction)/2;
		double weight = ((objects.get(0).size + objects.get(1).size)/2)
						/( ((objects.get(0).distance + objects.get(1).distance)/2)
						* (objects.get(1).direction - objects.get(0).direction) );

		//for all after the first gap
		for(int i = 1; i < objects.size()-1; i++) {
			//get weight (a heuristic for speed)
			float w = ((objects.get(i).size + objects.get(i+1).size)/2)
						/( ((objects.get(i).distance + objects.get(i+1).distance)/2)
						* (objects.get(i+1).direction - objects.get(i).direction) );
			//if better direction, then choose direction bisecting the two objects
			if(w < weight) {
				weight = w;
				evade_direction = (objects.get(i).direction + objects.get(i+1).direction)/2;
			}
		}
		return evade_direction;
	}

	public class BucketbotEvade implements BucketbotState {
		public String getStateName() { return "Evade"; }
		public void act(BucketbotBase self) {
			setDrawBolded(true);
			
			if(curTime < cruiseUntil)
				return;
			
			MersenneTwisterFast rand = SimulationWorld.rand;
			
			//if doing something else (stateQueue isn't empty), are trying to move to a new location,
			// but there's another bucketbot at that location, then sit and wait most of the time
			if(stateQueue.size() > 1
					&& stateQueue.get(1).getClass() == BucketbotMove.class
					&& !SimulationWorldSimpleExample.getSimulationWorld().map.isBucketbotMoveValid(self,
							((BucketbotMove)stateQueue.get(1)).moveToX, ((BucketbotMove)stateQueue.get(1)).moveToY)) {
				
				//usually sit and wait
				if(rand.nextFloat() < .7f)
					return;
			}
			
			setTargetSpeed(getMaxVelocity());
			
			float min_visible_distance = 3 * getRadius();
			float new_direction = getBestEvadeDirection(min_visible_distance);
			if(getDirection() != new_direction)
				setDirection(new_direction);

			if(rand.nextFloat() < .5f){
				stateQueue.remove(0);
				setDrawBolded(false);
				if(stateQueue.size() > 0)
					stateQueue.get(0).act(self);
			}
		}
	}
	BucketbotEvade bucketbotEvade = this.new BucketbotEvade();
	
	public class BucketbotMove implements BucketbotState {
		public String getStateName() { return "Move"; }
		public float moveToX, moveToY;
		public BucketbotMove(float x, float y) {
			moveToX = x;	moveToY = y;
		}
		public void act(BucketbotBase self) {
			alphabetsoup.framework.Map map = SimulationWorldSimpleExample.getSimulationWorld().getMap();
			float cur_speed = getSpeed();	//called once for code efficiency (since getSpeed does a sqrt)

			//find distance to be covered before next planned update
			getNextEventTime(curTime);
			double time_interval = Math.max(getMinUntil() - curTime, 0.001);
			float distance_to_be_covered = (float)(Math.max(cur_speed, getTargetSpeed()) * time_interval);
			distance_to_be_covered += 2*getRadius();	//count its radius and the radius of another object
			//make sure see at least a minimal distance regardless of speed
			float min_visible_distance = 3 * getRadius();
			distance_to_be_covered = Math.max(distance_to_be_covered, min_visible_distance);
			
			//get visible objects within the distance of the next planned update
			Collection<Circle> visible_objects = map.getBucketbotsWithinDistance(getX(), getY(), distance_to_be_covered);
			if(self.getBucket() != null)
				visible_objects.addAll(map.getBucketsWithinDistance(getX(), getY(), distance_to_be_covered));

			//don't want to do anything yet,
			// unless something is now visible that wasn't before,
			// a collision occured (speed == 0)
			// timer is ready to do something else
			if( ((getBucket() == null && visible_objects.size() == 1) 
						|| (getBucket() != null && visible_objects.size() == 2) )
					&& cur_speed > 0.0f
					&& curTime < cruiseUntil)
				return;
 

			//if something other than bucketbot (and bucket if applicable) is near, evade
			if((getBucket() == null && visible_objects.size() > 1) 
					|| (getBucket() != null && visible_objects.size() > 2) ) {
				
				//find closest object
				float min_dist2 = Float.POSITIVE_INFINITY;	//minimum distance squared
				for(Circle c : visible_objects) {
					if(c == self || c == self.getBucket())
						continue;
					
					//see if infront of bucketbot 
					float object_direction = (float)Math.atan2(c.getY() - getY(), c.getX() - getX());
					float relative_direction = angleDifference(object_direction, getDirection());
					if(Math.abs(relative_direction) > Math.PI / 4 + 0.5)	//just beyond 1/4 circle, to make sure they don't get stuck
						continue;
					
					//see if it's closer than any other
					float dist2 = (getX()-c.getX())*(getX()-c.getX()) + (getY()-c.getY())*(getY()-c.getY());
					if(dist2 < min_dist2)
						min_dist2 = dist2;
				}
				
				//if anything is closer than this constant value, then evade...
				if(min_dist2 < min_visible_distance*min_visible_distance ) {
					float new_direction = getBestEvadeDirection(min_visible_distance);

					if(getDirection() != new_direction)
						setDirection(new_direction);

					setTargetSpeed(getMaxVelocity());
					cruiseUntil = curTime + 0.25f;

					frustration = 0.0f;
					stateQueue.add(0, bucketbotEvade);
					return;
				}
			}
			
			//if trying to move, but can't try evading
			if(cur_speed > 0.0f) {
				stuckCount = 0;
			}
			else { //not moving...
				stuckCount++;
				if(stuckCount > 3) {
					frustration = 0.0f;
					stateQueue.add(0, bucketbotEvade);
					return;
				}
			}
			
			//find distance to goal
			float goal_distance = getDistance(moveToX, moveToY);
			
			//make sure tolerance is less than the map's tolerance/3 to make sure that if it sets a bucket down,
			// the next one bucketbot will be able to pick it up (and could also be tolerance/3 away)
			// better fudge factor than tolerance / 2 (which is the minimum that will work)
			float tolerance = map.getTolerance() / 3;
			
			//if close enough to goal and stopped moving, then do next action
			if(goal_distance < tolerance && cur_speed == 0.0f) {
				frustration = 0.0f;
				
				stateQueue.remove(0);
				if(stateQueue.size() > 0)
					stateQueue.get(0).act(self);
				return;
			}
			
			//if not facing the right way (within tolerance), or heading outside of the map, turn so it is a good direction
			double projected_x = getX() + goal_distance * Math.cos(getDirection());
			double projected_y = getY() + goal_distance * Math.sin(getDirection());
			if( (projected_x - moveToX) * (projected_x - moveToX)
						+ (projected_y - moveToY) * (projected_y - moveToY)
					>= tolerance * tolerance
					|| projected_x + getRadius() > map.getWidth() || projected_x - getRadius() < 0.0
					|| projected_y + getRadius() > map.getHeight() || projected_y - getRadius() < 0.0 ) {

				setDirection((float)Math.atan2(moveToY - getY(), moveToX - getX()));
				if(cur_speed > 0)
					setTargetSpeed(0.0f);
				cruiseUntil = getAccelerateUntil();
				return;
			}
			
			//going the right direction -now figure out speed

			//if too close to stop, then stop as fast as possible
			float decel_time = cur_speed / getMaxAcceleration();
			float decel_distance = getMaxAcceleration()/2 * decel_time * decel_time;
			if(cur_speed > 0.0f && decel_distance > goal_distance) {
				setTargetSpeed(0.0f);
				cruiseUntil = getAccelerateUntil();
				frustration = (frustration + 1.0f) / 2;
				return;
			}
			
			//get new velocity based on frustration
			float cur_vel = getMaxVelocity() * Math.max(1.0f-frustration, 0.0078125f);
			setTargetSpeed(cur_vel);

			//see if have room to accelerate to full speed and still decelerate
			float accel_time = getTargetSpeedDifference() / getMaxAcceleration();
			float accel_distance = cur_speed * accel_time + getMaxAcceleration()/2 * accel_time * accel_time;
			decel_time = cur_vel / getMaxAcceleration();
			decel_distance = getMaxAcceleration()/2 * accel_time * accel_time;

			//if enough room to fully accelerate, do so
			if(accel_distance + decel_distance <= goal_distance)
				cruiseUntil = getAccelerateUntil();
			else { //don't have time to fully accelerate
				//having this code spread out with sub-steps dramatically helps the java compiler have
				// better performance
				double cos_dir = Math.cos(getDirection());
				double sin_dir = Math.sin(getDirection());
				double x_accel = getMaxAcceleration() * cos_dir;
				double y_accel = getMaxAcceleration() * sin_dir;
				if(Math.abs(x_accel) > Math.abs(y_accel)) {
					double x_goal = goal_distance * cos_dir;
					cruiseUntil = curTime + 
							sqrt2 * (Math.sqrt(2*x_accel*x_goal + getXVelocity()*getXVelocity())
										- sqrt2*getXVelocity())
								/ (2*x_accel);
				}
				else {
					double y_goal = goal_distance * sin_dir;
					cruiseUntil = curTime +
							sqrt2 * (Math.sqrt(2*y_accel*y_goal + getYVelocity()*getYVelocity())
										- sqrt2*getYVelocity())
								/ (2*y_accel);
				}				
			}
		} //act()
	} //class BucketbotMove
}
