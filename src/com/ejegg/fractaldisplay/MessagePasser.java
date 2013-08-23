package com.ejegg.fractaldisplay;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.util.Log;

public class MessagePasser {

	private HashMap<MessageType, List<WeakReference<MessageListener>>> listeners = new HashMap<MessageType, List<WeakReference<MessageListener>>>(); 
	
	public enum MessageType {
		EDIT_MODE_CHANGED,
		SCALE_MODE_CHANGED,
		STATE_CHANGED,
		STATE_CHANGING,
		UNDO_ENABLED_CHANGED,
		NEW_POINTS_AVAILABLE,
		RENDER_MODE_CHANGED,
		SCREEN_TOUCHED,
		CAMERA_MOTION_CHANGED,
		ACCUMULATION_MOTION_CHANGED,
		CAMERA_MOVED,
	}
	
	public interface MessageListener {
		void ReceiveMessage(MessageType type, boolean value);
	}
	
	public synchronized void SendMessage(MessageType type, boolean value) {
		Log.d("MessagePasser", "Got message of type " + type + ", value " + value);
		List<WeakReference<MessageListener>> list = listeners.get(type);
		if (list == null) return;
		int size = list.size();
		if (list == null || size == 0) {
			return;
		}
		//List<WeakReference<MessageListener>> toRemove = new ArrayList<WeakReference<MessageListener>>();
		Log.d("MessagePasser", "Sending message to " + list.size() + " listeners");
		
		//Stack<Integer> toRemove = new Stack<Integer>();
		
		for (int i = size - 1; i >= 0; i-- ) {
			MessageListener listener = list.get(i).get();
			if (listener == null) {
				list.remove(i);
				Log.d("MessagePasser", "Removing garbage collected listener for " + type + " at index " + i);
			} else {
				listener.ReceiveMessage(type, value);
			}
		}
		
		/*for (WeakReference<MessageListener> maybeListener : list) {
			MessageListener listener = maybeListener.get();
			if (listener == null) {
				toRemove.add(maybeListener);
			} else {
				listener.ReceiveMessage(type, value);
			}
		}
		
		for (WeakReference<MessageListener> deadListener : toRemove) {
			list.remove(deadListener);
			Log.d("MessagePasser", "Removing garbage collected listener for " + type);
		}*/
	}
	
	public synchronized void Subscribe(MessageListener listener, MessageType... messageTypes) {
		Log.d("MessagePasser", "Adding listener of type " +  listener.getClass().getCanonicalName());
		for ( MessageType type : messageTypes) {
			List<WeakReference<MessageListener>> list = listeners.get(type);
			if (list == null) {
				list = new ArrayList<WeakReference<MessageListener>>();;
				listeners.put(type, list);
			}
			Log.d("MessagePasser", "For message type " + type + ", listener is at index " + list.size());			
			list.add(new WeakReference<MessageListener>(listener) );
		}
	}
}
