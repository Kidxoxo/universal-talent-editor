package com.kidxoxo.talenteditor.command;

import com.kidxoxo.talenteditor.TalentEditorMod;
import com.kidxoxo.talenteditor.catalog.AttributeCatalog;
import com.kidxoxo.talenteditor.catalog.AttributeDescriptor;
import com.kidxoxo.talenteditor.network.EditorNetwork;
import com.kidxoxo.talenteditor.runtime.RuntimeSnapshot;
import com.kidxoxo.talenteditor.runtime.TalentAttributeRuntime;
import com.kidxoxo.talenteditor.runtime.TriggerTraceEntry;
import com.kidxoxo.talenteditor.runtime.ConditionEvaluator;
import com.kidxoxo.talenteditor.synthesis.SynthesisResult;
import com.kidxoxo.talenteditor.player.PlayerTalents;
import com.kidxoxo.talenteditor.domain.TalentDefinition;
import com.kidxoxo.talenteditor.domain.ConfiguredItemDefinition;
import com.kidxoxo.talenteditor.resource.ClientResourceRequirementCatalog;
import com.kidxoxo.talenteditor.resource.ClientResourceStatusTracker;
import com.kidxoxo.talenteditor.item.ConfiguredItem;
import com.kidxoxo.talenteditor.validation.ValidationIssue;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Collection;
import java.util.Comparator;

public final class TalentEditorCommands {
    private static final int MAX_CATALOG_RESULTS = 40;
    private static final int MAX_ISSUES = 40;
    private static final ResourceLocation DEFAULT_POOL = new ResourceLocation(TalentEditorMod.MOD_ID, "default");

