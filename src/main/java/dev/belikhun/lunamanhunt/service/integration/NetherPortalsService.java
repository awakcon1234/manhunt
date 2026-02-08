package dev.belikhun.lunamanhunt.service.integration;

import org.bukkit.Bukkit;
import org.bukkit.PortalType;
import org.bukkit.plugin.Plugin;
import org.mvplugins.multiverse.netherportals.MultiverseNetherPortals;

public class NetherPortalsService {
	private final MultiverseNetherPortals netherPortals;

	public NetherPortalsService() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("Multiverse-NetherPortals");
		this.netherPortals = plugin instanceof MultiverseNetherPortals instance ? instance : null;
	}

	public void linkMatchWorlds(String overworld, String nether, String end) {
		if (netherPortals == null) {
			return;
		}
		if (isBlank(overworld) || isBlank(nether) || isBlank(end)) {
			return;
		}
		netherPortals.addWorldLink(overworld, nether, PortalType.NETHER);
		netherPortals.addWorldLink(nether, overworld, PortalType.NETHER);
		netherPortals.addWorldLink(overworld, end, PortalType.ENDER);
		netherPortals.addWorldLink(end, overworld, PortalType.ENDER);
	}

	public void unlinkMatchWorlds(String overworld, String nether, String end) {
		if (netherPortals == null) {
			return;
		}
		if (isBlank(overworld) || isBlank(nether) || isBlank(end)) {
			return;
		}
		netherPortals.removeWorldLink(overworld, nether, PortalType.NETHER);
		netherPortals.removeWorldLink(nether, overworld, PortalType.NETHER);
		netherPortals.removeWorldLink(overworld, end, PortalType.ENDER);
		netherPortals.removeWorldLink(end, overworld, PortalType.ENDER);
	}

	private boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
