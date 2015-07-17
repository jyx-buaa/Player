	/**
	 * 
	 * @author Aekasitt Guruvanich, 9D Tech
	 *
	 */

package com.nined.player.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.common.base.Objects;

public class Post implements Parcelable {
	/*****************************/
	/**		Member Variables	**/
	/*****************************/
	private Long id;
	/*****************************/
	private Long parent;
	/*****************************/
	private Long author;
	/*****************************/
	private String content;
	/*****************************/
	private String contentFiltered;
	/*****************************/
	private String title;
	/*****************************/
	private String excerpt;
	/*****************************/
	private String status;
	/*****************************/
	private String commentStatus;
	/*****************************/
	private String pingStatus;
	/*****************************/
	private String password;
	/*****************************/
	private String name;
	/*****************************/
	private String toPing;
	/*****************************/
	private String pinged;
	/*****************************/
	private String guid;
	/*****************************/
	private Integer menuOrder;
	/*****************************/
	private String mimeType;
	/*****************************/
	private long commentCount;
	/*****************************/
	/**		Constructor			**/
	/*****************************/
	public Post() {
	}
	/*****************************/
	/**		Getters-Setters		**/
	/*****************************/
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the parent
	 */
	public Long getParent() {
		return parent;
	}
	/**
	 * @param parent the parent to set
	 */
	public void setParent(Long parent) {
		this.parent = parent;
	}
	/**
	 * @return the author
	 */
	public Long getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	public void setAuthor(Long author) {
		this.author = author;
	}
	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}
	/**
	 * @return the contentFiltered
	 */
	public String getContentFiltered() {
		return contentFiltered;
	}
	/**
	 * @param contentFiltered the contentFiltered to set
	 */
	public void setContentFiltered(String contentFiltered) {
		this.contentFiltered = contentFiltered;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the excerpt
	 */
	public String getExcerpt() {
		return excerpt;
	}
	/**
	 * @param excerpt the excerpt to set
	 */
	public void setExcerpt(String excerpt) {
		this.excerpt = excerpt;
	}
	/**
	 * @return the status
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * @return the commentStatus
	 */
	public String getCommentStatus() {
		return commentStatus;
	}
	/**
	 * @param commentStatus the commentStatus to set
	 */
	public void setCommentStatus(String commentStatus) {
		this.commentStatus = commentStatus;
	}
	/**
	 * @return the pingStatus
	 */
	public String getPingStatus() {
		return pingStatus;
	}
	/**
	 * @param pingStatus the pingStatus to set
	 */
	public void setPingStatus(String pingStatus) {
		this.pingStatus = pingStatus;
	}
	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}
	/**
	 * @param password the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the toPing
	 */
	public String getToPing() {
		return toPing;
	}
	/**
	 * @param toPing the toPing to set
	 */
	public void setToPing(String toPing) {
		this.toPing = toPing;
	}
	/**
	 * @return the pinged
	 */
	public String getPinged() {
		return pinged;
	}
	/**
	 * @param pinged the pinged to set
	 */
	public void setPinged(String pinged) {
		this.pinged = pinged;
	}
	/**
	 * @return the guid
	 */
	public String getGuid() {
		return guid;
	}
	/**
	 * @param guid the guid to set
	 */
	public void setGuid(String guid) {
		this.guid = guid;
	}
	/**
	 * @return the menuOrder
	 */
	public Integer getMenuOrder() {
		return menuOrder;
	}
	/**
	 * @param menuOrder the menuOrder to set
	 */
	public void setMenuOrder(Integer menuOrder) {
		this.menuOrder = menuOrder;
	}
	/**
	 * @return the mimeType
	 */
	public String getMimeType() {
		return mimeType;
	}
	/**
	 * @param mimeType the mimeType to set
	 */
	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}
	/**
	 * @return the commentCount
	 */
	public Long getCommentCount() {
		return commentCount;
	}
	/**
	 * @param commentCount the commentCount to set
	 */
	public void setCommentCount(Long commentCount) {
		this.commentCount = commentCount;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return Post.class.getName();
	}
	/*****************************/
	/**		Object Functions	**/
	/*****************************/
	@Override
	public int hashCode() {
		return Objects.hashCode(id, parent, title, name, password);
	}
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Post) {
			Post other = (Post) obj;
			return (Objects.equal(this.title, other.title)
					&& Objects.equal(this.name, other.name)
					&& Objects.equal(this.password, other.password)
					&& (this.id == other.id)
					&& (this.parent == other.parent));
		}
		return false;
	}
	/*****************************/
	/**		Parcel Functions	**/
	/*****************************/
	/**
	 * Parcel Constructor
	 */
	public Post(Parcel parcel) {
		//this.id = parcel.readLong();
		this.parent = parcel.readLong();
		this.author = parcel.readLong();
		this.content = parcel.readString();
		this.contentFiltered = parcel.readString();
		this.title = parcel.readString();
		this.excerpt = parcel.readString();
		this.status = parcel.readString();
		this.commentStatus = parcel.readString();
		this.pingStatus = parcel.readString();
		this.password= parcel.readString();
		this.name = parcel.readString();
		this.toPing = parcel.readString();
		this.pinged = parcel.readString();
		this.guid = parcel.readString();
		this.menuOrder = parcel.readInt();
		this.mimeType = parcel.readString();
		this.commentCount = parcel.readLong();
	}
	/**
	 * Parcelable: describeContents
	 * return 0
	 */
	@Override
	public int describeContents() {
		return 0;
	}
	/**
	 * Parcelable: writeToParcel
	 */
	@Override
	public void writeToParcel(Parcel parcel, int flags) {
		//parcel.writeLong(parent);
		parcel.writeLong(author);
		parcel.writeString(content);
		parcel.writeString(contentFiltered);
		parcel.writeString(title);
		parcel.writeString(excerpt);
		parcel.writeString(status);
		parcel.writeString(commentStatus);
		parcel.writeString(pingStatus);
		parcel.writeString(password);
		parcel.writeString(name);
		parcel.writeString(toPing);
		parcel.writeString(pinged);
		parcel.writeString(guid);
		parcel.writeInt(menuOrder);
		parcel.writeString(mimeType);
		parcel.writeLong(commentCount);
	}
	/**
	 * Parcelable: Creator
	 */
	public static Creator<Post> CREATOR = new Creator<Post>() {
		@Override
		public Post createFromParcel(Parcel source) {
			return new Post(source);
		}

		@Override
		public Post[] newArray(int size) {
			return new Post[size];
		}
		
	};
}