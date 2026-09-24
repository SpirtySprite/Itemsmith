package com.kirugoldzzzz.itemsmith;

import com.foliagui.FoliaGUI;
import com.kirugoldzzzz.itemsmith.api.ItemsmithApi;
import com.kirugoldzzzz.itemsmith.common.command.CommandBase;
import com.kirugoldzzzz.itemsmith.common.config.ConfigFile;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.platform.Telemetry;
import com.kirugoldzzzz.itemsmith.common.platform.UpdateChecker;
import com.kirugoldzzzz.itemsmith.common.scheduler.Scheduling;
import com.kirugoldzzzz.itemsmith.common.text.Messages;
import com.kirugoldzzzz.itemsmith.common.text.Palette;
import com.kirugoldzzzz.itemsmith.common.text.Tr;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;

public final class Itemsmith extends JavaPlugin {

    private static final int BSTATS_ID = 0;
    private static final String REPOSITORY = "SpirtySprite/Itemsmith";

    private final Telemetry telemetry = new Telemetry();

    @Override
    public void onEnable() {
        Scheduling.bind(this);
        ConfigFile settings = loadSettings();
        FoliaGUI.init(this);
        Guis.installTheme();
        ItemEditService service = new ItemEditService();
        ItemEditMenu menu = new ItemEditMenu(service);
        ItemLibrary library = new ItemLibrary(new java.io.File(getDataFolder(), "library.yml"));
        library.load();
        ItemEditCommand command = new ItemEditCommand(service, menu, library);
        command.onReload(this::loadSettings);
        bind("item", command);
        getServer().getServicesManager().register(ItemsmithApi.class, new ItemsmithService(service, menu), this,
                ServicePriority.Normal);
        if (settings.get().getBoolean("update-checker", true)) {
            new UpdateChecker(this, REPOSITORY, "itemsmith.admin.item").start();
        }
        telemetry.start(this, BSTATS_ID, Map.of());
    }

    private ConfigFile loadSettings() {
        ConfigFile settings = new ConfigFile(this, "config.yml").load();
        Tr.configure(this, settings.get().getString("language", "en"));
        Palette.apply(settings.get().getConfigurationSection("theme"));
        new ConfigFile(this, "lang/messages_fr.yml").load();
        Messages.load(new ConfigFile(this, Tr.messagesFile(this)).load().get());
        return settings;
    }

    @Override
    public void onDisable() {
        telemetry.stop();
        getServer().getServicesManager().unregisterAll(this);
        FoliaGUI.shutdown();
    }

    private void bind(String name, CommandBase executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning(Tr.t("La commande ") + name + Tr.t(" est absente du plugin.yml"));
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
