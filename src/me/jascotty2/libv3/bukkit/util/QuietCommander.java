/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com> Description:
 * Provides a method of executing console commands without printing to the console
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.libv3.bukkit.util;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;

//craftbukkit has a ServerCommandSender, but it prints things to the console ;)
public class QuietCommander implements CommandSender {

	final Plugin plugin;
	public boolean silent = true;
	public String loggerPrefix = "SilentCommand Message: ";

	public QuietCommander(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getName();
	}

	@Override
	public void sendMessage(String str) {
		if (!silent) {
			plugin.getLogger().info(loggerPrefix + str);
		}
	}

	@Override
	public void sendMessage(String[] strings) {
		if (!silent) {
			for (String s : strings) {
				plugin.getLogger().info(loggerPrefix + s);
			}
		}
	}

	@Override
	public boolean isOp() {
		return true;
	}

	@Override
	public Server getServer() {
		return plugin.getServer();
	}

	@Override
	public boolean isPermissionSet(String string) {
		return true;
	}

	@Override
	public boolean isPermissionSet(Permission prmsn) {
		return true;
	}

	@Override
	public boolean hasPermission(String string) {
		return true;
	}

	@Override
	public boolean hasPermission(Permission prmsn) {
		return true;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, String string, boolean bln, int i) {
		return null;
	}

	@Override
	public PermissionAttachment addAttachment(Plugin plugin, int i) {
		return null;
	}

	@Override
	public void removeAttachment(PermissionAttachment pa) {
	}

	@Override
	public void recalculatePermissions() {
	}

	@Override
	public Set<PermissionAttachmentInfo> getEffectivePermissions() {
		HashSet<PermissionAttachmentInfo> ret = new HashSet<PermissionAttachmentInfo>();
		ret.add(new PermissionAttachmentInfo(this, "*", null, true));
		return ret;
	}

	@Override
	public void setOp(boolean bln) {
	}

    private final CommandSender.Spigot spigot = new CommandSender.Spigot(){

        @Override
        public void sendMessage(net.md_5.bungee.api.chat.BaseComponent component) {
            QuietCommander.this.sendMessage(net.md_5.bungee.api.chat.TextComponent.toLegacyText(component));
        }

        @Override
        public /* varargs */ void sendMessage(net.md_5.bungee.api.chat.BaseComponent ... components) {
            QuietCommander.this.sendMessage(net.md_5.bungee.api.chat.TextComponent.toLegacyText(components));
        }
    };
	
	@Override
	public Spigot spigot() {
		return spigot;
	}

    @Override
    public void sendMessage(UUID uuid, String string) {
    }

    @Override
    public void sendMessage(UUID uuid, String[] strings) {
    }
}
