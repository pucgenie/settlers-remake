/*******************************************************************************
 * Copyright (c) 2014 - 2017
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *******************************************************************************/
package jsettlers.logic.buildings.others;

import java8.util.J8Arrays;
import java8.util.function.BiConsumer;
import java8.util.stream.Collectors;
import jsettlers.common.buildings.EBuildingType;
import jsettlers.common.buildings.IBuilding;
import jsettlers.common.mapobject.EMapObjectType;
import jsettlers.common.material.EMaterialType;
import jsettlers.common.position.RelativePoint;
import jsettlers.common.position.ShortPoint2D;
import jsettlers.logic.buildings.Building;
import jsettlers.logic.buildings.IBuildingsGrid;
import jsettlers.logic.buildings.stack.IRequestStack;
import jsettlers.logic.buildings.stack.multi.MultiRequestAndOfferStack;
import jsettlers.logic.buildings.stack.multi.MultiRequestStackSharedData;
import jsettlers.logic.buildings.stack.multi.StockSettings;
import jsettlers.logic.objects.StockBuildingPartMapObject;
import jsettlers.logic.player.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import static java8.util.stream.StreamSupport.stream;

/**
 * Created by Andreas Eberle.
 */
public class StockBuilding extends Building implements IBuilding.IStock {
	private static final HashMap<EMapObjectType, RelativePoint> RELATIVE_BUILDING_PARTS = new HashMap<>();
	static {
		RELATIVE_BUILDING_PARTS.put(EMapObjectType.STOCK_BUILDING_PART_SOUTH, new RelativePoint(2, 4));
		RELATIVE_BUILDING_PARTS.put(EMapObjectType.STOCK_BUILDING_PART_SOUTH_WEST, new RelativePoint(-2, 2));
		RELATIVE_BUILDING_PARTS.put(EMapObjectType.STOCK_BUILDING_PART_SOUTH_EAST, new RelativePoint(4, 2));
		RELATIVE_BUILDING_PARTS.put(EMapObjectType.STOCK_BUILDING_PART_NORTH_WEST, new RelativePoint(-4, -2));
		RELATIVE_BUILDING_PARTS.put(EMapObjectType.STOCK_BUILDING_PART_NORTH_EAST, new RelativePoint(2, -2));
		RELATIVE_BUILDING_PARTS.put(EMapObjectType.STOCK_BUILDING_PART_NORTH, new RelativePoint(-2, -4));
	}

	private final StockSettings stockSettings;

	public StockBuilding(Player player, ShortPoint2D position, IBuildingsGrid buildingsGrid) {
		super(EBuildingType.STOCK, player, position, buildingsGrid);
		stockSettings = new StockSettings(buildingsGrid.getRequestStackGrid().getPartitionStockSettings(position));
	}

	@Override
	protected List<? extends IRequestStack> createWorkStacks() {
		MultiRequestStackSharedData sharedData = new MultiRequestStackSharedData(stockSettings);

		List<MultiRequestAndOfferStack> newStacks = J8Arrays.stream(type.getRequestStacks())
				.map(relativeStack -> relativeStack.calculatePoint(this.pos))
				.map(position -> new MultiRequestAndOfferStack(grid.getRequestStackGrid(), position, type, super.getPriority(), sharedData))
				.collect(Collectors.toList());

		stream(newStacks).forEach(stockSettings::registerStockSettingsListener);

		return newStacks;
	}

	public void setAcceptedMaterial(EMaterialType materialType, boolean accept) {
		stockSettings.setAccepted(materialType, accept);
	}

	@Override
	public StockSettings getStockSettings() {
		return stockSettings;
	}

	@Override
	protected int subTimerEvent() {
		return -1;
	}

	@Override
	protected int constructionFinishedEvent() {
		return -1;
	}

	@Override
	protected EMapObjectType getFlagType() {
		return EMapObjectType.FLAG_DOOR;
	}

	@Override
	public boolean isOccupied() {
		return true;
	}

	@Override
	protected void killedEvent() {
		stockSettings.releaseSettings();
		removeBuildingPartMapObjects(super.getPos());
	}

	@Override
	protected void placedAtEvent(ShortPoint2D position) {
		addBuildingPartMapObjects(position);
	}

	private void addBuildingPartMapObjects(ShortPoint2D position) {
		forBuildingParts(position, (partPosition, mapObjectType) -> {
			StockBuildingPartMapObject mapObject = new StockBuildingPartMapObject(this, mapObjectType);
			grid.getMapObjectsManager().addMapObject(partPosition, mapObject);
		});
	}

	private void removeBuildingPartMapObjects(ShortPoint2D position) {
		forBuildingParts(position,
				(partPosition, mapObjectType) -> grid.getMapObjectsManager().removeMapObjectType(partPosition.x, partPosition.y, mapObjectType));
	}

	private static void forBuildingParts(ShortPoint2D position, BiConsumer<ShortPoint2D, EMapObjectType> consumer) {
		for (Entry<EMapObjectType, RelativePoint> relativePartEntry : RELATIVE_BUILDING_PARTS.entrySet()) {
			ShortPoint2D partPosition = relativePartEntry.getValue().calculatePoint(position);
			System.out.println("for building part at: " + partPosition + " type: " + relativePartEntry.getKey());
			consumer.accept(partPosition, relativePartEntry.getKey());
		}
	}
}
