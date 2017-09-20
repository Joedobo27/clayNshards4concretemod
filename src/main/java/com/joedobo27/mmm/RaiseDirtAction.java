package com.joedobo27.mmm;

import com.joedobo27.libs.action.ActionMaster;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.server.behaviours.Action;
import com.wurmonline.server.behaviours.ActionEntry;
import com.wurmonline.server.creatures.Creature;
import com.wurmonline.server.items.Item;
import com.wurmonline.server.skills.SkillList;
import org.gotti.wurmunlimited.modsupport.actions.ActionPerformer;
import org.gotti.wurmunlimited.modsupport.actions.ModAction;

import java.util.ArrayList;
import java.util.function.Function;

import static com.joedobo27.libs.action.ActionFailureFunction.*;
import static org.gotti.wurmunlimited.modsupport.actions.ActionPropagation.*;

public class RaiseDirtAction implements ModAction, ActionPerformer {

    private final ActionEntry actionEntry;
    private final int actionId;

    RaiseDirtAction(int actionId, ActionEntry actionEntry) {
        this.actionId = actionId;
        this.actionEntry = actionEntry;
    }

    @Override
    public short getActionId() {
        return (short)this.actionId;
    }

    @Override
    public boolean action(Action action, Creature performer, Item source, int tileX, int tileY, boolean onSurface,
                          int heightOffset, Tiles.TileBorderDirection borderDirection, long borderId, short actionId,
                          float counter) {
        if (actionId != this.actionId || !TerraformBehaviours.isMattock(source))
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        RaiseDirtTerraformer raiseDirtTerraformer;
        if (!RaiseDirtTerraformer.hashMapHasInstance(action)) {
            ArrayList<Function<ActionMaster, Boolean>> failureTestFunctions = new ArrayList<>();
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_INSUFFICIENT_STAMINA));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_SERVER_BOARDER_TOO_CLOSE));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_GOD_PROTECTED));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_PVE_VILLAGE_ENEMY_ACTION));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_PVP_VILLAGE_ENEMY_ACTION));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_CORNER_OCCUPIED_BY_FENCE));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_IS_OCCUPIED_BY_HOUSE));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_IS_OCCUPIED_BY_BRIDGE_SUPPORT));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_IS_OCCUPIED_BY_BRIDGE_EXIT));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_IS_OCCUPIED_BY_CAVE_ENTRANCE));
            failureTestFunctions.add(getFunction(FAILURE_FUNCTION_NO_DIRT_NEARBY));

            ConfigureActionOptions options = ConfigureOptions.getInstance().getRaiseDirtActionOptions();
            raiseDirtTerraformer = new RaiseDirtTerraformer(action, performer, source, SkillList.DIGGING, options.getMinSkill(),
                                                            options.getMaxSkill(), options.getLongestTime(),
                                                            options.getShortestTime(), options.getMinimumStamina(),
                                                            failureTestFunctions,
                                                            TerraformBehaviours.getOpposingCorner(
                                                                    performer, tileX, tileY, borderDirection), actionId);
        }
        else
            raiseDirtTerraformer = RaiseDirtTerraformer.actionDataWeakHashMap.get(action);

        if(raiseDirtTerraformer.isActionStartTime(counter) && raiseDirtTerraformer.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        if (raiseDirtTerraformer.isActionStartTime(counter)) {
            raiseDirtTerraformer.doActionStartMessages();
            raiseDirtTerraformer.setInitialTime(this.actionEntry);
            source.setDamage(source.getDamage() + 0.0015f * source.getDamageModifier());
            performer.getStatus().modifyStamina(-1000.0f);
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
        }

        if (!raiseDirtTerraformer.isActionTimedOut(action, counter))
            return propagate(action, CONTINUE_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        if (raiseDirtTerraformer.hasAFailureCondition())
            return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);

        raiseDirtTerraformer.modifyHeight(1);
        raiseDirtTerraformer.shouldRockBeDirt();
        raiseDirtTerraformer.destroyRaiseResource();
        raiseDirtTerraformer.shouldMutableBeDirt();
        raiseDirtTerraformer.doSkillCheckAndGetPower(counter);
        source.setDamage(source.getDamage() + 0.0015f * source.getDamageModifier());
        performer.getStatus().modifyStamina(-5000.0f);
        raiseDirtTerraformer.doActionEndMessages();
        return propagate(action, FINISH_ACTION, NO_SERVER_PROPAGATION, NO_ACTION_PERFORMER_PROPAGATION);
    }
}