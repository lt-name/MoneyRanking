package cn.lanink.moneyranking;

import cn.lanink.rankingapi.RankingAPI;
import cn.nukkit.Player;
import cn.nukkit.Server;
import lombok.Getter;
import me.onebone.economyapi.EconomyAPI;
import money.Money;
import net.lldv.llamaeconomy.LlamaEconomy;
import net.player.api.Point;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * 兼容多个经济核心
 *
 * @author SmallasWater
 */
public class MoneyProvider {

    private EconomyAPIType economyAPIType;

    public MoneyProvider() {
        if (Server.getInstance().getPluginManager().getPlugin("EconomyAPI") != null) {
            this.economyAPIType = EconomyAPIType.ECONOMY_API;
        } else if (Server.getInstance().getPluginManager().getPlugin("Money") != null) {
            this.economyAPIType = EconomyAPIType.MONEY;
        } else if (Server.getInstance().getPluginManager().getPlugin("LlamaEconomy") != null) {
            this.economyAPIType = EconomyAPIType.LLAMA_ECONOMY;
        } else if (Server.getInstance().getPluginManager().getPlugin("playerPoints") != null) {
            this.economyAPIType = EconomyAPIType.PLAYER_POINT;
        } else {
            this.economyAPIType = EconomyAPIType.NULL;
        }
    }

    public MoneyProvider(EconomyAPIType economyAPIType) {
        this.economyAPIType = economyAPIType;
        switch (this.economyAPIType) {
            case ECONOMY_API:
                if (Server.getInstance().getPluginManager().getPlugin("EconomyAPI") != null) {
                    return;
                }
                break;
            case MONEY:
                if (Server.getInstance().getPluginManager().getPlugin("Money") != null) {
                    return;
                }
                break;
            case PLAYER_POINT:
                if (Server.getInstance().getPluginManager().getPlugin("playerPoints") != null) {
                    return;
                }
                break;
            case LLAMA_ECONOMY:
                if (Server.getInstance().getPluginManager().getPlugin("LlamaEconomy") != null) {
                    return;
                }
                break;
        }
        this.economyAPIType = EconomyAPIType.NULL;
    }

    public enum EconomyAPIType {

        ECONOMY_API("EconomyAPI"),
        MONEY("Money"),
        PLAYER_POINT("PlayerPoints"),
        LLAMA_ECONOMY("LlamaEconomy"),
        NULL("Null");

        @Getter
        private final String name;

        EconomyAPIType(String name) {
            this.name = name;
        }

        public static boolean isExist(@NotNull String name) {
            for (EconomyAPIType economyAPIType : EconomyAPIType.values()) {
                if (economyAPIType.getName().equalsIgnoreCase(name)) {
                    return true;
                }
            }
            return false;
        }

        public static EconomyAPIType fromName(@NotNull String name) {
            for (EconomyAPIType economyAPIType : EconomyAPIType.values()) {
                if (economyAPIType.getName().equalsIgnoreCase(name)) {
                    return economyAPIType;
                }
            }
            return NULL;
        }

    }

    public EconomyAPIType getEconomyAPIType() {
        return economyAPIType;
    }

    public MoneyProvider setEconomyAPIType(EconomyAPIType money) {
        this.economyAPIType = money;
        return this;
    }

    public String getMonetaryUnit() {
        if (this.economyAPIType == EconomyAPIType.ECONOMY_API) {
            return EconomyAPI.getInstance().getMonetaryUnit();
        } else if (this.economyAPIType == EconomyAPIType.LLAMA_ECONOMY) {
            return LlamaEconomy.getAPI().getMonetaryUnit();
        }
        return "$";
    }

