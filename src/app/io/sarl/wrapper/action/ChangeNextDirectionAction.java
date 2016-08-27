package io.sarl.wrapper.action;

import org.intranet.elevator.model.Car;
import org.intranet.sim.event.EventQueue;
import org.json.JSONObject;

import io.sarl.wrapper.Direction;
import io.sarl.wrapper.WrapperModel;

public class ChangeNextDirectionAction extends Action
{
	private WrapperModel model;
	private int carId;
	private Direction direction;

	public ChangeNextDirectionAction(long actionId, WrapperModel model, JSONObject params)
	{
		super(actionId, model.getEventQueue());
		this.carId = params.getInt("car");
		String directionParam = params.getString("nextDirection");
		
		switch (directionParam)
		{
			case "up":
				this.direction = Direction.UP;
				break;
			case "down":
				this.direction = Direction.DOWN;
				break;
			default:
				this.direction = Direction.NONE;
				break;
		}
	}

	@Override
	protected ProcessingStatus performAction()
	{
		if (model.getCar(carId) == null)
		{
			failureReason = "No car with id: " + carId;
			return ProcessingStatus.FAILED;
		}
		
		if (model.getNextDirection(carId) == Direction.NONE)
		{
			failureReason = "Car with id: " + carId + " not in transit";
			return ProcessingStatus.FAILED;
		}
		
		if (direction == Direction.NONE)
		{
			failureReason = "Invalid 'nextDirection' parameter. Valid values are 'up' and 'down'";
			return ProcessingStatus.FAILED;
		}
		
		model.setNextDirection(carId, direction);
		return ProcessingStatus.COMPLETED;
	}

}
