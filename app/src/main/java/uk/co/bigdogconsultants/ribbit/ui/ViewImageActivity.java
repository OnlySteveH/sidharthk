package uk.co.bigdogconsultants.ribbit.ui;

import java.util.Timer;
import java.util.TimerTask;

import uk.co.bigdogconsultants.ribbit.R;
import android.app.Activity;
import android.app.Fragment;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;

public class ViewImageActivity extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_view_image);
		setupActionBar();
		
		ImageView imageView = (ImageView)findViewById(R.id.imageView); 
		Uri imageUri = getIntent().getData();
		Picasso.with(this).load(imageUri.toString()).into(imageView);
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				finish();
				
			}
		}, 20*1000);
		
	}
	
	private void setupActionBar() {
		getActionBar().setDisplayHomeAsUpEnabled(true);
	}

	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
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
			View rootView = inflater.inflate(R.layout.fragment_view_image,
					container, false);
			return rootView;
		}
	}

}