    public Map<String, Double> getAllPlayerMoney() {
        //debug
        if (RankingAPI.debug) {
            HashMap<String, Double> debugMap = new HashMap<>();
            debugMap.put("ltname", Double.MAX_VALUE);
            Random random = new Random();
            for (int i=0; i < 100000; i++) {
                debugMap.put("test" + random.nextInt(10000000), random.nextInt(1000000) * 1D);
            }
            return debugMap;
        }

        switch (this.economyAPIType) {
            case MONEY:
                HashMap<String, Double> map = new HashMap<>();
                for (String name : Money.getInstance().getPlayers()) {
                    map.put(name, (double) Money.getInstance().getMoney(name));
                }
                return map;
            case ECONOMY_API:
                return this.uuidToName(EconomyAPI.getInstance().getAllMoney());
            case PLAYER_POINT:
                LinkedHashMap<String, Double> pointMap = new LinkedHashMap<>();
                for (Map.Entry<String, Number> entry : Point.getPlayerRankingList().entrySet()) {
                    pointMap.put(entry.getKey(), Double.valueOf(entry.getValue().toString()));
                }
                return this.uuidToName(pointMap);
            case LLAMA_ECONOMY:
                return LlamaEconomy.getAPI().getAll();
            default:
                HashMap<String, Double> emptyMap = new HashMap<>();
                emptyMap.put(MoneyRanking.getInstance().getLanguage().translateString("ranking_empty_data"), -1D);
                return emptyMap;
        }
    }

    public Map<String, Double> uuidToName(Map<String, Double> uuidMap) {
        HashMap<String, Double> map = new HashMap<>();
        HashMap<String, Long> lastTimes = new HashMap<>();
        for (Map.Entry<String, Double> entry : uuidMap.entrySet()) {
            UUID uuid = UUID.fromString(entry.getKey());
            String name = Server.getInstance().getOfflinePlayer(uuid).getName();
            if (name == null || name.trim().equals("")) {
                continue;
            }
            long lastTime = MoneyRanking.getInstance().getPlayerLog().getLong(entry.getKey() + ".lastLoginTime", 0);
            //名称重复以最后登录时间为准
            if (map.containsKey(name)) {
                if (lastTime < lastTimes.getOrDefault(name, 0L)) {
                    continue;
                }
            }
            map.put(name, entry.getValue());
            lastTimes.put(name, lastTime);
        }
        return map;
    }

    public double myMoney(Player player) {
        return myMoney(player.getName());
    }

    private double myMoney(String player) {
        switch (this.economyAPIType) {
            case MONEY:
                if (Money.getInstance().getPlayers().contains(player)) {
                    return Money.getInstance().getMoney(player);
                }
                break;
            case ECONOMY_API:
                return EconomyAPI.getInstance().myMoney(player);
            case PLAYER_POINT:
                return Point.myPoint(player);
            case LLAMA_ECONOMY:
                return LlamaEconomy.getAPI().getMoney(player);
            default:
                break;
        }
        return 0;
    }

    public void addMoney(Player player, double money) {
        this.addMoney(player.getName(), money);
    }

    private void addMoney(String player, double money) {
        switch (this.economyAPIType) {
            case MONEY:
                if (Money.getInstance().getPlayers().contains(player)) {
                    Money.getInstance().addMoney(player, (float) money);
                    return;
                }
                break;
            case ECONOMY_API:
                EconomyAPI.getInstance().addMoney(player, money, true);
                break;
            case PLAYER_POINT:
                Point.addPoint(player, money);
                break;
            case LLAMA_ECONOMY:
                LlamaEconomy.getAPI().addMoney(player, money);
                break;
            default:
                break;
        }
    }

    public void reduceMoney(Player player, double money) {
        this.reduceMoney(player.getName(), money);
    }

    private void reduceMoney(String player, double money) {
        switch (this.economyAPIType) {
            case MONEY:
                if (Money.getInstance().getPlayers().contains(player)) {
                    Money.getInstance().reduceMoney(player, (float) money);
                    return;
                }
                break;
            case ECONOMY_API:
                EconomyAPI.getInstance().reduceMoney(player, money, true);
                break;
            case PLAYER_POINT:
                Point.reducePoint(player, money);
                break;
            case LLAMA_ECONOMY:
                LlamaEconomy.getAPI().reduceMoney(player, money);
                break;
            default:
                break;
        }
    }

}
