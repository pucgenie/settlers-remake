package jsettlers.logic.newmovable;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import jsettlers.common.map.shapes.HexGridArea;
import jsettlers.common.map.shapes.HexGridArea.HexGridAreaIterator;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.material.ESearchType;
import jsettlers.common.movable.EAction;
import jsettlers.common.movable.EDirection;
import jsettlers.common.movable.EMovableType;
import jsettlers.common.movable.IMovable;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.common.selectable.ESelectionType;
import jsettlers.logic.algorithms.fogofwar.IViewDistancable;
import jsettlers.logic.algorithms.path.IPathCalculateable;
import jsettlers.logic.algorithms.path.Path;
import jsettlers.logic.constants.Constants;
import jsettlers.logic.newmovable.interfaces.IDebugable;
import jsettlers.logic.newmovable.interfaces.IIDable;
import jsettlers.logic.newmovable.interfaces.INewMovableGrid;
import jsettlers.logic.newmovable.interfaces.IStrategyGrid;
import jsettlers.logic.timer.ITimerable;
import jsettlers.logic.timer.MovableTimer;
import random.RandomSingleton;

/**
 * Central Movable class of JSettlers.
 * 
 * @author Andreas Eberle
 * 
 */
public final class NewMovable implements ITimerable, IMovable, IPathCalculateable, IIDable, IDebugable, Serializable, IViewDistancable {
	private static final long serialVersionUID = 2472076796407425256L;
	private static final float WALKING_PROGRESS_INCREASE = 1.0f / (Constants.MOVABLE_STEP_DURATION * Constants.MOVABLE_INTERRUPTS_PER_SECOND);
	private static final short NOTHING_TO_DO_MAX_RADIUS = 3;

	private static final HashMap<Integer, NewMovable> movablesByID = new HashMap<Integer, NewMovable>();
	private static final ConcurrentLinkedQueue<NewMovable> allMovables = new ConcurrentLinkedQueue<NewMovable>();
	private static int nextID = Integer.MIN_VALUE;

	private final INewMovableGrid<NewMovable> grid;
	private final int id;

	private ENewMovableState state = ENewMovableState.SLEEPING;

	private EMovableType movableType;
	private NewMovableStrategy strategy;
	private byte player;

	private EMaterialType materialType = EMaterialType.NO_MATERIAL;
	private EAction movableAction = EAction.NO_ACTION;
	private EDirection direction;

	private float progress;
	private float progressIncrease;

	private ShortPoint2D position;

	private ShortPoint2D moveToRequest = null;
	private Path path;

	private float health = 1.0f;
	private boolean visible;
	private NewMovable pushedFrom;

	private boolean selected = false;
	private boolean soundPlayed = false;
	private boolean isRightstep = false;

	private float doingNothingProbablity = 0.06f;

	public NewMovable(INewMovableGrid<NewMovable> grid, EMovableType movableType, byte player) {
		this.grid = grid;
		this.player = player;
		this.strategy = NewMovableStrategy.getStrategy(this, movableType);

		// The test movable has no images, so display a bearer
		this.movableType = movableType == EMovableType.TEST_MOVABLE ? EMovableType.SWORDSMAN_L1 : movableType;

		this.direction = EDirection.values[RandomSingleton.getInt(0, 5)];

		MovableTimer.add(this);

		this.id = nextID++;
		movablesByID.put(this.id, this);
		allMovables.offer(this);
	}

	/**
	 * This method overrides the standard deserialize method to restore the movablesByID map and the nextID.
	 * 
	 * @param ois
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private final void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		movablesByID.put(this.id, this);
		allMovables.add(this);
		nextID = Math.max(nextID, this.id + 1);
		MovableTimer.add(this);
	}

	/**
	 * Tests if this movable can receive moveTo requests and if so, directs it to go to the given position.
	 * 
	 * @param targetPosition
	 */
	public final void moveTo(ShortPoint2D targetPosition) {
		if (movableType.isMoveToAble() && state != ENewMovableState.SLEEPING) {
			this.moveToRequest = targetPosition;
		}
	}

	/**
	 * Positions this movable at the given position on the it's grid.<br>
	 * If the movable is already located on the grid, it's removed from it's old position and then added to the new one.<br>
	 * If the given position is null, the movable will only be removed from the grid and thus get invisible.
	 * 
	 * @param position
	 * @return this movable
	 */
	public final NewMovable positionAt(ShortPoint2D position) {
		assert grid.isFreeForMovable(position.getX(), position.getY()) : "given position not free for movable! " + position;

		if (this.position != null) {
			grid.leavePosition(this.position, this);
			setState(ENewMovableState.SLEEPING);
		}
		this.position = position;

		if (position != null) {
			grid.enterPosition(this.position, this);
			setState(ENewMovableState.DOING_NOTHING);
		}

		return this;
	}

