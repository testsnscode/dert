package gov.nasa.arc.dert.io.geojson.json;



/**
 * Provides a GeoJSON MultiLineString object.
 *
 */
public class MultiLineString extends Geometry {

	private double[][][] coordinate;

	/**
	 * Constructor
	 * 
	 * @param jsonObject
	 */
	public MultiLineString(JsonObject jsonObject) {
		super(jsonObject, GeojsonType.MultiLineString);
		Object[] arrayN = jsonObject.getArray("coordinates");
		int n = arrayN.length;
		Object[] arrayM = (Object[])arrayN[0];
		int m = arrayM.length;
		Object[] pos = (Object[])arrayM[0];
		int posLength = pos.length;
		coordinate = new double[n][m][posLength];
		for (int i = 0; i < n; ++i) {
			arrayM = (Object[])arrayN[i];
			for (int j = 0; j < m; ++j) {
				pos = (Object[])arrayM[j];
				for (int p = 0; p < posLength; ++p) {
					coordinate[i][j][p] = ((Double)pos[p]).doubleValue();
				}
			}
		}
	}
	
	public MultiLineString(double[][][] coordinate) {
		super(GeojsonType.MultiLineString);
		this.coordinate = coordinate;
	}

	/**
	 * Get coordinates.
	 * 
	 * @return
	 */
	public double[][][] getCoordinates() {
		return (coordinate);
	}

}
