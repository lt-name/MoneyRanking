package cn.lanink.moneyranking;

import cn.lanink.moneyranking.form.FormCreate;
import cn.lanink.moneyranking.form.windows.AdvancedFormWindowCustom;
import cn.lanink.moneyranking.form.windows.AdvancedFormWindowSimple;
import cn.lanink.moneyranking.utils.Language;
import cn.lanink.moneyranking.utils.MetricsLite;
import cn.lanink.rankingapi.Ranking;
import cn.lanink.rankingapi.RankingAPI;
import cn.lanink.rankingapi.RankingFormat;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerLocallyInitializedEvent;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author lt_name
 */
public class MoneyRanking extends PluginBase implements Listener {

    public static final String VERSION = "?";

    private static MoneyRanking instance;

    @Getter
    private MoneyProvider defaultMoneyProvider;

    @Getter
    private final HashMap<MoneyProvider.EconomyAPIType, MoneyProvider> moneyProviders = new HashMap<>();

    @Getter
    private final HashMap<String, Ranking> rankings = new HashMap<>();

    @Getter
    private Language language;

    @Getter
    private Config playerLog;

    @Getter
    private final ConcurrentHashMap<String, String> nameCache = new ConcurrentHashMap<>();

    public static MoneyRanking getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        this.saveDefaultConfig();

        this.loadLanguage();
    }

    private void loadLanguage() {
        List<String> languageList = Arrays.asList("zh_CN", "en_US");
        for (String s : languageList) {
            this.saveResource("Language/" + s + ".properties");
        }
        String l = this.getConfig().getString("language", "zh_CN");
        this.language = new Language(new File(this.getDataFolder() + "/Language/" + l + ".properties"));
        if (languageList.contains(l)) {
            Config config = new Config(Config.PROPERTIES);
            config.load(this.getResource("Language/" + l + ".properties"));
            this.language.update(config);
        }
    }

    @Override
    public void onEnable() {
        this.defaultMoneyProvider = new MoneyProvider();

        this.playerLog = new Config(this.getDataFolder() + "/PlayerLog.yml", Config.YAML);

        HashMap<MoneyProvider.EconomyAPIType, MoneyProvider> moneyProviders = this.getMoneyProviders();
        moneyProviders.put(MoneyProvider.EconomyAPIType.ECONOMY_API, new MoneyProvider(MoneyProvider.EconomyAPIType.ECONOMY_API));
        moneyProviders.put(MoneyProvider.EconomyAPIType.MONEY, new MoneyProvider(MoneyProvider.EconomyAPIType.MONEY));
        moneyProviders.put(MoneyProvider.EconomyAPIType.PLAYER_POINT, new MoneyProvider(MoneyProvider.EconomyAPIType.PLAYER_POINT));
        moneyProviders.put(MoneyProvider.EconomyAPIType.LLAMA_ECONOMY, new MoneyProvider(MoneyProvider.EconomyAPIType.LLAMA_ECONOMY));

        this.getServer().getPluginManager().registerEvents(this, this);

        //等所有经济前置加载完成后加载排行榜
        this.getServer().getScheduler().scheduleTask(this, this::loadAllRanking);

        try {
            new MetricsLite(this, 12058);
        } catch (Exception ignored) {

        }

        this.getLogger().info("§eMoneyRanking §aEnabled！ Version:" + VERSION);
        this.getServer().getScheduler().scheduleTask(this, () ->
                this.getLogger().warning("§e MoneyRanking §ais a §efree §aplugin，github:§e https://github.com/lt-name/MoneyRanking"));
    }

    private void loadAllRanking() {
        HashMap<String, Map<String, Object>> map = this.getConfig().get("rankings", new HashMap<>());
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            this.loadRanking(entry.getKey(), entry.getValue());
        }
    }

    public void loadRanking(@NotNull String name, @NotNull Map<String, Object> data) {
        String levelName = (String) data.get("level");
        if (!this.getServer().loadLevel(levelName)) {
            this.getLogger().error(this.getLanguage().translateString("ranking_loadFailed_world", levelName));
            return;
        }
        Position position = new Position(
                (double) data.get("x"),
                (double) data.get("y"),
                (double) data.get("z"),
                this.getServer().getLevelByName(levelName)
        );

        Ranking ranking = RankingAPI.createRanking(this, name, position);

        MoneyProvider.EconomyAPIType moneyProvider = MoneyProvider.EconomyAPIType.fromName(
                (String) data.getOrDefault("moneyProvider", this.getDefaultMoneyProvider().getEconomyAPIType().toString()));
        ranking.setRankingList(() -> this.getMoneyProviders().get(moneyProvider).getAllPlayerMoney());

        if (data.containsKey("rankingFormat")) {
            try {
                RankingFormat rankingFormat = RankingFormat.getDefaultFormat();
                HashMap<String, String> formatDataMap = (HashMap<String, String>) data.getOrDefault("rankingFormat", new HashMap<>());
                rankingFormat.setTop(formatDataMap.get("top"));
                rankingFormat.setLine(formatDataMap.get("line"));
                rankingFormat.setLineSelf(formatDataMap.get("lineSelf"));
                rankingFormat.setBottom(formatDataMap.get("bottom"));
                ranking.setRankingFormat(rankingFormat);
            } catch (Exception e) {
                this.getLogger().error(this.getLanguage().translateString("ranking_format_error", name), e);
            }
        }

        Ranking oldRanking = this.getRankings().put(name, ranking);
        if (oldRanking != null) {
            oldRanking.close();
        }
    }

    @Override
    public void onDisable() {
        for (Ranking ranking : this.getRankings().values()) {
            ranking.close();
        }
        this.getRankings().clear();
        this.nameCache.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("moneyranking".equalsIgnoreCase(command.getName())) {
            if (sender.isPlayer()) {
                FormCreate.sendMainMenu((Player) sender);
            } else {
                sender.sendMessage(this.getLanguage().translateString("used_command_in_game"));
            }
            return true;
        }
        return false;
    }

    public String getNameByUUID(String uuid) {
        if (!this.nameCache.containsKey(uuid)) {
            String name = Server.getInstance().getOfflinePlayer(UUID.fromString(uuid)).getName();
            if (name == null || name.trim().equals("")) {
                return null;
            }
            this.nameCache.put(uuid, name);
        }
        return this.nameCache.get(uuid);
    }

    @EventHandler
    public void onPlayerFormResponded(PlayerFormRespondedEvent event) {
        if (AdvancedFormWindowSimple.onEvent(event.getWindow(), event.getPlayer())) {
            return;
        }
        AdvancedFormWindowCustom.onEvent(event.getWindow(), event.getPlayer());
    }

    @EventHandler
    public void onPlayerLocallyInitialized(PlayerLocallyInitializedEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();
        this.nameCache.put(uuid, player.getName());
        this.playerLog.set(uuid + ".lastLoginTime", System.currentTimeMillis());
        this.playerLog.save();
    }

}
