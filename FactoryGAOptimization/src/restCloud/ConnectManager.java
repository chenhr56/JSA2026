package restCloud;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

import aura.PopulationEntry;
import metrics.JsonConverter;

public class ConnectManager {

	boolean useCloud;
	Random random;

	boolean success = false;

	public ConnectManager(boolean useCloud, Random random) {
		this.useCloud = useCloud;
		this.random = random;
	}

	public void push(String cloudID, int islandID, List<PopulationEntry> pf, List<ParetoFrontCapsule> fpCaps) {
		if (useCloud) {
			// System.out.println("island " + cloudID + " going to push!");
			ParetoFrontCapsule list = new ParetoFrontCapsule(pf, null);
			CompletableFuture<ParetoFrontCapsule> o = new CompletableFuture<>();
			connect(o, cloudID, "push", list);

			List<List<Double>> objectives = new ArrayList<>();
			for (PopulationEntry entry : pf) {
				objectives.add(entry.getObjectives());
			}
			CompletableFuture<ObjectiveCapsule> o1 = new CompletableFuture<>();
			connectforDisplay(o1, cloudID, new ObjectiveCapsule(objectives));

		} else {

			ParetoFrontCapsule cap = fpCaps.get(islandID);
			cap.list = pf;

		}
	}

	public List<PopulationEntry> poll(String cloudID, int islandID, List<ParetoFrontCapsule> fpCaps) {
		List<PopulationEntry> out = null;
		if (useCloud) {
			List<PopulationEntry> dummyList = new ArrayList<>();
			ParetoFrontCapsule list = new ParetoFrontCapsule(dummyList, null);
			CompletableFuture<ParetoFrontCapsule> o = new CompletableFuture<ParetoFrontCapsule>();

			connect(o, cloudID, "poll", list);

			try {
				out = o.get().get();
			} catch (NullPointerException e) {
				// System.out.println("no results from cloud.");
			} catch (InterruptedException e) {
				// System.out.println("no results from cloud.");
			} catch (ExecutionException e) {
				// System.out.println("no results from cloud.");
			}
		} else {
			if (fpCaps.size() < 2)
				return null;
			int index = random.nextInt(fpCaps.size());
			while (index == islandID)
				index = random.nextInt(fpCaps.size());
			out = fpCaps.get(index).get();
		}
		return out;
	}

	public static String url = "http://gaserver-svc.default.svc.cluster.local:9090/";
	// http://www.shuai.zhao/

	private void connectforDisplay(CompletableFuture<ObjectiveCapsule> o, String id, ObjectiveCapsule objectives) {
		String configJSON = new metrics.JsonConverter().toJson(objectives);
		success = false;
		// int counter = 0;
		while (!success) {
			// System.out.println(++counter + " attempt begin");

			Unirest.post(url + "pushobject/" + id).header("accept", "application/json").body(configJSON)
					.asStringAsync(new Callback<String>() {

						public void failed(UnirestException e) {
							// System.out.println("The request has failed.");
							success = false;
						}

						public void completed(HttpResponse<String> response) {
							String body = response.getBody();

							JsonConverter jsonConverter = new JsonConverter();
							o.complete(jsonConverter.fromJson(body, ObjectiveCapsule.class));
							success = true;
							// System.out.println("The request succeed.");
						}

						public void cancelled() {
							// System.out.println("The request has been cancelled");
							success = false;
						}

					});
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
		}
		// System.out.println("Request finished.");

	}

	private void connect(CompletableFuture<ParetoFrontCapsule> o, String id, String action,
			ParetoFrontCapsule paretoFronts) {
		String configJSON = new metrics.JsonConverter().toJson(paretoFronts);
		success = false;
		// int counter = 0;
		while (!success) {
			// System.out.println(++counter + " attempt begin");

			Unirest.post(url + action + "/" + id).header("accept", "application/json").body(configJSON)
					.asStringAsync(new Callback<String>() {

						public void failed(UnirestException e) {
							// System.out.println("The request has failed.");
							success = false;
						}

						public void completed(HttpResponse<String> response) {
							String body = response.getBody();

							JsonConverter jsonConverter = new JsonConverter();
							o.complete(jsonConverter.fromJson(body, ParetoFrontCapsule.class));
							success = true;
							// System.out.println("The request succeed.");
						}

						public void cancelled() {
							// System.out.println("The request has been cancelled");
							success = false;
						}

					});
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
			}
		}
		// System.out.println("Request finished.");

	}

}
