package com.joedobo27.mmm;

import com.joedobo27.libs.TileUtilities;
import com.joedobo27.libs.action.ActionMaster;
import com.wurmonline.math.TilePos;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.Players;
import com.wurmonline.server.Server;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.items.ItemList;
import com.wurmonline.server.zones.Zone;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.WeakHashMap;
import java.util.function.Function;

class RaiseDirtTerraformer extends ActionMaster {

    private final TilePos targetedTile;
    private final int actionId;

    static WeakHashMap<Action, RaiseDirtTerraformer> actionDataWeakHashMap = new WeakHashMap<>();

    RaiseDirtTerraformer(Action action, Creature performer, @Nullable Item activeTool, int usedSkill, int minSkill,
                         int maxSkill, int longestTime, int shortestTime, int minimumStamina,
                         ArrayList<Function<ActionMaster, Boolean>> failureTestFunctions, TilePos targetedTile,
                         short actionId) {
        super(action, performer, activeTool, usedSkill, minSkill, maxSkill, longestTime, shortestTime, minimumStamina, failureTestFunctions);
        this.targetedTile = targetedTile;
        this.actionId = actionId;
        actionDataWeakHashMap.put(action, this);
    }

    static boolean hashMapHasInstance(Action action) {
        return actionDataWeakHashMap.containsKey(action);
    }

    boolean hasAFailureCondition() {
        return failureTestFunctions.stream()
                .anyMatch(function -> function.apply(this));
    }

    @SuppressWarnings("SameParameterValue")
    void modifyHeight(int modifier) {
        TileUtilities.setSurfaceHeight(this.targetedTile, TileUtilities.getSurfaceHeight(this.targetedTile) + modifier);
        Players.getInstance().sendChangedTile(this.targetedTile.x, this.targetedTile.y, this.performer.isOnSurface(),
                true);
    }

    void shouldRockBeDirt() {
        if (TileUtilities.getDirtDepth(this.targetedTile) <= 0)
            return;
        TilePos[] checkTiles = {this.targetedTile, this.targetedTile.West(), this.targetedTile.NorthWest(),
                this.targetedTile.North()};
        Arrays.stream(checkTiles)
                .filter(tilePos -> TileUtilities.getSurfaceTypeId(tilePos) == Tiles.TILE_TYPE_ROCK ||
                        TileUtilities.getSurfaceTypeId(tilePos) == Tiles.TILE_TYPE_CLIFF)
                .forEach(tilePos -> {
                    Server.modifyFlagsByTileType(tilePos.x, tilePos.y, Tiles.Tile.TILE_DIRT.id);
                    TileUtilities.setSurfaceTypeId(tilePos, Tiles.Tile.TILE_DIRT.id);
                    this.performer.getMovementScheme().touchFreeMoveCounter();
                    Players.getInstance().sendChangedTile(tilePos.x, tilePos.y, performer.isOnSurface(), true);
                    Zone zone = TileUtilities.getZoneSafe(tilePos, this.performer.isOnSurface());
                    if (zone != null)
                        zone.changeTile(tilePos.x, tilePos.y);
                });
    }

    void destroyRaiseResource() {
        Integer destroyItemTemplateId = null;
        if (this.actionId == MightyMattockMod.getRaiseDirtEntryId())
            destroyItemTemplateId = ItemList.dirtPile;
        if (this.actionId == MightyMattockMod.getRaiseRockEntryId())
            destroyItemTemplateId = ItemList.concrete;
        if (destroyItemTemplateId == null)
            return;
        Item[] items = this.getInventoryItems(destroyItemTemplateId);
        if (items == null || items.length == 0) {
            items = this.getGroundItems(destroyItemTemplateId, this.targetedTile);
        }
        if (items == null)
            return;
        Item item = items[Server.rand.nextInt(items.length)];
        if (item == null)
            return;
        item.setWeight(0, true);
    }

    /**
     * For the corner being dug there are 4 tiles affect. Check to see if the tile should be converted to dirt.
     * In the case where it's converted do the transformation and update the appropriate tile state values.
     */
    void shouldMutableBeDirt() {
        TilePos[] checkTiles = {this.targetedTile, this.targetedTile.West(), this.targetedTile.NorthWest(),
                this.targetedTile.North()};
        TilePos[] makeDirtPos = Arrays.stream(checkTiles)
                .filter(TileUtilities::isTileOverriddenByDirt)
                .toArray(TilePos[]::new);
        if (makeDirtPos == null || makeDirtPos.length == 0) {
            return;
        }
        Arrays.stream(makeDirtPos)
                .forEach(tilePos -> {
                    Server.modifyFlagsByTileType(tilePos.x, tilePos.y, Tiles.Tile.TILE_DIRT.id);
                    TileUtilities.setSurfaceTypeId(tilePos, Tiles.Tile.TILE_DIRT.id);
                    this.performer.getMovementScheme().touchFreeMoveCounter();
                    Players.getInstance().sendChangedTile(tilePos.x, tilePos.y, performer.isOnSurface(), true);
                    Zone zone = TileUtilities.getZoneSafe(tilePos, this.performer.isOnSurface());
                    if (zone != null)
                        zone.changeTile(tilePos.x, tilePos.y);
                });
    }

    double doSkillCheckAndGetPower(float counter) {
        if (this.usedSkill == null)
            return -1.0d;
        double difficulty = 1 + (TileUtilities.getSteepestSlope(this.targetedTile) / 5);
        return Math.max(1.0d,
                this.performer.getSkills().getSkillOrLearn(this.usedSkill).skillCheck(difficulty, this.activeTool,
                        0, false, counter));
    }

    @Override
    public TilePos getTargetTile() {
        return this.targetedTile;
    }
}