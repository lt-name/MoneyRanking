package cn.lanink.moneyranking;

import cn.lanink.rankingapi.Ranking;
import cn.lanink.rankingapi.RankingAPI;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.level.Position;
import cn.nukkit.plugin.PluginBase;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * @author lt_name
 */
public class MoneyRanking extends PluginBase {

    public static final String VERSION = "?";

    @Getter
    private MoneyProvider defaultMoneyProvider;

    @Getter
    private final HashMap<MoneyProvider.EconomyAPIType, MoneyProvider> moneyProviders = new HashMap<>();

    @Getter
    private final HashMap<String, Ranking> rankings = new HashMap<>();

    @Override
    public void onLoad() {
        this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        this.defaultMoneyProvider = new MoneyProvider();

        HashMap<MoneyProvider.EconomyAPIType, MoneyProvider> moneyProviders = this.getMoneyProviders();
        moneyProviders.put(MoneyProvider.EconomyAPIType.ECONOMY_API, new MoneyProvider(MoneyProvider.EconomyAPIType.ECONOMY_API));
        moneyProviders.put(MoneyProvider.EconomyAPIType.MONEY, new MoneyProvider(MoneyProvider.EconomyAPIType.MONEY));
        moneyProviders.put(MoneyProvider.EconomyAPIType.PLAYER_POINT, new MoneyProvider(MoneyProvider.EconomyAPIType.PLAYER_POINT));
        moneyProviders.put(MoneyProvider.EconomyAPIType.LLAMA_ECONOMY, new MoneyProvider(MoneyProvider.EconomyAPIType.LLAMA_ECONOMY));

        this.getServer().getScheduler().scheduleTask(this, this::loadAllRanking);
    }

    private void loadAllRanking() {
        HashMap<String, Map<String, Object>> map = this.getConfig().get("rankings", new HashMap<>());
        for (Map.Entry<String, Map<String, Object>> entry : map.entrySet()) {
            this.loadRanking(entry.getKey(), entry.getValue());
        }
    }

    private void loadRanking(String name, Map<String, Object> data) {
        String levelName = (String) data.get("level");
        if (!this.getServer().loadLevel(levelName)) {
            this.getLogger().error("世界：" + levelName + " 加载失败！无法加载此世界的排行榜！");
            return;
        }
        Position position = new Position(
                (double) data.get("x"),
                (double) data.get("y"),
                (double) data.get("z"),
                this.getServer().getLevelByName(levelName));
        Ranking ranking = RankingAPI.createRanking(this, name, position);
        String moneyProvider = (String) data.getOrDefault("moneyProvider", this.getDefaultMoneyProvider().getEconomyAPIType().toString());
        ranking.setRankingList(() -> this.getMoneyProviders().get(MoneyProvider.EconomyAPIType.valueOf(moneyProvider)).getAllPlayerMoney());
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
            if (args.length > 0) {
                switch (args[0]) {
                    case "add":
                        if (args.length > 1) {
                            if (!sender.isPlayer()) {
                                sender.sendMessage("§c请在游戏内使用此命令！");
                                return true;
                            }
                            Player player = (Player) sender;

                            String name = args[1];
                            HashMap<String, Map<String, Object>> rankings = this.getConfig().get("rankings", new HashMap<>());

                            HashMap<String, Object> data = new HashMap<>();
                            data.put("x", player.getX());
                            data.put("y", player.getY());
                            data.put("z", player.getZ());
                            data.put("level", player.getLevel().getFolderName());
                            if (args.length > 2) {
                                if (MoneyProvider.EconomyAPIType.isExist(args[2])) {
                                    data.put("moneyProvider", args[2]);
                                }else {
                                    sender.sendMessage("§c不存在 " + args[2] + " 经济核心！");
                                }
                            }

                            this.loadRanking(name, data);

                            rankings.put(name, data);
                            this.getConfig().set("rankings", rankings);
                            this.getConfig().save();

                            sender.sendMessage("§a排行榜添加成功！");
                        }else {
                            sender.sendMessage("§c请输入排行榜名称！");
                        }
                        break;
                    case "remove":
                        if (args.length > 1) {
                            HashMap<String, Map<String, Object>> rankings = this.getConfig().get("rankings", new HashMap<>());
                            if (rankings.containsKey(args[1])) {
                                rankings.remove(args[1]);
                                this.getConfig().set("rankings", rankings);
                                this.getConfig().save();
                            }
                            Ranking ranking = this.getRankings().remove(args[1]);
                            if (ranking != null) {
                                ranking.close();
                            }
                            sender.sendMessage("§a排行榜删除成功！");
                        }else {
                            sender.sendMessage("§c请输入排行榜名称！");
                        }
                        break;
                    default:
                        sender.sendMessage("§a/moneyranking add <排行榜名称> <经济核心> §e在当前位置添加一个经济排行榜\n" +
                                "§a/moneyranking remove <排行榜名称> §e删除一个排行榜");
                        break;
                }
            }
            return true;
        }
        return false;
    }

}
