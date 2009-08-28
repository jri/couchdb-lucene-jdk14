package com.github.rnewson.couchdb.lucene;

import java.util.Date;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.NativeObject;

/**
 * Because Rhino, inexplicably, can't convert Javascript objects to a readable
 * string.
 *
 * @author rnewson
 *
 */
final class Conversion {

    /**
     * Converts any object returned by Rhino to an object with a suitable
     * toString() output.
     */
    static Object convert(final Object obj) {
        if (obj instanceof NativeObject)
            return convertObject((NativeObject) obj);
        else if (obj instanceof NativeArray)
            return convertArray((NativeArray) obj);

        try {
            return Context.jsToJava(obj, Date.class);
        } catch (final EvaluatorException e) {
            // not a date (this sucks).
        }

        return obj;
    }

    private static Object convertObject(final NativeObject obj) {
        final JSONObject result = new JSONObject();

        Object[] objs = obj.getIds();
        for (int i = 0; i < objs.length; i++) {
            final Object id = objs[i];
            String key;
            Object value;
            if (id instanceof String) {
                key = (String) id;
                value = obj.get(key, obj);
            } else if (id instanceof Integer) {
                key = id.toString();
                value = obj.get(((Integer) id).intValue(), obj);
            } else {
                throw new IllegalArgumentException();
            }
            if (value instanceof NativeObject)
                value = convertObject((NativeObject) value);
            if (value instanceof NativeArray)
                value = convertArray((NativeArray) value);
            result.put(key, value);
        }

        return result;
    }

    private static Object convertArray(final NativeArray arr) {
        final int len = (int) arr.getLength();
        final JSONArray result = new JSONArray();

        for (int i = 0; i < len; i++) {
            Object value = arr.get(i, null);
            if (value instanceof NativeObject)
                value = convertObject((NativeObject) value);
            if (value instanceof NativeArray)
                value = convertArray((NativeArray) value);

            result.add(value);
        }
        return result;
    }
}
