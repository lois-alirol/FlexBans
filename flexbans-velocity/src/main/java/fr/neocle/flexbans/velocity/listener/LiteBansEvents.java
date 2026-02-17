package fr.neocle.flexbans.velocity.listener;

import fr.neocle.flexbans.database.DatabaseUtils;
import litebans.api.Entry;
import litebans.api.Events;

public class LiteBansEvents extends Events.Listener {
    public LiteBansEvents(DatabaseUtils databaseUtils) {
    }

    @Override
    public void entryAdded(Entry entry) {
    }

    @Override
    public void entryRemoved(Entry entry) {
    }
}
