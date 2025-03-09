package fr.formiko.mc.underilla.paper.listener;

import fr.formiko.mc.underilla.paper.Underilla;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;

public class WorldListener implements Listener {
    @EventHandler
    public void onServerEndLoading(ServerLoadEvent event) { Underilla.getInstance().runNextStepsAfterWorldInit(); }
}
