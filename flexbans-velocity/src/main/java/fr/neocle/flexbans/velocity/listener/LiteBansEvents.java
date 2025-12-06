package fr.neocle.flexbans.velocity.listener;

import fr.neocle.flexbans.database.DatabaseUtils;
import fr.neocle.flexbans.handler.web.cache.CountsCache;
import litebans.api.Entry;
import litebans.api.Events;

public class LiteBansEvents extends Events.Listener {
    public LiteBansEvents(DatabaseUtils databaseUtils) {
        CountsCache.update(null, false, true);
    }

    @Override
    public void entryAdded(Entry entry) {
        CountsCache.update(null, false, true);
    }

    @Override
    public void entryRemoved(Entry entry) {
        CountsCache.update(null, false, true);
    }
}
