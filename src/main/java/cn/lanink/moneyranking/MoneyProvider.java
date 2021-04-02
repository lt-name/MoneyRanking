package cn.lanink.moneyranking;

import cn.nukkit.Player;
import cn.nukkit.Server;
import lombok.Getter;
import me.onebone.economyapi.EconomyAPI;
import money.Money;
import net.lldv.llamaeconomy.LlamaEconomy;
import net.player.api.Point;

import java.util.*;


/**
 * 兼容多个经济核心
 *
 * @author SmallasWater
 */
public class MoneyProvider {

    private EconomyAPIType money;

    public MoneyProvider() {
        if (Server.getInstance().getPluginManager().getPlugin("EconomyAPI") != null) {
            this.money = EconomyAPIType.ECONOMY_API;
        } else if (Server.getInstance().getPluginManager().getPlugin("Money") != null) {
            this.money = EconomyAPIType.MONEY;
        } else if (Server.getInstance().getPluginManager().getPlugin("LlamaEconomy") != null) {
            this.money = EconomyAPIType.LLAMA_ECONOMY;
        } else if (Server.getInstance().getPluginManager().getPlugin("playerPoints") != null) {
            this.money = EconomyAPIType.PLAYER_POINT;
        } else {
            this.money = EconomyAPIType.NULL;
        }
    }

    public enum EconomyAPIType {

        ECONOMY_API("EconomyAPI"),
        MONEY("Money"),
        PLAYER_POINT("playerPoints"),
        LLAMA_ECONOMY("LlamaEconomy"),
        NULL("Null");

        @Getter
        private final String name;

        EconomyAPIType(String name) {
            this.name = name;
        }

    }

    public EconomyAPIType getEconomyAPIType() {
        return money;
    }

    public MoneyProvider setEconomyAPIType(EconomyAPIType money) {
        this.money = money;
        return this;
    }

    public String getMonetaryUnit() {
        if (this.money == EconomyAPIType.ECONOMY_API) {
            return EconomyAPI.getInstance().getMonetaryUnit();
        } else if (this.money == EconomyAPIType.LLAMA_ECONOMY) {
            return LlamaEconomy.getAPI().getMonetaryUnit();
        }
        return "$";
    }

    public Map<String, Double> getAllPlayerMoney() {
        switch (this.money) {
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
                return new HashMap<>();
        }
    }

    public Map<String, Double> uuidToName(Map<String, Double> uuidMap) {
        HashMap<String, Double> map = new HashMap<>();
        for (Map.Entry<String, Double> entry : uuidMap.entrySet()) {
            UUID uuid = UUID.fromString(entry.getKey());
            String name = Server.getInstance().getOfflinePlayer(uuid).getName();
            if (name == null || name.trim().equals("")) {
                continue;
            }
            map.put(name, entry.getValue());
        }
        return map;
    }

    public double myMoney(Player player) {
        return myMoney(player.getName());
    }

    private double myMoney(String player) {
        switch (this.money) {
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
        switch (this.money) {
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
        switch (this.money) {
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
