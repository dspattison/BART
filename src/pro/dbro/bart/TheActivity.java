/*
 *  Copyright (C) 2012  David Brodsky
 *	This file is part of Open BART.
 *
 *  Open BART is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Open BART is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Open BART.  If not, see <http://www.gnu.org/licenses/>.
*/


package pro.dbro.bart;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.location.Location;
import android.location.LocationManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.crittercism.app.Crittercism;


// TODO: access to recent stations

public class TheActivity extends Activity {
	static Context c;
	TableLayout tableLayout;
	LinearLayout tableContainerLayout;
	static String lastRequest="";
	Resources res;
	AutoCompleteTextView destinationTextView;
	AutoCompleteTextView originTextView;
	TextView fareTv;
	TextView stopServiceTv;
	LinearLayout infoLayout;
	
	ArrayList timerViews = new ArrayList();
	CountDownTimer timer;
	long maxTimer = 0;
	
	// route that the usher service should access
	public static route usherRoute; 
	// real time info for current station of interest in route
	// set on completion of etdresponse
	// freshness of response is available in currentEtdResponse.Date
	public static etdResponse currentEtdResponse;
	
	// time in ms to allow a currentEtdResponse to be considered 'fresh'
	private final long CURRENT_ETD_RESPONSE_FRESH_MS = 60*1000;
	
	// determines whether UI is automatically updated after api request by handleResponse(response)
	// set to false in events where a routeResponse is displayed BEFORE an etdresponse was cached
	// in currentEtdResponse.
	// etdResponse has the real-time station info, while routeResponse is based on the BART schedule
	// private boolean updateUIOnResponse = true;
	
	private SharedPreferences prefs;
	private SharedPreferences.Editor editor;
	
	public final static String BART_API_ROOT = "http://api.bart.gov/api/";
	public final static String BART_API_KEY="MW9S-E7SL-26DU-VV8V";
	
	//AutoComplete behavior on origin/destination inputs matches against these strings
	static final String[] STATIONS = new String[] {
		"12th St. Oakland City Center","16th St. Mission (SF)","19th St. Oakland",
		"24th St. Mission (SF)","Ashby (Berkeley)","Balboa Park (SF)","Bay Fair (San Leandro)",
		"Castro Valley","Civic Center (SF)","Coliseum/Oakland Airport","Colma","Concord",
		"Daly City","Downtown Berkeley","Dublin/Pleasanton","El Cerrito del Norte","El Cerrito Plaza",
		"Embarcadero (SF)","Fremont","Fruitvale (Oakland)","Glen Park (SF)","Hayward","Lafayette",
		"Lake Merritt (Oakland)","MacArthur (Oakland)","Millbrae","Montgomery St. (SF)",
		"North Berkeley","North Concord/Martinez","Orinda","Pittsburg/Bay Point","Pleasant Hill",
		"Powell St. (SF)","Richmond","Rockridge (Oakland)","San Bruno","San Francisco Int'l Airport SFO",
		"San Leandro","South Hayward","South San Francisco","Union City","Walnut Creek","West Oakland"
    };
	
	static final double[] LATITUDES = new double[] {
		37.803664,
		37.765062,
		37.80787,
		37.752254,
		37.853024,
		37.72198087,
		37.697185,
		37.690754,
		37.779528,
		37.754006,
		37.684638,
		37.973737,
		37.70612055,
		37.869867,
		37.701695,
		37.925655,
		37.9030588,
		37.792976,
		37.557355,
		37.774963,
		37.732921,
		37.670399,
		37.893394,
		37.797484,
		37.828415,
		37.599787,
		37.789256,
		37.87404,
		38.003275,
		37.87836087,
		38.018914,
		37.928403,
		37.784991,
		37.936887,
		37.844601,
		37.637753,
		37.6159,
		37.72261921,
		37.63479954,
		37.664174,
		37.591208,
		37.905628,
		37.699759,
		37.80467476,
	};
	
	static final double[] LONGITUDES = new double[] {
		-122.271604,
		-122.419694,
		-122.269029,
		-122.418466,
		-122.26978,
		-122.4474142,
		-122.126871,
		-122.075567,
		-122.413756,
		-122.197273,
		-122.466233,
		-122.029095,
		-122.4690807,
		-122.268045,
		-121.900367,
		-122.317269,
		-122.2992715,
		-122.396742,
		-121.9764,
		-122.224274,
		-122.434092,
		-122.087967,
		-122.123801,
		-122.265609,
		-122.267227,
		-122.38666,
		-122.401407,
		-122.283451,
		-122.024597,
		-122.1837911,
		-121.945154,
		-122.056013,
		-122.406857,
		-122.353165,
		-122.251793,
		-122.416038,
		-122.392534,
		-122.1613112,
		-122.0575506,
		-122.444116,
		-122.017867,
		-122.067423,
		-121.928099,
		-122.2945822
	};
	
	//Convert plain text to BART API station string representations
	static final HashMap<String, String> STATION_MAP = new HashMap<String, String>() {
		{
			put("12th St. Oakland City Center", "12th");put("16th St. Mission (SF)", "16th");put("19th St. Oakland", "19th");
			put("24th St. Mission (SF)", "24th");put("Ashby (Berkeley)", "ashb");put("Balboa Park (SF)", "balb");put("Bay Fair (San Leandro)", "bayf");
			put("Castro Valley", "cast");put("Civic Center (SF)", "civc");put("Coliseum/Oakland Airport", "cols");put("Colma", "colm");
			put("Concord", "conc");put("Daly City", "daly");put("Downtown Berkeley", "dbrk");put("Dublin/Pleasanton", "dubl");
			put("El Cerrito del Norte", "deln");put("El Cerrito Plaza", "plza");put("Embarcadero (SF)", "embr");put("Fremont", "frmt");
			put("Fruitvale (Oakland)", "ftvl");put("Glen Park (SF)", "glen");put("Hayward", "hayw");put("Lafayette", "lafy");
			put("Lake Merritt (Oakland)", "lake");put("MacArthur (Oakland)", "mcar");put("Millbrae", "mlbr");put("Montgomery St. (SF)", "mont");
			put("North Berkeley", "nbrk");put("North Concord/Martinez", "ncon");put("Orinda", "orin");put("Pittsburg/Bay Point", "pitt");
			put("Pleasant Hill", "phil");put("Powell St. (SF)", "powl");put("Richmond", "rich");put("Rockridge (Oakland)", "rock");
			put("San Bruno", "sbrn");put("San Francisco Int'l Airport SFO", "sfia");put("San Leandro", "sanl");put("South Hayward", "shay");
			put("South San Francisco", "ssan");put("Union City", "ucty");put("Walnut Creek", "wcrk");put("West Oakland", "woak");
		}
	};
	
