package com.zeoldcraft.nixiumex;

import java.io.File;
import java.io.IOException;

import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.annotations.api;
import com.laytonsmith.commandhelper.CommandHelperPlugin;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.*;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.EmptyClipboardException;
import com.sk89q.worldedit.LocalSession;
import com.sk89q.worldedit.LocalWorld;
import com.sk89q.worldedit.MaxChangedBlocksException;
import com.sk89q.worldedit.Vector;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.WorldEditException;
import com.sk89q.worldedit.bukkit.BukkitCommandSender;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.data.DataException;
import com.sk89q.worldedit.schematic.SchematicFormat;
import com.zeoldcraft.nixiumex.Nixium.NFunction;

public class WEClipboard {

	// CH's local player, based from console
	private static CHCommandSender player;
	// CH's console-based session
	private static LocalSession session;
	
	private static WorldEditPlugin we() {
		return CommandHelperPlugin.wep;
	}
	
	public static CHCommandSender getLocalPlayer() {
		if (player == null) {
			player = new CHCommandSender();
		}
		return player;
	}
	
	public static LocalSession getLocalSession() {
		if (session == null) {
			session = CommandHelperPlugin.wep.getWorldEdit().getSession(getLocalPlayer());
		}
		return session;
	}
	
	public static LocalWorld getLocalWorld(String name) {
		for (LocalWorld w : we().getWorldEdit().getServer().getWorlds()) {
			if (w.getName().equals(name)) {
				return w;
			}
		}
		return null;
	}
	
	// modified from com.sk89q.worldedit.LocalSession.createEditSession
	private static EditSession createEditSession(CHCommandSender player, String world, boolean fastMode) {
		LocalWorld w = getLocalWorld(world);
		player.setWorld(w);
		EditSession editSession = WorldEdit.getInstance().getEditSessionFactory()
				.getEditSession(getLocalWorld(world), -1, null, player);
		editSession.setFastMode(fastMode);

		return editSession;
	}
	
	// Class required for working with loggers
	public static class CHCommandSender extends BukkitCommandSender {

		public CHCommandSender() {
			super(we(), we().getServerInterface(), we().getServer().getConsoleSender());
		}
		
		private LocalWorld world;

		@Override
		public LocalWorld getWorld() {
			return world;
		}
		
		public void setWorld(LocalWorld w) {
			world = w;
		}
	}
	
	@api
	public static class skcb_load extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.PluginInternalException, ExceptionType.IOException};
		}

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			Static.checkPlugin("WorldEdit", t);
			WorldEdit we = CommandHelperPlugin.wep.getWorldEdit();
			// Based on: com.sk89q.worldedit.commands.SchematicCommands.load
			String filename = args[0].val();
			File savedir = we.getWorkingDirectoryFile(we.getConfiguration().saveDir);
			try {
				File schematic = we.getSafeOpenFile(getLocalPlayer(), savedir, filename, "schematic", "schematic");
				if (!schematic.exists()) {
					throw new IOException("Schematic " + filename + " could not be found.");
				}
				getLocalSession().setClipboard(SchematicFormat.getFormat(schematic).load(schematic));
			} catch (WorldEditException e) {
				throw new ConfigRuntimeException(e.getMessage(), ExceptionType.PluginInternalException, t);
			} catch (IOException e) {
				throw new ConfigRuntimeException(e.getMessage(), ExceptionType.IOException, t);
			} catch (DataException e) {
				throw new ConfigRuntimeException(e.getMessage(), ExceptionType.IOException, t);
			}
			return new CVoid(t);
		}

		public String getName() {
			return "skcb_load";
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		public String docs() {
			return "void {filename} Loads a schematic from file. It will use the directory specified in WorldEdit's config.";
		}
	}
	
	@api
	public static class skcb_rotate extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.RangeException, ExceptionType.NotFoundException};
		}

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			int angle = Static.getInt32(args[0], t);
			if (angle % 90 != 0) {
				throw new ConfigRuntimeException("Expected rotation angle to be a multiple of 90",
						ExceptionType.RangeException, t);
			}
			try {
				getLocalSession().getClipboard().rotate2D(angle);
			} catch (EmptyClipboardException e) {
				throw new ConfigRuntimeException("The clipboard is empty, copy something to it first!",
						ExceptionType.NotFoundException, t);
			}
			return new CVoid(t);
		}

		public String getName() {
			return "skcb_rotate";
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}

		public String docs() {
			return "void {int} Given a multiple of 90, rotates the clipboard by that number.";
		}
	}
	
	//@api
	public static class skcb_copy extends NFunction {

		public ExceptionType[] thrown() {
			// TODO Auto-generated method stub
			return null;
		}

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			// TODO Auto-generated method stub
			return null;
		}

		public String getName() {
			return "skcb_copy";
		}

		public Integer[] numArgs() {
			// TODO Auto-generated method stub
			return null;
		}

		public String docs() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	@api
	public static class skcb_paste extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.NotFoundException, ExceptionType.RangeException};
		}

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			Static.checkPlugin("WorldEdit", t);
			boolean noAir = false;
			boolean entities = false;
			boolean fastmode = false;
			if (args.length >= 2) {
				noAir = Static.getBoolean(args[1]);
			}
			if (args.length == 3) {
				entities = Static.getBoolean(args[2]);
			}
			MCLocation loc = ObjectGenerator.GetGenerator().location(args[0], null, t);
			Vector vec = new Vector(loc.getX(), loc.getY(), loc.getZ());
			EditSession editor = createEditSession(getLocalPlayer(), loc.getWorld().getName(), fastmode);
			
			try {
				getLocalSession().getClipboard().paste(editor, vec, noAir, entities);
			} catch (MaxChangedBlocksException e) {
				throw new ConfigRuntimeException("Attempted to change more blocks than allowed.",
						ExceptionType.RangeException, t);
			} catch (EmptyClipboardException e) {
				throw new ConfigRuntimeException("The clipboard is empty, copy something to it first!",
						ExceptionType.NotFoundException, t);
			}
			
			return new CVoid(t);
		}

		public String getName() {
			return "skcb_paste";
		}

		public Integer[] numArgs() {
			return new Integer[]{1, 2, 3};
		}

		public String docs() {
			return "void {location, [ignoreAir], [entities]} Pastes a schematic as if a player was standing at the location."
					+ " If ignoreAir is true, air blocks from the schematic will not replace blocks in the world."
					+ " If entities is true, any entities stored in the clipboard will be pasted as well."
					+ " Both ignoreAir and entities default to false.";
		}
	}
}
