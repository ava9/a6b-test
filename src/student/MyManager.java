package student;

import game.Truck;
import game.Parcel;
import game.Board;
import game.Edge;
import game.Node;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;


public class MyManager extends game.Manager {

	private Board b;
	ArrayList<Truck> t;

	@SuppressWarnings("unchecked")
	
	@Override
	public void run() {
		int count = 0;
		b = getBoard();
		t = b.getTrucks();

		//Allocate parcels to trucks
		for (Truck truck: t){
			truck.setUserData(new LinkedList<Parcel>());
		}
		
		//Allocate list of parcels to trucks
		for (Parcel parcel: b.getParcels()){
			
			
			if(count >= t.size()){
				count = 0;
			}
			
			((LinkedList<Parcel>)t.get(count).getUserData()).add(parcel);
			
			count++;
		}
		
		/**Debugging**/
		for (Truck truck: t){
			System.out.println("Truck: " + truck + ", Parcels:" + truck.getUserData());
		}
		
		//Move trucks to initial state
		for (Truck truck: t){
			
			LinkedList<Parcel> list = (LinkedList<Parcel>)truck.getUserData();
			
			if (list != null){

				Node node = list.get(0).getLocation();
				LinkedList<Node> bestPath = Dijkstra(truck.getLocation(), node);	
				
				/**Debugging**/
				if (bestPath.size() > 0){
					System.out.println("Path: " + bestPath);
				}
				System.out.println("Parcel location: " + node + ", Dijkstra's destination: " + bestPath.get(bestPath.size() - 1));
				
				//Ensure trucks pick up the parcel using the shortest path
				if ((bestPath != null) && (bestPath.size() > 0)){
					truck.setTravelPath(bestPath);
				}
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public void truckNotification(Truck truck, Notification notification) {
		
		//Current truck status
		switch(notification){
				case DROPPED_OFF_PARCEL:
					System.out.println("DROPPED_OFF_PARCEL");
					break;
				case GOING_TO_CHANGED:
					System.out.println("GOING_TO_CHANGED");
					break;
				case LOCATION_CHANGED:
					System.out.println("LOCATION_CHANGED");
					break;
				case PARCEL_AT_NODE:
					System.out.println("PARCEL_AT_NODE");			
					break;
				case PICKED_UP_PARCEL:
					System.out.println("PICKED_UP_PARCEL");
					break;
				case STATUS_CHANGED:
					System.out.println("STATUS_CHANGED");
					break;
				case TRAVELING_TO_CHANGED:
					System.out.println("TRAVELING_TO_CHANGED");
					break;
				case WAITING:
					
							//We are done delivering parcels
							if (((LinkedList<Parcel>)truck.getUserData()).size() == 0){
								System.out.println("WAIT");
								
								//Truck is already at the start position
								if (truck.getLocation().equals(b.getTruckDepot())){
									return;
								}
								
								//Move truck to the truck depot
								else{
									System.out.println("MOVE");
									Node node = b.getTruckDepot();
									moveTruck(truck, node);
								}
							}
							
							//We still have parcels to deliver
							else{
								
								//Truck is not carrying anything
								if (truck.getLoad() == null){
									System.out.println("PICK UP PARCEL");
									synchronized(this){
										
										//Truck has no parcel right now; pick up one
										Parcel parcel = ((LinkedList<Parcel>)truck.getUserData()).get(0);
										if (truck.getLocation().equals(parcel.getLocation())){
											truck.pickupLoad(parcel);
											
											//Move the parcel to its destination
											moveTruck(truck, parcel.destination);
										}
									}
								}
								else{
									
									//Truck has parcel already
									System.out.println("DROP OFF PARCEL");
									synchronized(this){
										Parcel parcel = truck.getLoad();
										
										//Initial parcel
										if (truck.getLocation().equals(parcel.destination)){
											((LinkedList<Parcel>)truck.getUserData()).poll();
											truck.dropoffLoad();
											
											//Further parcels
											if (((LinkedList<Parcel>)truck.getUserData()).size() != 0){
												parcel = ((LinkedList<Parcel>)truck.getUserData()).get(0);
												moveTruck(truck, parcel.getLocation());
											}
										}
									}
								}
							}
							break;
		}
	}
	
	public synchronized void moveTruck(Truck truck, Node node){
		if (truck.getLocation().equals(node)){
			return;
		}
		
		//Ensure trucks traverse shortest path to parcel
		LinkedList<Node> bestPath = Dijkstra(truck.getLocation(), node);
		truck.setTravelPath(bestPath);
	}
	
	public LinkedList<Node> Dijkstra(Node first, Node last){
		
		//Visited cities and distances
		HashMap<Node, Integer> nodeDistance = new HashMap<Node, Integer>();
		
		//Pairs of nodes and previously visited nodes
		HashMap<Node, Node> previousNodes = new HashMap<Node, Node>();
		
		//Set all nodes other than the start node to be at a large distance
		for (Node node: this.getBoard().getNodes()){
			nodeDistance.put(node, Integer.MAX_VALUE);
			previousNodes.put(node, null);
		}
		
		//Initialization of shortest path algorithm
		nodeDistance.put(first, 0);
		GriesHeap<Node> heap = new GriesHeap<Node>();
		heap.add(first, 0);
		
		//Keep getting shortest paths until end node is reached
		while ((last != heap.peek()) && (heap.size() > 0)){
			Node currentNode = heap.poll();
			
			for (Edge edge: currentNode.getExits()){
				
				//Get neighbor node
				Node nextNode = edge.getOther(currentNode);
				
				//Update distance from current node
				int currentDistance = nodeDistance.get(currentNode) + edge.length;
				int nextDistance = nodeDistance.get(nextNode);
				
				//We have a new shortest path
				if (currentDistance < nextDistance){
					
					nodeDistance.put(nextNode, currentDistance);
					
					previousNodes.put(nextNode, currentNode);
					
					try {
						heap.add(nextNode, currentDistance);
					}
					catch(IllegalArgumentException i){
						heap.updatePriority(nextNode, currentDistance);
					}
				}
			}
		}
		
		//Get shortest path
		LinkedList<Node> node = new LinkedList<Node>();
		Node currentNode = last;
		while (currentNode != null){
			node.push(currentNode);
			currentNode = previousNodes.get(currentNode);
		}
		return node;
	}

}
