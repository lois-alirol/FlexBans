package fr.neocle.flexbans.util.permissions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.node.Node;
import net.luckperms.api.cacheddata.CachedMetaData; // Import nécessaire
import net.luckperms.api.node.NodeEqualityPredicate;
import net.luckperms.api.util.Tristate;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class FlexBansPermissionLookup {

    public static CompletableFuture<String> getUserPrefix(UUID uuid) {
        LuckPerms luckPerms = getLuckPerms();

        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                return null;
            }

            CachedMetaData metaData = user.getCachedData().getMetaData();

            String prefix = metaData.getPrefix();
            return prefix == null ? "" : prefix;
        });
    }

    public static CompletableFuture<Set<String>> getFlexBansPermissions(UUID uuid) {
        LuckPerms luckPerms = getLuckPerms();

        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                return Set.of();
            }

            return user.getNodes().stream()
                    .filter(Node::getValue)
                    .map(Node::getKey)
                    .filter(key -> key.startsWith("flexbans."))
                    .collect(Collectors.toSet());
        });
    }

    public static CompletableFuture<Boolean> givePermission(UUID uuid, String permission) {
        LuckPerms luckPerms = getLuckPerms();

        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                return false;
            }

            Node node = Node.builder(permission).value(true).build();
            Tristate state = user.data().contains(node, NodeEqualityPredicate.EXACT);

            if (state == Tristate.UNDEFINED) {
                user.data().add(node);
                luckPerms.getUserManager().saveUser(user);
            }

            return true;
        });
    }

    public static CompletableFuture<Boolean> removePermission(UUID uuid, String permission) {
        LuckPerms luckPerms = getLuckPerms();

        return luckPerms.getUserManager().loadUser(uuid).thenApply(user -> {
            if (user == null) {
                return false;
            }

            Node node = Node.builder(permission).value(true).build();
            Tristate state = user.data().contains(node, NodeEqualityPredicate.EXACT);

            if (state == Tristate.TRUE) {
                user.data().remove(node);
                luckPerms.getUserManager().saveUser(user);
            }

            return true;
        });
    }

    private static LuckPerms getLuckPerms() {
        try {
            return LuckPermsProvider.get();
        } catch (IllegalStateException ex) {
            throw new IllegalStateException(
                    "LuckPerms is not loaded or not present on this platform.", ex
            );
        }
    }
}