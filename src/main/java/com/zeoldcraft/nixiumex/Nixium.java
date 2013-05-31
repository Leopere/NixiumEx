package com.zeoldcraft.nixiumex;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.FireworkEffect;
import org.bukkit.event.Listener;

import com.laytonsmith.PureUtilities.StringUtils;
import com.laytonsmith.PureUtilities.Version;
import com.laytonsmith.abstraction.MCColor;
import com.laytonsmith.abstraction.MCCommandSender;
import com.laytonsmith.abstraction.MCLocation;
import com.laytonsmith.abstraction.MCPlayer;
import com.laytonsmith.abstraction.StaticLayer;
import com.laytonsmith.abstraction.bukkit.BukkitMCColor;
import com.laytonsmith.abstraction.bukkit.BukkitMCLocation;
import com.laytonsmith.abstraction.bukkit.BukkitMCPlayer;
import com.laytonsmith.abstraction.bukkit.BukkitMCWorld;
import com.laytonsmith.abstraction.enums.MCFireworkType;
import com.laytonsmith.abstraction.enums.bukkit.BukkitMCFireworkType;
import com.laytonsmith.annotations.api;
import com.laytonsmith.core.CHVersion;
import com.laytonsmith.core.ObjectGenerator;
import com.laytonsmith.core.Security;
import com.laytonsmith.core.Static;
import com.laytonsmith.core.constructs.CArray;
import com.laytonsmith.core.constructs.CBoolean;
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.events.AbstractEvent;
import com.laytonsmith.core.events.BindableEvent;
import com.laytonsmith.core.events.Driver;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.exceptions.EventException;
import com.laytonsmith.core.exceptions.PrefilterNonMatchException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;

public class Nixium implements Listener {

    private static FireworkEffectPlayer player;
    
    static {
    	player = new FireworkEffectPlayer();
    }
    
    public static String docs() {
    	return "Special stuff made for the Nixium servers.";
    }
    
	public static abstract class NFunction extends AbstractFunction {

		public boolean isRestricted() {
			return true;
		}

		public Boolean runAsync() {
			return false;
		}

		public CHVersion since() {
			return CHVersion.V3_3_1;
		}
	}
	
	public static abstract class NEvent extends AbstractEvent {

		public BindableEvent convert(CArray manualObject) {
			throw new ConfigRuntimeException("Operation not supported", Target.UNKNOWN);
		}

		public Driver driver() {
			return Driver.EXTENSION;
		}

		public Version since() {
			return CHVersion.V3_3_1;
		}
	}
	
