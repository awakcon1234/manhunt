package dev.belikhun.lunamanhunt.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;

import java.util.List;
import java.util.Objects;

public class BasicCommandAdapter implements BasicCommand {
	private final String name;
	private final TabExecutor executor;
	private final Command command;

	public BasicCommandAdapter(String name, TabExecutor executor) {
		this.name = Objects.requireNonNull(name, "name");
		this.executor = Objects.requireNonNull(executor, "executor");
		this.command = new AdapterCommand(this.name);
	}

	@Override
	public void execute(CommandSourceStack source, String[] args) {
		CommandSender sender = source.getSender();
		executor.onCommand(sender, command, name, args == null ? new String[0] : args);
	}

	@Override
	public List<String> suggest(CommandSourceStack source, String[] args) {
		CommandSender sender = source.getSender();
		String[] safeArgs = args == null ? new String[0] : args;
		if (safeArgs.length == 0 || (safeArgs.length == 1 && safeArgs[0].isBlank())) {
			safeArgs = new String[] { "" };
		}
		List<String> suggestions = executor.onTabComplete(sender, command, name, safeArgs);
		return suggestions == null ? List.of() : suggestions;
	}

	private static final class AdapterCommand extends Command {
		AdapterCommand(String name) {
			super(name);
		}

		@Override
		public boolean execute(CommandSender sender, String commandLabel, String[] args) {
			return false;
		}
	}
}
