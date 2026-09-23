package com.kirugoldzzzz.itemsmith;

import com.foliagui.FoliaGUI;
import com.kirugoldzzzz.itemsmith.common.command.NexusCommand;
import com.kirugoldzzzz.itemsmith.common.config.ConfigFile;
import com.kirugoldzzzz.itemsmith.common.gui.Guis;
import com.kirugoldzzzz.itemsmith.common.scheduler.Scheduling;
import com.kirugoldzzzz.itemsmith.common.text.Messages;
import com.kirugoldzzzz.itemsmith.common.text.Tr;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Itemsmith extends JavaPlugin {

    @Override
    public void onEnable() {
        Scheduling.bind(this);
        ConfigFile settings = new ConfigFile(this, "config.yml").load();
        Tr.configure(this, settings.get().getString("language", "en"));
        FoliaGUI.init(this);
        Guis.installTheme();
        new ConfigFile(this, "lang/messages_fr.yml").load();
        Messages.load(new ConfigFile(this, Tr.messagesFile(this)).load().get());
        ItemEditService service = new ItemEditService();
        bind("item", new ItemEditCommand(service, new ItemEditMenu(service)));
    }

    @Override
    public void onDisable() {
        FoliaGUI.shutdown();
    }

    private void bind(String name, NexusCommand executor) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            getLogger().warning(Tr.t("La commande ") + name + Tr.t(" est absente du plugin.yml"));
            return;
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
    }
}
