package cn.lanink.moneyranking.form;

import cn.lanink.moneyranking.MoneyProvider;
import cn.lanink.moneyranking.MoneyRanking;
import cn.lanink.moneyranking.form.windows.AdvancedFormWindowCustom;
import cn.lanink.moneyranking.form.windows.AdvancedFormWindowSimple;
import cn.lanink.rankingapi.Ranking;
import cn.nukkit.Player;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;
import cn.nukkit.event.player.PlayerQuitEvent;
import cn.nukkit.form.response.FormResponseCustom;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.utils.Config;

import java.util.HashMap;
import java.util.Map;

import static cn.lanink.moneyranking.form.FormCreate.FORM_CACHE;

/**
 * @author lt_name
 */
public class FormListener implements Listener {

    private final MoneyRanking moneyRanking;

    public FormListener(MoneyRanking moneyRanking) {
        this.moneyRanking = moneyRanking;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        FORM_CACHE.remove(event.getPlayer());
    }

    @EventHandler
    public void onPlayerFormResponded(PlayerFormRespondedEvent event) {
        if (AdvancedFormWindowSimple.onEvent(event.getWindow(), event.getPlayer())) {
            return;
        }
        if (AdvancedFormWindowCustom.onEvent(event.getWindow(), event.getPlayer())) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null || event.getWindow() == null) {
            return;
        }
        FormCreate.FormType cache = FORM_CACHE.containsKey(player) ? FORM_CACHE.get(player).get(event.getFormID()) : null;
        if (cache == null) {
            return;
        }
        FORM_CACHE.get(player).remove(event.getFormID());
        if (event.getResponse() == null) {
            return;
        }
        if (event.getWindow() instanceof FormWindowCustom) {
            FormResponseCustom response = ((FormWindowCustom) event.getWindow()).getResponse();

            if (cache == FormCreate.FormType.ADD_RANKING_MENU) {
                String name = response.getInputResponse(0);
                String eapi = response.getDropdownResponse(1).getElementContent();

                Config config = this.moneyRanking.getConfig();
                HashMap<String, Map<String, Object>> rankings = config.get("rankings", new HashMap<>());

                HashMap<String, Object> data = new HashMap<>();
                data.put("x", player.getX());
                data.put("y", player.getY());
                data.put("z", player.getZ());
                data.put("level", player.getLevel().getFolderName());
                if (MoneyProvider.EconomyAPIType.isExist(eapi)) {
                    data.put("moneyProvider", eapi);
                } else {
                    player.sendMessage(this.moneyRanking.getLanguage().translateString("use_default_economic_core", eapi));
                }
                this.moneyRanking.loadRanking(name, data);
                rankings.put(name, data);
                config.set("rankings", rankings);
                config.save();
                player.sendMessage(this.moneyRanking.getLanguage().translateString("ranking_added_successfully"));
            }else if (cache == FormCreate.FormType.REMOVE_RANKING_MENU) {
                String name = response.getDropdownResponse(0).getElementContent();
                Config config = this.moneyRanking.getConfig();
                HashMap<String, Map<String, Object>> rankings = config.get("rankings", new HashMap<>());
                if (rankings.containsKey(name)) {
                    rankings.remove(name);
                    config.set("rankings", rankings);
                    config.save();
                }
                Ranking oldRanking = this.moneyRanking.getRankings().remove(name);
                if (oldRanking != null) {
                    oldRanking.close();
                }
                player.sendMessage(this.moneyRanking.getLanguage().translateString("ranking_deleted_successfully"));
            }
        }

    }

}
