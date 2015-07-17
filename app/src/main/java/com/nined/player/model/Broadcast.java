package com.nined.player.model;

import com.google.common.base.Objects;

public class Broadcast {
	private String name;
	private String url;
	private String type;
	
	/**
	 * Default Constructor
	 */
	public Broadcast () {
	}
	/*****************************/
	/** Getters and Setters		**/
	/*****************************/
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name to be set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the url
	 */
	public String getUrl() {
		return url;
	}
	/**
	 * @param url to be set
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type to be set
	 */
	public void setType(String type) {
		this.type = type;
	}
	
	/*****************************/
	/**		Object Functions	**/
	/*****************************/
	@Override
	public int hashCode() {
		return Objects.hashCode(name, url, type);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Broadcast) {
			Broadcast other = (Broadcast) obj;
			return (Objects.equal(this.name, other.name)
					&& Objects.equal(this.url, other.url)
					&& Objects.equal(this.type, other.type));
		}
		return false;
	}
}
