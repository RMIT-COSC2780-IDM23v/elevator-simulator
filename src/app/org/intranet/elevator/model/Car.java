/*
* Copyright 2003-2005 Neil McKellar and Chris Dailey
* All rights reserved.
*/
package org.intranet.elevator.model;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import org.intranet.sim.event.EventQueue;
import org.json.JSONObject;

import au.edu.rmit.elevatorsim.Direction;
import au.edu.rmit.elevatorsim.Transmittable;

/**
 * The states of Car are substates of MovableLocation:IDLE. Valid states:
 * <table border="1" cellspacing="0" cellpadding="2">
 * <tr>
 * <th rowspan="2">State</th>
 * <th colspan="2">Variables</th>
 * <th colspan="10">Transitions</th>
 * </tr>
 * <tr>
 * <th>destination</th>
 * <th>location</th>
 * <th>setDestination()</th>
 * <th>undock()</th>
 * <th>[MovableLocation.arrive()]</th>
 * </tr>
 * <tr>
 * <td>IDLE:UNDOCKED</td>
 * <td>null</td>
 * <td>null</td>
 * <td>MOVING or arrive(): DOCKED</td>
 * <td><i>Illegal</i></td>
 * <td><i>Impossible</i></td>
 * </tr>
 * <tr>
 * <td>MOVING</td>
 * <td>Set</td>
 * <td>null</td>
 * <td>MOVING or arrive(): DOCKED</td>
 * <td><i>Illegal</i></td>
 * <td>DOCKED<br/>
 * [docked()]</td>
 * </tr>
 * <tr>
 * <td>IDLE:UNDOCKING</td>
 * <td>Set</td>
 * <td>Set</td>
 * <td>UNDOCKING</td>
 * <td>MOVING</td>
 * <td><i>Impossible</i></td>
 * </tr>
 * <tr>
 * <td>IDLE:DOCKED</td>
 * <td>null</td>
 * <td>Set</td>
 * <td>UNDOCKING</td>
 * <td>UNDOCKED</td>
 * <td><i>Impossible</i></td>
 * </tr>
 * </table>
 * 
 * @author Neil McKellar and Chris Dailey
 * @author Joshua Beale
 */
public final class Car extends MovableLocation
{
	private String name;
	private Floor location;
	private Floor destination;
	private FloorRequestPanel panel = new FloorRequestPanel();
	private List<Listener> listeners = new ArrayList<Listener>();
	private float stoppingDistance;
	private int id;

	public interface Listener
	{
		void docked();
	}

	private class ArrivalMessage implements Transmittable
	{
		private int dest;

		public ArrivalMessage()
		{
			dest = destination.getFloorNumber();
		}

		public String getName()
		{
			return "carArrived";
		}

		public JSONObject getDescription()
		{
			JSONObject ret = new JSONObject();
			ret.put("floor", dest);
			ret.put("car", id);
			return ret;
		}
	}
	
	private class MovementTracker implements MovableLocation.Listener
	{
		private Floor origin;
		private Floor dest;
		private PriorityQueue<Floor> floorsToPass;

		public MovementTracker(Floor origin, Floor dest)
		{
			this.origin = origin;
			this.dest = dest;
			if (origin.getFloorNumber() == dest.getFloorNumber())
			{
				throw new IllegalArgumentException("no movement to track");
			}
			List<Floor> floorsInRange = new ArrayList<>(panel.getServicedFloors());
			floorsInRange.removeIf(f -> !floorInRange(f));
			
			floorsToPass = new PriorityQueue<>(floorsInRange.size(), (floor1, floor2) ->
			{
				int ret = floor1.getFloorNumber() - floor2.getFloorNumber();
				return (origin.getFloorNumber() < dest.getFloorNumber()) ? ret : 0 - ret;
			});
			
			floorsToPass.addAll(floorsInRange);
		}
		
		private boolean floorInRange(Floor floor)
		{
			return Math.min(origin.getFloorNumber(), dest.getFloorNumber()) < floor.getFloorNumber() &&
					Math.max(origin.getFloorNumber(), dest.getFloorNumber()) > floor.getFloorNumber();
		}
		
		@Override
		public void heightChanged(int height)
		{
			Floor floorToPass = floorsToPass.peek();
			if (floorToPass == null)
			{
				return;
			}
			
			if (origin.getFloorNumber() < dest.getFloorNumber())
			{
				if (height >= floorToPass.getHeight())
				{
					floorsToPass.poll();
					fireFloorPassed(floorToPass);
				}
				return;
			}
			if (origin.getFloorNumber() > dest.getFloorNumber())
			{
				if (height <= floorToPass.getHeight())
				{
					floorsToPass.poll();
					fireFloorPassed(floorToPass);
				}
				return;
			}
		}
		
		private void fireFloorPassed(Floor passed)
		{
			// TODO: this method
		}
	}

	public Car(EventQueue eQ, String name, float height, int capacity, int id, float stoppingDistance)
	{
		super(eQ, height, capacity);
		this.name = name;
		this.id = id;
		this.stoppingDistance = stoppingDistance;
	}

	/**
	 * Travel to specified destination floor
	 * 
	 * @param destination
	 *            Floor to travel to
	 */
	public void setDestination(Floor destination)
	{
		this.destination = destination;
		if (location == null) setDestinationHeight(destination.getHeight());
	}

	/**
	 * 
	 * @param floor
	 *            Destination
	 * @return Time to travel to specified floor
	 */
	public float getTravelTime(Floor floor)
	{
		float floorHeight = floor.getHeight();
		float travelDistance = floorHeight - getHeight();
		return getTravelTime(travelDistance);
	}

	/**
	* Undock from current location
	*/
	public void undock()
	{
		if (location == null) throw new IllegalStateException("Must be docked to undock");

		location = null;
		if (destination != null) setDestinationHeight(destination.getHeight());
	}

	/**
	 * @return Current destination floor
	 */
	public Floor getDestination()
	{
		return destination;
	}

	/**
	 * @return Current docked location floor
	 */
	public Floor getLocation()
	{
		return location;
	}

	/**
	 * @param listener
	 */
	public void addListener(Listener listener)
	{
		listeners.add(listener);
	}

	/**
	 * @param listener
	 */
	public void removeListener(Listener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * @return
	 */
	public String getName()
	{
		return name;
	}
	
	public int getId()
	{
		return id;
	}
	
	public float getStoppingDistance()
	{
		return stoppingDistance;
	}

	/**
	 * @return FloorRequestPanel of Car
	 */
	public FloorRequestPanel getFloorRequestPanel()
	{
		return panel;
	}

	/**
	 * Get Floor of undocked, non-travelling Car
	 * Really bad method? TODO re-factor me
	 * 
	 * @return Floor at specified height attached to FloorRequestPanel
	 * 
	 */
	public Floor getFloorAt()
	{
		if (destination == null && location == null) return panel.getFloorAt(getHeight());
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.intranet.elevator.model.MovableLocation#getRatePerSecond()
	 */
	public final float getRatePerSecond()
	{
		return (float) (1000 * 10.0 / 4030.0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.intranet.elevator.model.MovableLocation#arrive()
	 */
	protected void arrive()
	{
		location = destination;
		destination = null;
		panel.requestFulfilled(location);
		fireDockedEvent();
	}

	/**
	* Notify Listeners that Car has docked to destination Location
	*/
	private void fireDockedEvent()
	{
		for (Listener l : new ArrayList<Listener>(listeners))
			l.docked();
	}

	public Transmittable getArrivalMessage()
	{
		return new ArrivalMessage();
	}
}