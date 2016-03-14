package org.acme.filledpolygonerror;

import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.maps.android.PolyUtil;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.impl.CoordinateArraySequence;
import com.vividsolutions.jts.operation.overlay.PolygonBuilder;
import com.vividsolutions.jts.simplify.VWSimplifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by neal on 2016-03-13.
 */
public class MapUtils {

    private static <T> T deepCopy(T object, Class<T> type) {
        try {
            Gson gson = new Gson();
            return gson.fromJson(gson.toJson(object, type), type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Creates a simplified version of the given GeoJSON. This is expensive to do, so
     * don't do it too often.
     *
     * @param geojson
     * @param tolerance
     * @return
     */
    public static JsonObject simplify(JsonObject geojson, double tolerance) {

        geojson = deepCopy(geojson, JsonObject.class);

        JsonArray features = geojson.get("features").getAsJsonArray();
        Iterator<JsonElement> featureIterator = features.iterator();
        while (featureIterator.hasNext()) {
            JsonObject feature = featureIterator.next().getAsJsonObject();
            JsonObject geometry = feature.get("geometry").getAsJsonObject();
            String geometryType = geometry.get("type").getAsString();
            JsonArray coordinates = geometry.get("coordinates").getAsJsonArray();

            switch(geometryType) {
                case "Polygon": {
                    // Coordinates will be an array of arrays of tuples
                    // System.err.println("Polygon, coordinates size: " + coordinates.size());
                    for (int coordinateIndex = 0; coordinateIndex < coordinates.size(); coordinateIndex++) {
                        JsonArray coordinateSet = coordinates.get(coordinateIndex).getAsJsonArray();
                        //System.err.println("  ->  " + coordinateSet.size());

                        List<LatLng> latLngs = new ArrayList<>();
                        for (int tupleIndex = 0; tupleIndex < coordinateSet.size(); tupleIndex++) {
                            JsonArray tuple = coordinateSet.get(tupleIndex).getAsJsonArray();
                            double longitude = tuple.get(0).getAsDouble();
                            double latitude = tuple.get(1).getAsDouble();
                            latLngs.add(new LatLng(latitude, longitude));
                        }

                        Geometry simplifiedGeometry = VWSimplifier.simplify(makeGeometry(latLngs), tolerance);
                        List<LatLng> simplified = fromGeometry(simplifiedGeometry);

                        //List<LatLng> simplified = PolyUtil.simplify(latLngs, tolerance);

                        // Build the list of tuples back up again from the simplified polygon points
                        // System.err.println("         simplified ->  " + simplified.size());
                        updatePolygon(coordinateIndex, coordinates, simplified);
                    }
                    break;
                }
                case "MultiPolygon": {
                    // Coordinates will be an array of arrays of tuples
                    // System.err.println("MultiPolygon, polygon count: " + coordinates.size());
                    for (int coordinateIndex = 0; coordinateIndex < coordinates.size(); coordinateIndex++) {
                        JsonArray polygon = coordinates.get(coordinateIndex).getAsJsonArray();
                        // System.err.println("  ->  " + polygon.size());

                        for (int polygonIndex = 0; polygonIndex < polygon.size(); polygonIndex++) {
                            JsonArray polyCoordinates = polygon.get(polygonIndex).getAsJsonArray();
                            // System.err.println("     ->  " + polyCoordinates.size());

                            List<LatLng> latLngs = new ArrayList<>();
                            for (int tupleIndex = 0; tupleIndex < polyCoordinates.size(); tupleIndex++) {
                                JsonArray tuple = polyCoordinates.get(tupleIndex).getAsJsonArray();
                                double longitude = tuple.get(0).getAsDouble();
                                double latitude = tuple.get(1).getAsDouble();
                                latLngs.add(new LatLng(latitude, longitude));
                            }

                            Geometry simplifiedGeometry = VWSimplifier.simplify(makeGeometry(latLngs), tolerance);
                            List<LatLng> simplified = fromGeometry(simplifiedGeometry);

                            //List<LatLng> simplified = PolyUtil.simplify(latLngs, tolerance);

                            // Build the list of tuples back up again from the simplified polygon points
                            // System.err.println("         simplified ->  " + simplified.size());
                            updatePolygon(polygonIndex, polygon, simplified);
                        }
                    }
                    break;
                }
            }
        }

        return geojson;
    }

    private static List<LatLng> fromGeometry(Geometry simplifiedGeometry) {

        Coordinate[] coordinates = simplifiedGeometry.getCoordinates();
        ArrayList<LatLng> retval = new ArrayList<>();

        for (int i = 0; i < coordinates.length; i++) {
            retval.add(new LatLng(coordinates[i].x, coordinates[i].y));
        }

        return retval;
    }

    private static Geometry makeGeometry(List<LatLng> latLngs) {
        GeometryFactory factory = new GeometryFactory();

        Coordinate[] coordinates = new Coordinate[latLngs.size()];
        int i = 0;
        for (LatLng latLng : latLngs) {
            coordinates[i] = new Coordinate();
            Coordinate coordinate = coordinates[i++];
            coordinate.x = latLng.latitude;
            coordinate.y = latLng.longitude;
        }

        CoordinateSequence points = new CoordinateArraySequence(coordinates);

        LinearRing boundary = new LinearRing(points, factory);

        return new Polygon(boundary, null, factory);
    }

    private static void updatePolygon(int coordinateIndex, JsonArray polygon, List<LatLng> simplified) {
        JsonArray simplifiedArray = new JsonArray();
        for (LatLng latLng : simplified) {
            JsonArray tuple = new JsonArray();
            tuple.add(latLng.longitude);
            tuple.add(latLng.latitude);
            simplifiedArray.add(tuple);
        }
        polygon.set(coordinateIndex, simplifiedArray);
    }
}
