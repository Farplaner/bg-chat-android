package com.bluegartr.bgchat.model;

import java.util.Comparator;

/**
 * Created by tyang on 8/1/2014.
 */
public class UserComparator implements Comparator<ChatUser> {
    @Override
    public int compare(ChatUser user1, ChatUser user2) {
        int usergroup1 = user1.getGroup();
        int usergroup2 = user2.getGroup();

        if (usergroup1 == UserGroup.ADMINISTRATOR && usergroup2 == UserGroup.ADMINISTRATOR) {
            return user1.getName().compareToIgnoreCase(user2.getName());
        }

        if (usergroup1 == UserGroup.ADMINISTRATOR) {
            return -1;
        }

        if (usergroup2 == UserGroup.ADMINISTRATOR) {
            return 1;
        }

        if (usergroup1 == UserGroup.SUPER_MODERATOR && usergroup2 == UserGroup.SUPER_MODERATOR) {
            return user1.getName().compareToIgnoreCase(user2.getName());
        }

        if (usergroup1 == UserGroup.SUPER_MODERATOR) {
            return -1;
        }

        if (usergroup2 == UserGroup.SUPER_MODERATOR) {
            return 1;
        }

        if (usergroup1 == UserGroup.MODERATOR && usergroup2 == UserGroup.MODERATOR) {
            return user1.getName().compareToIgnoreCase(user2.getName());
        }

        if (usergroup1 == UserGroup.MODERATOR) {
            return -1;
        }

        if (usergroup2 == UserGroup.MODERATOR) {
            return 1;
        }

        return user1.getName().compareToIgnoreCase(user2.getName());
    }
}
