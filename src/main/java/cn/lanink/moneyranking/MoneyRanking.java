package cn.lanink.moneyranking;

import cn.lanink.moneyranking.form.FormCreate;
import cn.lanink.moneyranking.form.FormListener;
import cn.lanink.moneyranking.utils.Language;
import cn.lanink.rankingapi.Ranking;
import cn.lanink.rankingapi.RankingAPI;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author lt_name
 */
public class MoneyRanking extends PluginBase {

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

    public static MoneyRanking getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        this.saveDefaultConfig();

        this.saveResource("Language/zh_CN.properties");
        this.saveResource("Language/en_US.properties");
        this.language = new Language(new File(this.getDataFolder() + "/Language/" +
                                this.getConfig().getString("language", "zh_CN") + ".properties"));
    }

    @Override
    public void onEnable() {
        this.defaultMoneyProvider = new MoneyProvider();

        HashMap<MoneyProvider.EconomyAPIType, MoneyProvider> moneyProviders = this.getMoneyProviders();
        moneyProviders.put(MoneyProvider.EconomyAPIType.ECONOMY_API, new MoneyProvider(MoneyProvider.EconomyAPIType.ECONOMY_API));
        moneyProviders.put(MoneyProvider.EconomyAPIType.MONEY, new MoneyProvider(MoneyProvider.EconomyAPIType.MONEY));
        moneyProviders.put(MoneyProvider.EconomyAPIType.PLAYER_POINT, new MoneyProvider(MoneyProvider.EconomyAPIType.PLAYER_POINT));
        moneyProviders.put(MoneyProvider.EconomyAPIType.LLAMA_ECONOMY, new MoneyProvider(MoneyProvider.EconomyAPIType.LLAMA_ECONOMY));

        this.getServer().getPluginManager().registerEvents(new FormListener(this), this);

        //等所有经济前置加载完成后加载排行榜
        this.getServer().getScheduler().scheduleTask(this, this::loadAllRanking);

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

    public void loadRanking(String name, Map<String, Object> data) {
        String levelName = (String) data.get("level");
        if (!this.getServer().loadLevel(levelName)) {
            this.getLogger().error(this.getLanguage().translateString("ranking_loadFailed_world", levelName));
            return;
        }
        Position position = new Position(
                (double) data.get("x"),
                (double) data.get("y"),
                (double) data.get("z"),
                this.getServer().getLevelByName(levelName));
        Ranking ranking = RankingAPI.createRanking(this, name, position);
        MoneyProvider.EconomyAPIType moneyProvider = MoneyProvider.EconomyAPIType.fromName(
                (String) data.getOrDefault("moneyProvider", this.getDefaultMoneyProvider().getEconomyAPIType().toString()));
        ranking.setRankingList(() -> this.getMoneyProviders().get(moneyProvider).getAllPlayerMoney());
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

}
