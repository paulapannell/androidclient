/*
 * Kontalk Android client
 * Copyright (C) 2016 Kontalk Devteam <devteam@kontalk.org>

 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.kontalk.ui.adapter;

import java.util.regex.Pattern;

import org.kontalk.R;
import org.kontalk.message.CompositeMessage;
import org.kontalk.message.GroupCommandComponent;
import org.kontalk.ui.view.AudioPlayerControl;
import org.kontalk.ui.ComposeMessage;
import org.kontalk.ui.view.MessageListItem;
import org.kontalk.util.MessageUtils;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.RecyclerListener;
import android.widget.ListView;


public class MessageListAdapter extends CursorAdapter {

    private static final String TAG = ComposeMessage.TAG;

    private final LayoutInflater mFactory;
    private final Pattern mHighlight;
    private OnContentChangedListener mOnContentChangedListener;

    private AudioPlayerControl mAudioPlayerControl;

    public MessageListAdapter(Context context, Cursor cursor, Pattern highlight, ListView list, AudioPlayerControl audioPlayerControl) {
        super(context, cursor, false);
        mFactory = LayoutInflater.from(context);
        mHighlight = highlight;
        mAudioPlayerControl = audioPlayerControl;

        list.setRecyclerListener(new RecyclerListener() {
            public void onMovedToScrapHeap(View view) {
                if (view instanceof MessageListItem) {
                    ((MessageListItem) view).unbind();
                }
            }
        });
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        if (!(view instanceof MessageListItem)) {
            Log.e(TAG, "unexpected bound view: " + view);
            return;
        }

        MessageListItem headerView = (MessageListItem) view;
        CompositeMessage msg = CompositeMessage.fromCursor(context, cursor);

        long previous = -1;
        if (cursor.moveToPrevious()) {
            previous = MessageUtils.getMessageTimestamp(cursor);
            cursor.moveToNext();
        }

        headerView.bind(context, msg, mHighlight, previous, mAudioPlayerControl);
    }

    private boolean isEvent(Cursor cursor) {
        String mime = cursor.getString(CompositeMessage.COLUMN_BODY_MIME);
        return (GroupCommandComponent.supportsMimeType(mime));
    }

    @Override
    public int getItemViewType(int position) {
        Cursor c = (Cursor) getItem(position);
        int type = c.getInt(CompositeMessage.COLUMN_DIRECTION);
        // MyMessages.DIRECTION_* OR-ed with 2 for group events
        if (isEvent(c))
            type |= 2;
        return type;
    }

    @Override
    public int getViewTypeCount() {
        // incoming (0), incoming event (2), outgoing (1), outgoing event (3)
        // MyMessages.DIRECTION_* OR-ed with 2
        return 4;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int type = cursor.getInt(CompositeMessage.COLUMN_DIRECTION);
        boolean event = isEvent(cursor);
        MessageListItem view = (MessageListItem) mFactory
            .inflate(R.layout.message_list_item, parent, false);
        view.afterInflate(type, event);
        return view;
    }

    public interface OnContentChangedListener {
        void onContentChanged(MessageListAdapter adapter);
    }

    public void setOnContentChangedListener(OnContentChangedListener l) {
        mOnContentChangedListener = l;
    }

    @Override
    protected void onContentChanged() {
        Cursor c = getCursor();
        if (c != null && !c.isClosed() && mOnContentChangedListener != null) {
            mOnContentChangedListener.onContentChanged(this);
        }
    }
}