	//Convert BART API station string to plain text representation
	//This map is only used to populate UI, so the values (but NOT keys) are safe to meddle with.
	static final HashMap<String, String> REVERSE_STATION_MAP = new HashMap<String, String>(){
		{
			put("12th", "12th St. Oakland City Center");put("16th", "16th St. Mission (SF)");put("19th", "19th St. Oakland");
			put("24th", "24th St. Mission (SF)");put("ashb", "Ashby (Berkeley)");put("balb", "Balboa Park (SF)");put("bayf", "Bay Fair (San Leandro)");
			put("cast", "Castro Valley");put("civc", "Civic Center (SF)");put("cols", "Coliseum/Oakland Airport");put("colm", "Colma");
			put("conc", "Concord");put("daly", "Daly City");put("dbrk", "Downtown Berkeley");put("dubl", "Dublin/Pleasanton");
			put("deln", "El Cerrito del Norte");put("plza", "El Cerrito Plaza");put("embr", "Embarcadero (SF)");put("frmt", "Fremont");
			put("ftvl", "Fruitvale (Oakland)");put("glen", "Glen Park (SF)");put("hayw", "Hayward");put("lafy", "Lafayette");
			put("lake", "Lake Merritt (Oakland)");put("mcar", "MacArthur (Oakland)");put("mlbr", "Millbrae");put("mont", "Montgomery St. (SF)");
			put("nbrk", "North Berkeley");put("ncon", "North Concord/Martinez");put("orin", "Orinda");put("pitt", "Pittsburg/Bay Point");
			put("phil", "Pleasant Hill");put("powl", "Powell St. (SF)");put("rich", "Richmond");put("rock", "Rockridge (Oakland)");
			put("sbrn", "San Bruno");put("sfia", "SFO Airport");put("sanl", "San Leandro");put("shay", "South Hayward");
			put("ssan", "South San Francisco");put("ucty", "Union City");put("wcrk", "Walnut Creek");put("woak", "West Oakland");
		}
	};
	
	// Irregular etd Train Name - > bart terminal station abbreviation
	// list of all trainHeadStation values that aren't actually stations
	// i.e: Daly City/Millbrae, SFO/Milbrae
	// TODO: Make this a resource in /values 
	static final HashMap<String, String> KNOWN_SILLY_TRAINS = new HashMap<String, String>(){
		{
			put("SFIA/Millbrae", "mlbr");// SFIA is sfia
			put("Millbrae/Daly City", "mlbr"); //Daly City is daly
		}
	};
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crittercism.init(getApplicationContext(), "4f7a6cebb0931565250000f5");

        if(Integer.parseInt(Build.VERSION.SDK) < 11){
        	//If API 14+, The ActionBar will be hidden with this call
        	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        setContentView(R.layout.main);
        tableLayout = (TableLayout) findViewById(R.id.tableLayout);
        tableContainerLayout = (LinearLayout)findViewById(R.id.tableContainerLayout);
        c = this;
        res = getResources();
        prefs = getSharedPreferences("PREFS", 0);
        editor = prefs.edit();
       
        
        if(prefs.getBoolean("first_timer", true)){
        	TextView greetingTv = (TextView) View.inflate(c, R.layout.tabletext, null);
			greetingTv.setText(Html.fromHtml(getString(R.string.greeting)));
			greetingTv.setTextSize(18);
			greetingTv.setPadding(0, 0, 0, 0);
			greetingTv.setMovementMethod(LinkMovementMethod.getInstance());
        	new AlertDialog.Builder(c)
	        .setTitle("Welcome to Open BART")
	        .setIcon(R.drawable.ic_launcher)
	        .setView(greetingTv)
	        .setPositiveButton("Okay!", null)
	        .show();
        	
        	editor.putBoolean("first_timer", false);
	        editor.commit();
        }
        // LocalBroadCast Stuff
        LocalBroadcastManager.getInstance(this).registerReceiver(serviceStateMessageReceiver,
        	      new IntentFilter("service_status_change"));
        
        infoLayout = (LinearLayout) findViewById(R.id.infoLayout);
        
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_dropdown_item_1line, STATIONS);
        originTextView = (AutoCompleteTextView)
                findViewById(R.id.originTv);
         
        fareTv = (TextView) findViewById(R.id.fareTv);
        stopServiceTv = (TextView) findViewById(R.id.stopServiceTv);

        destinationTextView = (AutoCompleteTextView) findViewById(R.id.destinationTv);
        destinationTextView.setAdapter(adapter);
        originTextView.setAdapter(adapter);
        
        if(prefs.contains("origin") && prefs.contains("destination")){
        	//state= originTextView,destinationTextView
        	String origin = prefs.getString("origin", "");
        	String destination = prefs.getString("destination", "");
        	if(origin.compareTo("")!= 0)
        		originTextView.setThreshold(200); // disable auto-complete until new text entered
        	if(destination.compareTo("")!= 0)
        		destinationTextView.setThreshold(200); // disable auto-complete until new text entered
        	
    		originTextView.setText(origin);
    		destinationTextView.setText(destination);
    		validateInputAndDoRequest();
        }
        
        //set to nearest station (if any)
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		
		//if no gps location, then try network
		if (location == null) {
			Log.i("ClosetStation", "Trying network provider");
			location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
		}
		//make sure we got at least one
		if (location != null) {
			double longitude = location.getLongitude();
			double latitude = location.getLatitude();
			
			Log.i("ClosetStation", "Current position is: long:"+ longitude + " lat: " + latitude);
			
			int closetStationId = 0;
			String closetStation = STATIONS[closetStationId];
			double currentDistance = 100000000;//very far away
			for (int i = 0; i < LONGITUDES.length; i++) {
				//compute distance
				//this doesn't understand that the earth is round ()
				double stationDistance = Math.sqrt(
					Math.pow(longitude - LONGITUDES[i], 2) + 
					Math.pow(latitude - LATITUDES[i], 2));
				if (currentDistance > stationDistance) {
					currentDistance = stationDistance;
					closetStationId = i;
					closetStation = STATIONS[closetStationId];
					Log.i("ClosetStation", closetStation);
				}
			}
			Log.i("ClosetStation.final", closetStation);
			
	        originTextView.setText(closetStation);
	        validateInputAndDoRequest();
		} else {
			Log.i("ClosetStation", "no location info provided :(");
		}
       