	@Override
	public void timerEvent() {
		switch (state) {
		case SLEEPING:
			return;

		case DOING_NOTHING:
			doingNothingAction();
			break;

		case GOING_SINGLE_STEP:
		case PLAYING_ACTION:
			progressCurrentAction();
			break;

		case PATHING:
			pathingAction();
			break;
		}

		if (moveToRequest != null) {
			switch (state) {
			case PATHING:
				if (progress < 1) {
					break;
				} // if we are pathing and finished a step, calculate new path
				setState(ENewMovableState.DOING_NOTHING);
			case DOING_NOTHING:
				goToPos(moveToRequest); // progress is reset in here
				moveToRequest = null;
				break;
			}
		}

		if (state == ENewMovableState.DOING_NOTHING) {
			strategy.action();
		}
	}

	private void pathingAction() {
		if (progress >= 1) {
			if (path.isFinished() || !strategy.checkPathStepPreconditions()) { // if path is finished, or canceled by strategy return from here
				setState(ENewMovableState.DOING_NOTHING);
				movableAction = EAction.NO_ACTION;
				path = null;
				return;
			}

			direction = EDirection.getDirection(position.getX(), position.getY(), path.nextX(), path.nextY());

			if (grid.isFreeForMovable(path.nextX(), path.nextY())) { // if we can go on to the next step
				goSinglePathStep();
			} else { // step not possible, so try it next time
				grid.getMovableAt(path.nextX(), path.nextY()).push(this);
				return;
			}
		} else {
			progress += WALKING_PROGRESS_INCREASE;
		}
	}

	private void goSinglePathStep() {
		initGoingSingleStep(path.getNextPos());
		path.goToNextStep();
	}

	private void initGoingSingleStep(ShortPoint2D position) {
		movableAction = EAction.WALKING;
		progress -= 1;
		grid.leavePosition(this.position, this);
		grid.enterPosition(position, this);
		this.position = position;
		isRightstep = !isRightstep;
	}

	private void progressCurrentAction() {
		progress += progressIncrease;
		if (progress > 1.01) { // > 1.01 ensures that the image for 100 % is also shown for one cycle
			setState(ENewMovableState.DOING_NOTHING);
			this.movableAction = EAction.NO_ACTION;
		}
	}

	private void doingNothingAction() {
		float random = RandomSingleton.nextF();
		if (random <= doingNothingProbablity) {
			short x = position.getX(), y = position.getY();
			HexGridArea area = new HexGridArea(x, y, (short) 1, NOTHING_TO_DO_MAX_RADIUS);
			float dx = 0, dy = 0;
			HexGridAreaIterator iter = area.iterator();
			while (iter.hasNext()) {
				short currX = iter.getNextX();
				short currY = iter.getNextY();
				int factor;

				if (!grid.isInBounds(currX, currY) || grid.isBlocked(currX, currY)) {
					factor = iter.getCurrRadius() == 1 ? 10 : 4;
				} else if (grid.getMovableAt(currX, currY) != null) {
					factor = NOTHING_TO_DO_MAX_RADIUS - iter.getCurrRadius() + 1;
				} else {
					continue;
				}

				dx += (short) (x - currX) * factor;
				dy += (short) (y - currY) * factor;
			}

			if (Math.abs(dx) + Math.abs(dy) > 1.6f) {
				this.goInDirection(EDirection.getApproxDirection(0, 0, (int) dx, (int) dy));
				doingNothingProbablity = Math.min(doingNothingProbablity + 0.02f, 0.5f);
			} else {
				doingNothingProbablity = Math.max(doingNothingProbablity - 0.02f, 0.06f);
			}
		} else if (random >= 1 - Constants.MOVABLE_TURN_PROBABILITY) {
			lookInDirection(direction.getNeighbor(2 * RandomSingleton.getInt(0, 1) - 1));
		}
	}

	private void push(NewMovable pushingMovable) {
		switch (state) {
		case DOING_NOTHING:
			if (!goToRandomDirection(pushingMovable)) { // try to find free direction
				pushingMovable.goSinglePathStep(); // if no free direction found, exchange movables positions
				EDirection pushedFromDir = EDirection.getDirection(this.getPos(), pushingMovable.getPos());
				goInDirection(pushedFromDir);
			}
			break;

		case PATHING:
			if (this.progress >= 1) {
				if (pushingMovable.direction.getInverseDirection() == this.direction) { // two movables going in opposite direction against each other
					pushingMovable.goSinglePathStep();
					this.goSinglePathStep();
				} else {
					ShortPoint2D nextPos = this.direction.getNextHexPoint(this.position);
					if (grid.isFreeForMovable(nextPos.getX(), nextPos.getY())) {
						// this movable isn't blocked, so just let it's pathingAction() handle this
					} else if (pushedFrom == null) {
						this.pushedFrom = pushingMovable;
						grid.getMovableAt(nextPos.getX(), nextPos.getY()).push(this);
						this.pushedFrom = null;
					} else {
						while (pushingMovable != this) {
							pushingMovable.goSinglePathStep();
							pushingMovable = pushingMovable.pushedFrom;
						}
						this.goSinglePathStep();
					}
				}
			}
			break;

		case GOING_SINGLE_STEP:
		case PLAYING_ACTION:
			break; // just ignore

		case SLEEPING:
			assert false : "got pushed while sleeping: should not be possible!";
		}
	}

