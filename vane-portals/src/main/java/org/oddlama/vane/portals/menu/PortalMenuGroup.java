package org.oddlama.vane.portals.menu;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.portals.Portals;

public class PortalMenuGroup extends ModuleComponent<Portals> {

    public EnterNameMenu enterNameMenu;
    public ConsoleMenu consoleMenu;
    public SettingsMenu settingsMenu;
    public StyleMenu styleMenu;

    public PortalMenuGroup(Context<Portals> context) {
        super(context.namespace("Menus"));
        enterNameMenu = new EnterNameMenu(getContext());
        consoleMenu = new ConsoleMenu(getContext());
        settingsMenu = new SettingsMenu(getContext());
        styleMenu = new StyleMenu(getContext());
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