        ImageView map = (ImageView) findViewById(R.id.map);
        map.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				Intent intent = new Intent(c, MapActivity.class);
		        startActivity(intent);
			}
        	
        });
        
        ImageView reverse = (ImageView) findViewById(R.id.reverse);
        reverse.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View arg0) {
				Editable originTempText = originTextView.getText();
				originTextView.setText(destinationTextView.getText());
				destinationTextView.setText(originTempText);	
				
				validateInputAndDoRequest();
			}
        });
        	
        
        originTextView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View arg1, int position,
					long arg3) {
				AutoCompleteTextView originTextView = (AutoCompleteTextView)
		                findViewById(R.id.originTv);
				originTextView.setThreshold(200);
				hideSoftKeyboard(arg1);

				validateInputAndDoRequest();
			}
        });
        
        // Handles restoring TextView input when focus lost, if no new input entered
        // previous input is stored in the target View Tag attribute
        // Assumes the target view is a TextView
        // TODO:This works but starts autocomplete when the view loses focus after clicking outside the autocomplete listview
        OnFocusChangeListener inputOnFocusChangeListener = new OnFocusChangeListener(){
        	@Override
			public void onFocusChange(View inputTextView, boolean hasFocus) {
				if (inputTextView.getTag() != null && !hasFocus && ((TextView)inputTextView).getText().toString().compareTo("") == 0){
						//Log.v("InputTextViewTagGet","orig: "+ inputTextView.getTag());
						((TextView)inputTextView).setText(inputTextView.getTag().toString());	
				}
        	}
        };
              
        originTextView.setOnFocusChangeListener(inputOnFocusChangeListener);
        destinationTextView.setOnFocusChangeListener(inputOnFocusChangeListener);

        // When the TextView is clicked, store current text in TextView's Tag property, clear displayed text 
        // and enable Auto-Completing after first character entered
        OnTouchListener inputOnTouchListener = new OnTouchListener(){
        	@Override
			public boolean onTouch(View inputTextView, MotionEvent me) {
        		// Only perform this logic on finger-down
				if(me.getAction() == me.ACTION_DOWN){
					inputTextView.setTag( ((TextView)inputTextView).getText().toString());
					//Log.v("InputTextViewTagSet","orig: " + inputTextView.getTag());
					((AutoCompleteTextView)inputTextView).setThreshold(1);
					((TextView)inputTextView).setText("");
				}
				// Allow Android to handle additional actions - i.e: TextView takes focus
				return false;
			}
        };
        
        originTextView.setOnTouchListener(inputOnTouchListener);
        destinationTextView.setOnTouchListener(inputOnTouchListener);
        
        // Autocomplete ListView item select listener
        
        OnItemClickListener AutoCompleteItemClickListener = new OnItemClickListener(){
        	@Override
			public void onItemClick(AdapterView<?> parent, View arg1, int position,
					long arg3) {
				
				//If a valid origin station is not entered, return
				if(STATION_MAP.get(originTextView.getText().toString()) == null)
					return;
					
				// Actv not available as arg1
				AutoCompleteTextView destinationTextView = (AutoCompleteTextView)
		                findViewById(R.id.destinationTv);
				destinationTextView.setThreshold(200);
				hideSoftKeyboard(arg1);
				validateInputAndDoRequest();
				//lastRequest = "etd";
				//String url = "http://api.bart.gov/api/etd.aspx?cmd=etd&orig="+originStation+"&key=MW9S-E7SL-26DU-VV8V";
				// TEMP: For testing route function
				//lastRequest = "route";
				//bartApiRequest();
			}
        };
                
        destinationTextView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View arg1, int position,
					long arg3) {
				
				//If a valid origin station is not entered, return
				if(STATION_MAP.get(originTextView.getText().toString()) == null)
					return;
					
				// Actv not available as arg1
				AutoCompleteTextView destinationTextView = (AutoCompleteTextView)
		                findViewById(R.id.destinationTv);
				destinationTextView.setThreshold(200);
				hideSoftKeyboard(arg1);
				validateInputAndDoRequest();
				//lastRequest = "etd";
				//String url = "http://api.bart.gov/api/etd.aspx?cmd=etd&orig="+originStation+"&key=MW9S-E7SL-26DU-VV8V";
				// TEMP: For testing route function
				//lastRequest = "route";
				//bartApiRequest();
			}
        });

    }
    // Initialize settings menu
    @Override public boolean onCreateOptionsMenu(Menu menu) {
    	//Use setting-button context menu OR Action bar
    	if(Integer.parseInt(Build.VERSION.SDK) < 11){
	        MenuItem mi = menu.add(0,0,0,"About");
	        mi.setIcon(R.drawable.about);
    	}
    	else{
    		MenuInflater inflater = getMenuInflater();
    	    inflater.inflate(R.layout.actionitem, menu);
    	    //return true;
    	}
        return super.onCreateOptionsMenu(menu);
    }
    
