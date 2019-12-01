package com.bluegartr.bgchat;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.bluegartr.bgchat.model.ChatUser;
import com.bluegartr.bgchat.utils.ChatUtils;

import java.util.List;

/**
 * Created by Daniel on 7/28/14.
 */
public class ChatUsersAdapter extends ArrayAdapter<ChatUser> {
    private Context context;
    private List<ChatUser> users;
    private int resource;
    private int textViewResourceId;

    public ChatUsersAdapter(Context context, int resource, int textViewResourceId, List<ChatUser> users) {
        super(context, resource, textViewResourceId, users);

        this.context = context;
        this.users = users;
        this.resource = resource;
        this.textViewResourceId = textViewResourceId;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(resource, parent, false);
        TextView textView = (TextView) rowView.findViewById(textViewResourceId);

        ChatUser user = users.get(position);
        textView.setText(Html.fromHtml(ChatUtils.getNameSymbol(user) + ChatUtils.getColoredName(user.getName())));

        return rowView;
    }
}
