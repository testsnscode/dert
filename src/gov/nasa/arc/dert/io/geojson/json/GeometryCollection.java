package gov.nasa.arc.dert.io.geojson.json;

import java.util.ArrayList;

/**
 * Provides a GeoJSON GeometryCollection object, a collection of GeoJSON
 * Geometry objects.
 *
 */
public class GeometryCollection extends Geometry {

	private ArrayList<Geometry> geometryList;

	/**
	 * Constructor
	 * 
	 * @param jsonObject
	 */
	public GeometryCollection(JsonObject jsonObject) {
		super(jsonObject, GeojsonType.GeometryCollection);
		geometryList = new ArrayList<Geometry>();

		Object[] arrayN = jsonObject.getArray("geometries");
		int n = arrayN.length;
		for (int i = 0; i < n; ++i) {
			JsonObject jObj = (JsonObject)arrayN[i];
			Geometry geometry = Geometry.createGeometry(jObj);
			geometryList.add(geometry);
		}
	}

	/**
	 * Get the list of geometry objects in the collection.
	 * 
	 * @return
	 */
	public ArrayList<Geometry> getGeometryList() {
		return (geometryList);
	}

}
