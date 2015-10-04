/**
 * 
 */
package alphabetsoup.simulators.graphexample;

import java.util.*;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.Disk;

import alphabetsoup.framework.*;
import alphabetsoup.simulators.graphexample.SimulationWorldGraphExample;
import alphabetsoup.userinterface.Renderable;
import alphabetsoup.waypointgraph.Waypoint;
import alphabetsoup.base.*;

/**BucketbotGlobalResources implements a basic Bucketbot manager that uses basic queues of tasks
 * to dispense jobs to bucketbots.
 * @author Chris Hazard
 */
public class BucketbotManagerExample implements alphabetsoup.waypointgraph.BucketbotManager, Updateable, Renderable {
	protected LinkedHashSet<Bucket> usedBuckets = new LinkedHashSet<Bucket>();
	protected LinkedHashSet<Bucket> unusedBuckets = new LinkedHashSet<Bucket>();
	
	public BucketbotManagerExample(Bucket [] buckets) {
		for(Bucket b : buckets)
			unusedBuckets.add(b);
	}
	
	public static class LetterWordStationPair {
		public LetterWordStationPair(Letter l, WordStation s) {
			letter = l;	station = s;
		}
		public Letter letter;
		public WordStation station;
	}
	
	public static class LetterLetterStationPair {
		public LetterLetterStationPair(Letter l, LetterStation s) {
			letter = l;	station = s;
		}
		public Letter letter;
		public LetterStation station;
	}
	
	public static class LetterBucketPair {
		public LetterBucketPair(Letter l, Bucket b) {
			letter = l;	bucket = b;
		}
		public Letter letter;
		public Bucket bucket;
	}

	protected List<LetterWordStationPair> openLetterRequests = new ArrayList<LetterWordStationPair>();
	protected List<LetterLetterStationPair> availableLetters = new ArrayList<LetterLetterStationPair>();
	protected List<LetterBucketPair> lettersInBuckets = new ArrayList<LetterBucketPair>();
	
	protected List<Waypoint> unusedBucketStorageLocations = new ArrayList<Waypoint>();
	protected HashMap<Bucket,Waypoint> pendingBucketStorageLocations = new HashMap<Bucket,Waypoint>();
	protected HashMap<Bucket,Waypoint> usedBucketStorageLocations = new HashMap<Bucket,Waypoint>();
	
	/**Adds a new valid currently used location to store buckets on the map
	 */
	public void addNewUsedBucketStorageLocation(Bucket b, Waypoint w) {
		usedBucketStorageLocations.put(b, w);
	}
	
	/**Adds a new valid unused location to store buckets on the map
	 */
	public void addNewValidBucketStorageLocation(Waypoint w) {
		unusedBucketStorageLocations.add(w);
	}

	/**Called whenever a new Word has been assigned to a WordStation
	 * @param w Word assigned
	 * @param s WordStation the word was assigned to
	 */
	public void newWordAssignedToStation(Word w, WordStation s) {
		MersenneTwisterFast rand = SimulationWorldGraphExample.rand;
		for(Letter l : w.getOriginalLetters()) {
			if(openLetterRequests.size() > 0)
				openLetterRequests.add(rand.nextInt(openLetterRequests.size()), new LetterWordStationPair(l, s));
			else
				openLetterRequests.add(new LetterWordStationPair(l, s));
		}
	}
	
	/**Called whenever a new Letter has been assigned to a LetterStation
	 * @param l Letter assigned
	 * @param s LetterStation the Letter was assigned to
	 */
	public void newLetterBundleAssignedToStation(Letter l, LetterStation s) {
		MersenneTwisterFast rand = SimulationWorldGraphExample.rand;
		if(availableLetters.size() > 0)
			availableLetters.add(rand.nextInt(availableLetters.size()), new LetterLetterStationPair(l, s));
		else
			availableLetters.add(new LetterLetterStationPair(l, s));
	}
	
	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#getNextEventTime(double)
	 */
	public double getNextEventTime(double cur_time) {
		return Double.POSITIVE_INFINITY;
	}

	/* (non-Javadoc)
	 * @see alphabetsoup.framework.Updateable#update(double, double)
	 */
	public void update(double last_time, double cur_time) {

	}
	
