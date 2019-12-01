package com.bluegartr.bgchat.model;

import android.os.Parcel;
import android.os.Parcelable;

import sfs2x.client.entities.User;

/**
 * Created by tyang on 8/29/2014.
 */
public class ChatUser implements Parcelable {
    private String name;
    private int group;
    private boolean isAdmin;
    private boolean isItMe;

    public ChatUser(User user) {
        this.name = user.getName();
        this.group = user.getVariable("usergroup").getIntValue();
        this.isAdmin = user.isAdmin();
        this.isItMe = user.isItMe();
    }

    public ChatUser(Parcel in) {
        String[] data = new String[4];
        in.readStringArray(data);

        this.name = data[0];
        this.group = Integer.parseInt(data[1]);
        this.isAdmin = Boolean.parseBoolean(data[2]);
        this.isItMe = Boolean.parseBoolean(data[3]);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getGroup() {
        return group;
    }

    public void setGroup(int group) {
        this.group = group;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean isAdmin) {
        this.isAdmin = isAdmin;
    }

    public boolean isItMe() {
        return isItMe;
    }

    public void setItMe(boolean isItMe) {
        this.isItMe = isItMe;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int flags) {
        parcel.writeStringArray(new String[] {
                this.name,
                Integer.toString(this.group),
                Boolean.toString(this.isAdmin),
                Boolean.toString(this.isItMe)
        });
    }

    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public ChatUser createFromParcel(Parcel in) {
            return new ChatUser(in);
        }

        public ChatUser[] newArray(int size) {
            return new ChatUser[size];
        }
    };
}
