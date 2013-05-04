package com.zeoldcraft.nixiumex;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.FireworkEffect;

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
import com.laytonsmith.core.functions.FunctionBase;
import com.laytonsmith.core.functions.FunctionList;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;

public class Nixium {

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
			ParticleEffects part;
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
				part = ParticleEffects.valueOf(args[2].val().toUpperCase());
			} catch (IllegalArgumentException iae) {
				throw new ConfigRuntimeException("Particle should be one of "
					+ StringUtils.Join(ParticleEffects.values(), ", ", ", or "),
					ExceptionType.FormatException, t);
			}
			if (args.length == 4) {
				speed = Static.getDouble32(args[3], t);
			}
			for (MCPlayer p : pl) {
				try {
					part.sendToPlayer(((BukkitMCPlayer) p)._Player(),
							((BukkitMCLocation)loc)._Location(), 0, 0, 0, speed, 1);
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
			return new Integer[]{3, 4};
		}

		public String docs() {
			return "void {players, location, particle} Particle should be one of "
					+ StringUtils.Join(ParticleEffects.values(), ", ", ", or ");
		}
	}
	
	@api
	public static class write extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{ExceptionType.IOException};
		}

		public Construct exec(Target t, Environment environment,
				Construct... args) throws ConfigRuntimeException {
			try{// Create file
				FileWriter fstream = new FileWriter(args[0].val());
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
			return new Integer[]{2};
		}

		public String docs() {
			return "void {file, contents} Writes contents to file. What the hell else would it do?"
					+ " In seriousness, the base directory for this is the server directory.";
		}
	}
	
	private static Map<String,List<String>> funcs = new HashMap<String,List<String>>();
	
	private static void initf() {
		for (FunctionBase f : FunctionList.getFunctionList(api.Platforms.INTERPRETER_JAVA)) {
			String[] pack = f.getClass().getEnclosingClass().getName().split("\\.");
			String clazz = pack[pack.length - 1];
			if (!funcs.containsKey(clazz)) {
				funcs.put(clazz, new ArrayList<String>());
			}
			funcs.get(clazz).add(f.getName());
		}
	}
	
	@api
	public static class get_functions extends NFunction {

		public ExceptionType[] thrown() {
			return new ExceptionType[]{};
		}

		public Construct exec(Target t, Environment environment,
				Construct... args) throws ConfigRuntimeException {
			CArray ret = CArray.GetAssociativeArray(t);
			if (funcs.keySet().size() < 10) {
				initf();
			}
			for (String cname : funcs.keySet()) {
				CArray fnames = new CArray(t);
				for (String fname : funcs.get(cname)) {
					fnames.push(new CString(fname, t));
				}
				ret.set(new CString(cname, t), fnames, t);
			}
			return ret;
		}

		public String getName() {
			return "get_functions";
		}

		public Integer[] numArgs() {
			return new Integer[]{0};
		}

		public String docs() {
			return "array {} Returns an associative array of all loaded functions. The keys of this array are the"
					+ " names of the classes containing the functions (which you know as the sections of the API page),"
					+ " and the values are arrays of the names of the functions within those classes.";
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
