package com.sf.zoe;

import org.json.JSONArray;
import org.json.JSONObject;

public class JsonUtil {
	public static void main(String[] args) {
		JSONArray j = new JSONArray(
				"[{\"faceRectangle\":{\"height\":151,\"left\":233,\"top\":207,\"width\":151},\"scores\":"
						+ "{\"anger\":1.15142839E-05,\"contempt\":0.00137581164,\"disgust\":5.27670068E-07,\"fear\":4.27359161E-08,\"happiness\":1.1481673E-05,"
						+ "\"neutral\":0.9984194,\"sadness\":0.000175560912,\"surprise\":5.660774E-06}},"+
						"{\"faceRectangle\":{\"height\":151,\"left\":233,\"top\":207,\"width\":151},\"scores\":"
								+ "{\"anger\":1.15142839E-05,\"contempt\":0.00137581164,\"disgust\":5.27670068E-07,\"fear\":4.27359161E-08,\"happiness\":1.1481673E-05,"
								+ "\"neutral\":0.9984194,\"sadness\":0.000175560912,\"surprise\":5.660774E-06}}"
						+"]"
				);
		JSONObject jo = getScores(j);
		System.out.println(jo);
	}

	public static JSONObject getScores(JSONArray json) {
		JSONObject temp = null;
		JSONObject totalJson = new JSONObject(
				"{\"anger\":0.0,\"contempt\":0.0,\"disgust\":0.0,\"fear\":0.0,\"happiness\":0.0,"
						+ "\"neutral\":0.0,\"sadness\":0.0,\"surprise\":0.0}");
		String[] keys = JSONObject.getNames(totalJson);
		for (int i = 0; i < json.length(); i++) {
			temp = json.getJSONObject(i).getJSONObject("scores");
			for (int j = 0; j < keys.length; j++) {
				//System.out.println(toDouble(totalJson.get((keys[j])), 0) +"=="+ toDouble(temp.get((keys[j])), 0));
				totalJson.put(keys[j], (i==json.length()-1)
							?(toDouble(totalJson.get((keys[j])), 0) + toDouble(temp.get((keys[j])), 0))/json.length()
							:(toDouble(totalJson.get((keys[j])), 0) + toDouble(temp.get((keys[j])), 0)));
			}
		}
		return totalJson;
	}

	public static double toDouble(Object o, double def) {
		if (o == null || !(o instanceof String || o instanceof Number)) {
			return def;
		}
		try {
			return Double.parseDouble(o.toString());
		} catch (NumberFormatException e) {
		}
		return def;
	}
}
