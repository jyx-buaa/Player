	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.model;

public class Course extends Post{
	@Override
	public String getType() {
		return Course.class.getName();
	}
	@Override
	public Long getId() {
		return (long) Integer.parseInt(getGuid().substring(getGuid().lastIndexOf("=")+1));
	}
}
