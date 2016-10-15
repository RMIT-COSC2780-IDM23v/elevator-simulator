package au.edu.rmit.elevatorsim.action;

import org.intranet.elevator.model.Car;
import org.intranet.elevator.model.Floor;
import org.json.JSONObject;

import au.edu.rmit.elevatorsim.Direction;
import au.edu.rmit.elevatorsim.WrapperModel;

/**
 * Performs the 'sendCar' action
 * @author Joshua Richards
 */
public class SendCarAction extends Action
{
	private WrapperModel model;
	private int carId;
	private int floorId;
	private Direction nextDirection;

	public SendCarAction(long actionId, WrapperModel model, JSONObject params)
	{
		super(actionId, model.getEventQueue());
		this.model = model;
		this.carId = params.getInt("car");
		this.floorId = params.getInt("floor");
		
		String nextDirectionParam = params.getString("nextDirection");
		
		switch (nextDirectionParam)
		{
			case "up":
				this.nextDirection = Direction.UP;
				break;
			case "down":
				this.nextDirection = Direction.DOWN;
				break;
			default:
				// this causes performAction to return FAILED
				this.nextDirection = Direction.NONE;
				break;
		}
	}

	@Override
	protected ProcessingStatus performAction()
	{
		Floor floor = model.getFloor(floorId);
		Car car = model.getCar(carId);
		if (car == null)
		{
			failureReason = "No car with id: " + carId;
			return ProcessingStatus.FAILED;
		}
		
		if (floor == null)
		{
			failureReason = "No floor with id: " + floorId;
			return ProcessingStatus.FAILED;
		}

		if (nextDirection == Direction.NONE)
		{
			failureReason = "Invalid value for 'nextDirection' parameter. " +
				"Valid values are 'up' and 'down'";
			return ProcessingStatus.FAILED;
		}
		
		if (model.getNextDirection(carId) != Direction.NONE)
		{
			return changeDestination();
		}
		
		// do the action
		model.setNextDirection(carId, nextDirection);
		car.setDestination(floor);
		
		return ProcessingStatus.IN_PROGRESS;
	}
	
	private ProcessingStatus changeDestination()
	{
		Floor floor = model.getFloor(floorId);
		Car car = model.getCar(carId);
		
		float currentHeight = car.getHeight();
		float lastDestHeight = car.getDestination().getHeight();
		float newDestHeight = floor.getHeight();
		Direction currDirection = (currentHeight < lastDestHeight) ?
				Direction.UP : Direction.DOWN;
		
		if (currDirection == Direction.UP)
		{
			if (newDestHeight < currentHeight)
			{
				failureReason = "Car " + carId + " already past floor " + floorId;
				return ProcessingStatus.FAILED;
			}
		}
		else
		{
			if (newDestHeight > currentHeight)
			{
				failureReason = "Car " + carId + " already past floor " + floorId;
				return ProcessingStatus.FAILED;
			}
		}
		
		car.setDestination(floor);
		model.setNextDirection(carId, nextDirection);
		return ProcessingStatus.IN_PROGRESS;
	}

}
