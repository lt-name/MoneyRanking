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
        simple.addButton(new ElementButton("添加新的排行榜"));
        simple.addButton(new ElementButton("删除已有排行榜"));
        showFormWindow(player, simple, FormType.MAIN_MENU);
    }

    public static void sendAddRankingMenu(@NotNull Player player) {
        FormWindowCustom custom = new FormWindowCustom(PLUGIN_NAME);
        custom.addElement(new ElementInput("排行榜名称"));
        ArrayList<String> list = new ArrayList<>();
        for (MoneyProvider.EconomyAPIType economyAPIType : MoneyProvider.EconomyAPIType.values()) {
            if (economyAPIType == MoneyProvider.EconomyAPIType.NULL) {
                continue;
            }
            list.add(economyAPIType.getName());
        }
        custom.addElement(new ElementDropdown("经济API", list));
        showFormWindow(player, custom, FormType.ADD_RANKING_MENU);
    }

    public static void sendRemoveRankingMenu(@NotNull Player player) {
        FormWindowCustom custom = new FormWindowCustom(PLUGIN_NAME);
        custom.addElement(new ElementDropdown("选择要删除的排行榜", new ArrayList<>(MoneyRanking.getInstance().getRankings().keySet())));
        showFormWindow(player, custom, FormType.REMOVE_RANKING_MENU);
    }

}
