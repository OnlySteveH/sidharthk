package uk.co.bigdogconsultants.ribbit.ui;

import java.util.ArrayList;
import java.util.List;

import uk.co.bigdogconsultants.ribbit.R;
import uk.co.bigdogconsultants.ribbit.adapters.MessageAdapter;
import uk.co.bigdogconsultants.ribbit.utilities.ParseConstants;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;

public class InboxFragment extends ListFragment {
	
	protected List<ParseObject> mMessages;
	protected SwipeRefreshLayout mSwipeRefreshLayout;
	protected ParseRelation<ParseUser> mFriendsRelation;
	protected ParseUser mCurrentUser;
	
	@SuppressWarnings("deprecation")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View rootView = inflater.inflate(R.layout.fragment_inbox, container, false);
		mSwipeRefreshLayout = (SwipeRefreshLayout)rootView.findViewById(R.id.swipeRefreshLayout);
		mSwipeRefreshLayout.setOnRefreshListener(mOnRefreshListener);
		mSwipeRefreshLayout.setColorScheme(R.color.swipe_refresh_1, 
				R.color.swipe_refresh_2, 
				R.color.swipe_refresh_3, 
				R.color.swipe_refresh_4);
		
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		getActivity().setProgressBarIndeterminate(true);
		retrieveMessages();
	}

	private void retrieveMessages() {
		mCurrentUser = ParseUser.getCurrentUser(); // added
		mFriendsRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION); // added
		ParseQuery<ParseUser> relationQuery = mFriendsRelation.getQuery();
		//relationQuery.addAscendingOrder(ParseConstants.KEY_USERNAME);


		// build query
		ParseQuery<ParseObject> query = new ParseQuery<ParseObject>(ParseConstants.CLASS_MESSAGES);
		query.whereEqualTo(ParseConstants.KEY_RECIPIENT_IDS, ParseUser.getCurrentUser().getObjectId());
		query.whereMatchesQuery(ParseConstants.KEY_FRIENDS_RELATION, relationQuery);

		// original code
		query.addDescendingOrder(ParseConstants.KEY_CREATED_AT);
		query.findInBackground(new FindCallback<ParseObject>() {
			
			@Override
			public void done(List<ParseObject> messages, ParseException e) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				
				if (mSwipeRefreshLayout.isRefreshing()) {
					mSwipeRefreshLayout.setRefreshing(false);
				}
				
				if(e == null) {
					// we found messages
					mMessages = messages;
					String[] usernames = new String[mMessages.size()];
					int i = 0;
					for(ParseObject message : mMessages) {
						usernames[i] = message.getString(ParseConstants.KEY_SENDER_NAME);
						i++;
					}
					if(getListView().getAdapter() == null) {
						MessageAdapter adapter = new MessageAdapter(
								getListView().getContext(), 
								mMessages);
						setListAdapter(adapter);
					}
					else {
						// refresh the adapter
						((MessageAdapter)getListView().getAdapter()).refill(mMessages);
					}
				}
				
			}
		});
	}
	
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		
		super.onListItemClick(l, v, position, id);
		
		ParseObject message = mMessages.get(position);
		String messageType = message.getString(ParseConstants.KEY_FILE_TYPE);
		ParseFile file = message.getParseFile(ParseConstants.KEY_FILE);
		Uri fileUri = Uri.parse(file.getUrl());
		
		if(messageType.equals(ParseConstants.TYPE_IMAGE)) {
			// view image
			Intent intent = new Intent(getActivity(), ViewImageActivity.class);
			intent.setData(fileUri);
			startActivity(intent);
			
		}	
		else {
			// view video
			Intent intent = new Intent(Intent.ACTION_VIEW, fileUri);
			intent.setDataAndType(fileUri, "video/*");
			startActivity(intent);
		}
		// delete
		List<String> ids = message.getList(ParseConstants.KEY_RECIPIENT_IDS);
		
		if(ids.size() == 1) {
			//delete all
			message.deleteInBackground();
		}
		else {
			// delete user & save
			ids.remove(ParseUser.getCurrentUser().getObjectId());
			ArrayList<String> idsToRemove = new ArrayList<String>();
			idsToRemove.add(ParseUser.getCurrentUser().getObjectId());
			message.removeAll(ParseConstants.KEY_RECIPIENT_IDS, idsToRemove);
			message.saveInBackground();
			
		}
		
				
	}
	protected OnRefreshListener mOnRefreshListener = new OnRefreshListener(){
		
		public void onRefresh() {
			Toast.makeText(getActivity(), "We're refreshing!", Toast.LENGTH_SHORT).show();
			retrieveMessages();
		}
	};
	
}