	private boolean goToRandomDirection(NewMovable pushingMovable) {
		int offset = RandomSingleton.getInt(0, EDirection.NUMBER_OF_DIRECTIONS - 1);
		EDirection pushedFromDir = EDirection.getDirection(this.getPos(), pushingMovable.getPos());

		for (int i = 0; i < EDirection.NUMBER_OF_DIRECTIONS; i++) {
			EDirection currDir = EDirection.values[(i + offset) % EDirection.NUMBER_OF_DIRECTIONS];
			if (currDir != pushedFromDir && goInDirection(currDir)) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Converts this movable to a movable of the given {@link EMovableType}.
	 * 
	 * @param movableType
	 */
	final void convertTo(EMovableType movableType) {
		this.movableType = movableType;
		this.strategy = NewMovableStrategy.getStrategy(this, movableType);
		setState(ENewMovableState.DOING_NOTHING);
	}

	/**
	 * Sets the material this movable is carrying to the given one.
	 * 
	 * @param materialType
	 * @return {@link EMaterialType} that has been set before.
	 */
	final EMaterialType setMaterial(EMaterialType materialType) {
		assert materialType != null : "MaterialType may not be null";
		EMaterialType former = this.materialType;
		this.materialType = materialType;
		return former;
	}

	/**
	 * Lets this movable execute the given action with given duration.
	 * 
	 * @param movableAction
	 *            action to be animated.
	 * @param duration
	 *            duration the animation should last (in seconds).
	 */
	final void playAction(EAction movableAction, float duration) {
		assert state == ENewMovableState.DOING_NOTHING : "can't do playAction() if state isn't DOING_NOTHING. curr state: " + state;

		this.movableAction = movableAction;
		setState(ENewMovableState.PLAYING_ACTION);
		this.progressIncrease = 1.0f / (duration * Constants.MOVABLE_INTERRUPTS_PER_SECOND);
		this.progress = 0;
		this.soundPlayed = false;
	}

	/**
	 * Lets this movable look in the given direction.
	 * 
	 * @param direction
	 */
	final void lookInDirection(EDirection direction) {
		this.direction = direction;
	}

	/**
	 * Lets this movable go to the given position.
	 * 
	 * @param targetPos
	 *            position to move to.
	 * @return true if it was possible to calculate a path to the given position<br>
	 *         false if it wasn't possible to get a path.
	 */
	final boolean goToPos(ShortPoint2D targetPos) {
		assert state == ENewMovableState.DOING_NOTHING : "can't do goToPos() if state isn't DOING_NOTHING. curr state: " + state;

		Path path = grid.calculatePathTo(this, targetPos);
		if (path == null) {
			return false;
		} else {
			followPath(path);
			return true;
		}
	}

	/**
	 * Tries to go a step in the given direction.
	 * 
	 * @param direction
	 *            direction to go
	 * @return true if the step can and will immediately be executed. <br>
	 *         false if the target position is generally blocked or a movable occupies that position.
	 */
	final boolean goInDirection(EDirection direction) {
		ShortPoint2D pos = direction.getNextHexPoint(position);
		if (isValidPosition(pos) && grid.isFreeForMovable(pos.getX(), pos.getY())) {
			initGoingSingleStep(pos);
			this.direction = direction;
			progressIncrease = WALKING_PROGRESS_INCREASE;
			setState(ENewMovableState.GOING_SINGLE_STEP);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Forces the movable to go a step in the given direction (if it is not blocked).
	 * 
	 * @param direction
	 *            direction to go
	 * @return true if the step can and will immediately be executed. <br>
	 *         false if the target position is blocked for this movable.
	 */
	final boolean forceGoInDirection(EDirection direction) {
		ShortPoint2D targetPos = direction.getNextHexPoint(position);
		if (isValidPosition(targetPos)) {
			this.followPath(new Path(targetPos));
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 
	 * @return {@link IStrategyGrid} that can be used by the strategy to gain informations from the grid.
	 */
	final IStrategyGrid getStrategyGrid() {
		return grid;
	}

	final void setPos(ShortPoint2D position) {
		if (visible) {
			grid.leavePosition(this.position, this);
			grid.enterPosition(position, this);
		}

		this.position = position;
	}

	final void setVisible(boolean visible) {
		if (this.visible == visible) { // nothing to change
		} else if (this.visible) { // is visible and gets invisible
			grid.leavePosition(position, this);
		} else {
			grid.enterPosition(position, this);
		}

		this.visible = visible;
	}

	/**
	 * 
	 * @param dijkstra
	 *            if true, dijkstra algorithm is used<br>
	 *            if false, in area finder is used.
	 * @param centerX
	 * @param centerY
	 * @param radius
	 * @param searchType
	 * @return
	 */
	final boolean preSearchPath(boolean dikjstra, short centerX, short centerY, short radius, ESearchType searchType) {
		assert state == ENewMovableState.DOING_NOTHING : "this method can only be invoked in state DOING_NOTHING";

		if (dikjstra) {
			this.path = grid.searchDijkstra(this, centerX, centerY, radius, searchType);
		} else {
			this.path = grid.searchInArea(this, centerX, centerY, radius, searchType);
		}

		if (path != null) {
			return true;
		} else {
			return false;
		}
	}

	final void followPresearchedPath() {
		assert this.path != null : "path mustn't be null to be able to followPresearchedPath()!";
		followPath(this.path);
	}

	private void followPath(Path path) {
		this.path = path;
		setState(ENewMovableState.PATHING);
		this.movableAction = EAction.NO_ACTION;
		progress = 1;
	}

	/**
	 * Checks if the given position is free or blocked for this movable.
	 * 
	 * @param pos
	 *            position to be checked
	 * @return true if the given position can be accessed by this movable
	 */
	private boolean isValidPosition(ShortPoint2D pos) {
		return grid.isInBounds(pos.getX(), pos.getY()) && !grid.isBlocked(pos.getX(), pos.getY())
				&& (!this.needsPlayersGround() || grid.getPlayer(pos.getX(), pos.getY()) == this.getPlayer());
	}

	/**
	 * Sets the state to the given one and resets the movable to a clean start of this state.
	 * 
	 * @param newState
	 */
	private void setState(ENewMovableState newState) {
		this.state = newState;
	}

	/**
	 * Used for networking to identify movables over the network.
	 * 
	 * @param id
	 *            id to be looked for
	 * @return returns the movable with the given ID<br>
	 *         or null if the id can not be found
	 */
	public final static NewMovable getMovableByID(int id) {
		return movablesByID.get(id);
	}

	public final static ConcurrentLinkedQueue<NewMovable> getAllMovables() {
		return allMovables;
	}

	/**
	 * kills this movable.
	 */
	@Override
	public final void kill() {
		MovableTimer.remove(this);
		grid.leavePosition(this.position, this);
		this.health = 0;
		this.strategy.killedEvent(path != null ? path.getTargetPos() : null);

		movablesByID.remove(this.getID());
		allMovables.remove(this);

		grid.addSelfDeletingMapObject(position, EMapObjectType.GHOST, 1, player);
	}

	@Override
	public final byte getPlayer() {
		return player;
	}

	@Override
	public final boolean isSelected() {
		return selected;
	}

	@Override
	public final void setSelected(boolean selected) {
		this.selected = selected;
	}

	@Override
	public final void stopOrStartWorking(boolean stop) {
		// TODO Auto-generated method stub
	}

	@Override
	public final ESelectionType getSelectionType() {
		return movableType.getSelectionType();
	}

	@Override
	public final void setSoundPlayed() {
		this.soundPlayed = true;
	}

	@Override
	public final boolean isSoundPlayed() {
		return soundPlayed;
	}

	@Override
	public final EMovableType getMovableType() {
		return movableType;
	}

	@Override
	public final EAction getAction() {
		return movableAction;
	}

	@Override
	public final EDirection getDirection() {
		return direction;
	}

	@Override
	public final float getMoveProgress() {
		return progress;
	}

	@Override
	public final EMaterialType getMaterial() {
		return materialType;
	}

	@Override
	public final ShortPoint2D getPos() {
		return position;
	}

	@Override
	public final float getHealth() {
		return health;
	}

	@Override
	public final boolean isRightstep() {
		return isRightstep;
	}

	@Override
	public final boolean needsPlayersGround() {
		return movableType.needsPlayersGround();
	}

	@Override
	public final short getViewDistance() {
		return Constants.MOVABLE_VIEW_DISTANCE;
	}

	@Override
	public final void debug() {
		System.out.println("debug");
	}

	@Override
	public final int getID() {
		return id;
	}

}
