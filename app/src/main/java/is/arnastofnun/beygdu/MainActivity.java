package is.arnastofnun.beygdu;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.software.shell.fab.ActionButton;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import is.arnastofnun.AsyncTasks.BinAsyncTask;
import is.arnastofnun.AsyncTasks.SkrambiAsyncTask;
import is.arnastofnun.BeygduTutorial.TutorialActivity;
import is.arnastofnun.DB.DBController;
import is.arnastofnun.parser.WordResult;
import is.arnastofnun.utils.CustomDialog;
import is.arnastofnun.utils.InputValidator;
import is.arnastofnun.utils.NetworkStateListener;
import is.arnastofnun.utils.NotificationDialog;


/**
 * @author Jón Friðrik, Arnar, Snær, Máni
 * @since 05.11.14
 * @version 1.0
 * 
 * The initial activity of the program. Has a Text input og a button for initializing the search.
 * Also has an actionbar where the user can open other activities such as AboutActivity
 * and send an email to the creator of the database
 * 
 */
public class MainActivity extends NavDrawer implements CustomDialog.DialogListener, View.OnKeyListener {

    /**
     * Progress dialog to be used in Async Task
     */
    ProgressDialog progressDialog;

	/**
	 * The result from the parser search.
     * ---Depricated
	 */

	//public ParserResult pR = new ParserResult();

    /**
     *  Errorsymbols for the inputvalidator
     */
    private String INPUTVALIDATOR_ERRORSYMBOLS = "1234567890!@#$%^&*()_+=-[]{}|?><.,:;";

    /**
     * string containing the last searched word, removes the pain of NullPointerException handling
     */
    private String lastSearchedWord = "";

    private void setLastSearchedWord(String str) {
        this.lastSearchedWord = str;
    }
    /**
     * The WordResult Document, containing all data on searched word
     */
    public WordResult wR;

	public void setWordResult(WordResult wordResult) {
		this.wR = wordResult;
        checkWordCount();
	}

    /**
     * LatoBold - default font bold
     * LatoSemiBold - default font semibold
     * LatoLight - default font
     * width - sceen width
     * height - screen height
     */
    //Fonts
    private Typeface LatoBold;
    private Typeface LatoSemiBold;
    private Typeface LatoLight;

    //Screen width
    private float width;
    private float height;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        /**
         * Not setting the content view here since we are
         * inflating it in the NavDrawer (see below)
         */
        // setContentView(R.layout.activity_main);

		/**
        * Inflate the layout into the NavDrawer layout
        * where `frameLayout` is a FrameLayout in the layout for the
        * NavDrawer (see file nav_base_layout)
        */
        getLayoutInflater().inflate(R.layout.activity_main, frameLayout);

        /**
         * Setting the title and what item is checked
         */
        mDrawerList.setItemChecked(position,true);
        setTitle(R.string.app_name);

        getActionBar().setDisplayHomeAsUpEnabled(false);

		checkNetworkState();
        /**
         * Animate header text and set it's size
         */
        headerText();