@Override public boolean onOptionsItemSelected(MenuItem item) {
		//settings context menu ID pre API 11 and action bar item post API 11
		if(item.getItemId() == 0 || item.getItemId() == R.id.menu_about){
			TextView aboutTv = (TextView) View.inflate(c, R.layout.tabletext, null);
			aboutTv.setText(Html.fromHtml(res.getStringArray(R.array.aboutDialog)[1]));
			aboutTv.setPadding(10, 0, 10, 0);
			aboutTv.setTextSize(18);
			aboutTv.setMovementMethod(LinkMovementMethod.getInstance());
			new AlertDialog.Builder(c)
	        .setTitle(res.getStringArray(R.array.aboutDialog)[0])
	        .setIcon(R.drawable.ic_launcher)
	        .setView(aboutTv)
	        .setPositiveButton("Okay!", null)
	        .show();
			return true;
		}
		return false;
    }
    //CALLED-BY: originTextView and destinationTextView item-select listeners
    //CALLS: HTTP requester: RequestTask
    public void bartApiRequest(String request, boolean updateUI){
    	String url = BART_API_ROOT;
    	if (request.compareTo("etd") == 0){
    		url += "etd.aspx?cmd=etd&orig="+STATION_MAP.get(originTextView.getText().toString());
    	}
    	else if (request.compareTo("route") == 0){
    		url += "sched.aspx?cmd=depart&a=3&b=0&orig="+STATION_MAP.get(originTextView.getText().toString())+"&dest="+STATION_MAP.get(destinationTextView.getText().toString());
    	}
    	url += "&key="+BART_API_KEY;
    	//Log.v("BART API",url);
    	new RequestTask(request, updateUI).execute(url);
    	// Set loading indicator
    	// I find this jarring when network latency is low
    	// TODO: set a countdown timer and only indicate loading after a threshold
    	//fareTv.setVisibility(0);
    	//fareTv.setText("Loading...");
    }
    
    public static void hideSoftKeyboard (View view) {
        InputMethodManager imm = (InputMethodManager)c.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
      }
    
    //CALLED-BY: HTTP requester: RequestTask
    //CALLS: Bart API XML response parsers
    public void parseBart(String response, String request, boolean updateUI){
    	// Clear loading indicator
    	//fareTv.setText("");
    	//fareTv.setVisibility(View.GONE);
    	if (response=="error"){
			new AlertDialog.Builder(c)
	        .setTitle(res.getStringArray(R.array.networkErrorDialog)[0])
	        .setMessage(res.getStringArray(R.array.networkErrorDialog)[1])
	        .setPositiveButton("Bummer", null)
	        .show();
    	}
    	else if(request.compareTo("etd") == 0)
    		new BartStationEtdParser(updateUI).execute(response);
    	else if(request.compareTo("route") == 0)
    		new BartRouteParser(updateUI).execute(response);
    }
    
    //CALLED-BY: Bart API XML response parsers: BartRouteParser, BartEtdParser
    //CALLS: the appropriate method to update the UI if updateUI is true
    //		 else cache the response (if it includes realtime info)
    public void handleResponse(Object response, boolean updateUI){
    	if(updateUI){
			//If special messages exist from a previous request, remove them
	    	if (tableContainerLayout.getChildCount() > 1)
	    		tableContainerLayout.removeViews(1, tableContainerLayout.getChildCount()-1);
	    	if (response instanceof etdResponse){
	    		currentEtdResponse = (etdResponse) response;
	    		//Log.v("ETD_CACHE","ETD SAVED");
	    		displayEtdResponse((etdResponse) response);
	    	}
	    	else if (response instanceof routeResponse){
	    		//Log.v("ETD_CACHE","ETD ROUTE DISPLAY");
	    		
	    		displayRouteResponse(updateRouteResponseWithEtd((routeResponse)response));
	    	}
    	}
    	else{
    		// if response is not being displayed cache it if it's real-time info
    		if (response instanceof etdResponse){
    			currentEtdResponse = (etdResponse) response;
    			sendEtdResponseToService();
    			//Log.v("ETD_CACHE","ETD SAVED");
    		}
    	}
    }

    //CALLED-BY: handleResponse() if updateUIOnResponse is true
    //Updates the UI with data from a routeResponse
    public void displayRouteResponse(routeResponse routeResponse){
    	// Previously, if the device's locale wasn't in Pacific Standard Time
    	// Responses with all expired routes could present, causing a looping refresh cycle
    	// This is now remedied by coercing response dates into PDT
    	
    	if(routeResponseIsLoopy(routeResponse)){
    		Log.d("Loopy RouteResponse","durn loops");
    		return;
    	}
    	else{
    		Log.d("NonLoopy RouteResponse","all good");
    	}
    	
    		
    	if(timer != null)
    		timer.cancel(); // cancel previous timer
    	timerViews = new ArrayList(); // release old ETA text views
    	maxTimer = 0;
    	try{
	    	fareTv.setVisibility(0);
	    	fareTv.setText("$"+routeResponse.routes.get(0).fare);
	    	tableLayout.removeAllViews();
	    	//Log.v("DATE",new Date().toString());
	    	long now = new Date().getTime();
	    
	    	for (int x=0;x<routeResponse.routes.size();x++){
	    		
	    		route thisRoute = routeResponse.routes.get(x);
	        	TableRow tr = (TableRow) View.inflate(c, R.layout.tablerow, null);
	        	tr.setPadding(0, 20, 0, 0);
	    		LinearLayout legLayout = (LinearLayout) View.inflate(c, R.layout.routelinearlayout, null);
	
	    		for(int y=0;y<thisRoute.legs.size();y++){
	    			TextView trainTv = (TextView) View.inflate(c, R.layout.tabletext, null);
	    			trainTv.setPadding(0, 0, 0, 0);
	    			trainTv.setTextSize(20);
	    			trainTv.setGravity(3); // set left gravity
	    			if (y>0){
	    				trainTv.setText("transfer at "+ REVERSE_STATION_MAP.get(((leg)thisRoute.legs.get(y-1)).disembarkStation.toLowerCase()));
	    				trainTv.setPadding(0, 0, 0, 0);
	    				legLayout.addView(trainTv);
	    				trainTv.setTextSize(14);
	    				trainTv = (TextView) View.inflate(c, R.layout.tabletext, null);
	    				trainTv.setPadding(0, 0, 0, 0);
	    				trainTv.setTextSize(20);
	        			trainTv.setGravity(3); // set left gravity
	    				trainTv.setText("to "+REVERSE_STATION_MAP.get(((leg)thisRoute.legs.get(y)).trainHeadStation.toLowerCase()));
	    			}
	    			else
	    				trainTv.setText("take " +REVERSE_STATION_MAP.get(((leg)thisRoute.legs.get(y)).trainHeadStation));
	    			
	    			legLayout.addView(trainTv);
	
	    		}
	    		
	    		if(thisRoute.legs.size() == 1){
	    			legLayout.setPadding(0, 10, 0, 0); // Address detination train and ETA not aligning 
	    		}
	    		
	    		tr.addView(legLayout);
	    		
	    		TextView arrivalTimeTv = (TextView) View.inflate(c, R.layout.tabletext, null);
	    		arrivalTimeTv.setPadding(10, 0, 0, 0);
	    		arrivalTimeTv.setTextSize(36);
	    		//Log.v("DEPART_DATE",thisRoute.departureDate.toString());
	    		
	    		// Don't report a train that may JUST be leaving with a negative ETA
	    		long eta;
	        	if(thisRoute.departureDate.getTime()-now <= 0){
	        		eta = 0;
	        	}
	        	else{
	        		eta = thisRoute.departureDate.getTime()-now;
	        	}
	        	timerViews.add(arrivalTimeTv);
	        	if(eta > maxTimer){
	        		maxTimer = eta;
	        	}
	        	// Set timeTv Tag to departure date for interpretation by ViewCountDownTimer
	        	arrivalTimeTv.setTag(thisRoute.departureDate.getTime());
	        	// Display eta less than 1m as "<1"
	        	if(eta < 60*1000)
	        		arrivalTimeTv.setText("<1"); // TODO - remove this? Does countdown tick on start
	        	else
	        		arrivalTimeTv.setText(String.valueOf(eta/(1000*60))); // TODO - remove this? Does countdown tick on start
	    		//new ViewCountDownTimer(arrivalTimeTv, eta, 60*1000).start();
	    		tr.addView(arrivalTimeTv);
	    		// Set the Row View (containing train names and times) Tag to the route it represents
	    		tr.setTag(thisRoute);
	    		tableLayout.addView(tr);
	    		tr.setOnLongClickListener(new OnLongClickListener(){
	
					@Override
					public boolean onLongClick(View arg0) {
						Log.d("RouteViewTag",((route)arg0.getTag()).toString());
						usherRoute = (route)arg0.getTag();
						TextView guidanceTv = (TextView) View.inflate(c, R.layout.tabletext, null);
						guidanceTv.setText(Html.fromHtml(getString(R.string.service_prompt)));
						guidanceTv.setTextSize(18);
						guidanceTv.setPadding(0, 0, 0, 0);
						new AlertDialog.Builder(c)
		                .setTitle("Route Guidance")
		                .setIcon(R.drawable.ic_launcher)
		                .setView(guidanceTv)
		                .setPositiveButton(R.string.service_start_button, new DialogInterface.OnClickListener() {
		                    
		                    public void onClick(DialogInterface dialog, int which) {
		                    	Intent i = new Intent(c, UsherService.class);
		                    	//i.putExtra("departure", ((leg)usherRoute.legs.get(0)).boardStation);
		                    	//Log.v("SERVICE","Starting");
		                    	if(usherServiceIsRunning()){
		                        	stopService(i);
		                    	}
		                    	startService(i);
		                    }
	
						 })
		                .setNeutralButton("Cancel", null)
		                .show();
						return true; // consumed the long click
					}
	    			
	    		});
	    		tr.setOnClickListener(new OnClickListener(){
	
					@Override
					public void onClick(View arg0) {
						int index = tableLayout.indexOfChild(arg0); // index of clicked view. Expanded view will always be +1
						route thisRoute = (route) arg0.getTag();
						if (!thisRoute.isExpanded){ // if route not expanded
							thisRoute.isExpanded = true;
							LinearLayout routeDetail = (LinearLayout) View.inflate(c, R.layout.routedetail, null);
							TextView arrivalTv = (TextView) View.inflate(c, R.layout.tabletext, null);
							SimpleDateFormat curFormater = new SimpleDateFormat("h:mm a"); 
							//arrivalTv.setTextColor(0xFFC9C7C8);
							arrivalTv.setText("arrives "+curFormater.format(thisRoute.arrivalDate));
							arrivalTv.setTextSize(20);
							routeDetail.addView(arrivalTv);
							if(thisRoute.bikes){
								ImageView bikeIv = (ImageView) View.inflate(c, R.layout.bikeimage, null);
								routeDetail.addView(bikeIv);
							}
							tableLayout.addView(routeDetail, index+1);
						}
						else{
							thisRoute.isExpanded = false;
							tableLayout.removeViewAt(index+1);
						}
						
					}
	    		});
	    	}
	    	if (routeResponse.specialSchedule != null){
	    		ImageView specialSchedule = (ImageView)View.inflate(c, R.layout.specialschedulelayout, null);
	    		specialSchedule.setTag(routeResponse.specialSchedule);
	    		specialSchedule.setOnClickListener(new OnClickListener(){
	
					@Override
					public void onClick(View arg0) {
					    TextView specialScheduleTv = (TextView) View.inflate(c, R.layout.tabletext, null);
					    specialScheduleTv.setPadding(0, 0, 0, 0);
					    specialScheduleTv.setText(Html.fromHtml(arg0.getTag().toString()));
					    specialScheduleTv.setTextSize(16);
					    specialScheduleTv.setMovementMethod(LinkMovementMethod.getInstance());
					    new AlertDialog.Builder(c)
				        .setTitle("Route Alerts")
				        .setIcon(R.drawable.warning)
				        .setView(specialScheduleTv)
				        .setPositiveButton("Bummer", null)
				        .show();
						
					}
	    			
	    		});
	    		tableLayout.addView(specialSchedule, tableLayout.getChildCount());
	    	}
	    	timer = new ViewCountDownTimer(timerViews, "route", maxTimer, 30*1000);
	    	timer.start();
    	}catch(Throwable t){
    		//Log.v("WTF",t.getStackTrace().toString());
    		
    	}
    }
    
    // Update route times with ETAs from cached etd response
    private routeResponse updateRouteResponseWithEtd(routeResponse input){
    	
    	//TODO: Confirm that currentEtdResponse has all ready been verified fresh
    	if(currentEtdResponse == null)
    		return input;
    	// BUGFIX: Using Date().getTime() could possibly return a time different than BART's API Locale
    	// Bart doesn't provide timezone info in their date responses, so consider whether to coerce their responses to PST
    	// In this instance, we can simply use the time returned with the etd response
    	//long now = new Date().getTime();
    	long now = input.date.getTime();
    	int numRoutes = input.routes.size();
    	int numEtds = currentEtdResponse.etds.size();
    	int lastLeg;
    	HashMap<Integer,Integer> routeToEtd = new HashMap<Integer, Integer>();
    	//find proper destination etds in currentEtdResponse
    	//match times in routeResponse to times in proper etds
    	
    	// ASSUMPTION: etds and routes are sorted by time, increasing
    	
    	// For each route
    	for(int x=0;x<numRoutes;x++){
    		lastLeg = ((route)input.routes.get(x)).legs.size()-1;
    		// For each possible etd match
    		for(int y=0;y<numEtds;y++){
    		// DEBUG
    		try{
    			//Check that destination train is listed in terminal-station format. Ex: "Fremont" CounterEx: 'SFO/Milbrae'
    			if (!STATION_MAP.containsKey(((etd)currentEtdResponse.etds.get(y)).destination)){
    				// If this is not a known silly-named train terminal station
    				if (!KNOWN_SILLY_TRAINS.containsKey(((etd)currentEtdResponse.etds.get(y)).destination)){
    					// Let's try and guess what it is
    					boolean station_guessed = false;
    					for(int z = 0; z < STATIONS.length; z++){
    						
    						// Can we match a station name within the silly-train name?
    						// haystack.indexOf(needle1);
    						if ( (((etd)currentEtdResponse.etds.get(y)).destination).indexOf(STATIONS[z]) != -1){
    							// Set the etd destination to the guessed real station name
    							((etd)currentEtdResponse.etds.get(y)).destination = STATIONS[z];
    							station_guessed = true;
    						}
    					}
    					if (!station_guessed){
    						break; //We have to give up on updating routes based on this utterly silly-named etd
    					}
    				}
    				else{
    					// Set the etd destination station to the real station name
    					((etd)currentEtdResponse.etds.get(y)).destination = KNOWN_SILLY_TRAINS.get(((etd)currentEtdResponse.etds.get(y)).destination);
    					//break;
    				}		
    			} // end STATION_MAP silly-name train check and replace
    			
    				// Comparing BART station abbreviations
    			if (STATION_MAP.get(((etd)currentEtdResponse.etds.get(y)).destination).compareTo(((leg)((route)input.routes.get(x)).legs.get(0)).trainHeadStation) == 0 ){
	    			//If matching etd is not all ready matched to a route, match it to this one
    				if (!routeToEtd.containsKey(x) && !routeToEtd.containsValue(y)){
	    				routeToEtd.put(x, y);
	    				//Log.v("routeToEtd","Route: " + String.valueOf(x)+ " Etd: " + String.valueOf(y));
    				}
    				else{
    					//if the etd is all ready claimed by a route, go to next etd
    					continue;
    				}
	    		}
	    		else if (STATION_MAP.get(((etd)currentEtdResponse.etds.get(y)).destination).compareTo(((leg)((route)input.routes.get(x)).legs.get(lastLeg)).trainHeadStation) == 0 ){
	    			if (!routeToEtd.containsKey(x) && !routeToEtd.containsValue(y)){
	    				routeToEtd.put(x, y);
	    				//Log.v("routeToEtd","Route: " + String.valueOf(x)+ " Etd: " + String.valueOf(y));
    				}
    				else{
    					//if the etd is all ready claimed by a route, go to next etd
    					continue;
    				}
	    		}
    			
    		}catch(Throwable T){
    			// Likely, a train with destination listed as a
    			// special tuple and not an actual station name
    			// was encountered 
    			//Log.v("WTF", "Find me");
    		}
    		}// end etd for loop
    		
    	}// end route for loop

    	Integer[] routesToUpdate = (Integer[])((routeToEtd.keySet()).toArray(new Integer[0]));
    	for(int x=0;x< routeToEtd.size();x++){
    		//Log.v("routeToEtd","Update Route: " + String.valueOf(routesToUpdate[x])+ " w/Etd: " + String.valueOf(routeToEtd.get(x)));
    		// etd ETA - route ETA (ms)
    		//Log.v("updateRR", "etd: "+ new Date((now + ((etd)currentEtdResponse.etds.get(routeToEtd.get(routesToUpdate[x]))).minutesToArrival*60*1000)).toString()+" route: "+ new Date(((route)input.routes.get(routesToUpdate[x])).departureDate.getTime()).toString());
    		long timeCorrection = (now + ((etd)currentEtdResponse.etds.get(routeToEtd.get(routesToUpdate[x]))).minutesToArrival*60*1000) - ((route)input.routes.get(routesToUpdate[x])).departureDate.getTime();
    		//Log.v("updateRRCorrection",String.valueOf(timeCorrection/(1000*60))+"m");
    		// Adjust the arrival date based on the difference in departure dates
    		((route)input.routes.get(routesToUpdate[x])).arrivalDate.setTime(((route)input.routes.get(routesToUpdate[x])).arrivalDate.getTime() + timeCorrection);
    		// Adjust departure date similarly
    		((route)input.routes.get(routesToUpdate[x])).departureDate.setTime(((route)input.routes.get(routesToUpdate[x])).departureDate.getTime() + timeCorrection);
    		//((route)input.routes.get(routesToUpdate[x])).departureDate = new Date(now + ((etd)currentEtdResponse.etds.get(routeToEtd.get(routesToUpdate[x]))).minutesToArrival*60*1000);
			
    		// Update all leg times
    		for(int y=0;y<input.routes.get(routesToUpdate[x]).legs.size();y++){
	    		// Adjust leg's board time
	    		((leg)((route)input.routes.get(routesToUpdate[x])).legs.get(y)).boardTime.setTime(((leg)((route)input.routes.get(routesToUpdate[x])).legs.get(y)).boardTime.getTime() + timeCorrection);
				// Adjust leg's disembark time
	    		((leg)((route)input.routes.get(routesToUpdate[x])).legs.get(y)).disembarkTime.setTime(((leg)((route)input.routes.get(routesToUpdate[x])).legs.get(y)).disembarkTime.getTime() + timeCorrection);
    		}
    	}
    	return input;
    	
    		// OLD method of updating, for humor
    	
    		// for every first leg train of each route
    		//ArrayList routesToUpdate = new ArrayList();
    		/*
    		for(int y=0;y<numRoutes;y++){
    			// if the etd train matches the first leg of this route, update it's departureTime with etd value
    			// OR if the etd train matches the last leg of this route, update with first leg
    			lastLeg = ((route)input.routes.get(y)).legs.size()-1;
	    		if (STATION_MAP.get(((etd)currentEtdResponse.etds.get(x)).destination).compareTo(((leg)((route)input.routes.get(y)).legs.get(0)).trainHeadStation) == 0 ){
	    			routesToUpdate.add(y);
	    			if (!etdsToUpdateWith.contains(x))
	    				etdsToUpdateWith.add(x);
	    		}
	    		else if (STATION_MAP.get(((etd)currentEtdResponse.etds.get(x)).destination).compareTo(((leg)((route)input.routes.get(y)).legs.get(lastLeg)).trainHeadStation) == 0 ){
	    			routesToUpdate.add(y);
	    			if (!etdsToUpdateWith.contains(x))
	    				etdsToUpdateWith.add(x);
	    		}
    		}
    		for(int y=0;y<routesToUpdate.size();y++){
    			if(y==etdsToUpdateWith.size())
    				break;
    			//TODO: verify boardTime is what routeResponse timer views are set by
    			((route)input.routes.get((Integer) routesToUpdate.get(y))).departureDate = new Date(now + ((etd)currentEtdResponse.etds.get((Integer) etdsToUpdateWith.get(y))).minutesToArrival*60*1000);
    			//TODO: evaluate whether the first leg boardTime also needs to be updated. I think it does for UsherService
    			((leg)((route)input.routes.get((Integer) routesToUpdate.get(y))).legs.get(0)).boardTime = new Date(now + ((etd)currentEtdResponse.etds.get((Integer) etdsToUpdateWith.get(y))).minutesToArrival*60*1000);
    		}
    	}*/
    	
    }
    
    //CALLED-BY: handleResponse() if updateUIOnResponse is true
    //Updates the UI with data from a etdResponse
    public void displayEtdResponse(etdResponse etdResponse){
    	if(timer != null)
    		timer.cancel(); // cancel previous timer
    	long now = new Date().getTime();
    	timerViews = new ArrayList(); // release old ETA text views
    	maxTimer = 0; // reset maxTimer
    	fareTv.setText("");
    	fareTv.setVisibility(View.GONE);
		tableLayout.removeAllViews();
		String lastDestination = "";
		
		// Display the alert ImageView and create a click listener to display alert html
		if (etdResponse.message != null){

    		ImageView specialScheduleImageView = (ImageView)View.inflate(c, R.layout.specialschedulelayout, null);
    		// Tag the specialScheduleImageView with the message html
    		if(etdResponse.message.contains("No data matched your criteria."))
    			specialScheduleImageView.setTag("This station is closed for tonight.");
    		else
    			specialScheduleImageView.setTag(Html.fromHtml(etdResponse.message));
    		
    		// Set the OnClickListener for the specialScheduleImageView to display the tagged message html
    		specialScheduleImageView.setOnClickListener(new OnClickListener(){
    			
				@Override
				public void onClick(View arg0) {
				    TextView specialScheduleTv = (TextView) View.inflate(c, R.layout.tabletext, null);
				    specialScheduleTv.setPadding(0, 0, 0, 0);
				    specialScheduleTv.setText(Html.fromHtml(arg0.getTag().toString()));
				    specialScheduleTv.setTextSize(16);
				    specialScheduleTv.setMovementMethod(LinkMovementMethod.getInstance());
				    new AlertDialog.Builder(c)
			        .setTitle("Station Alerts")
			        .setIcon(R.drawable.warning)
			        .setView(specialScheduleTv)
			        .setPositiveButton("Bummer", null)
			        .show();
				
				}
    			
    		});
    		
    		tableContainerLayout.addView(specialScheduleImageView);
    	}
		
		TableRow tr = (TableRow) View.inflate(c, R.layout.tablerow_right, null);
		LinearLayout destinationRow = (LinearLayout) View.inflate(c, R.layout.destination_row, null);
		//TextView timeTv =(TextView) View.inflate(c, R.layout.tabletext, null);
		int numAlt = 0;
		for(int x=0;x<etdResponse.etds.size();x++){
			if (etdResponse.etds.get(x) == null)
				break;
			etd thisEtd = (etd)etdResponse.etds.get(x);
			if (thisEtd.destination != lastDestination){ // new train destination
				numAlt = 0;
				tr = (TableRow) View.inflate(c, R.layout.tablerow_right, null);
				tr.setPadding(0, 0, 10, 0);
				destinationRow = (LinearLayout) View.inflate(c, R.layout.destination_row, null);
				TextView destinationTv = (TextView) View.inflate(c, R.layout.destinationlayout, null);
				if(x==0)
					destinationTv.setPadding(0, 0, 0, 0);
				//bullet.setWidth(200);
				//destinationTv.setPadding(0, 0, 0, 0);
				destinationTv.setTextSize(28);
				destinationTv.setText(thisEtd.destination);
				TextView timeTv = (TextView) View.inflate(c, R.layout.tabletext, null);
				// Display eta less than 1m as "<1"
				if(thisEtd.minutesToArrival == 0)
					timeTv.setText("<1");
				else
					timeTv.setText(String.valueOf(thisEtd.minutesToArrival));
				timeTv.setSingleLine(false);
				timeTv.setTextSize(36);
				//timeTv.setPadding(30, 0, 0, 0);
				long counterTime = thisEtd.minutesToArrival * 60*1000;
				if (counterTime > maxTimer){
					maxTimer = counterTime;
				}
				timeTv.setTag(counterTime+now);
				timerViews.add(timeTv);
	    		//new ViewCountDownTimer(timeTv, counterTime, 60*1000).start();
				//text.setWidth(120);
	    		destinationRow.addView(destinationTv);
				//tr.addView(destinationTv);
				tr.addView(timeTv);
				tr.setTag(thisEtd);
				tableLayout.addView(destinationRow);
				tableLayout.addView(tr);
				tr.setOnClickListener(new OnClickListener(){

					@Override
					public void onClick(View arg0) {
						int index = tableLayout.indexOfChild(arg0); // index of clicked view. Expanded view will always be +1
						etd thisEtd = (etd) arg0.getTag();
						if (!thisEtd.isExpanded){ // if route not expanded
							thisEtd.isExpanded = true;
							LinearLayout routeDetail = (LinearLayout) View.inflate(c, R.layout.routedetail, null);
							TextView platformTv = (TextView) View.inflate(c, R.layout.tabletext, null);
							platformTv.setPadding(0, 0, 0, 0);
							platformTv.setText("platform "+thisEtd.platform);
							platformTv.setTextSize(20);
							routeDetail.addView(platformTv);
							if(thisEtd.bikes){
								ImageView bikeIv = (ImageView) View.inflate(c, R.layout.bikeimage, null);
								routeDetail.addView(bikeIv);
							}
							tableLayout.addView(routeDetail, index+1);
						}
						else{
							thisEtd.isExpanded = false;
							tableLayout.removeViewAt(index+1);
						}
						
					}
	    		});
			}
			else{ // append next trains arrival time to existing destination display
				//timeTv.append(String.valueOf(", "+thisEtd.minutesToArrival));
				numAlt++;
				TextView nextTimeTv =(TextView) View.inflate(c, R.layout.tabletext, null);
				//nextTimeTv.setTextSize(36-(5*numAlt));
				nextTimeTv.setTextSize(36);
				nextTimeTv.setText(String.valueOf(thisEtd.minutesToArrival));
				//nextTimeTv.setPadding(30, 0, 0, 0);
				if (numAlt == 1)	//0xFFF06D2F  C9C7C8
					nextTimeTv.setTextColor(0xFFC9C7C8);
				else if (numAlt == 2)
					nextTimeTv.setTextColor(0xFFA8A7A7);
				long counterTime = thisEtd.minutesToArrival * 60*1000;
				nextTimeTv.setTag(counterTime+now);
				if (counterTime > maxTimer){
					maxTimer = counterTime;
				}
				timerViews.add(nextTimeTv);

	    		//new ViewCountDownTimer(nextTimeTv, counterTime, 60*1000).start();
				tr.addView(nextTimeTv);
			}
			lastDestination = thisEtd.destination;
		} // end for
		//scrolly.scrollTo(0, 0);
		timer = new ViewCountDownTimer(timerViews, "etd", maxTimer, 30*1000);
		timer.start();
	} 
    
    private void validateInputAndDoRequest(){
    	long now = new Date().getTime();
    	if(STATION_MAP.get(originTextView.getText().toString()) != null){
			if(STATION_MAP.get(destinationTextView.getText().toString()) != null){
				//if an etd response is cached, is fresh, and is for the route departure station:
				//temp testing
				if(currentEtdResponse != null){
					long timeCheck = (now - currentEtdResponse.date.getTime());
					boolean stationCheck = (currentEtdResponse.station.compareTo(originTextView.getText().toString()) == 0 );
				
					//Log.v("CACHE_CHECK",String.valueOf(timeCheck) + " " + String.valueOf(stationCheck)+ " " + currentEtdResponse.date.toString());
				}
				if(currentEtdResponse != null && 
						(now - currentEtdResponse.date.getTime() < CURRENT_ETD_RESPONSE_FRESH_MS) && 
							(currentEtdResponse.station.compareTo(originTextView.getText().toString()) == 0 )){
					
					//Log.v("ETD_CACHE","Cache found");
					bartApiRequest("route", true);
				}
				// if an appropriate etd cache is not available, fetch it now
				else{
					//("ETD_CACHE","Cache ETD and display ROUTE");
					bartApiRequest("etd",false);
					bartApiRequest("route", true);
				}
			}
			else{
				bartApiRequest("etd", true);
			}
		}
    }
    
    @Override
    public void onPause(){
    	//Log.v("onPause","pausin for a cause");
    	super.onPause();
    	// Save text input state
    	editor.putString("origin", originTextView.getText().toString());
    	editor.putString("destination",destinationTextView.getText().toString());
    	editor.commit();
    }
    
    // Called when message received
    private BroadcastReceiver serviceStateMessageReceiver = new BroadcastReceiver() {
    	  @Override
    	  public void onReceive(Context context, Intent intent) {
    	    // Get extra data included in the Intent
    	    int status = intent.getIntExtra("status", -1);
    	    if(status == 0){ // service stopped
    	    	Log.d("TheActivity-BroadcastReceived", "service stopped");
    	    	stopServiceTv.setVisibility(View.GONE);
    	    }
    	    else if(status == 1){ // service started
    	    	Log.d("TheActivity-BroadcastReceived", "service started");
    	    	stopServiceTv.setVisibility(0);
            	stopServiceTv.setOnClickListener(new OnClickListener(){

    				@Override
    				public void onClick(View v) {
    					Intent i = new Intent(c, UsherService.class);
                    	//i.putExtra("departure", ((leg)usherRoute.legs.get(0)).boardStation);
                    	//Log.v("SERVICE","Stopping");
                    	stopService(i);
                    	v.setVisibility(View.GONE);	
    				}
            	});
    	    }
    	    else if(status == 2){//temporarily test this as avenue for countdowntimer to signal views need refreshing
    	    	Log.d("TheActivity-BroadcastReceived", "countdown timer expired");
    	    	bartApiRequest(intent.getStringExtra("request"), true);
    	    }
    	    else if(status == 3){// Sent by RequestTask upon completion
    	    	Log.d("TheActivity-BroadcastReceived", "requestTask complete");
    	    	parseBart(intent.getStringExtra("result"), intent.getStringExtra("request"), intent.getBooleanExtra("updateUI",true));
    	    }
    	    else if(status == 4){ // Sent by BartRouteParser / BartStationEtdParser upon completion
    	    	Log.d("TheActivity-BroadcastReceived", "Bart parser complete");
    	    	// I'm amazed that the result's Class (etdResponse, routeResponse) can be introspected from the Serializable!
    	    	// Watch how handleResponse operates as intended!
    	    	
    	    	// TODO: Address infinite looping here when response result returns all 0m trains
    	    	// i.e: after BART service has ended for a station
    	    	handleResponse(intent.getSerializableExtra("result"), intent.getBooleanExtra("updateUI", true));
    	    }
    	    
    	  }
    	};
    	
	@Override
	protected void onResume() {
	  // Unregister since the activity is about to be closed.
		//Log.v("SERVICE_STATE",String.valueOf(usherServiceIsRunning()));
		if(usherServiceIsRunning()){
			stopServiceTv.setVisibility(0);
        	stopServiceTv.setOnClickListener(new OnClickListener(){

				@Override
				public void onClick(View v) {
					Intent i = new Intent(c, UsherService.class);
                	//i.putExtra("departure", ((leg)usherRoute.legs.get(0)).boardStation);
                	//Log.v("SERVICE","Stopping");
                	stopService(i);
                	v.setVisibility(View.GONE);	
				}
        	});
		}
	  super.onResume();
	}
	
	@Override
	protected void onDestroy() {
	  // Unregister since the activity is about to be closed.
	  LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceStateMessageReceiver);
	  super.onDestroy();
	}
	
	// Called in onResume() to ensure stop service button available as necessary
	private boolean usherServiceIsRunning() {
	    ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
	        if ("pro.dbro.bart.UsherService".equals(service.service.getClassName())) {
	            return true;
	        }
	    }
	    return false;
	}
    
	//Sends message to service with etd data
	private void sendEtdResponseToService() { // 0 = service stopped , 1 = service started, 2 = refresh view with call to bartApiRequest(), 3 = 
		  int status = 5; // hardcode status for calling UsherService with new etdResponse
		  //Log.d("sender", "Sending AsyncTask message");
	  	  Intent intent = new Intent("service_status_change");
	  	  // You can also include some extra data.
	  	  intent.putExtra("status", status);
	  	  intent.putExtra("etdResponse", (Serializable) currentEtdResponse);
	  	  LocalBroadcastManager.getInstance(TheActivity.c).sendBroadcast(intent);
	  	}
	
	// DEBUG method to check if route response contains all 0m entries
	// NOTE: CountDownTimer uses departureDate for display and countdown set
	//		 route used to determine if the route detail row needs to be hidden
	private boolean routeResponseIsLoopy(routeResponse rR){
		long now = new Date().getTime();
		//Log.v("DisplayRoute",rR.routes.get(0).departureDate.toString());
		for(int x = 0; x< rR.routes.size();x++){
			// if at least one route doesn't have a 0 eta, this response isn't loopy
			if(rR.routes.get(x).departureDate.getTime() - now > 0){
				return false;
			}
		}
		// this response has all 0 etas, so it's loopy
		return true;
	}
    
}