	public void bucketPickedUp(Bucketbot r, Bucket b) {
		
	}

	public void bucketSetDown(Bucketbot r, Bucket b, Waypoint w) {
		
	}
	
	/**Bucketbots should call requestNewTask when they are idle and have no tasks
	 * @param r reference to the Bucketbot requesting a new task
	 */
	public void requestNewTask(Bucketbot r) {
		
		//if robot has a bucket, store it
		if(r.getBucket() != null && unusedBucketStorageLocations.size() > 0) {
			Waypoint location = unusedBucketStorageLocations.get(0);
			unusedBucketStorageLocations.remove(0);
			pendingBucketStorageLocations.put(r.getBucket(), location);
			r.<BucketbotTask>assignTask(BucketbotTask.createTaskSTORE_BUCKET(r.getBucket(), location));
			return;
		}

		//robot doesn't have a bucket

		//if have letters in buckets, send one off to service a word station
		if(lettersInBuckets.size() > 0) {
			
			LetterBucketPair lbp = null;
			for(int i = 0; i < lettersInBuckets.size(); i++) {
				
				lbp = lettersInBuckets.get(i);
				Bucket b = lbp.bucket;
				
				//if bucket is currently unused and also stored, then it can be used for this task
				if(unusedBuckets.contains(b) && usedBucketStorageLocations.containsKey(b) ) {
					
					Letter l = lbp.letter;

					//find word that needs this letter
					for(int j = 0; j < openLetterRequests.size(); j++) {
						if(openLetterRequests.get(j).letter.doesMatch(l)) {
							r.<BucketbotTask>assignTask(BucketbotTask.createTaskTAKE_BUCKET_TO_WORD_STATION(
									b, l, openLetterRequests.get(j).station, null	));

							//mark things as used
							unusedBuckets.remove(b);
							usedBuckets.add(b);	
							
							pendingBucketStorageLocations.put(b, usedBucketStorageLocations.get(b) );
							usedBucketStorageLocations.remove(b);

							lettersInBuckets.remove(i);
							openLetterRequests.remove(j);
							return;
						}
					} //for each openLetterRequests
				} //if usedBuckets contains lettersInBuckets's bucket
			} //for each lettersInBuckets
		} //if lettersInBuckets.size() > 0
		
		//no letters available in buckets or no free buckets, so get something from a letter station
		// if there are available letters
		if(unusedBuckets.size() > 0
				&& availableLetters.size() > 0) {
			//find a bucket that has room
			Bucket b = null;
			int bundle_size = SimulationWorld.getSimulationWorld().letterStations[0].getBundleSize();
			for(Bucket i : unusedBuckets) {
				if(i.getLetters().size() + bundle_size <= i.getCapacity()
						&& usedBucketStorageLocations.containsKey(i)) {
					b = i;
					break;
				}
			}
			
			//if bucket is free with free capacity, grab the letter
			if(b != null) {
				//get first available letter
				Letter l = availableLetters.get(0).letter;
				LetterStation s = availableLetters.get(0).station;
			
				r.assignTask(BucketbotTask.createTaskTAKE_BUCKET_TO_LETTER_STATION(b, l, s	));
	
				//mark things as being used
				unusedBuckets.remove(b);
				usedBuckets.add(b);
				
				pendingBucketStorageLocations.put(b, usedBucketStorageLocations.get(b) );
				usedBucketStorageLocations.remove(b);
	
				availableLetters.remove(0);
				return;
			}
		} //if unusedBuckets.size() > 0 && availableLetters.size() > 0
		
		//no tasks are available.  go somewhere so the robot doesn't get in the way
//		r.assignTask(BucketbotTask.createTaskMOVE( (map.getWidth()-2*r.getRadius())* rand.nextFloat() + r.getRadius(),
//													(map.getHeight()-2*r.getRadius()) * rand.nextFloat() + r.getRadius()) );
	}