        /**
         * Set the key handler on the edit text box to capture the Enter key event
         */
        EditText editText = (EditText) findViewById(R.id.mainSearch);
        editText.setOnKeyListener(this);
        /*
        Button b = (Button) findViewById(R.id.tutbutton);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), TutorialActivity.class);
                startActivity(intent);
            }
        });
        */
	}

    /**
     * @author Snær Seljan
     * @since 22.03.15
     * @version 2.0
     *
     * This method changes text size depending on screen sizes
     */

    public void headerText() {
        //Set typeface for fonts
        LatoLight = Typeface.createFromAsset(getAssets(), "fonts/Lato-Light.ttf");

        ActionButton circlestar = (ActionButton) findViewById(R.id.circleStar);
        TextView header = (TextView)findViewById(R.id.title);
        TableRow rowSearch = (TableRow) findViewById(R.id.search_row);

        TextView copyright = (TextView)findViewById(R.id.copyright);
        copyright.setTypeface(LatoLight);

        header.setTypeface(LatoLight);


        Animation animateFromTop = AnimationUtils.loadAnimation(this, R.animator.animator);
        Animation fadeIn = AnimationUtils.loadAnimation(this, R.animator.fadein);
        Animation openScale = AnimationUtils.loadAnimation(this, R.animator.activity_open_scale);
        rowSearch.startAnimation(fadeIn);
        header.startAnimation(animateFromTop);
        circlestar.playShowAnimation();
    }


    /*public void animationTest(TextView name) {
        AnimatorSet set = (AnimatorSet) AnimatorInflater.loadAnimator(this, R.animator.animator);
        set.setTarget(header);
        set.start();
    }*/

    /**
     * This method converts device specific pixels to density independent pixels.
     * @param px A value in px (pixels) unit. Which we need to convert into db
     * @return A float value to represent dp equivalent to px value
     * Snær Seljan
     */
    public float convertPixelsToDp(float px){
        Resources resources = this.getResources();
        DisplayMetrics metrics = resources.getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160f);
        return dp;
    }


    public int getScreenWidth() {
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        return width;
    }

	/**
	 * Checks if the user is connected to a network.
	 * TODO - Should be implemented so that it shows a dialog if 
	 * the user is not connected
	 */
	private void checkNetworkState() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		
		NetworkInfo ni = cm.getActiveNetworkInfo();
		if (ni == null) {
			Toast.makeText(MainActivity.this,
					"Þú ert ekki nettengdur.", Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
//		MenuInflater inflater = getMenuInflater();
//		inflater.inflate(R.menu.main, menu);
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * @return item 
	 *  a switch for all the possibilies in the ActionBar. 
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        /**
         *
        switch (item.getItemId()) {
		case R.id.action_about:
			Intent intent1 = new Intent(this, AboutActivity.class);
			startActivity(intent1);
			break;
		case R.id.action_mail:
			sendEmail();
			break;
		}
         */
		return super.onOptionsItemSelected(item);
	}
	
	
	/**
	 * Send a post to sth132@hi.is
	 */
	protected void sendEmail() {
		Log.i("Senda post", "");
		String[] TO = {"sth132@hi.is"};
		String[] CC = {"sth132@hi.is"};
		Intent emailIntent = new Intent(Intent.ACTION_SEND);
		emailIntent.setData(Uri.parse("mailto:"));
		emailIntent.setType("text/plain");
		emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
		emailIntent.putExtra(Intent.EXTRA_CC, CC);
		emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Þitt vidfang");
		emailIntent.putExtra(Intent.EXTRA_TEXT, "Skilabod her");
		try {
			startActivity(Intent.createChooser(emailIntent, "Sendu post....."));
			finish();
			Log.i("Buin ad senda post...", "");
		} catch (android.content.ActivityNotFoundException ex) {
			Toast.makeText(MainActivity.this,
					"Engin póst miðill uppsettur í þessu tæki.", Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * @param view the view points to this activity. 
	 * Runs when the user pushes the search button
	 * Constructs a new Async thread which fetches the results for the searchString which was inputted by the user.
	 * If nothing or more than one word was inputted the user gets a Toast notification.
	 * The input can have 1 or two space characters infront and behind it.
	 */
	public void btnOnClick(@SuppressWarnings("unused") View view){
		EditText editText = (EditText) findViewById(R.id.mainSearch);
		String word = editText.getText().toString();
		
		if(word.isEmpty())  {
			//Toast.makeText(this, "Vinsamlegasta sláið inn orð í reitinn hér að ofan", Toast.LENGTH_SHORT).show();
            Bundle bundle = new Bundle();
            bundle.putBoolean("isError", true);
            bundle.putString("message", "Vinsamlegasta sláið inn leitarorð");
            android.app.DialogFragment errorDialog = new NotificationDialog();
            errorDialog.setArguments(bundle);
            errorDialog.show(getFragmentManager(), "0");
		}
        else {
            setLastSearchedWord(word);
            InputValidator iValidator = new InputValidator(word, INPUTVALIDATOR_ERRORSYMBOLS);
            if(iValidator.isLegal()) {
                try {
                    setWordResult(new BinAsyncTask(MainActivity.this)
                            .execute(word, "1").get());

                }
                catch (Exception e) {
                    setWordResult(null);
                    //TODO : Errorhandling, Edit : taken care of in checkWordCount
                    //Toast.makeText(MainActivity.this, "Ekki tokst ad na sambandi vid bin", Toast.LENGTH_SHORT).show();
                }
            }
            else {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isError", true);
                bundle.putString("message", iValidator.getErrorCode());
                android.app.DialogFragment errorDialog = new NotificationDialog();
                errorDialog.setArguments(bundle);
                errorDialog.show(getFragmentManager(), "0");
            }
        }


	}
    public void cacheClick(@SuppressWarnings("unused") View view){
        Intent intent = new Intent(MainActivity.this, Cache.class);
        startActivity(intent);
        overridePendingTransition(R.animator.activity_open_scale,R.animator.activity_close_translate);
    }


    public void statisticsClick(@SuppressWarnings("unused") View view){
        Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
        startActivity(intent);
        overridePendingTransition(R.animator.activity_open_scale,R.animator.activity_close_translate);
    }

    public void googleClick(@SuppressWarnings("unused")  View view) {
        Intent intent = new Intent(MainActivity.this, MapsActivity.class);
        startActivity(intent);
    }

    public void authorClick(@SuppressWarnings("unused")  View view) {
        Intent intent = new Intent(MainActivity.this, AuthorActivity.class);
        startActivity(intent);
    }

    public void phoneClick(@SuppressWarnings("unused")  View view) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:8666665"));
        startActivity(callIntent);
    }






	
	/**
	 * @param word the searchword
	 * @return word converted to UTF-8
	 */
	private String convertToUTF8(String word) {
		try {
			word = URLEncoder.encode(word, "UTF-8");
			return word;
		}
		catch( UnsupportedEncodingException e ) {
			return "";
		}
	}
	
	/**
	 * 
	 * @param a the string
	 * @return the string without spacecharactes (" ")
	 */
	private boolean islegalInput(String a) {
        /* TODO: FIX THIS
	    if (a.equals("")) {
	    	return false;
	    } else {
			ArrayList<Integer> starts = new ArrayList<Integer>();  
		    Pattern pattern = Pattern.compile("\\s");
		    Matcher matcher = pattern.matcher(a);
		    
		    while( matcher.find() ) {
		      starts.add(matcher.start());
		    }
		    if( starts.get(0) == a.length()-1 || (starts.get(0) == 0 && starts.size() == 1) ||
		      (starts.get(0) == 0 && starts.get(1) == a.length()-1 && starts.size() == 2) ) {
		      return true;
		    } 
		    return false;
	    }
	    */
        return true;
        
	  }
	
	private String replaceSpaces(String a) {
	    return a.replaceAll("\\s+","");
	  }

    private String[] intArrayToStringArray(int[] id) {
        String[] returnArray = new String[id.length];

        for(int i = 0; i < id.length; i++) {
            returnArray[i] = Integer.toString(id[i]);
        }

        return returnArray;
    }

	/**
	 * sees if the results are:
	 * <strong>Partial hit: </strong> many words with the different meaning spelled the same way.
	 * <strong>Critical hit: </strong> the word is found and the results have been set in WordResults
	 * Or no result.
	 */
	private void checkWordCount() {

        if(this.wR == null) {
            if(!new NetworkStateListener(this).isConnectionActive()) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isError", true);
                bundle.putString("message", "Beygðu nær ekki sambandi við veraldarvefinn");
                android.app.DialogFragment errorDialog = new NotificationDialog();
                errorDialog.setArguments(bundle);
                errorDialog.show(getFragmentManager(), "0");
            }
            else {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isError", true);
                bundle.putString("message", "Beygðu nær ekki sambandi við gagnagrunn Árnastofnunnar");
                android.app.DialogFragment errorDialog = new NotificationDialog();
                errorDialog.setArguments(bundle);
                errorDialog.show(getFragmentManager(), "0");
            }
            return;
        }

        try {
            DBController controller = new DBController(MainActivity.this);
            if(controller.fetchObeygjanlegt(wR.getSearchWord()) != null) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isError", false);
                bundle.putString("message", "Orðið " + wR.getSearchWord() + " er óbeygjanlegt");
                android.app.DialogFragment notiDialog = new NotificationDialog();
                notiDialog.setArguments(bundle);
                notiDialog.show(getFragmentManager(), "0");
                return;
            }

        }
        catch (Exception e) {
            // Do Nothing
        }


        String desc = this.wR.getDescription();

        if(desc.equals("SingleHit")) {
            WordResult word = this.wR;
            createNewActivity(word);
            return;
        }
        else if(desc.equals("MultiHit")) {
            Bundle bundle = new Bundle();
            bundle.putInt("id", 0);
            bundle.putString("title", wR.getMultiHitDescriptions().length + " " + getString(R.string.MultiHitDialog));
            bundle.putString("positiveButtonText", getString(R.string.PositiveButton));
            bundle.putString("negativeButtonText", getString(R.string.NegativeButton));
            bundle.putStringArray("descriptions", wR.getMultiHitDescriptions());
            bundle.putStringArray("descriptionActions", intArrayToStringArray(wR.getMultiHitIds()));
            android.app.DialogFragment multiDialog = new CustomDialog();
            multiDialog.setArguments(bundle);
            multiDialog.show(getFragmentManager(), "0");
            return;
        }
        else {
            String[] correctedWords;
            try {
                correctedWords = new SkrambiAsyncTask(this).execute(wR.getSearchWord()).get();
                if( correctedWords[0] != null ) {
                    Bundle bundle = new Bundle();
                    bundle.putInt("id", 1);
                    bundle.putString("title", getString(R.string.SkrambiDialog));
                    bundle.putString("positiveButtonText", getString(R.string.PositiveButton));
                    bundle.putString("negativeButtonText", getString(R.string.NegativeButton));
                    bundle.putStringArray("descriptions", correctedWords);
                    bundle.putStringArray("descriptionActions", correctedWords);
                    android.app.DialogFragment multiDialog = new CustomDialog();
                    multiDialog.setArguments(bundle);
                    multiDialog.show(getFragmentManager(), "0");
                    return;
                }
                // TODO : REMOVE DEBUG STATEMENT
                else {
                    Bundle bundle = new Bundle();
                    bundle.putBoolean("isError", true);
                    bundle.putString("message", correctedWords[0]);
                    android.app.DialogFragment notiDialog = new NotificationDialog();
                    notiDialog.setArguments(bundle);
                    notiDialog.show(getFragmentManager(), "0");
                    return;
                }
            }
            catch (Exception e) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("isError", false);
                bundle.putString("message", "Engar leitarniðurstöður fundust");
                android.app.DialogFragment notiDialog = new NotificationDialog();
                notiDialog.setArguments(bundle);
                notiDialog.show(getFragmentManager(), "0");
                return;
            }

        }

	}

	/**
	 * 
	 * @param word the WordResult
	 * Constructs a new activity with the result.
	 */
	private void createNewActivity(WordResult word) {
		Intent intent = new Intent(this, BeygingarActivity.class);
		intent.putExtra("word", word);
		startActivity(intent);
        overridePendingTransition(R.animator.activity_open_scale,R.animator.activity_close_translate);
	}

    private void manageDialogFragmentOutput(String word) {
        if(islegalInput(word)) {
            if(word.contains(" ")) {
                word = replaceSpaces(word);
            }
            word = convertToUTF8(word);
            BinHelper bHelper = new BinHelper(MainActivity.this);
            this.wR = bHelper.sendThread(word, 0);
            if(this.wR != null) {
                checkWordCount();
            }
            else {
                Log.w("app", "wR is null");
                Toast.makeText(getApplicationContext(), "Villa i manageDialogFragmentOutput", Toast.LENGTH_SHORT).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Engin leit fannst", Toast.LENGTH_SHORT).show();
        }
    }

    // INTERFACE HANDLER FOR CUSTOMDIALOG RESULTS
    @Override
    public void onPositiveButtonClick(String selectedItem, int id) {

        switch (id) {
            case 0:
                try {
                    setWordResult(new BinAsyncTask(MainActivity.this).execute(selectedItem, "0").get());
                }
                catch (Exception e) {
                    setWordResult(null);
                }
            case 1:
                try {
                    setWordResult(new BinAsyncTask(MainActivity.this).execute(selectedItem, "1").get());
                }
                catch (Exception f) {
                    setWordResult(null);
                }
            default:
                // Do Nothing

        }

        BinHelper binHelper = new BinHelper(MainActivity.this);
        try {
            setWordResult(binHelper.sendThread(selectedItem, 1));
        }
        catch (Exception e) {
            Log.w("Exception", e);
            Toast.makeText(MainActivity.this, "Engin leit fannst", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNegativeButtonClick(String errorString) {

    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction()==KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
            btnOnClick(v);
            return true;
        }
        return false;
    }
}