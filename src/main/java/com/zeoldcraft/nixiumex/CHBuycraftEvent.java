package com.zeoldcraft.nixiumex;

import com.laytonsmith.core.events.BindableEvent;
import com.zeoldcraft.BuycraftEvent;

public class CHBuycraftEvent implements BindableEvent {

	BuycraftEvent bc;
	public CHBuycraftEvent(BuycraftEvent bcwantstodoacommand) {
		this.bc = bcwantstodoacommand;
	}
	
	/**
	 * Returns the username used on buycraft
	 * @return username
	 */
	public String getUsername() {
		return bc.getUsername();
	}
	
	/**
	 * Returns the command issued by buycraft
	 * @return command
	 */
	public String getCommand() {
		return bc.getCommand();
	}
	
	/**
	 * Changes the command, if you want
	 * @param newCommand
	 */
	public void setCommand(String newCommand) {
		bc.setCommand(newCommand);
	}
	
	/**
	 * Gets whether the command will be run when the event is done processing
	 * @return Whether the command will be run
	 */
	public boolean getWillRun() {
		return bc.getWillRun();
	}
	
	/**
	 * Sets whether the command will be run when the event is done processing
	 * @param shouldRun
	 */
	public void setWillRun(boolean willRun) {
		bc.setWillRun(willRun);
	}
	
	public Object _GetObject() {
		return bc;
	}
}
