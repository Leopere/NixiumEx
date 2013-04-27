package com.zeoldcraft.nixiumex;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.FireworkEffect;

import com.laytonsmith.PureUtilities.StringUtils;
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
import com.laytonsmith.core.constructs.CInt;
import com.laytonsmith.core.constructs.CString;
import com.laytonsmith.core.constructs.CVoid;
import com.laytonsmith.core.constructs.Construct;
import com.laytonsmith.core.constructs.Target;
import com.laytonsmith.core.environments.CommandHelperEnvironment;
import com.laytonsmith.core.environments.Environment;
import com.laytonsmith.core.exceptions.ConfigRuntimeException;
import com.laytonsmith.core.functions.AbstractFunction;
import com.laytonsmith.core.functions.Exceptions;
import com.laytonsmith.core.functions.Exceptions.ExceptionType;

public class NixFunx {

    private static FireworkEffectPlayer player;
    
    static {
    	player = new FireworkEffectPlayer();
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
}
