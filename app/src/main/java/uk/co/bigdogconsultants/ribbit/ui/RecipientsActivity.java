package uk.co.bigdogconsultants.ribbit.ui;

import java.util.ArrayList;
import java.util.List;

import uk.co.bigdogconsultants.ribbit.R;
import uk.co.bigdogconsultants.ribbit.adapters.UserAdapter;
import uk.co.bigdogconsultants.ribbit.utilities.FileHelper;
import uk.co.bigdogconsultants.ribbit.utilities.ParseConstants;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.ParseQuery;
import com.parse.ParseRelation;
import com.parse.ParseUser;
import com.parse.SaveCallback;

public class RecipientsActivity extends Activity {
	
	public static final String TAG = RecipientsActivity.class.getSimpleName();
	
	protected List<ParseUser> mFriends;
	protected ParseRelation<ParseUser> mFriendsRelation; 
	protected ParseUser mCurrentUser;
	protected MenuItem mSendMenuItem;
	protected Uri mMediaUri;
	protected String mFileType;
	protected GridView mGridView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.user_grid);
		
		mGridView = (GridView)findViewById(R.id.friendsGrid);
		mGridView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		mGridView.setOnItemClickListener(mOnItemClickListener);
		
		TextView emptyTextView = (TextView)findViewById(android.R.id.empty);
		mGridView.setEmptyView(emptyTextView);
		
		mMediaUri = getIntent().getData();
		mFileType = getIntent().getExtras().getString(ParseConstants.KEY_FILE_TYPE);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recipients, menu);
		mSendMenuItem = menu.getItem(0);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		switch(item.getItemId()) {
		case R.id.action_send:
			ParseObject message = createMessage();
			if(message == null) {
				//error
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setMessage(R.string.error_selecting_file)
				.setTitle(R.string.file_error_title)
				.setPositiveButton(android.R.string.ok, null);
				AlertDialog dialog = builder.create();
				dialog.show();
			}
			else {
				send(message);
				finish();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_recipients,
					container, false);
			return rootView;
		}
	}
	
	public void onResume() {
		super.onResume();
		
		mCurrentUser = ParseUser.getCurrentUser();
		mFriendsRelation = mCurrentUser.getRelation(ParseConstants.KEY_FRIENDS_RELATION);
		
		setProgressBarIndeterminateVisibility(true);
		
		ParseQuery<ParseUser> query = mFriendsRelation.getQuery();
		query.addAscendingOrder(ParseConstants.KEY_USERNAME);
		query.findInBackground(new FindCallback<ParseUser>() {;
			@Override
			public void done(List<ParseUser> friends, ParseException e) {
				setProgressBarIndeterminateVisibility(false);
				if( e == null) {
					
					mFriends = friends;
					String[] usernames = new String[mFriends.size()];
					int i = 0;
					for(ParseUser user : mFriends) {
						usernames[i] = user.getUsername();
						i++;
					}
					if(mGridView.getAdapter() == null){
						UserAdapter adapter = new UserAdapter(RecipientsActivity.this, mFriends);
						mGridView.setAdapter(adapter);
					}
					else {
						((UserAdapter)mGridView.getAdapter()).refill(mFriends);
					}
				}
				else {
					Log.e(TAG, e.getMessage());
					AlertDialog.Builder builder = new AlertDialog.Builder(mGridView.getContext());
					builder.setMessage(e.getMessage())
						.setTitle(R.string.error_title)
						.setPositiveButton(android.R.string.ok, null);
					AlertDialog dialog = builder.create();
					dialog.show();	
				}
				
			}
			
		});
	}
	protected ParseObject createMessage() {
		ParseObject message = new ParseObject(ParseConstants.CLASS_MESSAGES);
		message.put(ParseConstants.KEY_SENDER_ID, ParseUser.getCurrentUser().getObjectId());
		message.put(ParseConstants.KEY_SENDER_NAME, ParseUser.getCurrentUser().getUsername());
		message.put(ParseConstants.KEY_RECIPIENT_IDS, getRecipientIds());
		message.put(ParseConstants.KEY_FILE_TYPE, mFileType);
		
		byte[] fileBytes = FileHelper.getByteArrayFromFile(this,  mMediaUri);
		if(fileBytes == null) {
			return null;
		}
		else {
			if(mFileType.equals(ParseConstants.TYPE_IMAGE)) {
				fileBytes = FileHelper.reduceImageForUpload(fileBytes);
			}
			String fileName = FileHelper.getFileName(this, mMediaUri, mFileType);
			ParseFile file = new ParseFile(fileName, fileBytes);
			message.put(ParseConstants.KEY_FILE, file);
		
			return message;
		}
	}
	
	protected ArrayList<String> getRecipientIds(){
		ArrayList<String> recipientIds = new ArrayList<String>();
		for(int i = 0; i < mGridView.getCount(); i++) {
			if(mGridView.isItemChecked(i)) {
				recipientIds.add(mFriends.get(i).getObjectId());
			}
		}
		return recipientIds;
	}
	
	protected void send(ParseObject message) {
		message.saveInBackground(new SaveCallback() {
			@Override
			public void done(ParseException e) {
				if(e == null) {
					//success
					Toast.makeText(RecipientsActivity.this, R.string.message_sent, Toast.LENGTH_LONG).show();
					sendPushNotifications();
				}
				else {
					AlertDialog.Builder builder = new AlertDialog.Builder(RecipientsActivity.this);
					builder.setMessage(R.string.error_sending_message)
						.setTitle(R.string.error_title)
						.setPositiveButton(android.R.string.ok, null);
					AlertDialog dialog = builder.create();
					dialog.show();
					
				}
			}
			
		});
	}
	
	protected OnItemClickListener mOnItemClickListener = new OnItemClickListener(){

		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position,
				long id) {
			if(mGridView.getCheckedItemCount() > 0) {
				
				mSendMenuItem.setVisible(true);
			}
			else {
				mSendMenuItem.setVisible(false);
			}
			
			ImageView checkImageView = (ImageView)view.findViewById(R.id.checkImageView);
			
			if(mGridView.isItemChecked(position)) {
				// add recipient
				checkImageView.setVisibility(View.VISIBLE);
			}
			else {
				// remove them
				checkImageView.setVisibility(View.INVISIBLE);
			}
			
		}
		
	};
	
	protected void sendPushNotifications(){
		ParseQuery<ParseInstallation> query = ParseInstallation.getQuery();
		query.whereContainedIn(ParseConstants.KEY_USER_ID, getRecipientIds());
		
		//  send push notification
		ParsePush push = new ParsePush();
		push.setQuery(query);
		push.setMessage(getString(R.string.push_message, 
				ParseUser.getCurrentUser().getUsername()));
		push.sendInBackground();
	}
}