    private TalentEditorCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal(TalentEditorMod.MOD_ID)
                .then(Commands.literal("book").executes(context -> book(context.getSource())))
                .then(Commands.literal("draw")
                        .executes(context -> draw(context.getSource(), DEFAULT_POOL))
                        .then(Commands.argument("pool", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        TalentEditorMod.SNAPSHOTS.active().pools().keySet(), builder
                                ))
                                .executes(context -> draw(
                                        context.getSource(), ResourceLocationArgument.getId(context, "pool")
                                ))))
                .then(Commands.literal("synthesize")
                        .then(Commands.argument("talent", ResourceLocationArgument.id())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        TalentEditorMod.SNAPSHOTS.active().talents().values().stream()
                                                .filter(talent -> talent.synthesis().enabled())
                                                .map(TalentDefinition::id),
                                        builder
                                ))
                                .executes(context -> synthesize(
                                        context.getSource(), ResourceLocationArgument.getId(context, "talent")
                                ))))
                .then(Commands.literal("open")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> open(context.getSource())))
                .then(Commands.literal("grant")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("talent", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                TalentEditorMod.SNAPSHOTS.active().talents().keySet(), builder
                                        ))
                                        .executes(context -> grant(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "talent")
                                        )))))
                .then(Commands.literal("revoke")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("talent", ResourceLocationArgument.id())
                                        .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                TalentEditorMod.SNAPSHOTS.active().talents().keySet(), builder
                                        ))
                                        .executes(context -> revoke(
                                                context.getSource(),
                                                EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "talent")
                                        )))))
                .then(Commands.literal("inspect")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> inspect(
                                        context.getSource(), EntityArgument.getPlayer(context, "target")
                                ))))
                .then(Commands.literal("tokens")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1_000_000))
                                                .executes(context -> addTokens(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                )))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(0, 1_000_000))
                                                .executes(context -> setTokens(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        IntegerArgumentType.getInteger(context, "amount")
                                                ))))))
                .then(Commands.literal("counters")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("get")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("counter", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        TalentEditorMod.SNAPSHOTS.active().counters().keySet(), builder
                                                ))
                                                .executes(context -> getCounter(
                                                        context.getSource(),
                                                        EntityArgument.getPlayer(context, "target"),
                                                        ResourceLocationArgument.getId(context, "counter")
                                                )))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("counter", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        TalentEditorMod.SNAPSHOTS.active().counters().keySet(), builder
                                                ))
                                                .then(Commands.argument("value", LongArgumentType.longArg(
                                                                0L, 1_000_000_000_000L
                                                        ))
                                                        .executes(context -> setCounter(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                ResourceLocationArgument.getId(context, "counter"),
                                                                LongArgumentType.getLong(context, "value")
                                                        ))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("counter", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        TalentEditorMod.SNAPSHOTS.active().counters().keySet(), builder
                                                ))
                                                .then(Commands.argument("amount", LongArgumentType.longArg(
                                                                -1_000_000_000_000L, 1_000_000_000_000L
                                                        ))
                                                        .executes(context -> addCounter(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                ResourceLocationArgument.getId(context, "counter"),
                                                                LongArgumentType.getLong(context, "amount")
                                                        ))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("counter", ResourceLocationArgument.id())
                                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                                        TalentEditorMod.SNAPSHOTS.active().counters().keySet(), builder
                                                ))
                                                .executes(context -> resetCounter(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                ResourceLocationArgument.getId(context, "counter")
                                                ))))))
                .then(Commands.literal("items")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("list")
                                .executes(context -> configuredItems(context.getSource(), ""))
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(context -> configuredItems(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "query")
                                        ))))
                        .then(Commands.literal("give")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                                .suggests((context, builder) ->
                                                        SharedSuggestionProvider.suggestResource(
                                                                TalentEditorMod.SNAPSHOTS.active()
                                                                        .configuredItems().keySet(), builder
                                                        ))
                                                .executes(context -> giveConfiguredItem(
                                                        context.getSource(),
                                                        EntityArgument.getPlayers(context, "targets"),
                                                        ResourceLocationArgument.getId(context, "item"), 1
                                                ))
                                                .then(Commands.argument("count", IntegerArgumentType.integer(
                                                                1, 2_304
                                                        ))
                                                        .executes(context -> giveConfiguredItem(
                                                                context.getSource(),
                                                                EntityArgument.getPlayers(context, "targets"),
                                                                ResourceLocationArgument.getId(context, "item"),
                                                                IntegerArgumentType.getInteger(context, "count")
                                                        )))))))
                .then(Commands.literal("status")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> status(context.getSource())))
                .then(Commands.literal("resources")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> resources(context.getSource())))
                .then(Commands.literal("trace")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> traceStatus(context.getSource()))
                        .then(Commands.literal("on")
                                .executes(context -> setTrace(context.getSource(), true)))
                        .then(Commands.literal("off")
                                .executes(context -> setTrace(context.getSource(), false)))
                        .then(Commands.literal("clear")
                                .executes(context -> clearTrace(context.getSource())))
                        .then(Commands.literal("show")
                                .executes(context -> showTrace(context.getSource(), 10))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 40))
                                        .executes(context -> showTrace(
                                                context.getSource(),
                                                IntegerArgumentType.getInteger(context, "count")
                                        )))))
                .then(Commands.literal("validate")
                        .requires(source -> source.hasPermission(2))
                        .executes(context -> validate(context.getSource())))
                .then(Commands.literal("catalog")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.literal("attributes")
                                .executes(context -> attributes(context.getSource(), ""))
                                .then(Commands.argument("query", StringArgumentType.greedyString())
                                        .executes(context -> attributes(
                                                context.getSource(),
                                                StringArgumentType.getString(context, "query")
                                        ))))));
    }

    private static int draw(CommandSourceStack source, ResourceLocation poolId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            EditorNetwork.sendDrawResult(player, TalentEditorMod.DRAWS.open(player, poolId));
            return 1;
        } catch (Exception ex) {
            source.sendFailure(Component.translatable("message.talent_editor.player_only"));
            return 0;
        }
    }

    private static int synthesize(CommandSourceStack source, ResourceLocation targetId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            SynthesisResult result = TalentEditorMod.SYNTHESES.synthesize(player, targetId);
            Component message = synthesisMessage(result);
            if (!result.successful()) {
                source.sendFailure(message);
                return 0;
            }
            TalentAttributeRuntime.refresh(player);
            EditorNetwork.sendPlayerState(player, false);
            source.sendSuccess(() -> message, false);
            return 1;
        } catch (Exception ex) {
            source.sendFailure(Component.translatable("message.talent_editor.player_only"));
            return 0;
        }
    }

    public static Component synthesisMessage(SynthesisResult result) {
        String key = "command.talent_editor.synthesis."
                + result.status().name().toLowerCase(Locale.ROOT);
        return result.detail().isPresent()
                ? Component.translatable(key, result.target().toString(), result.detail().orElseThrow().toString())
                : Component.translatable(key, result.target().toString());
    }

    private static int addTokens(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            int amount
    ) {
        players.forEach(player -> {
            PlayerTalents.addDrawTokens(player, amount);
            EditorNetwork.sendPlayerState(player, false);
        });
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.tokens_added", amount, players.size()
        ), true);
        return players.size();
    }

    private static int setTokens(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            int amount
    ) {
        players.forEach(player -> {
            PlayerTalents.setDrawTokens(player, amount);
            EditorNetwork.sendPlayerState(player, false);
        });
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.tokens_set", amount, players.size()
        ), true);
        return players.size();
    }

    private static int getCounter(
            CommandSourceStack source,
            ServerPlayer player,
            ResourceLocation counter
    ) {
        long value = PlayerTalents.counter(player, counter);
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.counter_value",
                player.getGameProfile().getName(), counter.toString(), value
        ), false);
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static int setCounter(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            ResourceLocation counter,
            long value
    ) {
        players.forEach(player -> {
            PlayerTalents.setCounter(player, counter, value);
            EditorNetwork.sendPlayerState(player, false);
        });
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.counter_set", counter.toString(), value, players.size()
        ), true);
        return players.size();
    }

    private static int addCounter(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            ResourceLocation counter,
            long amount
    ) {
        players.forEach(player -> {
            PlayerTalents.addCounter(player, counter, amount);
            EditorNetwork.sendPlayerState(player, false);
        });
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.counter_added", amount, counter.toString(), players.size()
        ), true);
        return players.size();
    }

    private static int resetCounter(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            ResourceLocation counter
    ) {
        players.forEach(player -> {
            TalentEditorMod.COUNTERS.resetCounter(player, counter);
            EditorNetwork.sendPlayerState(player, false);
        });
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.counter_reset", counter.toString(), players.size()
        ), true);
        return players.size();
    }

    private static int book(CommandSourceStack source) {
        try {
            EditorNetwork.sendPlayerState(source.getPlayerOrException(), true);
            return 1;
        } catch (Exception ex) {
            source.sendFailure(Component.translatable("message.talent_editor.player_only"));
            return 0;
        }
    }

    private static int grant(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            ResourceLocation talentId
    ) {
        TalentDefinition talent = TalentEditorMod.SNAPSHOTS.active().talents().get(talentId);
        if (talent == null) {
            source.sendFailure(Component.translatable("command.talent_editor.unknown_talent", talentId.toString()));
            return 0;
        }
        if (!talent.enabled() || !talent.available()) {
            source.sendFailure(Component.translatable("command.talent_editor.unavailable_talent", talentId.toString()));
            return 0;
        }
        int changed = 0;
        int denied = 0;
        for (ServerPlayer player : players) {
            if (!ConditionEvaluator.test(talent.acquireCondition(), player)) {
                denied++;
                player.sendSystemMessage(Component.translatable(
                        "command.talent_editor.requirements_not_met", talent.name().fallbackText()
                ));
                continue;
            }
            if (!PlayerTalents.grant(player, talentId)) continue;
            changed++;
            TalentAttributeRuntime.refresh(player);
            EditorNetwork.sendPlayerState(player, false);
            TalentEditorMod.PRESENTATIONS.acquired(player, talent, false);
            player.sendSystemMessage(Component.translatable(
                    "command.talent_editor.granted_to_player", talent.name().fallbackText()
            ));
        }
        int finalChanged = changed;
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.grant_success", talentId.toString(), finalChanged
        ), true);
        if (denied > 0) {
            int finalDenied = denied;
            source.sendFailure(Component.translatable(
                    "command.talent_editor.grant_denied", talentId.toString(), finalDenied
            ));
        }
        return changed;
    }

    private static int revoke(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            ResourceLocation talentId
    ) {
        int changed = 0;
        for (ServerPlayer player : players) {
            if (!PlayerTalents.revoke(player, talentId)) continue;
            changed++;
            TalentAttributeRuntime.refresh(player);
            EditorNetwork.sendPlayerState(player, false);
            player.sendSystemMessage(Component.translatable(
                    "command.talent_editor.revoked_from_player", talentId.toString()
            ));
        }
        int finalChanged = changed;
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.revoke_success", talentId.toString(), finalChanged
        ), true);
        return changed;
    }

    private static int inspect(CommandSourceStack source, ServerPlayer player) {
        List<ResourceLocation> owned = PlayerTalents.owned(player).stream()
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.inspect_header", player.getGameProfile().getName(), owned.size()
        ), false);
        owned.forEach(id -> {
            TalentDefinition definition = TalentEditorMod.SNAPSHOTS.active().talents().get(id);
            String suffix = definition == null ? "" : " — " + definition.name().fallbackText();
            source.sendSuccess(() -> Component.literal("- " + id + suffix), false);
        });
        return owned.size();
    }

    private static int open(CommandSourceStack source) {
        try {
            EditorNetwork.sendBootstrap(source.getPlayerOrException(), true);
            return 1;
        } catch (Exception ex) {
            source.sendFailure(Component.translatable("message.talent_editor.player_only"));
            return 0;
        }
    }

    private static int status(CommandSourceStack source) {
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        var triggerDiagnostics = TalentEditorMod.TRIGGERS.diagnostics();
        int lootRuleCount = snapshot.configuredItemLootByTable().values().stream()
                .mapToInt(List::size).sum() + snapshot.configuredItemLootPatterns().size();
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Talent Editor revision=%d hash=%s rarities=%d pools=%d talents=%d counters=%d layouts=%d items=%d lootTables=%d lootPatterns=%d lootRules=%d activeLayout=%s activeAttributes=%d effectImmunities=%d events=%d triggerCalls=%d matched=%d actions=%d cooldownSkips=%d chanceMisses=%d limitSkips=%d reentrantSkips=%d trace=%s(%d/%d, overwritten=%d)",
                snapshot.revision(), shortHash(snapshot.contentHash()), snapshot.rarities().size(),
                snapshot.pools().size(), snapshot.talents().size(), snapshot.counters().size(),
                snapshot.layouts().size(), snapshot.configuredItems().size(),
                snapshot.configuredItemLootByTable().size(), snapshot.configuredItemLootPatterns().size(),
                lootRuleCount, snapshot.bookLayout().id(),
                snapshot.attributeBindings().size(), snapshot.effectImmunityBindings().size(),
                snapshot.triggersByEvent().size(), triggerDiagnostics.eventCalls(),
                triggerDiagnostics.matchedTriggers(), triggerDiagnostics.appliedActions(),
                triggerDiagnostics.cooldownSkips(), triggerDiagnostics.chanceMisses(),
                triggerDiagnostics.activationLimitSkips(), triggerDiagnostics.reentrantSkips(),
                TalentEditorMod.TRIGGERS.traceStatus().enabled() ? "on" : "off",
                TalentEditorMod.TRIGGERS.traceStatus().size(),
                TalentEditorMod.TRIGGERS.traceStatus().capacity(),
                TalentEditorMod.TRIGGERS.traceStatus().overwritten()
        )), false);
        return snapshot.talents().size();
    }

    private static int resources(CommandSourceStack source) {
        RuntimeSnapshot snapshot = TalentEditorMod.SNAPSHOTS.active();
        ClientResourceRequirementCatalog.Result catalog = ClientResourceRequirementCatalog.from(snapshot);
        List<ClientResourceStatusTracker.Report> reports = TalentEditorMod.CLIENT_RESOURCES.snapshot();
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT,
                "Client resources revision=%d hash=%s total=%d textures=%d sounds=%d truncated=%s clients=%d",
                snapshot.revision(), shortHash(snapshot.contentHash()), catalog.requirements().size(),
                catalog.textureCount(), catalog.soundCount(), catalog.truncated(), reports.size()
        )), false);
        if (reports.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No online client resource reports."), false);
            return 0;
        }
        for (ClientResourceStatusTracker.Report report : reports) {
            boolean stale = report.revision() != snapshot.revision()
                    || !report.contentHash().equals(snapshot.contentHash());
            source.sendSuccess(() -> Component.literal(String.format(
                    Locale.ROOT,
                    "- %s state=%s available=%d/%d missing=%d unexpected=%d hashed=%d%s",
                    report.playerName(), stale ? "stale" : report.state().name().toLowerCase(Locale.ROOT),
                    report.available(), report.required(), report.missing(), report.unexpected(), report.hashed(),
                    report.catalogTruncated() ? " catalog-truncated" : ""
            )), false);
            report.missingEntries().stream().limit(8).forEach(missing -> source.sendSuccess(() ->
                    Component.literal("    missing " + missing.kind().name().toLowerCase(Locale.ROOT)
                            + " " + missing.id()), false));
            if (report.missingEntries().size() > 8) {
                source.sendSuccess(() -> Component.literal(
                        "    ... and " + (report.missingEntries().size() - 8) + " retained missing entries"
                ), false);
            }
        }
        int hashMismatches = 0;
        for (var requirement : catalog.requirements()) {
            if (requirement.kind()
                    != com.kidxoxo.talenteditor.resource.ClientResourceRequirement.Kind.TEXTURE) continue;
            java.util.SortedSet<String> hashes = new java.util.TreeSet<>();
            reports.stream()
                    .filter(report -> report.revision() == snapshot.revision()
                            && report.contentHash().equals(snapshot.contentHash()))
                    .flatMap(report -> report.statuses().stream())
                    .filter(status -> status.requirement().equals(requirement)
                            && status.available() && !status.sha256().isEmpty())
                    .map(com.kidxoxo.talenteditor.resource.ClientResourceStatus::sha256)
                    .forEach(hashes::add);
            if (hashes.size() <= 1) continue;
            hashMismatches++;
            if (hashMismatches <= 20) {
                String variants = hashes.stream().map(TalentEditorCommands::shortHash)
                        .collect(java.util.stream.Collectors.joining(","));
                String line = "    texture hash mismatch " + requirement.id() + " variants=" + variants;
                source.sendSuccess(() -> Component.literal(line), false);
            }
        }
        int finalHashMismatches = hashMismatches;
        source.sendSuccess(() -> Component.literal(
                "Texture hash mismatches across current client reports=" + finalHashMismatches
                        + (finalHashMismatches > 20 ? " (showing first 20)" : "")
        ), false);
        return reports.size();
    }

    private static int configuredItems(CommandSourceStack source, String query) {
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        List<ConfiguredItemDefinition> matches = TalentEditorMod.SNAPSHOTS.active().configuredItems().values()
                .stream()
                .filter(item -> normalized.isBlank()
                        || item.id().toString().toLowerCase(Locale.ROOT).contains(normalized)
                        || item.name().fallbackText().toLowerCase(Locale.ROOT).contains(normalized))
                .sorted(Comparator.comparing(item -> item.id().toString()))
                .toList();
        matches.stream().limit(MAX_CATALOG_RESULTS).forEach(item -> source.sendSuccess(() ->
                Component.literal(String.format(
                        Locale.ROOT, "[%s] %s — %s model=%d glint=%s attributes=%d use=%s loot=%d",
                        item.enabled() && item.available() ? "active" : "inactive", item.id(),
                        item.name().fallbackText(), item.customModelData(), item.glint(),
                        item.attributeModifiers().size(), item.use().enabled(), item.loot().size()
                )), false));
        source.sendSuccess(() -> Component.literal(
                "Matched " + matches.size() + " configured item(s); showing at most " + MAX_CATALOG_RESULTS
        ), false);
        return matches.size();
    }

    private static int giveConfiguredItem(
            CommandSourceStack source,
            Collection<ServerPlayer> players,
            ResourceLocation itemId,
            int count
    ) {
        ConfiguredItemDefinition definition = TalentEditorMod.SNAPSHOTS.active().configuredItems().get(itemId);
        if (definition == null) {
            source.sendFailure(Component.translatable("command.talent_editor.unknown_configured_item", itemId));
            return 0;
        }
        if (!definition.enabled() || !definition.available()) {
            source.sendFailure(Component.translatable("command.talent_editor.unavailable_configured_item", itemId));
            return 0;
        }
        for (ServerPlayer player : players) {
            int remaining = count;
            while (remaining > 0) {
                int stackCount = Math.min(64, remaining);
                ItemStack stack = ConfiguredItem.create(definition, stackCount);
                int created = stack.getCount();
                player.getInventory().add(stack);
                if (!stack.isEmpty()) player.drop(stack, false);
                remaining -= created;
            }
        }
        source.sendSuccess(() -> Component.translatable(
                "command.talent_editor.configured_item_given", count, itemId.toString(), players.size()
        ), true);
        return players.size();
    }

    private static int traceStatus(CommandSourceStack source) {
        var status = TalentEditorMod.TRIGGERS.traceStatus();
        source.sendSuccess(() -> Component.literal(String.format(
                Locale.ROOT, "Trigger trace %s; retained=%d/%d overwritten=%d",
                status.enabled() ? "enabled" : "disabled", status.size(), status.capacity(),
                status.overwritten()
        )), false);
        return status.size();
    }

    private static int setTrace(CommandSourceStack source, boolean enabled) {
        TalentEditorMod.TRIGGERS.setTraceEnabled(enabled);
        source.sendSuccess(() -> Component.literal("Trigger trace " + (enabled ? "enabled" : "disabled") + "."),
                true);
        return 1;
    }

    private static int clearTrace(CommandSourceStack source) {
        TalentEditorMod.TRIGGERS.clearTrace();
        source.sendSuccess(() -> Component.literal("Trigger trace cleared."), true);
        return 1;
    }

    private static int showTrace(CommandSourceStack source, int maximum) {
        List<TriggerTraceEntry> entries = TalentEditorMod.TRIGGERS.recentTrace(maximum);
        if (entries.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Trigger trace contains no entries."), false);
            return 0;
        }
        for (TriggerTraceEntry entry : entries) {
            String steps = entry.actionResults().stream().limit(8)
                    .map(result -> (result.branch() == TriggerTraceEntry.Branch.PRIMARY ? "p" : "f")
                            + result.index() + ":" + result.type().getPath()
                            + "=" + (result.applied() ? "ok" : "fail"))
                    .collect(java.util.stream.Collectors.joining(","));
            int omitted = Math.max(0, entry.attemptedActions() - Math.min(8, entry.actionResults().size()));
            if (omitted > 0) steps += (steps.isEmpty() ? "" : ",") + "...+" + omitted;
            String displayedSteps = steps.isEmpty() ? "-" : steps;
            source.sendSuccess(() -> Component.literal(String.format(
                    Locale.ROOT,
                    "#%d tick=%d player=%s event=%s talent=%s#%s result=%s chance=%.6f actions=%d/%d steps=%s",
                    entry.sequence(), entry.playerTick(), entry.playerName(), entry.event(), entry.talent(),
                    entry.trigger(), entry.outcome().name().toLowerCase(Locale.ROOT),
                    entry.effectiveChance(), entry.appliedActions(), entry.attemptedActions(), displayedSteps
            )), false);
        }
        return entries.size();
    }

    private static int validate(CommandSourceStack source) {
        List<ValidationIssue> issues = TalentEditorMod.SNAPSHOTS.lastReport().issues();
        if (issues.isEmpty()) {
            source.sendSuccess(() -> Component.literal("Talent definitions are valid."), false);
            return 0;
        }
        issues.stream().limit(MAX_ISSUES).forEach(issue ->
                source.sendSuccess(() -> Component.literal(
                        issue.severity() + " " + issue.code() + " " + issue.source() + " " +
                                issue.path() + ": " + issue.message()
                ), false)
        );
        if (issues.size() > MAX_ISSUES) {
            source.sendSuccess(() -> Component.literal(
                    "... " + (issues.size() - MAX_ISSUES) + " more issue(s)"
            ), false);
        }
        return (int) TalentEditorMod.SNAPSHOTS.lastReport().errorCount();
    }

    private static int attributes(CommandSourceStack source, String query) {
        LivingEntity target = source.getEntity() instanceof LivingEntity living ? living : null;
        String normalized = query.toLowerCase(Locale.ROOT);
        List<AttributeDescriptor> matches = AttributeCatalog.capture(target).stream()
                .filter(attribute -> normalized.isBlank()
                        || attribute.id().toString().toLowerCase(Locale.ROOT).contains(normalized)
                        || attribute.descriptionId().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
        matches.stream().limit(MAX_CATALOG_RESULTS).forEach(attribute ->
                source.sendSuccess(() -> Component.literal(formatAttribute(attribute)), false)
        );
        source.sendSuccess(() -> Component.literal(
                "Matched " + matches.size() + " attribute(s); showing at most " + MAX_CATALOG_RESULTS
        ), false);
        return matches.size();
    }

    private static String formatAttribute(AttributeDescriptor descriptor) {
        String bounds = descriptor.minValue() == null
                ? ""
                : " range=" + descriptor.minValue() + ".." + descriptor.maxValue();
        return descriptor.id() + " default=" + descriptor.defaultValue() + bounds
                + " sync=" + descriptor.clientSyncable()
                + " attached=" + descriptor.attachedToTarget()
                + " description=" + descriptor.descriptionId();
    }

    private static String shortHash(String hash) {
        return hash.length() <= 12 ? hash : hash.substring(0, 12);
    }
}
