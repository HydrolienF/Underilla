package fr.formiko.mc.underilla.paper.cleaning;

import fr.formiko.mc.underilla.paper.Underilla;
import fr.formiko.mc.underilla.paper.io.UnderillaConfig.SetEntityTypeKeys;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;

public class CleanEntities {

    public static void cleanEntities(Chunk chunk) {
        for (Entity entity : chunk.getEntities()) {
            if (Underilla.getUnderillaConfig().isEntityTypeInSet(SetEntityTypeKeys.CLEAN_ENTITY_TO_REMOVE, entity.getType())) {
                entity.remove();
                // Final transformation that can be override by other plugins
            } else if (Underilla.getInstance().hasEndEntityTransformer()) {
                Underilla.getInstance().getEndEntityTransformer().accept(entity);
            }
        }
    }
}
