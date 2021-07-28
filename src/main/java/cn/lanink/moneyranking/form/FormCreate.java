package cn.lanink.moneyranking.form;

import cn.lanink.moneyranking.MoneyProvider;
import cn.lanink.moneyranking.MoneyRanking;
import cn.lanink.moneyranking.form.element.ResponseElementButton;
import cn.lanink.moneyranking.form.windows.AdvancedFormWindowCustom;
import cn.lanink.moneyranking.form.windows.AdvancedFormWindowSimple;
import cn.lanink.moneyranking.utils.Language;
import cn.lanink.rankingapi.Ranking;
import cn.nukkit.Player;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.utils.Config;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author lt_name
 */
public class FormCreate {

    public static final String PLUGIN_NAME = "MoneyRanking";

    public static final HashMap<Player, HashMap<Integer, FormType>> FORM_CACHE = new HashMap<>();

    private FormCreate() {

    }

    public enum FormType {

        MAIN_MENU,
        ADD_RANKING_MENU,
        REMOVE_RANKING_MENU

    }

    private static void showFormWindow(@NotNull Player player, @NotNull FormWindow window, @NotNull FormType formType) {
        FORM_CACHE.computeIfAbsent(player, i -> new HashMap<>()).put(player.showFormWindow(window), formType);
    }

    public static void sendMainMenu(@NotNull Player player) {
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(PLUGIN_NAME);
        Language language = MoneyRanking.getInstance().getLanguage();
        simple.addButton(new ResponseElementButton(language.translateString("add_new_ranking"))
                .onClicked(FormCreate::sendAddRankingMenu));
        simple.addButton(new ResponseElementButton(language.translateString("set_ranking_format"))
                .onClicked(FormCreate::sendSetFormatMenu));
        simple.addButton(new ResponseElementButton(language.translateString("delete_existing_ranking"))
                .onClicked(FormCreate::sendRemoveRankingMenu));
        player.showFormWindow(simple);
    }

    public static void sendAddRankingMenu(@NotNull Player player) {
        FormWindowCustom custom = new FormWindowCustom(PLUGIN_NAME);
        custom.addElement(new ElementInput(MoneyRanking.getInstance().getLanguage().translateString("ranking_name")));
        ArrayList<String> list = new ArrayList<>();
        for (MoneyProvider.EconomyAPIType economyAPIType : MoneyProvider.EconomyAPIType.values()) {
            if (economyAPIType == MoneyProvider.EconomyAPIType.NULL) {
                continue;
            }
            list.add(economyAPIType.getName());
        }
        custom.addElement(new ElementDropdown(MoneyRanking.getInstance().getLanguage().translateString("economic_API"), list));
        showFormWindow(player, custom, FormType.ADD_RANKING_MENU);
    }

    public static void sendRemoveRankingMenu(@NotNull Player player) {
        FormWindowCustom custom = new FormWindowCustom(PLUGIN_NAME);
        custom.addElement(new ElementDropdown(MoneyRanking.getInstance().getLanguage().translateString("select_the_ranking_to_delete"),
                new ArrayList<>(MoneyRanking.getInstance().getRankings().keySet())));
        showFormWindow(player, custom, FormType.REMOVE_RANKING_MENU);
    }

    public static void sendSetFormatMenu(@NotNull Player player) {
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(PLUGIN_NAME);
        for (Map.Entry<String, Ranking> entry : MoneyRanking.getInstance().getRankings().entrySet()) {
            simple.addButton(new ResponseElementButton(entry.getKey()).onClicked(cp -> sendSetRankingFormat(cp, entry.getKey())));
        }
        simple.onClosed(FormCreate::sendMainMenu);
        player.showFormWindow(simple);
    }

    public static void sendSetRankingFormat(@NotNull Player player, @NotNull String rankingName) {
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(PLUGIN_NAME);
        Language language = MoneyRanking.getInstance().getLanguage();
        Config config = MoneyRanking.getInstance().getConfig();
        HashMap<String, Object> map = (HashMap<String, Object>) config.get("rankings", new HashMap<>()).get(rankingName);
        LinkedHashMap<String, String> rankingFormat = (LinkedHashMap<String, String>) map.getOrDefault("rankingFormat", new LinkedHashMap<String, String>());

        custom.addElement(new ElementInput(
                language.translateString("ranking_format_set_title"),
                "",
                rankingFormat.getOrDefault("top", "§b<<§a[§e%name%§a]§b>>"))
        );
        custom.addElement(new ElementInput(
                language.translateString("ranking_format_set_line"),
                "",
                rankingFormat.getOrDefault("line", "§bTop[%ranking%] §a%player% §c- §b%score%"))
        );
        custom.addElement(new ElementInput(
                language.translateString("ranking_format_set_lineSelf"),
                "",
                rankingFormat.getOrDefault("lineSelf", "§bTop[%ranking%] §e%player%(me) §c- §b%score%"))
        );
        custom.addElement(new ElementInput(
                language.translateString("ranking_format_set_bottom"),
                "",
                rankingFormat.getOrDefault("bottom", "§b<<§a[§e%name%§a]§b>>"))
        );

        custom.onResponded((formResponseCustom, cp) -> {
            rankingFormat.put("top", formResponseCustom.getInputResponse(0));
            rankingFormat.put("line", formResponseCustom.getInputResponse(1));
            rankingFormat.put("lineSelf", formResponseCustom.getInputResponse(2));
            rankingFormat.put("bottom", formResponseCustom.getInputResponse(3));

            map.put("rankingFormat", rankingFormat);

            HashMap<String, Map<String, Object>> rankings = config.get("rankings", new HashMap<>());
            rankings.put(rankingName, map);
            config.set("rankings", rankings);
            config.save();

            MoneyRanking.getInstance().loadRanking(rankingName, map);

            cp.sendMessage(language.translateString("ranking_format_setSuccess", rankingName));
        });
        custom.onClosed(FormCreate::sendSetFormatMenu);

        player.showFormWindow(custom);
    }

}
