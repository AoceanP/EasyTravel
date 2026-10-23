package com.easytravel.web.ops;

import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * WHAT:   Reads Eureka's /eureka/apps JSON.
 * WHY:    Kept apart from the network code so it can be unit tested with a
 *         sample of Eureka's real format.
 *
 * @author AoceanP
 */
final class Registry
{
    /**
     * WHAT:   Not instantiable.
     * WHY:    Static helper.
     */
    private Registry()
    {
    }

    /**
     * WHAT:   The applications with at least one instance UP.
     * WHY:    Eureka nests applications → instances; a list with one element
     *         is sometimes sent as a bare object, so both shapes are read.
     * PRE:    apps is Eureka's reply
     * RETURN: application names as Eureka writes them (upper case)
     *
     * @param apps the reply
     * @return the names
     */
    static List<String> upNames(JsonObject apps)
    {
        List<String> names = new ArrayList<>();
        JsonObject root = apps.getJsonObject("applications");
        if (root == null) return names;
        for (JsonObject app : objects(root.get("application")))
        {
            boolean up = objects(app.get("instance")).stream().anyMatch(i -> "UP".equals(i.getString("status", "")));
            if (up)
            {
                names.add(app.getString("name", ""));
            }
        }
        return names;
    }

    /**
     * WHAT:   A JSON value as a list of objects.
     * PRE:    none
     * RETURN: the objects; empty for null or other types
     *
     * @param value an array, an object, or null
     * @return the objects
     */
    private static List<JsonObject> objects(JsonValue value)
    {
        if (value instanceof JsonArray array)
        {
            return array.stream().filter(v -> v instanceof JsonObject).map(v -> (JsonObject) v).toList();
        }
        if (value instanceof JsonObject single)
        {
            return List.of(single);
        }
        return List.of();
    }
}
