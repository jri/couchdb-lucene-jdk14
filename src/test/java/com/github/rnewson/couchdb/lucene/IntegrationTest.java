package com.github.rnewson.couchdb.lucene;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import net.sf.json.JSONObject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This test class requires a running couchdb instance on localhost port 5984.
 * 
 * @author rnewson
 * 
 */
public class IntegrationTest {

    private static final String base = "http://localhost:5984/";
    private static final String dbname = "lucenetestdb";

    private static final Database db = new Database(base);

    private static boolean enabled = false;

    static {
        try {
            // Is couchdb running?
            db.deleteDatabase(dbname);
            // Is couchdb-lucene hooked up?
            db.getDoc(dbname, "_fti");
            enabled = true;
        } catch (final IOException e) {
            // Ignored.
            System.out.println("couchdb is not running or couchdb-lucene is not configured, integration testing was skipped.");
        }
    }

    @Before
    public void setup() throws IOException, InterruptedException {
        if (!enabled)
            return;

        SECONDS.sleep(6);
        db.createDatabase(dbname);

        final String ddoc = "{\"fulltext\": {\"idx\": {\"index\":\"function(doc) {var ret=new Document(); ret.add(doc.content); return ret;}\"}}}";
        assertThat(db.saveDocument(dbname, "_design/lucene", ddoc), is(true));
    }

    @After
    public void teardown() throws IOException {
        if (!enabled)
            return;
        db.deleteDatabase(dbname);
    }

    @Test
    public void index() throws IOException, InterruptedException {
        if (!enabled)
            return;

        for (int i = 0; i < 50; i++) {
            assertThat(db.saveDocument(dbname, "doc-" + i, "{\"content\":\"hello\"}"), is(true));
        }

        SECONDS.sleep(10);

        final JSONObject indexState = db.getDoc(dbname, "_fti");
        assertThat(indexState.getInt("doc_count"), is(51));

        final JSONObject queryResult = db.getDoc(dbname, "/_fti/lucene/idx?q=hello");
        assertThat(queryResult.getInt("total_rows"), is(50));
    }

    @Test
    public void longIndex() throws IOException, InterruptedException {
        if (!enabled)
            return;

        for (int i = 0; i < 20; i++) {
            assertThat(db.saveDocument(dbname, "doc-" + i, "{\"content\":\"hello\"}"), is(true));
            MILLISECONDS.sleep(500);
        }

        SECONDS.sleep(6);

        final JSONObject indexState = db.getDoc(dbname, "_fti");
        assertThat(indexState.getInt("doc_count"), is(21));

        final JSONObject queryResult = db.getDoc(dbname, "/_fti/lucene/idx?q=hello");
        assertThat(queryResult.getInt("total_rows"), is(20));
    }

}
