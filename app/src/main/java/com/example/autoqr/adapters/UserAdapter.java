package com.example.autoqr.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

import com.example.autoqr.R;
import com.example.autoqr.models.User;

import java.util.List;

public class UserAdapter extends BaseAdapter {

    private Context context;
    private List<User> users;
    private LayoutInflater inflater;
    private UserActionListener listener;

    public interface UserActionListener {
        void onEditUser(User user);
        void onDeleteUser(User user);
        void onToggleUserStatus(User user);
    }

    public UserAdapter(Context context, List<User> users, UserActionListener listener) {
        this.context = context;
        this.users = users;
        this.listener = listener;
        this.inflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return users.size();
    }

    @Override
    public Object getItem(int position) {
        return users.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_user, parent, false);
            holder = new ViewHolder();
            holder.tvName = convertView.findViewById(R.id.tvName);
            holder.tvUserId = convertView.findViewById(R.id.tvUserId);
            holder.switchActive = convertView.findViewById(R.id.switchActive);
            holder.btnEdit = convertView.findViewById(R.id.btnEdit);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        User user = users.get(position);

        holder.tvName.setText(user.getName());
        holder.tvUserId.setText("ID: " + user.getUserId());
        holder.switchActive.setChecked(user.isActive());

        // Set switch listener
        holder.switchActive.setOnCheckedChangeListener(null);
        holder.switchActive.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleUserStatus(user);
            }
        });

        // Set button listeners
        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEditUser(user);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDeleteUser(user);
            }
        });

        float alpha = user.isActive() ? 1.0f : 0.6f;
        holder.tvName.setAlpha(alpha);
        holder.tvUserId.setAlpha(alpha);

        return convertView;
    }

    private static class ViewHolder {
        TextView tvName;
        TextView tvUserId;
        Switch switchActive;
        Button btnEdit;
        Button btnDelete;
    }

    public void updateUsers(List<User> newUsers) {
        this.users = newUsers;
        notifyDataSetChanged();
    }
}