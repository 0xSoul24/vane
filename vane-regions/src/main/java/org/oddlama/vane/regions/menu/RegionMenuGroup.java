package org.oddlama.vane.regions.menu;

import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.Regions;

public class RegionMenuGroup extends ModuleComponent<Regions> {

    public EnterRegionNameMenu enterRegionNameMenu;
    public EnterRegionGroupNameMenu enterRegionGroupNameMenu;
    public EnterRoleNameMenu enterRoleNameMenu;
    public MainMenu mainMenu;
    public RegionGroupMenu regionGroupMenu;
    public RegionMenu regionMenu;
    public RoleMenu roleMenu;

    public RegionMenuGroup(Context<Regions> context) {
        super(context.namespace("Menus"));
        enterRegionNameMenu = new EnterRegionNameMenu(getContext());
        enterRegionGroupNameMenu = new EnterRegionGroupNameMenu(getContext());
        enterRoleNameMenu = new EnterRoleNameMenu(getContext());
        mainMenu = new MainMenu(getContext());
        regionGroupMenu = new RegionGroupMenu(getContext());
        regionMenu = new RegionMenu(getContext());
        roleMenu = new RoleMenu(getContext());
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}
}
