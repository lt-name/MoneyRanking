package cn.lanink.moneyranking.form;

import cn.lanink.moneyranking.MoneyProvider;
import cn.lanink.moneyranking.MoneyRanking;
import cn.nukkit.Player;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.window.FormWindow;
import cn.nukkit.form.window.FormWindowCustom;
import cn.nukkit.form.window.FormWindowSimple;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;

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
        FormWindowSimple simple = new FormWindowSimple(PLUGIN_NAME, "");
        simple.addButton(new ElementButton(MoneyRanking.getInstance().getLanguage().translateString("add_new_ranking")));
        simple.addButton(new ElementButton(MoneyRanking.getInstance().getLanguage().translateString("delete_existing_ranking")));
        showFormWindow(player, simple, FormType.MAIN_MENU);
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

}
