package com.vladsilin.kivaloans;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
	Context context;
	ListView lister;
	TextView positionDisplay;
	Button previous;
	Button next;

	protected int numParams = 4;
	protected int totalPages;
	private int increment = 0;
	protected int totalItems;
	protected int ItemsPerPage;

	protected String loanUrl = "http://api.kivaws.org/v1/loans/search.json?country_code=US";
	private static final String LOANS = "loans";
	private static final String LOCATION = "location";
	private static final String NAME = "name";
	private static final String TOWN = "town";
	private static final String DATE = "posted_date";
	private static final String AMOUNT = "loan_amount";

	JSONArray loans = null;
	ArrayList<Spanned[]> jsonArray;
	ListAdapter listAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);

		lister = (ListView) findViewById(R.id.lister);
		previous = (Button) findViewById(R.id.prev);
		next = (Button) findViewById(R.id.next);
		positionDisplay = (TextView) findViewById(R.id.title);

		lister = (ListView) findViewById(R.id.lister);
		jsonArray = new ArrayList<Spanned[]>();
		context = this;

		next.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				newPage(true);
				showPage(increment);
			}
		});
		previous.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				newPage(false);
				showPage(increment);
			}
		});

		LoanListTask loadTask = new LoanListTask();
		loadTask.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private void newPage(boolean forward) {
		if (increment + 1 == totalPages && forward) {
			increment = 0;
		} else if (increment == 0 && !forward) {
			increment = totalPages - 1;
		} else {
			if (forward)
				increment++;
			else
				increment--;
		}
	}

	protected void showPage(int pageNumber) {
		positionDisplay.setText("Loan " + (pageNumber + 1) + " of "
				+ totalPages);
		listAdapter = new ArrayAdapter<CharSequence>(context,
				R.layout.text_center, jsonArray.get(pageNumber));
		lister.setAdapter(listAdapter);
	}

	public class LoanListTask extends AsyncTask<Void, Void, Void> {
		ProgressDialog loadingDialog;

		@Override
		protected void onPreExecute() {
			loadingDialog = new ProgressDialog(context);
			loadingDialog.setTitle(context.getString(R.string.loading));
			loadingDialog.setCancelable(false);
			loadingDialog.show();
			super.onPreExecute();
		}

		@Override
		protected Void doInBackground(Void... params) {
			HttpClient client = new DefaultHttpClient();
			HttpGet getRequest = new HttpGet(loanUrl);

			try {
				HttpResponse response = client.execute(getRequest);
				StatusLine status = response.getStatusLine();

				if (status.getStatusCode() != 200) {
					Toast.makeText(context, "Unable to access server",
							Toast.LENGTH_SHORT).show();
					return null;
				}

				InputStream jsonStream = response.getEntity().getContent();
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(jsonStream));
				String line;
				StringBuilder builder = new StringBuilder();

				while ((line = reader.readLine()) != null) {
					builder.append(line);
				}

				String rawJsonData = builder.toString();

				JSONObject json = new JSONObject(rawJsonData);
				loans = json.getJSONArray(LOANS);

				for (int x = 0; x < loans.length(); x++) {
					JSONObject l = loans.getJSONObject(x);

					String name = l.getString(NAME);
					JSONObject location = l.getJSONObject(LOCATION);
					String town = location.getString(TOWN);
					String date = l.getString(DATE);
					String amount = l.getString(AMOUNT);

					try {
						SimpleDateFormat dateFormat = new SimpleDateFormat(
								"yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
						date = DateFormat.getDateInstance(DateFormat.LONG)
								.format(dateFormat.parse(date));

						NumberFormat currencyFormatter = NumberFormat
								.getCurrencyInstance(Locale.US);
						amount = currencyFormatter.format(Double
								.parseDouble(amount));
					} catch (NumberFormatException nfe) {
						Toast.makeText(context, "Unable to parse loan amount",
								Toast.LENGTH_SHORT).show();
					} catch (ParseException pe) {
						Toast.makeText(context, "Unable to parse date",
								Toast.LENGTH_SHORT).show();
					}

					Spanned[] temp = {
							Html.fromHtml(context.getString(R.string.name_tag)
									+ name),
							Html.fromHtml(context.getString(R.string.town_tag)
									+ town),
							Html.fromHtml(context.getString(R.string.date_tag)
									+ date),
							Html.fromHtml(context
									.getString(R.string.amount_tag) + amount) };
					jsonArray.add(temp);
				}

				totalItems = jsonArray.size() * numParams;
				ItemsPerPage = numParams;
				int overflow = totalItems % ItemsPerPage;
				overflow = overflow == 0 ? 0 : 1;
				totalPages = totalItems / ItemsPerPage + overflow;

			} catch (ClientProtocolException e) {
				e.printStackTrace();
				Toast.makeText(context, "Client protocol error",
						Toast.LENGTH_SHORT).show();
			} catch (IOException e) {
				e.printStackTrace();
				Toast.makeText(context, "IO Error", Toast.LENGTH_SHORT).show();
			} catch (JSONException e) {
				e.printStackTrace();
				Toast.makeText(context, "JSON Error", Toast.LENGTH_SHORT)
						.show();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			super.onPostExecute(result);
			if (loadingDialog.isShowing())
				loadingDialog.dismiss();
			showPage(0);
		}
	}
}
