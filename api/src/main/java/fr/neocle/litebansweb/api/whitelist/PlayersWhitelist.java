package fr.neocle.litebansweb.api.whitelist;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.*;
import org.yaml.snakeyaml.representer.Representer;

import fr.neocle.litebansweb.api.events.EventDispatcher;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayersWhitelist {
    private final Path configFile;
    private final Logger logger;
    private final EventDispatcher eventDispatcher;
    private final Yaml yaml;

    public PlayersWhitelist(Path configFile, Logger logger, EventDispatcher eventDispatcher) {
        this.configFile = configFile;
        this.logger = logger;
        this.eventDispatcher = eventDispatcher;
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setProcessComments(true);
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setProcessComments(true);
        this.yaml = new Yaml(new Constructor(loaderOptions), new Representer(dumperOptions), dumperOptions, loaderOptions);
    }

    /**
     * Adds a player to the whitelist.
     *
     * @param playerName The player to add
     * @return true if added, false if already exists
     */
    public boolean addPlayer(String playerName) {
        boolean added = modifyWhitelist(playerName, true);

        if (added) {
            eventDispatcher.playerWhitelistedEvent(playerName);
        }

        return added;
    }

    /**
     * Removes a player from the whitelist.
     *
     * @param playerName The player to remove
     * @return true if removed, false if not found
     */
    public boolean removePlayer(String playerName) {
        boolean removed = modifyWhitelist(playerName, false);

        if (removed) {
            eventDispatcher.playerUnwhitelistedEvent(playerName);
        }

        return removed;
    }

    /**
     * Gets a list of all whitelisted players.
     * @return List of player names
     */
    public List<String> getWhitelistedPlayers() {
        if (!Files.exists(configFile)) {
            logger.warning("Config file not found: " + configFile);
            return new ArrayList<>();
        }
        
        Node root = readYamlRoot();
        if (!(root instanceof MappingNode mappingNode)) return new ArrayList<>();

        for (NodeTuple tuple : mappingNode.getValue()) {
            if (isPasswordLoginNode(tuple)) {
                return extractAllowedPlayers(tuple);
            }
        }
        return new ArrayList<>();
    }

    /**
     * Checks if a player is whitelisted.
     * @param username name of the player
     * @return true if the player is whitelisted, false otherwise
     */
    public boolean isPlayerWhitelisted(String playerName) {
        return getWhitelistedPlayers().contains(playerName);
    }

    private Node readYamlRoot() {
        try (FileReader reader = new FileReader(configFile.toFile())) {
            return yaml.compose(reader);
        } catch (IOException e) {
            logger.severe("Failed to read config file: " + e.getMessage());
            return null;
        }
    }

    private boolean isPasswordLoginNode(NodeTuple tuple) {
        return tuple.getKeyNode() instanceof ScalarNode keyNode && "password_login".equals(keyNode.getValue());
    }

    private List<String> extractAllowedPlayers(NodeTuple tuple) {
        if (tuple.getValueNode() instanceof MappingNode passwordLoginNode) {
            for (NodeTuple innerTuple : passwordLoginNode.getValue()) {
                if (innerTuple.getKeyNode() instanceof ScalarNode keyNode && "allowed_players".equals(keyNode.getValue())) {
                    if (innerTuple.getValueNode() instanceof SequenceNode sequenceNode) {
                        return sequenceNode.getValue().stream()
                                .filter(node -> node instanceof ScalarNode)
                                .map(node -> ((ScalarNode) node).getValue())
                                .collect(Collectors.toList());
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    private synchronized boolean modifyWhitelist(String playerName, boolean isAdding) {
        if (!Files.exists(configFile)) {
            logger.warning("Config file not found: " + configFile);
            return false;
        }

        Node root;
        try (FileReader reader = new FileReader(configFile.toFile())) {
            root = yaml.compose(reader);
        } catch (IOException e) {
            logger.severe("Failed to read config file: " + e.getMessage());
            return false;
        }

        if (root == null || !(root instanceof MappingNode mappingNode)) {
            logger.warning("Invalid or empty config file.");
            return false;
        }

        boolean updated = isAdding ? updateAllowedPlayers(mappingNode, playerName) : removeAllowedPlayer(mappingNode, playerName);

        if (updated) {
            try (FileWriter writer = new FileWriter(configFile.toFile())) {
                yaml.serialize(root, writer);
            } catch (IOException e) {
                logger.severe("Failed to write config file: " + e.getMessage());
                return false;
            }
        }

        return updated;
    }

    private boolean updateAllowedPlayers(MappingNode root, String playerName) {
        return modifyAllowedPlayersList(root, playerName, true);
    }

    private boolean removeAllowedPlayer(MappingNode root, String playerName) {
        return modifyAllowedPlayersList(root, playerName, false);
    }

    private boolean modifyAllowedPlayersList(MappingNode root, String playerName, boolean isAdding) {
        for (NodeTuple tuple : root.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode && "password_login".equals(keyNode.getValue())) {
                if (tuple.getValueNode() instanceof MappingNode passwordLoginNode) {
                    return modifyPlayerList(passwordLoginNode, playerName, isAdding);
                }
            }
        }
        return false;
    }

    private boolean modifyPlayerList(MappingNode passwordLoginNode, String playerName, boolean isAdding) {
        for (NodeTuple tuple : passwordLoginNode.getValue()) {
            if (tuple.getKeyNode() instanceof ScalarNode keyNode && "allowed_players".equals(keyNode.getValue())) {
                if (tuple.getValueNode() instanceof SequenceNode sequenceNode) {
                    List<Node> playerNodes = sequenceNode.getValue();

                    if (isAdding) {
                        for (Node node : playerNodes) {
                            if (node instanceof ScalarNode scalar && scalar.getValue().equals(playerName)) {
                                return false; // Player already exists
                            }
                        }
                        playerNodes.add(new ScalarNode(Tag.STR, playerName, null, null, DumperOptions.ScalarStyle.PLAIN));
                    } else {
                        List<Node> updatedNodes = playerNodes.stream()
                                .filter(node -> !(node instanceof ScalarNode scalar && scalar.getValue().equals(playerName)))
                                .collect(Collectors.toList());

                        if (updatedNodes.size() == playerNodes.size()) {
                            return false; // Player was not found
                        }

                        sequenceNode.getValue().clear();
                        sequenceNode.getValue().addAll(updatedNodes);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}
