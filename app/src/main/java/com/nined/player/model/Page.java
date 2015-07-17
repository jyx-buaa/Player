	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.model;

public class Page extends Post{
	@Override
	public String getType() {
		return Page.class.getName();
	}
	/**
	 * TODO: Change this ugly shit
	 */
	@Override
	public Long getId() {
		try {
			long id = (long) Integer.parseInt(getGuid().substring(getGuid().lastIndexOf("=")+1));
			return id;
		} catch (NumberFormatException e) {
		}
		return (long) 1290; //Mijireh Secure Checkout
	}
}
