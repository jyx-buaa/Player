	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.model;

public class User {
	/*****************************/
	/**		Member Variables	**/
	/*****************************/
	private Long ID;
	/*****************************/
	private String username;
	/*****************************/
	private String name;
	/*****************************/
	private String first_name;
	/*****************************/
	private String last_name;
	/*****************************/
	private String nick_name;
	/*****************************/
	private String slug;
	/*****************************/
	private String URL;
	/*****************************/
	private String avatar;
	/*****************************/
	private String description;
	/*****************************/
	private String registered;
	/*****************************/
	//private String meta;
	/*****************************/
	public Long getID() {
		return ID;
	}
	public void setID(Long iD) {
		ID = iD;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFirst_name() {
		return first_name;
	}
	public void setFirst_name(String first_name) {
		this.first_name = first_name;
	}
	public String getLast_name() {
		return last_name;
	}
	public void setLast_name(String last_name) {
		this.last_name = last_name;
	}
	public String getNick_name() {
		return nick_name;
	}
	public void setNick_name(String nick_name) {
		this.nick_name = nick_name;
	}
	public String getSlug() {
		return slug;
	}
	public void setSlug(String slug) {
		this.slug = slug;
	}
	public String getURL() {
		return URL;
	}
	public void setURL(String uRL) {
		URL = uRL;
	}
	public String getAvatar() {
		return avatar;
	}
	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getRegistered() {
		return registered;
	}
	public void setRegistered(String registered) {
		this.registered = registered;
	}
}