	@api(environments={CommandHelperEnvironment.class})
	public static class firework_effect extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{};
		}

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			MCCommandSender cmd = environment.getEnv(CommandHelperEnvironment.class).GetCommandSender();
			MCLocation loc;
			if (args.length == 1) {
				if (cmd instanceof MCPlayer) {
					loc = ((MCPlayer) cmd).getLocation();
				} else {
					throw new ConfigRuntimeException("Location cannot be determined from 1 arg without a player.",
							ExceptionType.PlayerOfflineException, t);
				}
			} else {
				loc = ObjectGenerator.GetGenerator().location(args[1], null, t);
			}
			CArray options;
			if (!(args[0] instanceof CArray)) {
				throw new ConfigRuntimeException("Argument 1 was expected to be an array",
						ExceptionType.FormatException, t);
			}
			options = (CArray) args[0];
			boolean flicker = false;
			boolean trail = true;
			Set<MCColor> colors = new HashSet<MCColor>();
			Set<MCColor> fade = new HashSet<MCColor>();
			MCFireworkType type = MCFireworkType.BALL;
			
			if(options.containsKey("flicker")){
				flicker = Static.getBoolean(options.get("flicker", t));
			}
			if(options.containsKey("trail")){
				trail = Static.getBoolean(options.get("trail", t));
			}
			if(options.containsKey("colors")){
				colors = parseColors(options.get("colors", t), t);
			} else {
				colors.add(MCColor.WHITE);
			}
			if(options.containsKey("fade")){
				fade = parseColors(options.get("fade"), t);
			}
			if(options.containsKey("type")){
				try{
					type = MCFireworkType.valueOf(options.get("type").val().toUpperCase());
				} catch(IllegalArgumentException e){
					throw new Exceptions.FormatException("Invalid type: " + options.get("type").val(), t);
				}
			}
			FireworkEffect.Builder fw = FireworkEffect.builder();
			fw = fw.flicker(flicker)
				.trail(trail)
				.with(BukkitMCFireworkType.getConvertor().getConcreteEnum(type));
			for(MCColor color : colors){
				fw = fw.withColor(BukkitMCColor.GetColor(color));
			}
			
			for(MCColor color : fade){
				fw = fw.withFade(BukkitMCColor.GetColor(color));
			}
			try {
				player.playFirework(((BukkitMCWorld) loc.getWorld()).__World(),
						((BukkitMCLocation) loc)._Location(), fw.build());
			} catch (Exception e) {
				throw new ConfigRuntimeException(e.getMessage(), ExceptionType.PluginInternalException, t);
			}
			return new CVoid(t);
		}

		public String getName() {
			return "firework_effect";
		}

		private Set<MCColor> parseColors(Construct c, Target t){
			Set<MCColor> colors = new HashSet<MCColor>();
			if(c instanceof CArray){
				CArray ca = ((CArray)c);
				if(ca.size() == 3
						&& ca.get(0) instanceof CInt
						&& ca.get(1) instanceof CInt
						&& ca.get(2) instanceof CInt
						){
					//It's a single custom color
					colors.add(parseColor(ca, t));
				} else {
					for(String key : ca.keySet()){
						Construct val = ca.get(key);
						if(val instanceof CArray){
							colors.add(parseColor(((CArray)val), t));
						} else if(val instanceof CString){
							colors.addAll(parseColor(((CString)val), t));
						}
					}
				}
			} else if(c instanceof CString){
				colors.addAll(parseColor(((CString)c), t));
			}
			return colors;
		}
		
		private MCColor parseColor(CArray ca, Target t){
			return StaticLayer.GetConvertor().GetColor(
							Static.getInt32(ca.get(0), t), 
							Static.getInt32(ca.get(1), t), 
							Static.getInt32(ca.get(2), t)
						);
		}
		
		private List<MCColor> parseColor(CString cs, Target t){
			String split[] = cs.val().split("\\|");
			List<MCColor> colors = new ArrayList<MCColor>();
			for(String s : split){
				if(MCColor.STANDARD_COLORS.containsKey(s.toUpperCase())){
					 colors.add(MCColor.STANDARD_COLORS.get(s.toUpperCase()));
				} else {
					throw new Exceptions.FormatException("Unknown color type: " + s, t);
				}
			}
			return colors;
		}
		
		public Integer[] numArgs() {
			return new Integer[]{1, 2};
		}

		public String docs() {
			return "void {array, [location]} Plays a firework, using the same options array as launch_firework";
		}
	}
	
	@api
	public static class send_particle extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.PlayerOfflineException};
		}

		public Construct exec(Target t, Environment environment,
				Construct... args) throws ConfigRuntimeException {
			List<MCPlayer> pl = new ArrayList<MCPlayer>();
			ParticleEffect part;
			int amount = 1;
			MCLocation loc = ObjectGenerator.GetGenerator().location(args[1], null, t);
			float speed = 0;
			if (args[0] instanceof CArray) {
				for (String k : ((CArray) args[0]).keySet()) {
					pl.add(Static.GetPlayer(((CArray) args[0]).get(k, t), t));
				}
			} else {
				pl.add(Static.GetPlayer(args[0], t));
			}
			try {
				part = ParticleEffect.valueOf(args[2].val().toUpperCase());
			} catch (IllegalArgumentException iae) {
				throw new ConfigRuntimeException("Particle should be one of "
					+ StringUtils.Join(ParticleEffect.values(), ", ", ", or "),
					ExceptionType.FormatException, t);
			}
			if (args.length >= 4) {
				amount = Static.getInt32(args[3], t);
			}
			if (args.length == 5) {
				speed = Static.getDouble32(args[4], t);
			}
			for (MCPlayer p : pl) {
				try {
					ParticleEffect.sendToPlayer(part, ((BukkitMCPlayer) p)._Player(),
							((BukkitMCLocation)loc)._Location(), 0, 0, 0, speed, amount);
				} catch (Exception e) {
					throw new ConfigRuntimeException(e.getMessage(), ExceptionType.PluginInternalException, t);
				}
			}
			return new CVoid(t);
		}

		public String getName() {
			return "send_particle";
		}

		public Integer[] numArgs() {
			return new Integer[]{3, 4, 5};
		}

		public String docs() {
			return "void {players, location, particle, [amount], [speed]} Particle should be one of "
					+ StringUtils.Join(ParticleEffect.values(), ", ", ", or ");
		}
	}
	
	@api
	public static class send_crack extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.PluginInternalException, ExceptionType.CastException,
					ExceptionType.FormatException, ExceptionType.PlayerOfflineException};
		}

		public Construct exec(Target t, Environment environment,
				Construct... args) throws ConfigRuntimeException {
			List<MCPlayer> pl = new ArrayList<MCPlayer>();
			MCLocation loc = ObjectGenerator.GetGenerator().location(args[1], null, t);
			int type = Static.getInt32(args[2], t);
			byte data = Static.getInt8(args[3], t);
			int amount = 1;
			boolean icon = true;
			if (args[0] instanceof CArray) {
				for (String k : ((CArray) args[0]).keySet()) {
					pl.add(Static.GetPlayer(((CArray) args[0]).get(k, t), t));
				}
			} else {
				pl.add(Static.GetPlayer(args[0], t));
			}
			if (args.length >= 5) {
				amount = Static.getInt32(args[4], t);
			}
			if (args.length == 6) {
				icon = Static.getBoolean(args[5]);
			}
			for (MCPlayer p : pl) {
				try {
					ParticleEffect.sendCrackToPlayer(icon, type, data, ((BukkitMCPlayer) p)._Player(),
							((BukkitMCLocation)loc)._Location(), 0, 0, 0, amount);
				} catch (Exception e) {
					throw new ConfigRuntimeException(e.getMessage(), ExceptionType.PluginInternalException, t);
				}
			}
			return null;
		}

		public String getName() {
			return "send_crack";
		}

		public Integer[] numArgs() {
			return new Integer[]{4, 5, 6};
		}

		public String docs() {
			return "void {players, location, type, data, [amount], [icon]} I don't know what this does. Icon is boolean.";
		}
	}
	
	@api
	public static class write extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.IOException, ExceptionType.SecurityException};
		}

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			String location = args[0].val();
			boolean fixed = false;
			if (args.length == 3) {
				fixed = Static.getBoolean(args[2]);
			}
			location = fixed ? new File(location).getAbsolutePath() : new File(t.file().getParentFile(), location).getAbsolutePath();
			if (!Security.CheckSecurity(location)) {
				throw new ConfigRuntimeException("You do not have permission to access the file '" + location + "'",
					Exceptions.ExceptionType.SecurityException, t);
			}
			try{// Create file
				FileWriter fstream = new FileWriter(location);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(args[1].val());
				//Close the output stream
				out.close();
			}catch (Exception e){//Catch exception if any
				throw new ConfigRuntimeException("Could not write to the file.", ExceptionType.IOException, t);
			}
			return new CVoid(t);
		}

		public String getName() {
			return "write";
		}

		public Integer[] numArgs() {
			return new Integer[]{2, 3};
		}

		public String docs() {
			return "void {file, contents, [useServerDir]} Writes contents to the file relative to the script."
					+ " If useServerDir is true, the path will be relative to the server jar instead of the script.";
		}
	}
	
	@api
	public static class file_exists extends NFunction {

		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			String location = args[0].val();
			File file = new File(t.file().getParentFile(), location);
			return new CBoolean(file.exists(), t);
		}

		public ExceptionType[] thrown() {
			return new ExceptionType[]{};
		}

		public String docs() {
			return "boolean {file} Returns whether the given file exists, relative to the script.";
		}

		public String getName() {
			return "file_exists";
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}
	}
	
	@api
	public static class is_directory extends NFunction {

		public Construct exec(Target t, Environment environment, Construct... args) throws ConfigRuntimeException {
			String location = args[0].val();
			File file = new File(t.file().getParentFile(), location);
			location = file.getAbsolutePath();
			if (!Security.CheckSecurity(location)) {
				throw new ConfigRuntimeException("You do not have permission to access the file '" + location + "'",
					Exceptions.ExceptionType.SecurityException, t);
			}
			if (!file.exists()) {
				throw new ConfigRuntimeException("The specified file does not exist.", ExceptionType.IOException, t);
			}
			return new CBoolean(file.isDirectory(), t);
		}

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.IOException, ExceptionType.SecurityException};
		}

		public String docs() {
			return "boolean {file} Returns whether the given file relative to the script is a directory or not.";
		}

		public String getName() {
			return "is_directory";
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}
	}
	
	@api
	public static class directory_contents extends NFunction {

		public Construct exec(Target t, Environment env, Construct... args) throws ConfigRuntimeException {
			String location = args[0].val();
			File file = new File(t.file().getParentFile(), location);
			location = file.getAbsolutePath();
			if (!Security.CheckSecurity(location)) {
				throw new ConfigRuntimeException("You do not have permission to access the file '" + location + "'",
					Exceptions.ExceptionType.SecurityException, t);
			}
			if (!file.exists()) {
				throw new ConfigRuntimeException("The file at "+location+" does not exist.", ExceptionType.IOException, t);
			}
			if (!file.isDirectory()) {
				throw new ConfigRuntimeException("The file at "+location+" is not a directory", ExceptionType.IOException, t);
			}
			CArray ret = new CArray(t);
			for (String path : file.list()) {
				ret.push(new CString(path, t));
			}
			return ret;
		}

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.SecurityException, ExceptionType.IOException};
		}

		public String docs() {
			return "array {directory} Returns an array of the filenames inside a given directory relative to the script.";
		}

		public String getName() {
			return "directory_contents";
		}

		public Integer[] numArgs() {
			return new Integer[]{1};
		}
	}
	
	@api
	public static class buycraft_command extends NEvent {

		public String getName() {
			return "buycraft_command";
		}

		public String docs() {
			return "{}"
					+ " Fires when Buycraft tries to run a command triggered by a purchase or expiry"
					+ " {username: the name used to make the buycraft purchase | command | willrun}"
					+ " {command: The command to run | willrun: Whether the command will run when this is over}"
					+ " {}";
		}

		public boolean matches(Map<String, Construct> prefilter, BindableEvent e) throws PrefilterNonMatchException {
			if (e instanceof CHBuycraftEvent) {
				return true;
			}
			return false;
		}

		public Map<String, Construct> evaluate(BindableEvent e) throws EventException {
			if (e instanceof CHBuycraftEvent) {
				CHBuycraftEvent event = (CHBuycraftEvent) e;
				Target t = Target.UNKNOWN;
				Map<String, Construct> ret = evaluate_helper(e);
				ret.put("username", new CString(event.getUsername(), t));
				ret.put("command", new CString(event.getCommand(), t));
				ret.put("willrun", new CBoolean(event.getWillRun(), t));
				return ret;
			} else {
				throw new EventException("Event didn't work dammit.");
			}
		}

		public boolean modifyEvent(String key, Construct value, BindableEvent event) {
			if (event instanceof CHBuycraftEvent) {
				CHBuycraftEvent e = (CHBuycraftEvent) event;
				if (key.equals("command")) {
					e.setCommand(value.val());
				}
				if (key.equals("willrun")) {
					e.setWillRun(Static.getBoolean(value));
				}
			}
			return false;
		}
	}
}
