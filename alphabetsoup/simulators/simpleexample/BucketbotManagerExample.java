/**
 * 
 */
package alphabetsoup.simulators.simpleexample;

import java.util.*;

import alphabetsoup.framework.*;
import alphabetsoup.base.*;

/**BucketbotGlobalResources implements a basic Bucketbot manager that uses basic queues of tasks
 * to dispense jobs to bucketbots.
 * @author Chris Hazard
 */
public class BucketbotManagerExample implements Updateable {
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
	
	protected List<Circle> unusedBucketStorageLocations = new ArrayList<Circle>();
	protected HashMap<Bucket,Circle> pendingBucketStorageLocations = new HashMap<Bucket,Circle>();
	protected HashMap<Bucket,Circle> usedBucketStorageLocations = new HashMap<Bucket,Circle>();
	
	/**Adds a new valid currently used location to store buckets on the map
	 */
	public void addNewUsedBucketStorageLocation(Bucket b) {
		Circle c = new Circle(0.0f, b.getX(), b.getY());
		usedBucketStorageLocations.put(b, c);
	}
	
	/**Adds a new valid unused location to store buckets on the map
	 */
	public void addNewValidBucketStorageLocation(float x, float y) {
		unusedBucketStorageLocations.add(new Circle(0.0f, x, y));
	}

	/**Called whenever a new Word has been assigned to a WordStation
	 * @param w Word assigned
	 * @param s WordStation the word was assigned to
	 */
	public void newWordAssignedToStation(Word w, WordStation s) {
		MersenneTwisterFast rand = SimulationWorldSimpleExample.rand;
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
		MersenneTwisterFast rand = SimulationWorldSimpleExample.rand;
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
	
	/**Bucketbots should call requestNewTask when they are idle and have no tasks
	 * @param r reference to the Bucketbot requesting a new task
	 */
	public void requestNewTask(Bucketbot r) {
		
		alphabetsoup.framework.Map map = SimulationWorldSimpleExample.getSimulationWorld().map;
		MersenneTwisterFast rand = SimulationWorldSimpleExample.rand;
		
		//if robot has a bucket, store it
		if(r.getBucket() != null && unusedBucketStorageLocations.size() > 0) {
			Circle location = unusedBucketStorageLocations.get(0);
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
									b, l, openLetterRequests.get(j).station, null));

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
				r.assignTask(BucketbotTask.createTaskTAKE_BUCKET_TO_LETTER_STATION(b, l, s));
	
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
		r.assignTask(BucketbotTask.createTaskMOVE( (map.getWidth()-2*r.getRadius())* rand.nextFloat() + r.getRadius(),
													(map.getHeight()-2*r.getRadius()) * rand.nextFloat() + r.getRadius()) );
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
			Circle location = pendingBucketStorageLocations.get(t.getBucket());
			pendingBucketStorageLocations.remove(t.getBucket());
			usedBucketStorageLocations.put(t.getBucket(), location);
		}
		
		if(t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_WORD_STATION
				|| t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_LETTER_STATION) {
			//move from pending list to unused
			Circle location = pendingBucketStorageLocations.get(t.getBucket());
			pendingBucketStorageLocations.remove(t.getBucket());
			unusedBucketStorageLocations.add(location);
		}
		
		//recheck bucket inventory
		//TODO this is inefficient; only change lettersInBuckets when inventory changes?
		lettersInBuckets.clear();
		for(Bucket b : SimulationWorldSimpleExample.getSimulationWorld().getBuckets()) {
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
				Circle location = pendingBucketStorageLocations.get(t.getBucket());
				pendingBucketStorageLocations.remove(t.getBucket());
				unusedBucketStorageLocations.add(location);
			}
		}
		
		if(t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_WORD_STATION
				|| t.getTaskType() == BucketbotTask.TaskType.TAKE_BUCKET_TO_LETTER_STATION) {
			//if never took bucket, then move bucket storage to used list
			if(r.getBucket() == null) {
				Circle location = pendingBucketStorageLocations.get(t.getBucket());
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
}
