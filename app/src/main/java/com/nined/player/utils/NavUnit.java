/**
 *
 * @author Aekasitt Guruvanich, 9D Tech
 *
 */

package com.nined.player.utils;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Aekasitt on 7/17/2015.
 */
public class NavUnit implements Parcelable {
    private int group, child;
    public NavUnit() {};
    public NavUnit(int group, int child) {
        this.group = group;
        this.child = child;
    }
    public NavUnit(Parcel parcel) {
        this.group = parcel.readInt();
        this.child = parcel.readInt();
    }

    /**
     * @return the group
     */
    public int getGroup() {
        return group;
    }

    /**
     * @param group to be set
     */
    public void setGroup(int group) {
        this.group = group;
    }

    /**
     * @return the child
     */
    public int getChild() {
        return child;
    }

    /**
     * @param child to be set
     */
    public void setChild(int child) {
        this.child = child;
    }

    @Override
    public int describeContents() {
        return 0;
    }
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.group);
        dest.writeInt(this.child);
    }
    /**
     * Parcelable: CREATOR
     */
    public static Parcelable.Creator<NavUnit> CREATOR = new Parcelable.Creator<NavUnit>() {
        @Override
        public NavUnit createFromParcel(Parcel parcel) {
            return new NavUnit(parcel);
        }

        @Override
        public NavUnit[] newArray(int size) {
            return new NavUnit[size];
        }
    };
}