	/**Bucketbots should call taskComplete when an assigned task has been completed 
	 * @param r Bucketbot which has completed a task
	 * @param t task which was completed -implementations may use any object types as a task
	 */
	public void taskComplete(Bucketbot r, BucketbotTask t) {
		if(t == null)
			return;
		//tell bucketbot its task is done
		r.assignTask(null);
		
		//if had bucket, but no longer, make bucket free again
		if(t.getBucket() != null && r.getBucket() == null) {
			unusedBuckets.add(t.getBucket());
			usedBuckets.remove(t.getBucket());
		}
		
		if(t.getTaskType() == BucketbotTask.TaskType.STORE_BUCKET) {
			//move from pending list to used
			Waypoint location = pendingBucketStorageLocations.get(t.getBucket());
			pendingBucketStorageLocations.remove(t.getBucket());
			usedBucketStorageLocations.put(t.getBucket(), location);
		}
		
		if(t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_WORD_STATION
				|| t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_LETTER_STATION) {
			//move from pending list to unused
			Waypoint location = pendingBucketStorageLocations.get(t.getBucket());
			pendingBucketStorageLocations.remove(t.getBucket());
			unusedBucketStorageLocations.add(location);
		}
		
		//recheck bucket inventory
		//TODO this is inefficient; only change lettersInBuckets when inventory changes?
		lettersInBuckets.clear();
		for(Bucket b : SimulationWorldGraphExample.getSimulationWorld().getBuckets()) {
			for(Letter l : b.getLetters())
				lettersInBuckets.add(new LetterBucketPair(l, b));
		}
	}
	
	/**Bucketbots should call taskAborted when an assigned task has been aborted 
	 * @param r Bucketbot which has aborted a task
	 * @param t task which was aborted -implementations may use any object types as a task
	 */
	public void taskAborted(Bucketbot r, BucketbotTask t) {
		if(t == null || r == null)
			return;
		
		//tell bucketbot its task is done
		r.assignTask(null);
		
		//if it had a bucket, free it
		if(t.getBucket() != null && r.getBucket() == null) {
			unusedBuckets.add(t.getBucket());
			usedBuckets.remove(t.getBucket());
		}
		
		if(t.getTaskType() == BucketbotTask.TaskType.STORE_BUCKET) {
			//if still has bucket, move bucket storage from pending list to unused
			if(r.getBucket() != null) {
				Waypoint location = pendingBucketStorageLocations.get(t.getBucket());
				pendingBucketStorageLocations.remove(t.getBucket());
				unusedBucketStorageLocations.add(location);
			}
		}
		
		if(t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_WORD_STATION
				|| t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_LETTER_STATION) {
			//if never took bucket, then move bucket storage to used list
			if(r.getBucket() == null) {
				Waypoint location = pendingBucketStorageLocations.get(t.getBucket());
				pendingBucketStorageLocations.remove(t.getBucket());
				usedBucketStorageLocations.put(t.getBucket(), location);
			}
		}
		
		//place task back in queue
		if(t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_WORD_STATION)
			openLetterRequests.add(0, new LetterWordStationPair(t.getLetter(), t.getWordStation()));
		else if(t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_LETTER_STATION)
			availableLetters.add(0, new LetterLetterStationPair(t.getLetter(), t.getLetterStation()));
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
			disk.draw(0.0f, SimulationWorldGraphExample.getSimulationWorld().map.getTolerance(), 10, 1);
			GL11.glPopMatrix();
			
			//draw the paths
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex2f(w.getX(), w.getY());
			GL11.glVertex2f(b.getX(), b.getY());			
			GL11.glEnd();
		}
		
		GL11.glColor4ub((byte)0xFF, (byte)0xFF, (byte)0x0, (byte)0xFF);
		for(Bucket b : pendingBucketStorageLocations.keySet()) {
			Waypoint w = pendingBucketStorageLocations.get(b);
			
			//draw the center
			GL11.glPushMatrix();
			GL11.glTranslatef(w.getX(), w.getY(), 0.0f);
			disk.draw(0.0f, SimulationWorldGraphExample.getSimulationWorld().map.getTolerance(), 10, 1);
			GL11.glPopMatrix();
			
			//draw the paths
			GL11.glBegin(GL11.GL_LINES);
			GL11.glVertex2f(w.getX(), w.getY());
			GL11.glVertex2f(b.getX(), b.getY());			
			GL11.glEnd();
		}
	}

	public void renderOverlayDetails() {
		
	}
	
	public void renderDetails() {
		
	}

	public boolean isMouseOver(float mouse_x, float mouse_y) {
		return false;
	}

}
