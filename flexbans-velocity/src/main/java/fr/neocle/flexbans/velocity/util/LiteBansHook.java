package fr.neocle.flexbans.velocity.util;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.velocity.listener.LiteBansEvents;
import litebans.api.Events;

public class LiteBansHook {
    public static void register(DatabaseUtils dbUtils) {
        LiteBansEvents listener = new LiteBansEvents(dbUtils);
        Events.get().register(listener);
    }
}